#!/usr/bin/env python3
import os
import platform
import shutil
import subprocess
import sys
import tempfile
import tkinter as tk
from tkinter import ttk, messagebox, filedialog

from character_import import import_characters, is_character_directory

APP_ROOT = os.path.dirname(os.path.abspath(__file__))
WINDOW_CONF = os.path.join(APP_ROOT, "window.conf")
TITLES_CONF = os.path.join(APP_ROOT, "titles.conf")
SETTINGS_PROPS = os.path.join(APP_ROOT, "settings.properties")
CHARACTERS_DIR = os.path.join(APP_ROOT, "characters")
IMG_DIR = os.path.join(APP_ROOT, "img")
CURRENT_CHARACTER = os.path.join(APP_ROOT, ".current_character")
LOCK_FILE = os.path.join(tempfile.gettempdir(), "shimeji-settings.lock")

IS_WINDOWS = platform.system() == "Windows"
RUN_SCRIPT = os.path.join(APP_ROOT, "launch.bat" if IS_WINDOWS else "launch.sh")

HEADER = "Fensterversatz in den folgenden Zeilen angeben: x, y, Breitenzuschlag, Höhenzuschlag. Leere Angaben entsprechen 0."
DEFAULT_WINDOW = ["0", "0", "0", "0"]
_lock_handle = None


def acquire_single_instance_lock():
    global _lock_handle
    try:
        _lock_handle = open(LOCK_FILE, "a+")
        if IS_WINDOWS:
            import msvcrt
            try:
                msvcrt.locking(_lock_handle.fileno(), msvcrt.LK_NBLCK, 1)
            except OSError:
                return False
        else:
            import fcntl
            try:
                fcntl.flock(_lock_handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            except OSError:
                return False
        _lock_handle.seek(0)
        _lock_handle.truncate()
        _lock_handle.write(str(os.getpid()))
        _lock_handle.flush()
        return True
    except Exception:
        return True


def list_characters():
    if not os.path.isdir(CHARACTERS_DIR):
        return []
    names = []
    for name in sorted(os.listdir(CHARACTERS_DIR)):
        path = os.path.join(CHARACTERS_DIR, name)
        if os.path.isdir(path) and is_character_directory(path):
            names.append(name)
    return names


def detect_current_character():
    if os.path.exists(CURRENT_CHARACTER):
        with open(CURRENT_CHARACTER, "r", encoding="utf-8", errors="replace") as f:
            val = f.read().strip()
        if val:
            return val
    return "Ayaka"


def set_current_character(name):
    with open(CURRENT_CHARACTER, "w", encoding="utf-8") as f:
        f.write(name + "\n")


def apply_character(name):
    src = os.path.join(CHARACTERS_DIR, name)
    if not is_character_directory(src):
        raise ValueError(f"Unvollständige Figur: {name}. Benötigt werden shime1.png bis shime46.png.")
    if not os.path.exists(IMG_DIR):
        os.makedirs(IMG_DIR, exist_ok=True)
    for i in range(1, 47):
        shutil.copyfile(os.path.join(src, f"shime{i}.png"), os.path.join(IMG_DIR, f"shime{i}.png"))
    set_current_character(name)


def read_window_conf():
    vals = DEFAULT_WINDOW[:]
    if os.path.exists(WINDOW_CONF):
        with open(WINDOW_CONF, "r", encoding="utf-8", errors="replace") as f:
            lines = [line.rstrip("\n") for line in f.readlines()]
        nums = [line.strip() for line in lines[1:] if line.strip()]
        for i in range(min(4, len(nums))):
            vals[i] = nums[i]
    return vals


def write_window_conf(vals):
    content = HEADER + "\n" + "\n".join(vals) + "\n"
    with open(WINDOW_CONF, "w", encoding="utf-8") as f:
        f.write(content)


def read_titles_conf():
    if not os.path.exists(TITLES_CONF):
        return ""
    with open(TITLES_CONF, "r", encoding="utf-8", errors="replace") as f:
        return f.read()


def write_titles_conf(text):
    with open(TITLES_CONF, "w", encoding="utf-8") as f:
        f.write(text)


def read_settings_props():
    props = {"selfCloningEnabled": True, "scalePercent": 100}
    if not os.path.exists(SETTINGS_PROPS):
        return props
    with open(SETTINGS_PROPS, "r", encoding="utf-8", errors="replace") as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            key = k.strip()
            if key == "selfCloningEnabled":
                props[key] = v.strip().lower() == "true"
            elif key == "scalePercent":
                try:
                    props[key] = max(25, min(200, int(v.strip())))
                except ValueError:
                    props[key] = 100
    return props


def write_settings_props(self_cloning_enabled, scale_percent):
    scale_percent = int(scale_percent)
    if not 25 <= scale_percent <= 200:
        raise ValueError("Die Skalierung muss zwischen 25 und 200 Prozent liegen.")
    lines = []
    if os.path.exists(SETTINGS_PROPS):
        with open(SETTINGS_PROPS, "r", encoding="utf-8") as f:
            lines = [line for line in f if line.split("=", 1)[0].strip()
                     not in ("selfCloningEnabled", "scalePercent")]
    content = "".join(lines).rstrip("\n")
    if content:
        content += "\n"
    content += "selfCloningEnabled=%s\nscalePercent=%d\n" % (
        "true" if self_cloning_enabled else "false", scale_percent)
    temp_path = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8",
                                         dir=os.path.dirname(SETTINGS_PROPS), delete=False) as f:
            temp_path = f.name
            f.write(content)
        os.replace(temp_path, SETTINGS_PROPS)
    finally:
        if temp_path and os.path.exists(temp_path):
            os.unlink(temp_path)


def restart_shimeji():
    if IS_WINDOWS:
        subprocess.run(
            'wmic process where "name=\'java.exe\' or name=\'javaw.exe\'" get commandline,processid',
            shell=True,
            capture_output=True
        )
        # Laufende Java-Instanzen von Shimeji.jar beenden
        ps_cmd = 'Get-WmiObject Win32_Process | Where-Object { $_.CommandLine -like "*Shimeji.jar*" } | ForEach-Object { $_.Terminate() }'
        subprocess.run(["powershell", "-Command", ps_cmd], capture_output=True)
        subprocess.Popen([RUN_SCRIPT], cwd=APP_ROOT, shell=True)
    else:
        subprocess.run("pkill -f 'com.group_finity.mascot.Main'", shell=True)
        subprocess.Popen([RUN_SCRIPT], cwd=APP_ROOT)


class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Shimeji-Einstellungen")
        self.geometry("820x650")
        self.minsize(780, 600)
        self.configure(padx=12, pady=12)
        self.option_add("*Font", "Sans 10")
        self.character_var = tk.StringVar()
        self.self_clone_var = tk.BooleanVar(value=True)
        self.scale_var = tk.StringVar(value="100")
        self._syncing_scale = False
        self._build()
        self.load_values()

    def _build(self):
        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(4, weight=1)

        top = ttk.Frame(self)
        top.grid(row=0, column=0, sticky="ew")
        top.grid_columnconfigure(0, weight=1)
        ttk.Label(top, text="Shimeji-Einstellungen", font=("Sans", 14, "bold")).grid(row=0, column=0, sticky="w")
        ttk.Label(top, text="Figur, Skalierung, Fensterposition, Fenstertitel und Selbstvermehrung einstellen.").grid(row=1, column=0, sticky="w", pady=(4, 10))

        chars = ttk.LabelFrame(self, text="Figur")
        chars.grid(row=1, column=0, sticky="ew", pady=(0, 10))
        chars.grid_columnconfigure(1, weight=1)
        ttk.Label(chars, text="Aktuelle Figur").grid(row=0, column=0, sticky="w", padx=10, pady=10)
        self.character_combo = ttk.Combobox(chars, textvariable=self.character_var, state="readonly", values=list_characters())
        self.character_combo.grid(row=0, column=1, sticky="ew", padx=(0, 10), pady=10)
        ttk.Button(chars, text="Figur übernehmen", command=self.apply_character_only).grid(row=0, column=2, sticky="e", padx=(0, 10), pady=10)

        ttk.Label(chars, text="Skalierung").grid(row=1, column=0, sticky="w", padx=10, pady=(0, 8))
        self.scale_slider = ttk.Scale(chars, from_=25, to=200, command=self._scale_from_slider)
        self.scale_slider.grid(row=1, column=1, sticky="ew", padx=(0, 10), pady=(0, 8))
        scale_input = ttk.Frame(chars)
        scale_input.grid(row=1, column=2, sticky="e", padx=(0, 10), pady=(0, 8))
        ttk.Spinbox(scale_input, from_=25, to=200, increment=1, textvariable=self.scale_var,
                    width=5).pack(side="left")
        ttk.Label(scale_input, text=" %").pack(side="left")
        self.scale_var.trace_add("write", self._scale_from_input)
        ttk.Label(chars, text="25–200 % · 100 % = Originalgröße · Speichern wirkt auf alle laufenden Figuren.").grid(
            row=2, column=0, columnspan=3, sticky="w", padx=10, pady=(0, 10))

        imports = ttk.Frame(chars)
        imports.grid(row=3, column=0, columnspan=3, sticky="w", padx=10, pady=(0, 10))
        ttk.Button(imports, text="ZIP importieren …", command=self.import_zip).pack(side="left", padx=(0, 8))
        ttk.Button(imports, text="Ordner importieren …", command=self.import_folder).pack(side="left")

        behavior = ttk.LabelFrame(self, text="Verhalten")
        behavior.grid(row=2, column=0, sticky="ew", pady=(0, 10))
        ttk.Checkbutton(behavior, text="Selbstvermehrung erlauben", variable=self.self_clone_var).grid(row=0, column=0, sticky="w", padx=10, pady=10)

        offsets = ttk.LabelFrame(self, text="window.conf")
        offsets.grid(row=3, column=0, sticky="ew", pady=(0, 10))
        for i in range(2):
            offsets.grid_columnconfigure(i, weight=1)

        self.entries = {}
        fields = [
            ("X-Versatz", "x"),
            ("Y-Versatz", "y"),
            ("Breitenzuschlag", "w"),
            ("Höhenzuschlag", "h"),
        ]
        for idx, (label, key) in enumerate(fields):
            col = idx % 2
            row = idx // 2
            block = ttk.Frame(offsets)
            block.grid(row=row, column=col, sticky="ew", padx=10, pady=8)
            block.grid_columnconfigure(1, weight=1)
            ttk.Label(block, text=label, width=16).grid(row=0, column=0, sticky="w", padx=(0, 8))
            ent = ttk.Entry(block, width=12)
            ent.grid(row=0, column=1, sticky="w")
            self.entries[key] = ent

        ttk.Label(offsets, text="Speichern und Shimeji neu starten, um die Änderungen zu sehen.").grid(row=2, column=0, columnspan=2, sticky="w", padx=10, pady=(0, 10))

        titles = ttk.LabelFrame(self, text="titles.conf")
        titles.grid(row=4, column=0, sticky="nsew")
        titles.grid_columnconfigure(0, weight=1)
        titles.grid_rowconfigure(1, weight=1)
        ttk.Label(titles, text="Ein Fenstertitel je Zeile. Leer lassen, um alle Fenster zu verwenden.").grid(row=0, column=0, sticky="w", padx=10, pady=(8, 6))

        text_wrap = ttk.Frame(titles)
        text_wrap.grid(row=1, column=0, sticky="nsew", padx=10, pady=(0, 10))
        text_wrap.grid_columnconfigure(0, weight=1)
        text_wrap.grid_rowconfigure(0, weight=1)
        self.text = tk.Text(text_wrap, height=10, wrap="word")
        yscroll = ttk.Scrollbar(text_wrap, orient="vertical", command=self.text.yview)
        self.text.configure(yscrollcommand=yscroll.set)
        self.text.grid(row=0, column=0, sticky="nsew")
        yscroll.grid(row=0, column=1, sticky="ns")

        buttons = ttk.Frame(self)
        buttons.grid(row=5, column=0, sticky="ew", pady=(12, 0))
        actions = [
            ("Speichern", self.save),
            ("Übernehmen + Neustart", self.apply_all),
            ("window.conf zurücksetzen", self.reset_window),
            ("Shimeji neu starten", self.restart),
            ("Programmordner öffnen", self.open_folder),
            ("Schließen", self.destroy),
        ]
        for index, (label, command) in enumerate(actions):
            buttons.grid_columnconfigure(index % 3, weight=1)
            ttk.Button(buttons, text=label, command=command).grid(
                row=index // 3, column=index % 3, sticky="ew", padx=4, pady=4
            )

    def _scale_from_slider(self, value):
        if not self._syncing_scale:
            self.scale_var.set(str(round(float(value))))

    def _scale_from_input(self, *_):
        try:
            value = int(self.scale_var.get())
        except ValueError:
            return
        if 25 <= value <= 200:
            self._syncing_scale = True
            try:
                self.scale_slider.set(value)
            finally:
                self._syncing_scale = False

    def load_values(self):
        chars = list_characters()
        self.character_combo["values"] = chars
        current = detect_current_character()
        if current not in chars and chars:
            current = chars[0]
        self.character_var.set(current)
        props = read_settings_props()
        self.self_clone_var.set(bool(props.get("selfCloningEnabled", True)))
        self.scale_var.set(str(props.get("scalePercent", 100)))
        vals = read_window_conf()
        for key, val in zip(["x", "y", "w", "h"], vals):
            self.entries[key].delete(0, "end")
            self.entries[key].insert(0, val)
        self.text.delete("1.0", "end")
        self.text.insert("1.0", read_titles_conf())

    def save(self):
        vals = [self.entries[k].get().strip() or "0" for k in ["x", "y", "w", "h"]]
        try:
            [int(v) for v in vals]
        except ValueError:
            messagebox.showerror("Ungültige Zahl", "Die vier Werte für window.conf müssen ganze Zahlen sein.")
            return False
        try:
            scale_percent = int(self.scale_var.get().strip())
            if not 25 <= scale_percent <= 200:
                raise ValueError
        except ValueError:
            messagebox.showerror("Ungültige Skalierung", "Bitte eine ganze Zahl von 25 bis 200 Prozent eingeben.")
            return False
        write_window_conf(vals)
        write_titles_conf(self.text.get("1.0", "end").rstrip() + "\n" if self.text.get("1.0", "end").strip() else "")
        write_settings_props(self.self_clone_var.get(), scale_percent)
        messagebox.showinfo("Gespeichert", "Einstellungen gespeichert. Die Skalierung wird auf alle laufenden Figuren angewendet.")
        return True

    def import_zip(self):
        path = filedialog.askopenfilename(title="Shimeji-ZIP importieren (z. B. von shimeji.org)",
                                          filetypes=[("Shimeji-ZIP", "*.zip")])
        if path:
            self.import_package(path)

    def import_folder(self):
        path = filedialog.askdirectory(title="Entpackten Shimeji-Ordner auswählen")
        if path:
            self.import_package(path)

    def import_package(self, path):
        try:
            names = import_characters(path, CHARACTERS_DIR)
            self.character_combo["values"] = list_characters()
            self.character_var.set(names[0])
            messagebox.showinfo("Figuren importiert", "Importiert: " + ", ".join(names) +
                                "\nMit ‚Übernehmen + Neustart‘ wird die ausgewählte Figur gestartet.")
        except Exception as error:
            messagebox.showerror("Import fehlgeschlagen", str(error))

    def apply_character_only(self):
        name = self.character_var.get().strip()
        if not name:
            messagebox.showerror("Keine Figur", "Bitte eine Figur auswählen.")
            return False
        try:
            apply_character(name)
        except Exception as error:
            messagebox.showerror("Figurenwechsel fehlgeschlagen", str(error))
            return False
        messagebox.showinfo("Figur übernommen", f"Figur übernommen: {name}")
        return True

    def apply_all(self):
        if not self.save():
            return
        if not self.apply_character_only():
            return
        self.restart()

    def reset_window(self):
        for key, val in zip(["x", "y", "w", "h"], DEFAULT_WINDOW):
            self.entries[key].delete(0, "end")
            self.entries[key].insert(0, val)

    def restart(self):
        try:
            restart_shimeji()
            messagebox.showinfo("Neu gestartet", "Shimeji wurde neu gestartet.")
        except Exception as e:
            messagebox.showerror("Neustart fehlgeschlagen", str(e))

    def open_folder(self):
        if IS_WINDOWS:
            os.startfile(APP_ROOT)
        else:
            subprocess.Popen(["xdg-open", APP_ROOT])


if __name__ == "__main__":
    if not acquire_single_instance_lock():
        sys.exit(0)
    App().mainloop()
