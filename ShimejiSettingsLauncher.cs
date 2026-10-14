using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Threading;
using System.Windows.Forms;

public class SettingsForm : Form
{
    private ComboBox comboCharacter;
    private CheckBox checkSelfClone;
    private TextBox txtX, txtY, txtW, txtH;
    private TextBox txtTitles;
    private Button btnSave, btnApplyChar, btnApplyRestart, btnResetWindow, btnRestart, btnOpenFolder, btnClose;

    private static readonly string AppRoot = AppDomain.CurrentDomain.BaseDirectory;
    private static readonly string WindowConf = Path.Combine(AppRoot, "window.conf");
    private static readonly string TitlesConf = Path.Combine(AppRoot, "titles.conf");
    private static readonly string SettingsProps = Path.Combine(AppRoot, "settings.properties");
    private static readonly string CharactersDir = Path.Combine(AppRoot, "characters");
    private static readonly string ImgDir = Path.Combine(AppRoot, "img");
    private static readonly string CurrentCharFile = Path.Combine(AppRoot, ".current_character");

    private const string HeaderText = "Fensterversatz in den folgenden Zeilen angeben: x, y, Breitenzuschlag, Höhenzuschlag. Leere Angaben entsprechen 0.";

    public SettingsForm()
    {
        InitializeComponent();
        LoadData();
    }

    private void InitializeComponent()
    {
        this.Text = "Shimeji-Einstellungen";
        this.Size = new Size(760, 650);
        this.MinimumSize = new Size(720, 600);
        this.StartPosition = FormStartPosition.CenterScreen;
        this.Font = new Font("Segoe UI", 9.5f);
        this.Padding = new Padding(12);

        var mainLayout = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            ColumnCount = 1,
            RowCount = 6,
            AutoScroll = true
        };
        mainLayout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        mainLayout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        mainLayout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        mainLayout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100f));
        mainLayout.RowStyles.Add(new RowStyle(SizeType.AutoSize));

        // Überschrift
        var lblTitle = new Label
        {
            Text = "Shimeji-Einstellungen",
            Font = new Font("Segoe UI", 14f, FontStyle.Bold),
            AutoSize = true,
            Margin = new Padding(0, 0, 0, 4)
        };
        var lblSub = new Label
        {
            Text = "Figur, Fensterposition, Fenstertitel und Selbstvermehrung einstellen.",
            AutoSize = true,
            Margin = new Padding(0, 0, 0, 10),
            ForeColor = Color.DimGray
        };

        var headerFlow = new FlowLayoutPanel { AutoSize = true, FlowDirection = FlowDirection.TopDown, Dock = DockStyle.Fill };
        headerFlow.Controls.Add(lblTitle);
        headerFlow.Controls.Add(lblSub);
        mainLayout.Controls.Add(headerFlow, 0, 0);

        // Gruppe 1: Figur
        var grpChar = new GroupBox { Text = "Figur", Dock = DockStyle.Fill, Padding = new Padding(10), AutoSize = true, Margin = new Padding(0, 0, 0, 10) };
        var charPanel = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 3, AutoSize = true };
        charPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
        charPanel.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f));
        charPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));

        var lblChar = new Label { Text = "Aktuelle Figur:", AutoSize = true, Anchor = AnchorStyles.Left, Margin = new Padding(0, 0, 10, 0) };
        comboCharacter = new ComboBox { DropDownStyle = ComboBoxStyle.DropDownList, Dock = DockStyle.Fill };
        btnApplyChar = new Button { Text = "Figur übernehmen", AutoSize = true, Padding = new Padding(6, 2, 6, 2) };
        btnApplyChar.Click += (s, e) => ApplyCharacterOnly();

        charPanel.Controls.Add(lblChar, 0, 0);
        charPanel.Controls.Add(comboCharacter, 1, 0);
        charPanel.Controls.Add(btnApplyChar, 2, 0);
        grpChar.Controls.Add(charPanel);
        mainLayout.Controls.Add(grpChar, 0, 1);

        // Gruppe 2: Verhalten
        var grpBehavior = new GroupBox { Text = "Verhalten", Dock = DockStyle.Fill, Padding = new Padding(10), AutoSize = true, Margin = new Padding(0, 0, 0, 10) };
        checkSelfClone = new CheckBox { Text = "Selbstvermehrung erlauben", AutoSize = true, Checked = true, Dock = DockStyle.Top };
        grpBehavior.Controls.Add(checkSelfClone);
        mainLayout.Controls.Add(grpBehavior, 0, 2);

        // Gruppe 3: window.conf
        var grpWindow = new GroupBox { Text = "window.conf", Dock = DockStyle.Fill, Padding = new Padding(10), AutoSize = true, Margin = new Padding(0, 0, 0, 10) };
        var winGrid = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 4, RowCount = 2, AutoSize = true };
        winGrid.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
        winGrid.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50f));
        winGrid.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
        winGrid.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50f));

        txtX = new TextBox { Text = "0", Width = 80 };
        txtY = new TextBox { Text = "0", Width = 80 };
        txtW = new TextBox { Text = "0", Width = 80 };
        txtH = new TextBox { Text = "0", Width = 80 };

        winGrid.Controls.Add(new Label { Text = "X-Versatz:", AutoSize = true, Anchor = AnchorStyles.Left }, 0, 0);
        winGrid.Controls.Add(txtX, 1, 0);
        winGrid.Controls.Add(new Label { Text = "Y-Versatz:", AutoSize = true, Anchor = AnchorStyles.Left }, 2, 0);
        winGrid.Controls.Add(txtY, 3, 0);

        winGrid.Controls.Add(new Label { Text = "Breitenzuschlag:", AutoSize = true, Anchor = AnchorStyles.Left }, 0, 1);
        winGrid.Controls.Add(txtW, 1, 1);
        winGrid.Controls.Add(new Label { Text = "Höhenzuschlag:", AutoSize = true, Anchor = AnchorStyles.Left }, 2, 1);
        winGrid.Controls.Add(txtH, 3, 1);

        grpWindow.Controls.Add(winGrid);
        mainLayout.Controls.Add(grpWindow, 0, 3);

        // Gruppe 4: titles.conf
        var grpTitles = new GroupBox { Text = "titles.conf (ein Fenstertitel je Zeile; leer = alle Fenster)", Dock = DockStyle.Fill, Padding = new Padding(10), Margin = new Padding(0, 0, 0, 10) };
        txtTitles = new TextBox { Multiline = true, ScrollBars = ScrollBars.Vertical, Dock = DockStyle.Fill };
        grpTitles.Controls.Add(txtTitles);
        mainLayout.Controls.Add(grpTitles, 0, 4);

        // Schaltflächen am unteren Rand
        var btnFlow = new FlowLayoutPanel { Dock = DockStyle.Fill, AutoSize = true, FlowDirection = FlowDirection.LeftToRight };
        btnSave = new Button { Text = "Speichern", AutoSize = true, Padding = new Padding(8, 4, 8, 4) };
        btnApplyRestart = new Button { Text = "Übernehmen + Neustart", AutoSize = true, Padding = new Padding(8, 4, 8, 4) };
        btnResetWindow = new Button { Text = "window.conf zurücksetzen", AutoSize = true, Padding = new Padding(8, 4, 8, 4) };
        btnRestart = new Button { Text = "Shimeji neu starten", AutoSize = true, Padding = new Padding(8, 4, 8, 4) };
        btnOpenFolder = new Button { Text = "Programmordner öffnen", AutoSize = true, Padding = new Padding(8, 4, 8, 4) };
        btnClose = new Button { Text = "Schließen", AutoSize = true, Padding = new Padding(8, 4, 8, 4) };

        btnSave.Click += (s, e) => SaveConfig(true);
        btnApplyRestart.Click += (s, e) => { if (SaveConfig(false) && ApplyCharacterOnly()) RestartShimeji(); };
        btnResetWindow.Click += (s, e) => { txtX.Text = "0"; txtY.Text = "0"; txtW.Text = "0"; txtH.Text = "0"; };
        btnRestart.Click += (s, e) => RestartShimeji();
        btnOpenFolder.Click += (s, e) => Process.Start("explorer.exe", AppRoot);
        btnClose.Click += (s, e) => this.Close();

        btnFlow.Controls.AddRange(new Control[] { btnSave, btnApplyRestart, btnResetWindow, btnRestart, btnOpenFolder, btnClose });
        mainLayout.Controls.Add(btnFlow, 0, 5);

        this.Controls.Add(mainLayout);
    }

    private void LoadData()
    {
        // Figurenliste füllen
        if (Directory.Exists(CharactersDir))
        {
            var chars = Directory.GetDirectories(CharactersDir)
                .Where(d => File.Exists(Path.Combine(d, "shime1.png")))
                .Select(Path.GetFileName)
                .OrderBy(n => n)
                .ToArray();
            comboCharacter.Items.AddRange(chars);

            string current = "Ayaka";
            if (File.Exists(CurrentCharFile))
            {
                string val = File.ReadAllText(CurrentCharFile).Trim();
                if (!string.IsNullOrEmpty(val)) current = val;
            }
            if (comboCharacter.Items.Contains(current))
                comboCharacter.SelectedItem = current;
            else if (comboCharacter.Items.Count > 0)
                comboCharacter.SelectedIndex = 0;
        }

        // settings.properties laden
        if (File.Exists(SettingsProps))
        {
            foreach (var line in File.ReadAllLines(SettingsProps))
            {
                string trimmed = line.Trim();
                if (trimmed.StartsWith("selfCloningEnabled=", StringComparison.OrdinalIgnoreCase))
                {
                    string val = trimmed.Substring("selfCloningEnabled=".Length).Trim();
                    checkSelfClone.Checked = val.Equals("true", StringComparison.OrdinalIgnoreCase);
                }
            }
        }

        // window.conf laden
        if (File.Exists(WindowConf))
        {
            var lines = File.ReadAllLines(WindowConf).Where(l => !string.IsNullOrWhiteSpace(l)).Skip(1).ToArray();
            if (lines.Length > 0) txtX.Text = lines[0].Trim();
            if (lines.Length > 1) txtY.Text = lines[1].Trim();
            if (lines.Length > 2) txtW.Text = lines[2].Trim();
            if (lines.Length > 3) txtH.Text = lines[3].Trim();
        }

        // titles.conf laden
        if (File.Exists(TitlesConf))
        {
            txtTitles.Text = File.ReadAllText(TitlesConf);
        }
    }

    private bool SaveConfig(bool showMessage)
    {
        int x, y, w, h;
        if (!int.TryParse(txtX.Text.Trim(), out x) || !int.TryParse(txtY.Text.Trim(), out y) ||
            !int.TryParse(txtW.Text.Trim(), out w) || !int.TryParse(txtH.Text.Trim(), out h))
        {
            MessageBox.Show("Die vier Werte für window.conf müssen ganze Zahlen sein.", "Eingabefehler", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return false;
        }

        File.WriteAllText(WindowConf, HeaderText + "\n" + x + "\n" + y + "\n" + w + "\n" + h + "\n");
        File.WriteAllText(TitlesConf, txtTitles.Text);
        File.WriteAllText(SettingsProps, "selfCloningEnabled=" + (checkSelfClone.Checked ? "true" : "false") + "\n");

        if (showMessage)
            MessageBox.Show("Einstellungen wurden gespeichert.", "Hinweis", MessageBoxButtons.OK, MessageBoxIcon.Information);
        return true;
    }

    private bool ApplyCharacterOnly()
    {
        string selected = comboCharacter.SelectedItem as string;
        if (string.IsNullOrEmpty(selected))
        {
            MessageBox.Show("Bitte eine Figur auswählen.", "Fehler", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            return false;
        }

        string srcDir = Path.Combine(CharactersDir, selected);
        if (!Directory.Exists(srcDir))
        {
            MessageBox.Show("Figurenordner nicht gefunden: " + selected, "Fehler", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return false;
        }

        Directory.CreateDirectory(ImgDir);
        for (int i = 1; i <= 46; i++)
        {
            string srcFile = Path.Combine(srcDir, "shime" + i + ".png");
            string dstFile = Path.Combine(ImgDir, "shime" + i + ".png");
            if (File.Exists(srcFile))
            {
                File.Copy(srcFile, dstFile, true);
            }
        }

        File.WriteAllText(CurrentCharFile, selected + "\n");
        return true;
    }

    private void RestartShimeji()
    {
        try
        {
            // Laufende Shimeji-Prozesse beenden
            var psCmd = "Get-WmiObject Win32_Process | Where-Object { $_.CommandLine -like '*Shimeji.jar*' } | ForEach-Object { $_.Terminate() }";
            var psiKill = new ProcessStartInfo("powershell", "-Command \"" + psCmd + "\"") { CreateNoWindow = true, UseShellExecute = false };
            var pKill = Process.Start(psiKill);
            if (pKill != null) pKill.WaitForExit();

            Thread.Sleep(500);

            // Shimeji.exe oder launch.bat starten
            string exePath = Path.Combine(AppRoot, "Shimeji.exe");
            string batPath = Path.Combine(AppRoot, "launch.bat");
            if (File.Exists(exePath))
            {
                Process.Start(new ProcessStartInfo(exePath) { WorkingDirectory = AppRoot });
            }
            else if (File.Exists(batPath))
            {
                Process.Start(new ProcessStartInfo(batPath) { WorkingDirectory = AppRoot, WindowStyle = ProcessWindowStyle.Hidden });
            }

            MessageBox.Show("Shimeji wurde neu gestartet.", "Hinweis", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }
        catch (Exception ex)
        {
            MessageBox.Show("Fehler beim Neustart von Shimeji: " + ex.Message, "Fehler", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }

    [STAThread]
    public static void Main()
    {
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        Application.Run(new SettingsForm());
    }
}
