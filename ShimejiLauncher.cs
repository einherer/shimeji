using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

class Program
{
    static string FindJavaExecutable(string baseDir)
    {
        // 1. Eingebettete oder portable JRE im Programmordner suchen
        string localJreW = Path.Combine(baseDir, "jre", "bin", "javaw.exe");
        if (File.Exists(localJreW)) return localJreW;
        string localJre = Path.Combine(baseDir, "jre", "bin", "java.exe");
        if (File.Exists(localJre)) return localJre;

        string localJavaW = Path.Combine(baseDir, "java", "bin", "javaw.exe");
        if (File.Exists(localJavaW)) return localJavaW;
        string localJava = Path.Combine(baseDir, "java", "bin", "java.exe");
        if (File.Exists(localJava)) return localJava;

        // 2. JAVA_HOME prüfen
        string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
        if (!string.IsNullOrEmpty(javaHome))
        {
            string pathW = Path.Combine(javaHome, "bin", "javaw.exe");
            if (File.Exists(pathW)) return pathW;
            string path = Path.Combine(javaHome, "bin", "java.exe");
            if (File.Exists(path)) return path;
        }

        // 3. üblichen Oracle-Java-Pfad prüfen
        string oracleW = @"C:\Program Files\Common Files\Oracle\Java\javapath\javaw.exe";
        if (File.Exists(oracleW)) return oracleW;
        string oracle = @"C:\Program Files\Common Files\Oracle\Java\javapath\java.exe";
        if (File.Exists(oracle)) return oracle;

        // 4. Umgebungsvariable PATH prüfen
        string pathEnv = Environment.GetEnvironmentVariable("PATH");
        if (!string.IsNullOrEmpty(pathEnv))
        {
            foreach (string p in pathEnv.Split(Path.PathSeparator))
            {
                try
                {
                    string fullPathW = Path.Combine(p.Trim(), "javaw.exe");
                    if (File.Exists(fullPathW)) return fullPathW;
                    string fullPath = Path.Combine(p.Trim(), "java.exe");
                    if (File.Exists(fullPath)) return fullPath;
                }
                catch { }
            }
        }

        return null;
    }

    [STAThread]
    static void Main()
    {
        string dir = AppDomain.CurrentDomain.BaseDirectory;
        string jarPath = Path.Combine(dir, "Shimeji.jar");

        if (!File.Exists(jarPath))
        {
            MessageBox.Show("Shimeji.jar wurde in diesem Ordner nicht gefunden:\n" + dir, "Shimeji-Fehler", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return;
        }

        string javaExe = FindJavaExecutable(dir);
        if (string.IsNullOrEmpty(javaExe))
        {
            MessageBox.Show("Java (JRE/JDK) wurde nicht gefunden!\nBitte Java 8 oder neuer installieren oder einen jre-Ordner in den Programmordner entpacken.", "Java-Laufzeitumgebung fehlt", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            return;
        }

        ProcessStartInfo psi = new ProcessStartInfo();
        psi.FileName = javaExe;
        psi.Arguments = "-Xmx512m -jar \"" + jarPath + "\"";
        psi.WorkingDirectory = dir;
        psi.UseShellExecute = true;
        psi.WindowStyle = ProcessWindowStyle.Hidden;

        try
        {
            Process.Start(psi);
        }
        catch (Exception ex)
        {
            MessageBox.Show("Fehler beim Starten von Shimeji: " + ex.Message, "Fehler beim Shimeji-Start", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }
}
