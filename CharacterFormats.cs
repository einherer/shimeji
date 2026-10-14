using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using System.Web.Script.Serialization;
using System.Xml;
using System.Xml.Linq;

// Read image references only; never evaluate expressions from downloaded animation files.
internal static class CharacterFormats
{
    private const int MaxMetadataBytes = 4 * 1024 * 1024;
    internal sealed class Group
    {
        internal string Label;
        internal Dictionary<string, CharacterImport.Frame> Frames;
    }

    private static string Reference(string folder, string value)
    {
        value = value.Replace('\\', '/');
        if (value.StartsWith("/") || value.Contains(":") || value.Split('/').Contains(".."))
            throw new InvalidDataException("Unsicherer Bild- oder Metadatenpfad: " + value);
        return string.Join("/", (folder + "/" + value).Split('/').Where(p => p.Length > 0 && p != "."));
    }

    private static string Read(CharacterImport.Frame frame)
    {
        using (var stream = frame.Open())
        using (var bytes = new MemoryStream())
        {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.Read(buffer, 0, buffer.Length)) > 0)
            {
                if (bytes.Length + count > MaxMetadataBytes)
                    throw new InvalidDataException("Die Animationsbeschreibung ist größer als 4 MiB.");
                bytes.Write(buffer, 0, count);
            }
            bytes.Position = 0;
            using (var reader = new StreamReader(bytes, System.Text.Encoding.UTF8, true)) return reader.ReadToEnd();
        }
    }

    private static object Parse(string data)
    {
        return new JavaScriptSerializer { MaxJsonLength = MaxMetadataBytes, RecursionLimit = 64 }.DeserializeObject(data);
    }
    private static Dictionary<string, object> Object(object value)
    {
        var result = value as Dictionary<string, object>;
        if (result == null) throw new InvalidDataException("Ungültiges JSON-Objekt in der Animationsbeschreibung.");
        return result;
    }
    private static object Value(Dictionary<string, object> obj, string key)
    {
        object value;
        return obj.TryGetValue(key, out value) ? value : null;
    }
    private static string Text(Dictionary<string, object> obj, string key, string fallback)
    {
        return Value(obj, key) as string ?? fallback;
    }
    private static int Number(object value)
    {
        if (!(value is int)) throw new InvalidDataException("Ungültige Ganzzahl in der Animationsbeschreibung.");
        return (int)value;
    }
    private static object[] Array(object value)
    {
        var result = value as object[];
        if (result == null) throw new InvalidDataException("Ungültige Animationsliste.");
        return result;
    }
    private static CharacterImport.Frame Find(Dictionary<string, CharacterImport.Frame> files, string path)
    {
        CharacterImport.Frame frame;
        if (!files.TryGetValue(path, out frame)) throw new InvalidDataException("Referenzierte Bild- oder Animationsdatei fehlt: " + path);
        return frame;
    }
    private static object[] Mapping()
    {
        using (var stream = typeof(CharacterFormats).Assembly.GetManifestResourceStream("Shimeji.CharacterFrames.json"))
        {
            if (stream == null) throw new InvalidDataException("Die interne Bewegungszuordnung fehlt. Bitte die Einstellungen neu erstellen.");
            using (var reader = new StreamReader(stream)) return Array(Parse(reader.ReadToEnd()));
        }
    }

    private static Dictionary<string, CharacterImport.Frame> XmlFrames(string folder, CharacterImport.Frame xml,
        Dictionary<string, CharacterImport.Frame> files, object[] mapping)
    {
        var settings = new XmlReaderSettings { DtdProcessing = DtdProcessing.Prohibit, XmlResolver = null, MaxCharactersInDocument = MaxMetadataBytes };
        XDocument document;
        using (var text = new StringReader(Read(xml)))
        using (var reader = XmlReader.Create(text, settings)) document = XDocument.Load(reader);
        var actions = new Dictionary<string, string[]>();
        foreach (var action in document.Descendants().Where(n => n.Name.LocalName == "Action" && n.Attribute("Name") != null))
        {
            var poses = action.Descendants().Where(n => n.Name.LocalName == "Pose" && n.Attribute("Image") != null)
                .Select(n => (string)n.Attribute("Image")).ToArray();
            if (poses.Length > 0) actions[(string)action.Attribute("Name")] = poses;
        }
        if (!new[] {"Stand", "Walk", "Falling"}.All(actions.ContainsKey))
            throw new InvalidDataException("Die XML-Datei enthält keine unterstützten Stand-, Walk- und Falling-Animationen.");
        var frames = new Dictionary<string, CharacterImport.Frame>();
        foreach (object item in mapping)
        {
            object[] row = Array(item);
            string action = (string)row[1];
            if (!actions.ContainsKey(action))
            {
                string alias = action == "SitAndSpinHeadAction" ? "HappyJumpAction" : action == "Divide1" ? "PullUpShimeji1" : "Stand";
                action = actions.ContainsKey(alias) ? alias : "Stand";
            }
            string[] poses = actions[action];
            string path = Reference(folder, poses[Math.Min(Number(row[2]), poses.Length - 1)].TrimStart('/'));
            if (!path.EndsWith(".png", StringComparison.OrdinalIgnoreCase)) throw new InvalidDataException("Die XML-Animation muss PNG-Bilder referenzieren.");
            frames["shime" + Number(row[0]) + ".png"] = Find(files, path);
        }
        return frames;
    }

    private static Group MobileFrames(string folder, CharacterImport.Frame entry, Dictionary<string, CharacterImport.Frame> files, object[] mapping)
    {
        var manifest = Object(Parse(Read(entry)));
        var schema = Object(Value(manifest, "animationSchema"));
        var sprites = Object(Value(manifest, "sprites"));
        if (!object.Equals(Value(manifest, "schemaVersion"), 1) || Text(schema, "schemaId", "") != "legacy_default_v1" ||
            !object.Equals(Value(schema, "version"), 1) || Text(sprites, "type", "") != "SEQUENCE")
            throw new InvalidDataException("Nicht unterstütztes Manifest-/Animationsformat (erwartet: legacy_default_v1, Version 1).");
        var animation = Object(Parse(Read(Find(files, Reference(folder, Text(schema, "path", "animation.json"))))));
        if (Text(animation, "schema_id", "") != "legacy_default_v1" || !object.Equals(Value(animation, "version"), 1))
            throw new InvalidDataException("Nicht unterstütztes JSON-Animationsschema.");
        var actions = new Dictionary<string, object[]>();
        foreach (object item in Array(Value(animation, "animations")))
        {
            var action = Object(item);
            object[] poses = Array(Value(action, "frames"));
            if (poses.Length > 0) actions[Text(action, "key", "")] = poses;
        }
        if (!new[] {"stand_left", "walk_left", "fall"}.All(actions.ContainsKey))
            throw new InvalidDataException("Stand-, Lauf- oder Fallanimation fehlt im JSON-Paket.");
        string pattern = Text(sprites, "filePattern", "");
        Match match = Regex.Match(pattern, @"^%0?([1-9]?\d?)d\.(webp|png)$");
        if (!match.Success) throw new InvalidDataException("Nicht unterstütztes Sprite-Dateimuster: " + pattern);
        int digits = match.Groups[1].Value.Length == 0 ? 0 : int.Parse(match.Groups[1].Value);
        int count = Number(Value(sprites, "spriteCount"));
        if (count < 1 || count > 10000) throw new InvalidDataException("Ungültige Sprite-Anzahl im Manifest.");
        string basePath = Reference(folder, Text(sprites, "basePath", "sprites/"));
        var frames = new Dictionary<string, CharacterImport.Frame>();
        foreach (object item in mapping)
        {
            object[] row = Array(item), poses;
            if (!actions.TryGetValue((string)row[3], out poses)) poses = actions["stand_left"];
            int sprite = Number(Value(Object(poses[Math.Min(Number(row[4]), poses.Length - 1)]), "sprite"));
            if (sprite < 0 || sprite >= count) throw new InvalidDataException("Ungültiger Sprite-Index in " + row[3]);
            string filename = sprite.ToString("D" + digits, System.Globalization.CultureInfo.InvariantCulture) + "." + match.Groups[2].Value;
            frames["shime" + Number(row[0]) + ".png"] = Find(files, Reference(basePath, filename));
        }
        return new Group { Label = Text(manifest, "name", System.IO.Path.GetFileName(folder)), Frames = frames };
    }

    internal static List<Group> Groups(List<CharacterImport.Frame> entries, string fallback)
    {
        var files = new Dictionary<string, CharacterImport.Frame>(StringComparer.OrdinalIgnoreCase);
        var groups = new SortedDictionary<string, Dictionary<string, CharacterImport.Frame>>(StringComparer.Ordinal);
        foreach (var frame in entries)
        {
            string path = Reference("", frame.Path);
            if (files.ContainsKey(path)) throw new InvalidDataException("Doppelte Datei im Paket: " + path);
            files[path] = frame;
            string name = System.IO.Path.GetFileName(path).ToLowerInvariant();
            if (!CharacterImport.FrameNames.Contains(name)) continue;
            string folder = path.Contains('/') ? path.Substring(0, path.LastIndexOf('/')) : "";
            if (!groups.ContainsKey(folder)) groups[folder] = new Dictionary<string, CharacterImport.Frame>();
            groups[folder][name] = frame;
        }
        foreach (string path in files.Keys)
        {
            if (!path.Equals("conf/actions.xml", StringComparison.OrdinalIgnoreCase) && !path.EndsWith("/conf/actions.xml", StringComparison.OrdinalIgnoreCase)) continue;
            string folder = path.Equals("conf/actions.xml", StringComparison.OrdinalIgnoreCase) ? "" : path.Substring(0, path.Length - "/conf/actions.xml".Length);
            if (files.Keys.Any(p => p.EndsWith(".png", StringComparison.OrdinalIgnoreCase) &&
                string.Equals(p.Contains('/') ? p.Substring(0, p.LastIndexOf('/')) : "", folder, StringComparison.OrdinalIgnoreCase)) && !groups.ContainsKey(folder))
                groups[folder] = new Dictionary<string, CharacterImport.Frame>();
        }
        object[] mapping = null;
        var result = new List<Group>();
        foreach (var group in groups)
        {
            var frames = group.Value;
            var missing = CharacterImport.FrameNames.Where(n => !frames.ContainsKey(n)).ToArray();
            if (missing.Length > 0)
            {
                CharacterImport.Frame xml;
                if (!files.TryGetValue(Reference(group.Key, "conf/actions.xml"), out xml))
                    throw new InvalidDataException("Unvollständige Figur in " + group.Key + ": Es fehlen " + string.Join(", ", missing));
                frames = XmlFrames(group.Key, xml, files, mapping ?? (mapping = Mapping()));
            }
            string folder = group.Key;
            string label = System.IO.Path.GetFileName(folder);
            if (label.Equals("img", StringComparison.OrdinalIgnoreCase) || label.Equals("images", StringComparison.OrdinalIgnoreCase))
                label = System.IO.Path.GetFileName(System.IO.Path.GetDirectoryName(folder) ?? "");
            result.Add(new Group { Label = label.Length == 0 ? fallback : label, Frames = frames });
        }
        foreach (var file in files)
            if (System.IO.Path.GetFileName(file.Key).Equals("manifest.json", StringComparison.OrdinalIgnoreCase))
            {
                string folder = file.Key.Contains('/') ? file.Key.Substring(0, file.Key.LastIndexOf('/')) : "";
                Group group = MobileFrames(folder, file.Value, files, mapping ?? (mapping = Mapping()));
                if (string.IsNullOrEmpty(group.Label)) group.Label = fallback;
                result.Add(group);
            }
        if (result.Count == 0) throw new InvalidDataException("Keine unterstützten Shimeji-Bilder gefunden. Erwartet werden Standard-PNGs, ein PNG/XML-Paket oder ein legacy_default_v1-Paket.");
        return result;
    }
}
