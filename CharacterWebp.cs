using System;
using System.Diagnostics;
using System.IO;

internal static class CharacterWebp
{
    private static uint Little(byte[] data, int offset, int length)
    {
        uint value = 0;
        for (int i = 0; i < length; i++) value |= (uint)data[offset + i] << (8 * i);
        return value;
    }
    private static string Tag(byte[] data, int offset)
    {
        return System.Text.Encoding.ASCII.GetString(data, offset, 4);
    }
    private static void CheckDimensions(byte[] data)
    {
        if (data.Length < 20 || Tag(data, 0) != "RIFF" || Tag(data, 8) != "WEBP")
            throw new InvalidDataException("Ungültige WebP-Bilddatei.");
        long offset = 12;
        while (offset + 8 <= data.Length)
        {
            int start = (int)offset + 8;
            uint size = Little(data, (int)offset + 4, 4), width = 0, height = 0;
            if (start + (long)size > data.Length) break;
            string kind = Tag(data, (int)offset);
            if (kind == "VP8X" && size >= 10)
            {
                if ((data[start] & 2) != 0) throw new InvalidDataException("Animierte WebP-Dateien werden nicht unterstützt.");
                width = Little(data, start + 4, 3) + 1; height = Little(data, start + 7, 3) + 1;
            }
            else if (kind == "VP8L" && size >= 5 && data[start] == 0x2f)
            {
                uint bits = Little(data, start + 1, 4);
                width = (bits & 0x3fff) + 1; height = ((bits >> 14) & 0x3fff) + 1;
            }
            else if (kind == "VP8 " && size >= 10 && data[start + 3] == 0x9d && data[start + 4] == 1 && data[start + 5] == 0x2a)
            {
                width = Little(data, start + 6, 2) & 0x3fff; height = Little(data, start + 8, 2) & 0x3fff;
            }
            if (width > 0 && height > 0)
            {
                if (width > 4096 || height > 4096) throw new InvalidDataException("Die Bildgröße muss zwischen 1 und 4096 Pixeln liegen.");
                return;
            }
            offset = start + (long)size + (size & 1);
        }
        throw new InvalidDataException("WebP-Bildgröße konnte nicht gelesen werden.");
    }

    internal static byte[] Convert(byte[] data, string target, string decoder)
    {
        CheckDimensions(data);
        if (!File.Exists(decoder))
        {
            using (var resource = typeof(CharacterWebp).Assembly.GetManifestResourceStream("Shimeji.dwebp.exe"))
            {
                if (resource == null) throw new InvalidDataException("Der integrierte WebP-Decoder fehlt. Bitte die Einstellungen neu erstellen.");
                using (var file = new FileStream(decoder, FileMode.CreateNew, FileAccess.Write)) resource.CopyTo(file);
            }
        }
        string source = Path.ChangeExtension(target, ".webp");
        File.WriteAllBytes(source, data);
        try
        {
            var info = new ProcessStartInfo(decoder,
                "-quiet -resize 128 128 -o \"" + target + "\" -- \"" + source + "\"")
            {
                UseShellExecute = false, CreateNoWindow = true, WindowStyle = ProcessWindowStyle.Hidden,
                RedirectStandardOutput = true, RedirectStandardError = true
            };
            using (var process = Process.Start(info))
            {
                var output = process.StandardOutput.ReadToEndAsync();
                var error = process.StandardError.ReadToEndAsync();
                if (!process.WaitForExit(15000))
                {
                    process.Kill();
                    process.WaitForExit();
                    throw new InvalidDataException("Zeitüberschreitung beim Konvertieren eines WebP-Bildes.");
                }
                output.Wait(); error.Wait();
                if (process.ExitCode != 0 || !File.Exists(target))
                    throw new InvalidDataException("WebP-Bild konnte nicht konvertiert werden: " + Path.GetFileName(target));
            }
            return File.ReadAllBytes(target);
        }
        finally { File.Delete(source); }
    }
}
