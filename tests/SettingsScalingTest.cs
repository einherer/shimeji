using System;
using System.IO;
using System.Reflection;
using System.Windows.Forms;

public class SettingsScalingTest
{
    private static T Field<T>(SettingsForm form, string name)
    {
        return (T)typeof(SettingsForm).GetField(name, BindingFlags.Instance | BindingFlags.NonPublic).GetValue(form);
    }

    private static void Check(bool condition, string message)
    {
        if (!condition) throw new Exception(message);
    }

    [STAThread]
    public static void Main()
    {
        string props = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "settings.properties");
        File.WriteAllText(props, "selfCloningEnabled=false\nother=kept\n");
        using (var form = new SettingsForm())
        {
            var numeric = Field<NumericUpDown>(form, "numericScale");
            var slider = Field<TrackBar>(form, "trackScale");
            Check(numeric.Value == 100 && numeric.Minimum == 25 && numeric.Maximum == 200, "defaults and limits");
            foreach (int value in new[] {25, 137, 200})
            {
                numeric.Value = value;
                Check(slider.Value == value, "numeric synchronizes slider");
                var saved = typeof(SettingsForm).GetMethod("SaveConfig", BindingFlags.Instance | BindingFlags.NonPublic).Invoke(form, new object[] {false});
                Check((bool)saved, "save succeeds");
                string text = File.ReadAllText(props);
                Check(text.Contains("scalePercent=" + value), "percentage saved");
                Check(text.Contains("other=kept") && text.Contains("selfCloningEnabled=false"), "other settings preserved");
                Check(File.ReadAllBytes(props)[0] != 0xef, "no BOM for Java properties");
            }
            slider.Value = 75;
            Check(numeric.Value == 75, "slider synchronizes numeric");
            numeric.Text = "123";
            typeof(SettingsForm).GetMethod("SaveConfig", BindingFlags.Instance | BindingFlags.NonPublic).Invoke(form, new object[] {false});
            Check(File.ReadAllText(props).Contains("scalePercent=123"), "typed value saved");
        }
        using (var form = new SettingsForm())
            Check(Field<NumericUpDown>(form, "numericScale").Value == 123, "percentage restored");
        Check(Directory.GetFiles(AppDomain.CurrentDomain.BaseDirectory, "*.tmp").Length == 0, "atomic save cleaned up");
        Console.WriteLine("Windows settings: limits, slider, typed value, persistence and atomic save passed.");
    }
}
