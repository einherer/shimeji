using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text.RegularExpressions;

// Read sprite files only. Downloaded launchers, JARs and behavior scripts are never extracted.
public static class CharacterImport
{
    private const int MaxEntries = 10000;
    private const int MaxFrameBytes = 16 * 1024 * 1024;
    private const long MaxTotalBytes = 256L * 1024 * 1024;
    public static readonly string[] FrameNames = Enumerable.Range(1, 46).Select(i => "shime" + i + ".png").ToArray();

    internal sealed class Frame
    {
        public string Path;
        public Func<Stream> Open;
    }

    public static bool IsCharacterDirectory(string path)
    {
        return FrameNames.All(name => File.Exists(System.IO.Path.Combine(path, name)));
    }

    private static string SafeName(string name)
    {
        name = Regex.Replace(name, "[<>:\"/\\\\|?*\\x00-\\x1f]", "_").Trim(' ', '.');
        if (name.Length > 100) name = name.Substring(0, 100).Trim(' ', '.');
        if (name.Length == 0 || Regex.IsMatch(name, @"^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\.|$)", RegexOptions.IgnoreCase))
            return "Shimeji";
        return name;
    }

    private static IEnumerable<Frame> FolderFrames(string root)
    {
        var pending = new Stack<string>();
        pending.Push(root);
        int count = 0;
        while (pending.Count > 0)
        {
            string directory = pending.Pop();
            foreach (string child in Directory.GetDirectories(directory).OrderBy(p => p, StringComparer.Ordinal))
                if ((File.GetAttributes(child) & FileAttributes.ReparsePoint) == 0) pending.Push(child);
            foreach (string path in Directory.GetFiles(directory).OrderBy(p => p, StringComparer.Ordinal))
            {
                if ((File.GetAttributes(path) & FileAttributes.ReparsePoint) != 0) continue;
                if (++count > MaxEntries) throw new InvalidDataException("Das Paket enthält zu viele Dateien (maximal 10000).");
                string file = path;
                yield return new Frame { Path = path.Substring(root.Length).TrimStart('\\', '/').Replace('\\', '/'), Open = () => File.OpenRead(file) };
            }
        }
    }

    public static string[] Import(string source, string charactersDir)
    {
        source = System.IO.Path.GetFullPath(source);
        charactersDir = System.IO.Path.GetFullPath(charactersDir);
        using (var archive = Directory.Exists(source) ? null : OpenZip(source))
        {
            var entries = new List<Frame>();
            if (archive == null) entries.AddRange(FolderFrames(source.TrimEnd('\\', '/')));
            else
            {
                if (archive.Entries.Count > MaxEntries) throw new InvalidDataException("Das Paket enthält zu viele Dateien (maximal 10000).");
                foreach (var entry in archive.Entries)
                {
                    string path = entry.FullName.Replace('\\', '/');
                    if (path.StartsWith("/") || path.Contains(":") || path.Split('/').Contains(".."))
                        throw new InvalidDataException("Unsicherer Dateipfad im ZIP-Paket: " + path);
                    if (!path.EndsWith("/"))
                    {
                        var file = entry;
                        entries.Add(new Frame { Path = path, Open = () => file.Open() });
                    }
                }
            }
            string fallback = archive == null ? System.IO.Path.GetFileName(source.TrimEnd('\\', '/')) : System.IO.Path.GetFileNameWithoutExtension(source);
            var groups = CharacterFormats.Groups(entries, fallback);

            Directory.CreateDirectory(charactersDir);
            string staging = System.IO.Path.Combine(charactersDir, ".import-" + Guid.NewGuid().ToString("N"));
            Directory.CreateDirectory(staging);
            var installed = new List<string>();
            try
            {
                long total = 0;
                int index = 0;
                foreach (var group in groups)
                {
                    string target = System.IO.Path.Combine(staging, (index++).ToString());
                    Directory.CreateDirectory(target);
                    var copied = new Dictionary<Frame, string>();
                    foreach (string name in FrameNames)
                    {
                        Frame frame = group.Frames[name];
                        string destination = System.IO.Path.Combine(target, name), previous;
                        if (copied.TryGetValue(frame, out previous))
                        {
                            File.Copy(previous, destination);
                            total += new FileInfo(destination).Length;
                        }
                        else
                        {
                            using (Stream stream = frame.Open())
                                total += CopyFrame(stream, destination, System.IO.Path.Combine(staging, "dwebp.exe"));
                            copied[frame] = destination;
                        }
                        if (total > MaxTotalBytes) throw new InvalidDataException("Die Figurenbilder sind zusammen größer als 256 MiB.");
                    }
                }
                var existing = new HashSet<string>(Directory.GetFileSystemEntries(charactersDir).Select(System.IO.Path.GetFileName), StringComparer.OrdinalIgnoreCase);
                index = 0;
                foreach (var group in groups)
                {
                    string label = SafeName(group.Label);
                    string name = label;
                    int suffix = 2;
                    while (existing.Contains(name)) name = label + " (" + (suffix++) + ")";
                    Directory.Move(System.IO.Path.Combine(staging, (index++).ToString()), System.IO.Path.Combine(charactersDir, name));
                    installed.Add(name);
                    existing.Add(name);
                }
                return installed.ToArray();
            }
            catch
            {
                foreach (string name in installed) Directory.Delete(System.IO.Path.Combine(charactersDir, name), true);
                throw;
            }
            finally { Directory.Delete(staging, true); }
        }
    }

    private static ZipArchive OpenZip(string source)
    {
        if (!System.IO.Path.GetExtension(source).Equals(".zip", StringComparison.OrdinalIgnoreCase))
            throw new InvalidDataException("Bitte ein ZIP-Paket oder einen entpackten Figurenordner auswählen.");
        return ZipFile.OpenRead(source);
    }

    private static long CopyFrame(Stream stream, string target, string decoder)
    {
        using (var data = new MemoryStream())
        {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.Read(buffer, 0, buffer.Length)) > 0)
            {
                if (data.Length + read > MaxFrameBytes) throw new InvalidDataException("Eine Bilddatei ist größer als 16 MiB.");
                data.Write(buffer, 0, read);
            }
            byte[] png = data.ToArray();
            if (png.Length >= 4 && png[0] == 82 && png[1] == 73 && png[2] == 70 && png[3] == 70)
                png = CharacterWebp.Convert(png, target, decoder);
            byte[] signature = {137, 80, 78, 71, 13, 10, 26, 10};
            if (png.Length < 33 || !png.Take(8).SequenceEqual(signature) || png[12] != 73 || png[13] != 72 || png[14] != 68 || png[15] != 82)
                throw new InvalidDataException("Ungültige PNG-Bilddatei: " + System.IO.Path.GetFileName(target));
            uint width = BigEndian(png, 16), height = BigEndian(png, 20);
            if (width < 1 || width > 4096 || height < 1 || height > 4096)
                throw new InvalidDataException("Die Bildgröße muss zwischen 1 und 4096 Pixeln liegen.");
            File.WriteAllBytes(target, png);
            return png.Length;
        }
    }

    private static uint BigEndian(byte[] data, int offset)
    {
        return ((uint)data[offset] << 24) | ((uint)data[offset + 1] << 16) | ((uint)data[offset + 2] << 8) | data[offset + 3];
    }
}
