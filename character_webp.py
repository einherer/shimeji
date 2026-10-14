"""Convert WebP sprites with the bundled Windows decoder or the system dwebp tool."""
import os
import shutil
import struct
import subprocess
from pathlib import Path


def webp_dimensions(data):
    if len(data) < 20 or data[:4] != b"RIFF" or data[8:12] != b"WEBP":
        raise ValueError("Ungültige WebP-Bilddatei.")
    offset = 12
    while offset + 8 <= len(data):
        kind, size = data[offset:offset + 4], int.from_bytes(data[offset + 4:offset + 8], "little")
        start = offset + 8
        if start + size > len(data):
            break
        payload = data[start:start + min(size, 10)]
        if kind == b"VP8X" and size >= 10:
            if payload[0] & 2:
                raise ValueError("Animierte WebP-Dateien werden nicht unterstützt.")
            return int.from_bytes(payload[4:7], "little") + 1, int.from_bytes(payload[7:10], "little") + 1
        if kind == b"VP8L" and size >= 5 and payload[0] == 0x2f:
            bits = int.from_bytes(payload[1:5], "little")
            return (bits & 0x3fff) + 1, ((bits >> 14) & 0x3fff) + 1
        if kind == b"VP8 " and size >= 10 and payload[3:6] == b"\x9d\x01\x2a":
            return struct.unpack("<H", payload[6:8])[0] & 0x3fff, struct.unpack("<H", payload[8:10])[0] & 0x3fff
        offset = start + size + (size & 1)
    raise ValueError("WebP-Bildgröße konnte nicht gelesen werden.")


def convert_webp(data, target):
    width, height = webp_dimensions(data)
    if not (1 <= width <= 4096 and 1 <= height <= 4096):
        raise ValueError("Die Bildgröße muss zwischen 1 und 4096 Pixeln liegen.")
    bundled = Path(__file__).parent / "lib" / "webp" / "dwebp.exe"
    decoder = str(bundled) if os.name == "nt" and bundled.is_file() else shutil.which("dwebp")
    if not decoder:
        raise ValueError("Für WebP-Importe wird dwebp benötigt. Unter Ubuntu/Debian: sudo apt install webp")
    source = target.with_suffix(".webp")
    source.write_bytes(data)
    try:
        result = subprocess.run([decoder, "-quiet", "-resize", "128", "128", "-o", str(target), "--", str(source)],
                                capture_output=True, timeout=15,
                                creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
        if result.returncode != 0 or not target.is_file():
            raise ValueError("WebP-Bild konnte nicht konvertiert werden: " + target.name)
        return target.read_bytes()
    finally:
        source.unlink()
