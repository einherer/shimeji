# shimeji

<p align="center">
  <img src="docs/images/repo-banner-de.png" alt="Shimeji-Banner" width="100%" />
</p>

<p align="center">
  <img src="docs/images/linux-shimeji-icon.png" alt="Shimeji-Symbol" width="64" height="64" />
</p>

<p align="center">
  <a href="https://github.com/ngoc-thu/shimeji/releases/latest">⬇ Neueste Version herunterladen</a>
</p>

Eine moderne, praktische Anwendung mit Desktop-Maskottchen (Shimeji) für **Windows 10/11 und Linux**. Sie bietet eine native Windows-API-Anbindung, eigenständige `.exe`-Startprogramme, Korrekturen für die DPI-Skalierung und eine integrierte Einstellungsoberfläche.

---

## ✨ Funktionen

- **Eigenständige Windows-Programme**:
  - `Shimeji.exe`: Startprogramm mit Maskottchensymbol; startet ohne schwarzes Konsolenfenster.
  - `ShimejiSettings.exe`: Eigenständiges Programm für die Einstellungen.
- **Native Windows-API-Anbindung**:
  - Erfassung der Fenster, Erkennung des aktiven Fensters und Kollisionserkennung an der Taskleiste über JNA (`User32` / `GDI32`).
- **Korrekturen für hohe DPI-Skalierung und Bewegungsphysik**:
  - Behebt fehlerhafte Bodenkollisionen und endloses Fallen bei skalierten Windows-Anzeigen (100 %, 125 %, 150 %, 200 %).
  - Eine Toleranz von ±2 Pixeln bei Boden- und Wandgrenzen ermöglicht sanftes Landen.
- **Integrierte Einstellungsoberfläche**:
  - `ShimejiSettings.exe` (native Windows-Oberfläche) / `shimeji_settings.py` (Python mit Tkinter).
  - Figurenskalierung von **25 % bis 200 %** per Schieberegler oder Prozentfeld; **Speichern** passt alle laufenden Figuren ohne Neustart an (100 % = Originalgröße).
  - Einfacher Figurenwechsel, Bearbeitung des Fensterversatzes in `window.conf`, Fensterfilter in `titles.conf` und Schalter für die Selbstvermehrung.
- **Mitgelieferte Figuren**:
  - **Ayaka** und **Hatsune Miku**.
- **Plattformübergreifende Unterstützung**:
  - Unterstützung für **Windows 10/11** und **Ubuntu / Linux mit X11**.

---

## 🎭 Mitgelieferte Figuren

<table>
  <tr>
    <td align="center"><strong>Ayaka</strong></td>
    <td align="center"><strong>Hatsune Miku</strong></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/images/ayaka-preview.png" alt="Vorschau von Ayaka" width="128" /></td>
    <td align="center"><img src="docs/images/miku-preview.png" alt="Vorschau von Hatsune Miku" width="128" /></td>
  </tr>
</table>

---

## 🚀 Schnellstart

### Unter Windows
1. Stelle sicher, dass **Java 8 oder neuer** (empfohlen: JDK 19 oder 21) und **Python 3.x** installiert sind.
2. **Maskottchen starten**: Doppelklick auf `Shimeji.exe` oder `launch.bat`.
3. **Einstellungen öffnen**: Doppelklick auf `ShimejiSettings.exe` oder `run-settings.bat`.

### Unter Linux
```bash
# Maskottchen starten
./launch.sh

# Einstellungsoberfläche starten
./run-settings.sh
```

---

## 🛠️ Aus dem Quelltext erstellen

### Unter Windows
Führe `build.bat` direkt in der Eingabeaufforderung oder in PowerShell aus. Eine Ant-Installation ist nicht erforderlich:

```cmd
build.bat
```

Dabei werden alle Java-Quellen (`src`, `src_generic`, `src_x11`, `src_win`) kompiliert und in `Shimeji.jar` gepackt. Anschließend werden `Shimeji.exe` und `ShimejiSettings.exe` mit `csc.exe` erstellt.

### Unter Linux
```bash
ant clean jar
```

---

## 📁 Eigene Figuren hinzufügen

### ZIPs von shimeji.org importieren

1. Lade auf [shimeji.org](https://shimeji.org/) ein Shimeji als ZIP herunter.
2. Öffne `ShimejiSettings.exe` oder `python shimeji_settings.py`.
3. Klicke auf **ZIP importieren …** und wähle die heruntergeladene Datei. Bereits entpackte Pakete lassen sich über **Ordner importieren …** hinzufügen.
4. Die Bildordner werden auch in verschachtelten Paketen automatisch gefunden, etwa unter `Paket/img/Figurenname/`. Enthält das Paket mehrere Figuren, werden alle importiert und stehen in der Auswahlliste.
5. Wähle die gewünschte Figur und klicke auf **Übernehmen + Neustart**.

Importierte Figuren liegen anschließend unter `characters/<Name>/`. Bereits vorhandene Figuren werden behalten; bei gleichem Namen erhält der neue Ordner einen Zusatz wie ` (2)`.

Unterstützte Formate:

- **Standard-PNGs**: `shime1.png` bis `shime46.png`.
- **Shimeji-EE mit benannten PNGs und XML**: etwa `stand.png`, `air.png` und `climb.png` mit `conf/actions.xml` im Figurenordner. Die Bildreferenzen der englischen `Action`-/`Pose`-Einträge werden den Bewegungen dieser App zugeordnet. Beispiele: Monika, Natsuki, Sayori und Yuri aus dem DDLC-Paket.
- **JSON/WebP-Pakete**: `manifest.json`, `animation.json` und `sprites/*.webp` mit Schema **`legacy_default_v1`, Version 1**. Der Name wird aus dem Manifest gelesen; die Bilder werden anhand der Animationszuordnung in PNGs mit 128 × 128 Pixeln konvertiert. Beispiel: Megumin.

Die Figuren verwenden weiterhin die Animationen und Verhaltensregeln dieser App. Fehlende Sonderposen werden durch passende vorhandene Posen oder die Standpose ersetzt; originale Übergänge, Bewegungsgeschwindigkeiten und eigene Verhaltensskripte werden nicht übernommen. Die Konvertierung ist daher eine Anpassung an diese App, keine vollständige Nachbildung der ursprünglichen Laufzeit.

Für die WebP-Konvertierung ist unter **Windows x64** der offizielle `dwebp`-Decoder in `ShimejiSettings.exe` eingebettet. Die Python-Oberfläche verwendet `lib/webp/dwebp.exe`. Unter **Ubuntu/Debian** installierst du ihn bei Bedarf mit `sudo apt install webp`; auf anderen Linux-Systemen muss `dwebp` im `PATH` liegen. Details zu Version, Herkunft und Lizenz stehen in [lib/webp/README.md](lib/webp/README.md).

Unbekannte Schemata, fehlende referenzierte Bilder und unvollständige Standard-Bildsätze werden mit einer Fehlermeldung abgelehnt. Der Import begrenzt Pakete auf 10.000 Dateien, Animationsbeschreibungen auf 4 MiB, einzelne Bilder auf 16 MiB / 4096 × 4096 Pixel und die importierten Bilder zusammen auf 256 MiB.

### Bilder manuell hinzufügen

Alternativ kannst du `characters/DEIN_FIGURENNAME/` anlegen und darin `shime1.png` bis `shime46.png` ablegen. Öffne danach die Einstellungen, wähle die Figur aus und klicke auf **Übernehmen + Neustart**.

---

## ⚙️ Konfigurationsdateien

- **`window.conf`**: Anpassungen des Fensterversatzes in der Reihenfolge `x`, `y`, `Breitenzuschlag`, `Höhenzuschlag`.
- **`titles.conf`**: Filtert Zielfenster anhand ihres Titels. Ein Titel pro Zeile; leer lassen, um alle aktiven Fenster zu verwenden.
- **`settings.properties`**: Speichert Verhaltenseinstellungen, `selfCloningEnabled=true|false` und `scalePercent=25..200` (Standard: `100`).

---

## 📜 Lizenz und Danksagung

Dieses Projekt übernimmt die ZLIB/LIBPNG-Lizenz des ursprünglichen Shimeji-Projekts von Yuki Yamada (Group Finity). Der Lizenztext steht im [Original](LICENSE) und als [deutsche Übersetzung](LICENSE.de) zur Verfügung.

- **Java Native Access (JNA)**: Unter der LGPL lizenziert.
- **Mozilla Rhino JS Engine**: Unter der MPL lizenziert.


## Import-Tests

Unter Windows führt `powershell -ExecutionPolicy Bypass -File tests/run_import_checks.ps1` die Python- und nativen Windows-Tests aus. Für die beiden Originalbeispiele zusätzlich `-SampleDirectory "C:\Pfad\zu\den\ZIPs"` angeben; erwartet werden dort `0fxxyxhc.zip` und `7rkbhgkk.zip`. Die Originaldateien werden nur in temporäre Testordner importiert und nicht ins Repository kopiert.

Die synthetischen Tests decken dieselben Paketstrukturen ohne fremde Figurenbilder ab. `tests/fixtures/pixel.webp` ist ein selbst erzeugtes Testpixel mit Alpha 128. Unter Linux: `python3 -B -m unittest discover -s tests -p 'test_*.py'` (Tkinter, eine grafische Sitzung und `dwebp` werden benötigt). Mit `SHIMEJI_SAMPLE_DIR` lassen sich auch dort die Originalbeispiele zusätzlich testen.
