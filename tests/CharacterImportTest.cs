using System;
using System.IO;
using System.IO.Compression;
using System.Linq;

public static class CharacterImportTest
{
    private static readonly byte[] Png = Convert.FromBase64String("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=");
    private static void Check(bool condition, string message)
    {
        if (!condition) throw new Exception(message);
    }
    private static void Add(ZipArchive zip, string name, byte[] bytes)
    {
        using (var stream = zip.CreateEntry(name).Open()) stream.Write(bytes, 0, bytes.Length);
    }
    private static void Package(string path, string[] folders, string missing, string extra, bool invalid)
    {
        if (File.Exists(path)) File.Delete(path);
        using (var zip = ZipFile.Open(path, ZipArchiveMode.Create))
        {
            foreach (string folder in folders)
                foreach (string name in CharacterImport.FrameNames)
                    if (name != missing) Add(zip, (folder.Length == 0 ? "" : folder + "/") + name, invalid ? new byte[] {1, 2, 3} : Png);
            if (extra != null) Add(zip, extra, new byte[] {0});
        }
    }
    private static void Reject(string zip, string characters)
    {
        string[] before = Directory.Exists(characters) ? Directory.GetDirectories(characters) : new string[0];
        bool rejected = false;
        try { CharacterImport.Import(zip, characters); }
        catch (InvalidDataException) { rejected = true; }
        catch (System.Xml.XmlException) { rejected = true; }
        Check(rejected, "invalid package must be rejected");
        Check(before.SequenceEqual(Directory.Exists(characters) ? Directory.GetDirectories(characters) : new string[0]), "rejection leaves existing figures unchanged and removes staging");
    }
    private static void FormatCases(string root)
    {
        string zip = Path.Combine(root, "formats.zip"), characters = Path.Combine(root, "formats");
        using (var archive = ZipFile.Open(zip, ZipArchiveMode.Create))
        {
            foreach (string name in new[] {"Monika", "Natsuki", "Sayori", "Yuri"})
            {
                string folder = "img/" + name + "/";
                Add(archive, folder + "stand.png", Png);
                Add(archive, folder + "walk.png", Png.Concat(new byte[] {42}).ToArray());
                string xml = "<Mascot xmlns=\"http://www.group-finity.com/Mascot\"><ActionList>" +
                    "<Action Name=\"Stand\"><Animation><Pose Image=\"/stand.png\"/></Animation></Action>" +
                    "<Action Name=\"Walk\"><Animation><Pose Image=\"/stand.png\"/><Pose Image=\"/walk.png\"/></Animation></Action>" +
                    "<Action Name=\"Falling\"><Animation><Pose Image=\"/stand.png\"/></Animation></Action>" +
                    "</ActionList></Mascot>";
                Add(archive, folder + "conf/actions.xml", System.Text.Encoding.UTF8.GetBytes(xml));
            }
        }
        Check(CharacterImport.Import(zip, characters).SequenceEqual(new[] {"Monika", "Natsuki", "Sayori", "Yuri"}), "DDLC-style XML names detected without shime1.png");
        Check(File.ReadAllBytes(Path.Combine(characters, "Monika/shime1.png")).SequenceEqual(Png), "XML stand mapping");
        Check(File.ReadAllBytes(Path.Combine(characters, "Monika/shime2.png")).Last() == 42, "XML walk mapping");
        Check(File.ReadAllBytes(Path.Combine(characters, "Monika/shime46.png")).SequenceEqual(Png), "XML fallback for absent cloning action");
        using (var archive = ZipFile.Open(zip, ZipArchiveMode.Update))
        {
            archive.GetEntry("img/Yuri/conf/actions.xml").Delete();
            Add(archive, "img/Yuri/conf/actions.xml", System.Text.Encoding.UTF8.GetBytes("<!DOCTYPE Mascot [<!ENTITY test SYSTEM 'file:///private'>]><Mascot/>"));
        }
        Reject(zip, characters);
        File.Delete(zip);
        string manifest = "{\"schemaVersion\":1,\"name\":\"Megumin\",\"animationSchema\":{\"path\":\"animation.json\",\"schemaId\":\"legacy_default_v1\",\"version\":1},\"sprites\":{\"type\":\"SEQUENCE\",\"basePath\":\"sprites/\",\"filePattern\":\"%04d.webp\",\"spriteCount\":1}}";
        string animation = "{\"schema_id\":\"legacy_default_v1\",\"version\":1,\"animations\":[{\"key\":\"stand_left\",\"frames\":[{\"sprite\":0}]},{\"key\":\"walk_left\",\"frames\":[{\"sprite\":0}]},{\"key\":\"fall\",\"frames\":[{\"sprite\":0}]}]}";
        byte[] webp = Convert.FromBase64String("UklGRh4AAABXRUJQVlA4TBEAAAAvAAAAEAdQqqrUr4CBiOh/AAA=");
        using (var archive = ZipFile.Open(zip, ZipArchiveMode.Create))
        {
            Add(archive, "manifest.json", System.Text.Encoding.UTF8.GetBytes(manifest));
            Add(archive, "animation.json", System.Text.Encoding.UTF8.GetBytes(animation));
            Add(archive, "sprites/0000.webp", webp);
        }
        Check(CharacterImport.Import(zip, characters).SequenceEqual(new[] {"Megumin"}), "JSON manifest name detected");
        using (var image = new System.Drawing.Bitmap(Path.Combine(characters, "Megumin/shime1.png")))
        {
            Check(image.Width == 128 && image.Height == 128, "WebP resized to desktop canvas");
            Check(image.GetPixel(0, 0).A == 128, "WebP alpha preserved");
        }
        Check(Directory.GetFiles(Path.Combine(characters, "Megumin")).Length == 46, "WebP produces all standard frames");
        using (var archive = ZipFile.Open(zip, ZipArchiveMode.Update))
        {
            archive.GetEntry("sprites/0000.webp").Delete();
        }
        Reject(zip, characters);
        using (var archive = ZipFile.Open(zip, ZipArchiveMode.Update))
        {
            archive.GetEntry("manifest.json").Delete();
            Add(archive, "manifest.json", System.Text.Encoding.UTF8.GetBytes(manifest.Replace("legacy_default_v1", "unknown")));
        }
        Reject(zip, characters);
        using (var archive = ZipFile.Open(zip, ZipArchiveMode.Update))
        {
            archive.GetEntry("manifest.json").Delete();
            Add(archive, "manifest.json", System.Text.Encoding.UTF8.GetBytes(manifest.Replace("sprites/", "../outside/")));
        }
        Reject(zip, characters);
        Console.WriteLine("Windows formats: named PNG/XML, DTD rejection, JSON/WebP, alpha, resizing, missing images and unknown schemas passed.");
    }
    public static void Main(string[] args)
    {
        string root = Path.Combine(Path.GetTempPath(), "shimeji-import-test-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(root);
        try
        {
            string zip = Path.Combine(root, "Download.zip"), characters = Path.Combine(root, "characters");
            Package(zip, new[] {"Package/img/Kinger"}, null, "Package/Shimeji-ee.jar", false);
            Check(CharacterImport.Import(zip, characters).SequenceEqual(new[] {"Kinger"}), "nested sprite folder detected");
            Check(Directory.GetFiles(Path.Combine(characters, "Kinger")).Length == 46, "only required sprites copied");
            Check(File.ReadAllBytes(Path.Combine(characters, "Kinger/shime1.png")).SequenceEqual(Png), "sprite bytes preserved");
            Check(CharacterImport.Import(zip, characters).SequenceEqual(new[] {"Kinger (2)"}), "duplicates do not overwrite");
            Package(zip, new[] {"img/Ayaka", "img/Miku"}, null, null, false);
            Check(CharacterImport.Import(zip, characters).SequenceEqual(new[] {"Ayaka", "Miku"}), "multiple characters imported");
            Package(zip, new[] {""}, null, null, false);
            Check(CharacterImport.Import(zip, characters)[0] == "Download", "flat ZIP uses archive name");
            Package(zip, new[] {"Pack/img"}, null, null, false);
            Check(CharacterImport.Import(zip, characters)[0] == "Pack", "img uses enclosing package name");
            string folder = Path.Combine(root, "Unpacked/img/Other");
            Directory.CreateDirectory(folder);
            foreach (string name in CharacterImport.FrameNames) File.WriteAllBytes(Path.Combine(folder, name.ToUpperInvariant()), Png);
            Check(CharacterImport.Import(Path.Combine(root, "Unpacked"), characters)[0] == "Other", "folder import normalizes uppercase filenames");
            Package(zip, new[] {"img/Bad"}, "shime46.png", null, false);
            Reject(zip, characters);
            Package(zip, new[] {"img/Bad"}, null, null, true);
            Reject(zip, characters);
            Package(zip, new string[0], null, "android.png", false);
            Reject(zip, characters);
            foreach (string path in new[] {"../outside.txt", "/outside.txt", "C:/outside.txt", @"..\outside.txt", "img/Bad/SHIME1.PNG"})
            {
                Package(zip, new[] {"img/Bad"}, null, path, false);
                Reject(zip, characters);
            }
            Package(zip, new[] {"img/Good"}, null, null, false);
            using (var archive = ZipFile.Open(zip, ZipArchiveMode.Update))
                foreach (string name in CharacterImport.FrameNames) Add(archive, "img/Invalid/" + name, new byte[] {0});
            Reject(zip, characters);
            Package(zip, new[] {"img/Bad"}, null, null, false);
            using (var archive = ZipFile.Open(zip, ZipArchiveMode.Update))
                for (int i = 0; i < 10000; i++) Add(archive, "ignored/" + i, new byte[0]);
            Reject(zip, characters);
            FormatCases(root);
            int sampleIndex = 0;
            foreach (string example in args)
            {
                string sample = Path.Combine(root, "sample" + (sampleIndex++));
                string[] names = CharacterImport.Import(example, sample);
                Check(names.Length > 0 && names.All(n => CharacterImport.IsCharacterDirectory(Path.Combine(sample, n))), "real shimeji.org ZIP imports");
                Console.WriteLine("Real shimeji.org ZIP: " + string.Join(", ", names));
            }
            Console.WriteLine("Windows import: nested/flat ZIPs, folders, multiple sets, collisions, invalid packages, unsafe paths, limits and rollback passed.");
        }
        finally { Directory.Delete(root, true); }
    }
}
