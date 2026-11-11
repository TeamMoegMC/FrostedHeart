#!/usr/bin/env python3
"""Generate the placeholder textures of the town notice-board theme (NoticeBoardTheme).

    python3 Scripts/gen_town_ui_textures.py [output_dir]

Writes PNGs to src/main/resources/assets/frostedheart/textures/gui/town/ by default. Every texture is
deterministic (fixed seeds), so rerunning reproduces the same files. Artists replacing a texture must keep
its size and nine-slice margin; the table below is the contract read by NoticeBoardIcons.java.

| file              | size        | nine-slice | content                                              |
|-------------------|-------------|------------|------------------------------------------------------|
| board.png         | 64x64       | 8          | horizontal planks, nails in the corners              |
| board_snow.png    | 64x12       | tiles in x | snow along the board's top edge with icicles         |
| paper.png         | 32x32       | 4          | off-white notice paper                               |
| paper_note.png    | 32x32       | 4          | yellow sticky note                                   |
| paper_grid.png    | 32x32       | 4          | grid paper of the debug maintenance sheet            |
| frost_1..3.png    | 32x32       | 8          | frost around a paper's edge, translucent overlay     |
| icicles.png       | 32x8        | tiles in x | icicles hanging under a frozen paper                 |
| pin.png           | 32x8        | -          | four 8x8 pins: red, blue, green, brass               |
| tape.png          | 24x8        | -          | a strip of tape                                      |
| stamp_round.png   | 32x32       | -          | white ink ring, tinted by code                       |
| stamp_square.png  | 24x24       | -          | white ink square, tinted by code                     |
| plaque.png        | 84x24       | 4          | three 28x24 tab plaques: normal, selected, debug red |
| bar.png           | 16x12       | 2          | progress track (rows 0-5) and white fill (rows 6-11) |
| stars.png         | 16x8        | -          | filled and empty rating star                         |
| tag_button.png    | 72x16       | 4          | three 24x16 paper-tag buttons: normal, hover, pressed|
| note_corner.png   | 24x12       | -          | folded corner of a yellow and of a white note        |
"""
import math
import os
import random
import sys

from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'frostedheart',
    'textures', 'gui', 'town')

WOOD = (133, 79, 11)
WOOD_DARK = (99, 56, 6)
WOOD_LIGHT = (186, 117, 23)
WOOD_EDGE = (74, 42, 5)
PAPER = (241, 239, 232)
PAPER_EDGE = (211, 209, 199)
NOTE = (250, 199, 117)
NOTE_EDGE = (239, 159, 39)
FROST = (230, 241, 251)
FROST_EDGE = (181, 212, 244)
ICE = (133, 183, 235)
IRON = (68, 68, 65)
IRON_LIGHT = (180, 178, 169)


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(color, amount):
    return tuple(clamp(c + amount) for c in color[:3])


def periodic_noise(seed, period_x, period_y=None):
    """Value noise that repeats every period, so nine-slice tiling shows no seam."""
    period_y = period_y or period_x
    rng = random.Random(seed)
    grid = [[rng.uniform(-1, 1) for _ in range(period_x)] for _ in range(period_y)]

    def at(x, y):
        return grid[y % period_y][x % period_x]
    return at


def new(w, h, fill=(0, 0, 0, 0)):
    return Image.new('RGBA', (w, h), fill)


def put(img, x, y, color, alpha=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(color[:3]) + (alpha,))


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name))
    print('wrote', name, img.size)


def nail(img, cx, cy):
    for dx in range(-1, 2):
        for dy in range(-1, 2):
            put(img, cx + dx, cy + dy, IRON)
    put(img, cx - 1, cy - 1, IRON_LIGHT)
    put(img, cx + 1, cy + 2, WOOD_EDGE, 160)
    put(img, cx + 2, cy + 1, WOOD_EDGE, 160)


def board():
    img = new(64, 64)
    grain = periodic_noise(11, 48, 16)
    fine = periodic_noise(12, 48, 48)
    for y in range(64):
        cy = (y - 8) % 48
        plank = (cy // 16) % 3
        in_plank = cy % 16
        base = [WOOD, shade(WOOD, -8), shade(WOOD, 6)][plank]
        for x in range(64):
            cx = (x - 8) % 48
            v = grain(cx, in_plank) * 10 + fine(cx, cy) * 5
            # long grain streaks along the plank
            v += math.sin((cx / 48.0) * math.pi * 2 * 3 + in_plank * 0.9 + plank) * 4
            color = shade(base, v)
            if in_plank == 15:
                color = WOOD_DARK
            elif in_plank == 0:
                color = shade(color, 10)
            put(img, x, y, color)
    for i in range(64):
        for t in range(2):
            put(img, i, t, WOOD_EDGE)
            put(img, i, 63 - t, WOOD_EDGE)
            put(img, t, i, WOOD_EDGE)
            put(img, 63 - t, i, WOOD_EDGE)
        put(img, i, 2, shade(WOOD_LIGHT, -20))
        put(img, 2, i, shade(WOOD_LIGHT, -20))
    for cx, cy in ((4, 4), (59, 4), (4, 59), (59, 59)):
        nail(img, cx, cy)
    save(img, 'board.png')


def icicle_strip(img, top, period, seed, min_len, max_len, start=0):
    rng = random.Random(seed)
    x = start
    while x < period:
        length = rng.randint(min_len, max_len)
        width = rng.choice((2, 3))
        for dy in range(length):
            half = max(0, int(round(width * (1 - dy / length))))
            for dx in range(-half // 2, half - half // 2 + (1 if dy == 0 else 0)):
                xx = (x + dx) % period
                edge = dx == -half // 2 or dy == length - 1
                put(img, xx, top + dy, ICE if edge else FROST, 235)
        x += rng.randint(4, 9)


def board_snow():
    img = new(64, 12)
    wave = periodic_noise(21, 64, 1)
    for x in range(64):
        depth = 4 + int(round((wave(x, 0) + 1) * 0.8))
        for y in range(depth):
            put(img, x, y, FROST if y < depth - 1 else FROST_EDGE)
    icicle_strip(img, 4, 64, 22, 3, 7, start=2)
    save(img, 'board_snow.png')


def paper(name, base, edge, seed, grid=False):
    img = new(32, 32)
    noise = periodic_noise(seed, 24, 24)
    rng = random.Random(seed + 1)
    for y in range(32):
        for x in range(32):
            v = noise((x - 4) % 24, (y - 4) % 24) * 3
            color = shade(base, v)
            if grid and (((x - 4) % 4 == 3) or ((y - 4) % 4 == 3)):
                color = tuple(clamp(a * 0.85 + b * 0.15) for a, b in zip(color, (55, 138, 221)))
            if x == 0 or y == 0 or x == 31 or y == 31:
                color = edge
            elif x == 1 or y == 1 or x == 30 or y == 30:
                color = shade(color, -6)
            put(img, x, y, color)
    # slightly ragged corners
    for cx, cy in ((0, 0), (31, 0), (0, 31), (31, 31)):
        put(img, cx, cy, (0, 0, 0), 0)
    if grid:
        # The margin rule sits in the left nine-slice border, so it is drawn once rather than once per tile.
        for y in range(2, 30):
            put(img, 3, y, (226, 75, 74), 170)
    for _ in range(6):
        x = rng.randint(6, 25)
        put(img, x, 0, (0, 0, 0), 0)
    save(img, name)


def frost(level):
    img = new(32, 32)
    rng = random.Random(30 + level)
    reach = {1: 0, 2: 3, 3: 6}[level]
    corner = {1: 7, 2: 8, 3: 8}[level]
    edge_noise = periodic_noise(31 + level, 16, 1)

    def frost_px(x, y, strength):
        alpha = clamp(150 + strength * 90)
        color = FROST if rng.random() > 0.25 else FROST_EDGE
        put(img, x, y, color, alpha)

    for y in range(32):
        for x in range(32):
            dx = min(x, 31 - x)
            dy = min(y, 31 - y)
            d_corner = math.hypot(dx, dy)
            if d_corner < corner - rng.random() * 3:
                frost_px(x, y, 1 - d_corner / corner)
                continue
            if reach:
                along = x if dy < dx else y
                limit = reach + int(round((edge_noise((along - 8) % 16, 0) + 1) * 1.5))
                if min(dx, dy) < limit and rng.random() > 0.15:
                    frost_px(x, y, 1 - min(dx, dy) / max(1, limit))
    # crystal sparkles
    for _ in range(6 * level):
        x, y = rng.randint(0, 31), rng.randint(0, 31)
        if min(x, 31 - x, y, 31 - y) <= reach + 2:
            put(img, x, y, (255, 255, 255), 255)
    save(img, 'frost_%d.png' % level)


def icicles():
    img = new(32, 8)
    icicle_strip(img, 0, 32, 41, 3, 8, start=1)
    save(img, 'icicles.png')


def pins():
    img = new(32, 8)
    colors = [((226, 75, 74), (163, 45, 45)), ((55, 138, 221), (24, 95, 165)),
              ((99, 153, 34), (59, 109, 17)), ((186, 117, 23), (133, 79, 11))]
    for i, (fill, rim) in enumerate(colors):
        ox = i * 8
        for y in range(8):
            for x in range(8):
                d = math.hypot(x - 3.5, y - 3.5)
                if d <= 3.0:
                    put(img, ox + x, y, fill if d < 2.2 else rim)
                elif d <= 3.6 and x >= 4 and y >= 4:
                    put(img, ox + x, y, (0, 0, 0), 70)
        put(img, ox + 2, 2, (255, 255, 255), 220)
    save(img, 'pin.png')


def tape():
    img = new(24, 8)
    rng = random.Random(51)
    for y in range(8):
        for x in range(24):
            if (x < 2 and rng.random() < 0.5) or (x > 21 and rng.random() < 0.5):
                continue
            put(img, x, y, shade(PAPER_EDGE, rng.uniform(-6, 6)), 190)
    save(img, 'tape.png')


def stamp_round():
    img = new(32, 32)
    rng = random.Random(61)
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - 15.5, y - 15.5)
            if 13.2 <= d <= 15.4 and rng.random() > 0.08:
                put(img, x, y, (255, 255, 255), clamp(rng.uniform(190, 255)))
    save(img, 'stamp_round.png')


def stamp_square():
    img = new(24, 24)
    rng = random.Random(62)
    for y in range(24):
        for x in range(24):
            edge = min(x, y, 23 - x, 23 - y)
            if edge <= 1 and rng.random() > 0.06:
                put(img, x, y, (255, 255, 255), clamp(rng.uniform(190, 255)))
    save(img, 'stamp_square.png')


def plaque():
    img = new(84, 24)
    grain = periodic_noise(71, 20, 16)
    bases = [WOOD, WOOD_LIGHT, (163, 45, 45)]
    borders = [WOOD_EDGE, WOOD_DARK, (121, 31, 31)]
    for i, (base, border) in enumerate(zip(bases, borders)):
        ox = i * 28
        for y in range(24):
            for x in range(28):
                color = shade(base, grain((x - 4) % 20, (y - 4) % 16) * 8)
                if x == 0 or y == 0 or x == 27 or y == 23:
                    color = border
                elif y == 1:
                    color = shade(color, 18)
                elif y == 22:
                    color = shade(color, -16)
                put(img, ox + x, y, color)
    save(img, 'plaque.png')


def bar():
    img = new(16, 12)
    for y in range(6):
        for x in range(16):
            color = (211, 209, 199)
            if y == 0:
                color = (180, 178, 169)
            if x == 0 or x == 15 or y == 5:
                color = shade(color, -10)
            put(img, x, y, color)
    for y in range(6):
        for x in range(16):
            v = 255 if y < 2 else (236 if y < 5 else 214)
            put(img, x, 6 + y, (v, v, v))
    save(img, 'bar.png')


def stars():
    img = new(16, 8)
    pts = []
    for k in range(10):
        r = 3.6 if k % 2 == 0 else 1.6
        a = -math.pi / 2 + k * math.pi / 5
        pts.append((3.5 + r * math.cos(a), 3.8 + r * math.sin(a)))

    def inside(px, py):
        hit = False
        j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                hit = not hit
            j = i
        return hit

    for y in range(8):
        for x in range(8):
            if inside(x + 0.5, y + 0.5):
                edge = not all(inside(x + 0.5 + dx, y + 0.5 + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                put(img, x, y, (133, 79, 11) if edge else (239, 159, 39))
                # The empty star must still read on cream paper: muted ink outline, a faint warm fill.
                put(img, 8 + x, y, (95, 94, 90) if edge else (222, 211, 190))
    save(img, 'stars.png')


def tag_button():
    img = new(72, 16)
    bases = [PAPER, (250, 238, 218), (211, 209, 199)]
    borders = [(136, 135, 128), (133, 79, 11), (95, 94, 90)]
    for i, (base, border) in enumerate(zip(bases, borders)):
        ox = i * 24
        for y in range(16):
            for x in range(24):
                color = base
                if x == 0 or y == 0 or x == 23 or y == 15:
                    color = border
                elif y == 14 and i != 2:
                    color = shade(base, -18)
                put(img, ox + x, y, color)
        for cx, cy in ((ox, 0), (ox + 23, 0), (ox, 15), (ox + 23, 15)):
            put(img, cx, cy, (0, 0, 0), 0)
    save(img, 'tag_button.png')


def note_corner():
    img = new(24, 12)
    for i, flap in enumerate(((239, 159, 39), (211, 209, 199))):
        ox = i * 12
        for y in range(12):
            for x in range(12):
                if x + y < 11:
                    put(img, ox + x, y, flap)
                elif x + y == 11:
                    put(img, ox + x, y, shade(flap, -30))
    save(img, 'note_corner.png')


if __name__ == '__main__':
    board()
    board_snow()
    paper('paper.png', PAPER, PAPER_EDGE, 81)
    paper('paper_note.png', NOTE, NOTE_EDGE, 82)
    paper('paper_grid.png', PAPER, PAPER_EDGE, 83, grid=True)
    for level in (1, 2, 3):
        frost(level)
    icicles()
    pins()
    tape()
    stamp_round()
    stamp_square()
    plaque()
    bar()
    stars()
    tag_button()
    note_corner()
