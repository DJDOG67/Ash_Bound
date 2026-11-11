"""Generates the placeholder 16x16 straight sword texture.

Usage: python tools/gen_straight_sword.py
Writes: src/main/resources/assets/ashbound/textures/item/straight_sword.png

Palette follows the art direction doc (charcoal outline, faded gold guard,
ember pommel). Replace the PNG with a hand-drawn one any time; this script
is only the starting point.
"""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/ashbound/textures/item/straight_sword.png"

OUTLINE = (30, 27, 24, 255)
BLADE_LIGHT = (222, 226, 230, 255)
BLADE_MID = (160, 166, 174, 255)
BLADE_DARK = (104, 108, 118, 255)
GOLD = (201, 162, 74, 255)
GOLD_DARK = (138, 104, 42, 255)
GRIP = (70, 52, 42, 255)
GRIP_DARK = (44, 33, 28, 255)
EMBER = (224, 116, 44, 255)


def main() -> None:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    # blade: diagonal from the guard up to the tip (top-right)
    for x in range(6, 14):
        y = 15 - x
        px[x, y] = BLADE_LIGHT          # edge catching light
        if x < 13:
            px[x + 1, y] = BLADE_MID    # flat
            px[x + 1, y + 1] = BLADE_DARK  # shadow side
    px[14, 1] = BLADE_LIGHT             # tip

    # cross guard, perpendicular to the blade
    for x, y in [(3, 9), (4, 10), (6, 12), (7, 13)]:
        px[x, y] = GOLD
    px[5, 11] = GOLD_DARK

    # grip and ember pommel
    px[4, 12] = GRIP
    px[3, 13] = GRIP_DARK
    px[2, 14] = EMBER

    # 1px charcoal outline around everything
    filled = {(x, y) for x in range(16) for y in range(16) if px[x, y][3] > 0}
    for x, y in filled:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in filled and px[nx, ny][3] == 0:
                px[nx, ny] = OUTLINE

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
