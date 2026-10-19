"""Draws the README logo, the launcher icon's kite on its sky, as docs/logo.png.

python tools/icon.py
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
S = 1024
K = 4  # supersampling


def main():
    n = S * K
    im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    u = n / 108

    def p(x, y):
        return (x * u, y * u)

    # Sky, a cloud, two hills.
    for y in range(n):
        t = y / n
        c = tuple(int(a + (b - a) * t) for a, b in zip((0x86, 0xB6, 0xE0), (0xD8, 0xE8, 0xF2)))
        d.line([(0, y), (n, y)], fill=c)
    d.ellipse([*p(16, 33), *p(40, 42)], fill=(255, 255, 255, 200))
    d.ellipse([*p(22, 28), *p(32, 38)], fill=(255, 255, 255, 200))
    d.polygon([p(x, 84 - 4 * math.sin(x / 108 * math.pi)) for x in range(0, 109)] + [p(108, 108), p(0, 108)], fill=(0x9C, 0xC0, 0x8A))
    d.polygon([p(x, 92 - 3 * math.sin(x / 108 * math.pi + 0.6)) for x in range(0, 109)] + [p(108, 108), p(0, 108)], fill=(0x79, 0xA8, 0x66))

    # String and tail.
    string = [p(53 - 13 * t ** 1.4, 47 + 45 * t) for t in [i / 40 for i in range(41)]]
    d.line(string, fill=(0x4A, 0x40, 0x38), width=int(1.2 * u), joint="curve")
    tail = [p(50 + 6 * t ** 1.2 + 2.5 * math.sin(t * 9), 68 + 22 * t) for t in [i / 40 for i in range(41)]]
    d.line(tail, fill=(0x4A, 0x40, 0x38), width=int(1.3 * u), joint="curve")
    for i, (t, c) in enumerate([(0.3, (0xD4, 0x54, 0x3D)), (0.7, (0xF7, 0xEB, 0xD3))]):
        x, y = tail[int(t * 40)]
        w = 4 * u
        d.polygon([(x, y), (x - w, y - w * 0.55), (x - w, y + w * 0.55)], fill=c)
        d.polygon([(x, y), (x + w, y - w * 0.55), (x + w, y + w * 0.55)], fill=c)

    # The kite, tilted 10°.
    cx, cy = 54, 42
    a = math.radians(10)

    def r(x, y):
        dx, dy = x - cx, y - cy
        return p(cx + dx * math.cos(a) - dy * math.sin(a), cy + dx * math.sin(a) + dy * math.cos(a))

    top, right, bottom, left, mid = r(54, 28), r(67, 42), r(54, 68), r(41, 42), r(54, 42)
    red, cream = (0xD4, 0x54, 0x3D), (0xF7, 0xEB, 0xD3)
    d.polygon([mid, top, right], fill=cream)
    d.polygon([mid, right, bottom], fill=red)
    d.polygon([mid, bottom, left], fill=cream)
    d.polygon([mid, left, top], fill=red)
    spar = (0x6B, 0x4B, 0x33)
    d.line([top, bottom], fill=spar, width=int(1.1 * u))
    d.line([left, right], fill=spar, width=int(1.1 * u))

    # Rounded square.
    mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, n - 1, n - 1], radius=int(n * 0.22), fill=255)
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    out = out.resize((S, S), Image.LANCZOS)
    (ROOT / "docs").mkdir(exist_ok=True)
    out.save(ROOT / "docs/logo.png")


if __name__ == "__main__":
    main()
