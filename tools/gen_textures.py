#!/usr/bin/env python3
"""
Generate the NEROAGRICULTURE texture + model set — a clean "hydroponics lab" look
in the shared Neroland family pipeline (cf. nerotech/tools/gen_textures.py).

Deterministic 32x32 pixel art (Nerospace's 16x recipes scaled x2). White / light-grey
composite housing with a bio-green accent (keyed to the mod logo's green), soil + foliage
organics for beds and crops, and the five fragment tiers coloured from the material catalog
(BuiltinMaterials.java) so textures stay in lockstep with the data.

This tool is the single source of truth for BOTH the PNG textures AND their block/blockstate/
item model JSON, so art and models can never drift. Re-run with --force to repaint the whole
set (additive by default: never clobbers an existing asset).

Outputs (under common/src/main/resources/assets/neroagriculture):
  textures/block/*.png, textures/item/*.png, textures/gui/*.png
  models/block/*.json, models/item/*.json, blockstates/*.json

Usage:  python3 tools/gen_textures.py [--force]
Deps:   Pillow
"""

import hashlib
import json
import math
import os
import random
import sys

from PIL import Image

# ---------------- paths ----------------
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "common/src/main/resources/assets/neroagriculture")
BLOCK_TEX = os.path.join(RES, "textures/block")
ITEM_TEX = os.path.join(RES, "textures/item")
GUI_TEX = os.path.join(RES, "textures/gui")
BLOCK_MODEL = os.path.join(RES, "models/block")
ITEM_MODEL = os.path.join(RES, "models/item")
BLOCKSTATE = os.path.join(RES, "blockstates")
for d in (BLOCK_TEX, ITEM_TEX, GUI_TEX, BLOCK_MODEL, ITEM_MODEL, BLOCKSTATE):
    os.makedirs(d, exist_ok=True)

S = 32
FORCE = "--force" in sys.argv
NS = "neroagriculture"

# ---------------- palette (RGBA) ----------------
CLEAR = (0, 0, 0, 0)

# Lab housing — bright composite panelling.
LAB = [(226, 233, 238, 255), (212, 221, 227, 255),
       (234, 241, 245, 255), (202, 212, 219, 255)]
LAB_LIGHT = (248, 252, 255, 255)
LAB_DARK = (150, 163, 173, 255)
LAB_SEAM = (120, 133, 143, 255)

# Stage 6: darken the lab palette a touch (keep the clean look, less stark white).
def _dk(_c, _f=0.85):
    return (int(_c[0] * _f), int(_c[1] * _f), int(_c[2] * _f)) + (tuple(_c[3:]) if len(_c) > 3 else (255,))
LAB = [_dk(_c) for _c in LAB]
LAB_LIGHT = _dk(LAB_LIGHT)
LAB_DARK = _dk(LAB_DARK)
LAB_SEAM = _dk(LAB_SEAM)
RECESS_FILL = (34, 44, 50, 255)
RECESS_EDGE = (16, 22, 26, 255)

# Bio-green accent — keyed to the mod logo (tools/gen_logo.py ACCENT family).
G0 = (22, 74, 34, 255)      # deep leaf shadow
G1 = (54, 132, 66, 255)     # leaf
G2 = (120, 200, 96, 255)    # accent (logo ACCENT)
G3 = (188, 240, 158, 255)   # bright (logo BRIGHT)
G4 = (236, 252, 228, 255)   # glow peak
GREEN_RAMP = [G0, G1, G2, G3, G4]
LED_G = (128, 224, 120, 255)
LED_CORE = (214, 255, 196, 255)

# Organics.
SOIL = [(84, 58, 40, 255), (66, 45, 31, 255), (100, 70, 48, 255), (74, 51, 35, 255)]
SOIL_DAMP = (52, 36, 26, 255)
WATER = [(40, 120, 96, 255), (66, 168, 128, 255), (128, 224, 176, 255)]  # nutrient tint
GLASS_TINT = (196, 232, 236, 90)
GLASS_FRAME = (214, 224, 228, 255)

# Amber (biofuel).
AMBER = [(120, 78, 28, 255), (170, 116, 44, 255), (216, 168, 74, 255), (244, 208, 120, 255)]

# ---- fragment tiers, derived from BuiltinMaterials.java (tier -> [material RGB]) ----
# Keep this table mirroring catalog/BuiltinMaterials.java; the tier colour is the mean of
# its materials, so a catalog colour change + re-run repaints the tier to match.
CATALOG = {
    "territe": [0x343434],
    "forgite": [0xC46B48, 0xD8D8D8, 0xF4D03F, 0xAA0000, 0x3154B5, 0xE8E1D4, 0x5E7C8C],
    "orbite": [0x55D6C8, 0x24C862, 0x82E6FF],
    "colonite": [0xDDEEFF],
    "voidite": [0x24545A, 0x5D347A, 0xC8D1E8],
}
TIER_ORDER = ["territe", "forgite", "orbite", "colonite", "voidite"]


def _hex_rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def _mean(colors):
    n = len(colors)
    r = sum((c >> 16) & 255 for c in colors) // n
    g = sum((c >> 8) & 255 for c in colors) // n
    b = sum(c & 255 for c in colors) // n
    return (r, g, b, 255)


TIER = {t: _mean(v) for t, v in CATALOG.items()}

PAINTED = {"block": set(), "item": set(), "gui": set()}


# ---------------- low-level helpers ----------------
def rng_for(name):
    return random.Random(int(hashlib.md5(name.encode()).hexdigest(), 16) & 0xffffffff)


def new_img():
    return Image.new("RGBA", (S, S), CLEAR)


def clamp(v):
    return 0 if v < 0 else 255 if v > 255 else int(v)


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def shade(c, f):
    """Multiply-ish shade toward black (f<1) or white (f>1)."""
    if f <= 1:
        return (clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f), c[3] if len(c) > 3 else 255)
    return mix(c, (255, 255, 255, 255), min(1.0, f - 1.0))


def noise_fill(img, palette, rng, x0=0, y0=0, x1=S, y1=S):
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = rng.choice(palette)


def bevel(img, light=LAB_LIGHT, dark=LAB_DARK, base=LAB[0]):
    px = img.load()
    light2 = mix(light, base, 0.45)
    dark2 = mix(dark, base, 0.35)
    for i in range(S):
        px[i, 0] = light
        px[0, i] = light
        px[i, S - 1] = dark
        px[S - 1, i] = dark
    for i in range(1, S - 1):
        px[i, 1] = light2
        px[1, i] = light2
        px[i, S - 2] = dark2
        px[S - 2, i] = dark2


def rivets(img, col=LAB_DARK, pts=((3, 3), (28, 3), (3, 28), (28, 28))):
    px = img.load()
    half = mix(LAB_LIGHT, col, 0.5)
    for (rx, ry) in pts:
        px[rx, ry] = LAB_LIGHT
        px[rx + 1, ry] = half
        px[rx, ry + 1] = half
        px[rx + 1, ry + 1] = col


def panel_base(name):
    img = new_img()
    noise_fill(img, LAB, rng_for(name))
    bevel(img)
    rivets(img)
    return img


def recess(px, x0, y0, x1, y1, fill=RECESS_FILL):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = fill
    for x in range(x0, x1 + 1):
        px[x, y0] = RECESS_EDGE
    for y in range(y0, y1 + 1):
        px[x0, y] = RECESS_EDGE
    for x in range(x0 + 1, x1 + 1):
        px[x, y1] = mix(LAB_LIGHT, LAB_DARK, 0.5)
    for y in range(y0 + 1, y1 + 1):
        px[x1, y] = mix(LAB_LIGHT, LAB_DARK, 0.5)


def led(px, x, y, col=LED_G, core=LED_CORE):
    for yy in range(y - 1, y + 3):
        for xx in range(x - 1, x + 3):
            if 0 <= xx < S and 0 <= yy < S:
                px[xx, yy] = mix(col, (0, 0, 0, 255), 0.6)
    for yy in range(y, y + 2):
        for xx in range(x, x + 2):
            if 0 <= xx < S and 0 <= yy < S:
                px[xx, yy] = col
    px[x, y] = core


def vglow(px, x, y0, y1, ramp):
    """Vertical accent conduit at column x from y0..y1."""
    n = max(1, y1 - y0)
    for y in range(y0, y1):
        t = (y - y0) / n
        idx = int(t * (len(ramp) - 1))
        px[x, y] = ramp[idx]


def save_tex(img, folder, name):
    d = {"block": BLOCK_TEX, "item": ITEM_TEX, "gui": GUI_TEX}[folder]
    PAINTED[folder].add(name + ".png")
    path = os.path.join(d, name + ".png")
    if os.path.exists(path) and not FORCE:
        print("skip", os.path.relpath(path, ROOT))
        return
    img.save(path)



def greyscale_tint_base(name):
    """Convert an item texture to neutral luminance (keeping alpha) so the item's custom_model_data
    tint multiplies to the resource's ingot colour cleanly. Used for the component-tinted seed/fragment."""
    from PIL import Image
    path = os.path.join(ITEM_TEX, name + ".png")
    img = Image.open(path).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            # perceptual luminance, lifted slightly so the tint has headroom to read
            lum = int(0.299 * r + 0.587 * g + 0.114 * b)
            lum = min(255, int(lum * 0.6 + 150 * 0.4))
            px[x, y] = (lum, lum, lum, a)
    img.save(path)

def write_json(folder, name, obj):
    d = {"block_model": BLOCK_MODEL, "item_model": ITEM_MODEL, "blockstate": BLOCKSTATE}[folder]
    path = os.path.join(d, name + ".json")
    with open(path, "w") as f:
        json.dump(obj, f, separators=(",", ":"))


# ================= BLOCK MODEL / STATE emitters =================
def block_state_simple(name, model=None):
    write_json("blockstate", name, {"variants": {"": {"model": f"{NS}:block/{model or name}"}}})


def model_cube_bottom_top(name, top, side, bottom):
    write_json("block_model", name, {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": f"{NS}:block/{top}", "bottom": f"{NS}:block/{bottom}",
                     "side": f"{NS}:block/{side}"}})


def model_cube_all(name, tex=None):
    write_json("block_model", name, {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"{NS}:block/{tex or name}"}})


def model_cube_all_cutout(name, tex=None):
    write_json("block_model", name, {
        "parent": "minecraft:block/cube_all",
        "render_type": "minecraft:cutout",
        "textures": {"all": f"{NS}:block/{tex or name}"}})


def _faces(tex_by_dir):
    return {d: {"texture": f"#{t}"} for d, t in tex_by_dir.items()}


def _el(frm, to, faces):
    return {"from": frm, "to": to, "faces": faces}


ALLS = {"down": "side", "up": "side", "north": "side", "south": "side", "west": "side", "east": "side"}


def model_machine(name, top, side, bottom):
    """Layered chassis: full-width plinth + inset body + raised console + four corner posts.
    Non-full-cube, so the block must be noOcclusion. Reads as a 3D machine from every side."""
    tex = {"top": f"{NS}:block/{top}", "side": f"{NS}:block/{side}",
           "bottom": f"{NS}:block/{bottom}", "particle": f"{NS}:block/{side}"}
    sides = dict(ALLS)
    plinth = _faces({**sides, "down": "bottom"})
    body = _faces({**sides, "up": "top"})
    console = _faces({**sides, "up": "top"})
    post = _faces(sides)
    els = [
        _el([0, 0, 0], [16, 3, 16], plinth),          # plinth
        _el([1, 3, 1], [15, 14, 15], body),           # inset main body
        _el([3, 14, 3], [13, 16, 13], console),       # raised console deck
        _el([0, 3, 0], [2, 15, 2], post),             # corner posts
        _el([14, 3, 0], [16, 15, 2], post),
        _el([0, 3, 14], [2, 15, 16], post),
        _el([14, 3, 14], [16, 15, 16], post),
    ]
    write_json("block_model", name, {"parent": "minecraft:block/block", "textures": tex, "elements": els})


def model_growbed(name, top, side, bottom):
    """Open tray: plinth + four raised walls + a recessed soil bed. Non-full-cube -> noOcclusion."""
    tex = {"top": f"{NS}:block/{top}", "side": f"{NS}:block/{side}",
           "bottom": f"{NS}:block/{bottom}", "particle": f"{NS}:block/{side}"}
    wall = _faces({"down": "side", "up": "side", "north": "side", "south": "side", "west": "side", "east": "side"})
    plinth = _faces({**ALLS, "down": "bottom"})
    soil = _faces({"up": "top", "down": "bottom", "north": "top", "south": "top", "west": "top", "east": "top"})
    els = [
        _el([0, 0, 0], [16, 4, 16], plinth),          # plinth
        _el([0, 4, 0], [16, 10, 2], wall),            # north wall
        _el([0, 4, 14], [16, 10, 16], wall),          # south wall
        _el([0, 4, 2], [2, 10, 14], wall),            # west wall
        _el([14, 4, 2], [16, 10, 14], wall),          # east wall
        _el([2, 4, 2], [14, 8, 14], soil),            # recessed soil bed
    ]
    write_json("block_model", name, {"parent": "minecraft:block/block", "textures": tex, "elements": els})


def model_beacon(name, top, side, bottom):
    """Beacon: plinth + tapered column + glowing lens cap. Non-full-cube -> noOcclusion."""
    tex = {"top": f"{NS}:block/{top}", "side": f"{NS}:block/{side}",
           "bottom": f"{NS}:block/{bottom}", "particle": f"{NS}:block/{side}"}
    base = _faces({**ALLS, "down": "bottom"})
    col = _faces(ALLS)
    lens = _faces({**ALLS, "up": "top"})
    els = [
        _el([2, 0, 2], [14, 3, 14], base),
        _el([5, 3, 5], [11, 11, 11], col),
        _el([3, 11, 3], [13, 16, 13], lens),
    ]
    write_json("block_model", name, {"parent": "minecraft:block/block", "textures": tex, "elements": els})


def item_from_block(name, model=None):
    write_json("item_model", name, {"parent": f"{NS}:block/{model or name}"})


def item_generated(name, layer=None):
    write_json("item_model", name, {"parent": "minecraft:item/generated",
                                    "textures": {"layer0": f"{NS}:item/{layer or name}"}})


# ================= MACHINE FACES =================
def _glyph(px, kind, ac):
    """Small central emblem painted into the machine face recess (12..20 area)."""
    lo = mix(ac, (0, 0, 0, 255), 0.35)
    hi = mix(ac, (255, 255, 255, 255), 0.4)
    if kind == "extractor":            # funnel + drip
        for y in range(11, 16):
            for x in range(12 + (y - 11), 20 - (y - 11)):
                px[x, y] = ac
        for y in range(16, 21):
            px[15, y] = hi
            px[16, y] = ac
        px[15, 21] = G4
    elif kind == "infuser":            # concentric rings
        for r, c in ((6, lo), (4, ac), (2, hi)):
            for a in range(0, 360, 20):
                x = 16 + int(r * math.cos(math.radians(a)))
                y = 16 + int(r * math.sin(math.radians(a)))
                px[x, y] = c
        px[16, 16] = G4
    elif kind == "synthesizer":        # seed capsule
        for y in range(10, 22):
            w = 3 if 13 < y < 19 else 2
            for x in range(16 - w, 16 + w):
                px[x, y] = ac if (x + y) % 2 else hi
        px[16, 12] = G4
    elif kind == "research":           # magnifier
        for a in range(0, 360, 15):
            x = 15 + int(5 * math.cos(math.radians(a)))
            y = 15 + int(5 * math.sin(math.radians(a)))
            px[x, y] = ac
        px[15, 15] = hi
        for i in range(3):
            px[19 + i, 19 + i] = lo
    elif kind == "dna":                # helix (genetics)
        for y in range(9, 23):
            off = int(3 * math.sin((y - 9) * 0.7))
            px[16 + off, y] = ac
            px[16 - off, y] = hi
            if y % 3 == 0:
                for x in range(16 - abs(off), 16 + abs(off) + 1):
                    px[x, y] = lo
    elif kind == "leaf":               # sprout (planter / greenhouse)
        for y in range(14, 23):
            px[16, y] = mix(G1, G0, 0.3)
        for i in range(4):
            px[16 - 1 - i, 15 + i] = G2
            px[16 + 1 + i, 15 + i] = G2
            px[16 - 2 - i, 15 + i] = G1
            px[16 + 2 + i, 15 + i] = G1
        px[16, 13] = G4
    elif kind == "blade":              # harvester scythe
        for i in range(11):
            px[10 + i, 20 - i] = hi if i % 2 else ac
            px[10 + i, 21 - i] = lo
        for y in range(12, 22):
            px[10, y] = LAB_DARK
    elif kind == "drop":               # fertiliser / bioreactor droplet
        for y in range(11, 22):
            w = int((y - 11) * 0.5)
            for x in range(16 - w, 16 + w + 1):
                px[x, y] = ac if (x + y) % 2 else hi
        px[16, 20] = G4
    elif kind == "flame":              # biofuel converter
        for y in range(12, 22):
            w = 4 - abs(y - 17) // 2
            for x in range(16 - w, 16 + w + 1):
                px[x, y] = mix(AMBER[2], AMBER[3], (y - 12) / 10)
        px[16, 13] = (255, 240, 180, 255)
    elif kind == "tower":              # crop tower column
        for y in range(10, 23):
            px[13, y] = ac
            px[19, y] = ac
        for y in (11, 15, 19):
            for x in range(13, 20):
                px[x, y] = hi
    elif kind == "globe":              # terraforming
        for a in range(0, 360, 12):
            x = 16 + int(6 * math.cos(math.radians(a)))
            y = 16 + int(6 * math.sin(math.radians(a)))
            px[x, y] = ac
        for x in range(11, 22):
            px[x, 16] = mix(ac, hi, 0.5)
        for y in range(11, 22):
            px[16, y] = mix(ac, hi, 0.5)
    else:                              # generic core
        recess(px, 13, 13, 18, 18, mix(ac, (0, 0, 0, 255), 0.4))
        px[15, 15] = G4
        px[16, 16] = G4


def gen_machine(name, kind, accent=G2, top_kind="grid"):
    # side (all four sides identical — machines have no FACING)
    img = panel_base(name)
    px = img.load()
    recess(px, 8, 8, 23, 23, RECESS_FILL)
    _glyph(px, kind, accent)
    led(px, 5, 5, LED_G)
    led(px, 26, 5, accent if accent != G2 else LED_G)
    vglow(px, 4, 9, 27, GREEN_RAMP)
    vglow(px, 27, 9, 27, GREEN_RAMP)
    save_tex(img, "block", name + "_side")

    # top — control deck
    img = panel_base(name + "_top")
    px = img.load()
    if top_kind == "vent":
        for y in range(6, 27, 3):
            for x in range(6, 26):
                px[x, y] = LAB_DARK
                px[x, y + 1] = LAB_SEAM
    else:  # grid console
        recess(px, 6, 6, 25, 25, (28, 38, 44, 255))
        for i in range(8, 25, 4):
            for j in range(8, 25, 4):
                px[i, j] = LED_G if (i + j) % 8 == 0 else mix(accent, (0, 0, 0, 255), 0.3)
        led(px, 20, 9, accent if accent != G2 else LED_G)
    save_tex(img, "block", name + "_top")

    # bottom — plain plate
    img = panel_base(name + "_bottom")
    save_tex(img, "block", name + "_bottom")

    model_machine(name, name + "_top", name + "_side", name + "_bottom")
    block_state_simple(name)
    item_from_block(name)


def gen_pollination_beacon():
    name = "pollination_beacon"
    # side
    img = panel_base(name)
    px = img.load()
    for y in range(6, 26):
        vglow(px, 15, 6, 26, GREEN_RAMP)
        vglow(px, 16, 6, 26, GREEN_RAMP)
    recess(px, 10, 20, 21, 26, RECESS_FILL)
    led(px, 5, 24, LED_G)
    led(px, 26, 24, LED_G)
    save_tex(img, "block", name + "_side")
    # top — emitter lens
    img = panel_base(name + "_top")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d < 3:
                px[x, y] = G4
            elif d < 6:
                px[x, y] = mix(G3, G2, (d - 3) / 3)
            elif d < 9:
                px[x, y] = mix(G2, G0, (d - 6) / 3)
    save_tex(img, "block", name + "_top")
    img = panel_base(name + "_bottom")
    save_tex(img, "block", name + "_bottom")
    model_beacon(name, name + "_top", name + "_side", name + "_bottom")
    block_state_simple(name)
    item_from_block(name)


# ================= GROW BEDS (tier-coloured) =================
def gen_grow_bed(name, tier):
    tc = TIER[tier]
    rng = rng_for(name)
    # side — tank with a tier-coloured level window
    img = new_img()
    noise_fill(img, LAB, rng)
    bevel(img)
    rivets(img)
    px = img.load()
    recess(px, 5, 12, 26, 24, mix(tc, (0, 0, 0, 255), 0.5))
    for y in range(14, 23):
        for x in range(6, 26):
            px[x, y] = mix(tc, WATER[1], 0.35) if (x + y) % 3 else mix(tc, (255, 255, 255, 255), 0.2)
    for x in range(6, 26):
        px[x, 14] = mix(tc, (255, 255, 255, 255), 0.5)  # surface catch-light
    led(px, 4, 5, mix(tc, LED_CORE, 0.4))
    save_tex(img, "block", name + "_side")
    # top — soil tray with tier sprouts
    img = new_img()
    noise_fill(img, [LAB[3], LAB[1]], rng)
    bevel(img)
    px = img.load()
    for y in range(4, 28):
        for x in range(4, 28):
            px[x, y] = rng.choice(SOIL)
    for x in range(4, 28):        # rim
        px[x, 4] = LAB_DARK
        px[x, 27] = LAB_SEAM
        px[4, x] = LAB_DARK
        px[27, x] = LAB_SEAM
    for (sx, sy) in ((10, 12), (20, 10), (14, 20), (22, 22), (9, 22)):
        px[sx, sy] = G1
        px[sx, sy - 1] = G2
        px[sx - 1, sy] = mix(G1, tc, 0.4)
        px[sx + 1, sy] = mix(G1, tc, 0.4)
    save_tex(img, "block", name + "_top")
    img = panel_base(name + "_bottom")
    save_tex(img, "block", name + "_bottom")
    model_growbed(name, name + "_top", name + "_side", name + "_bottom")
    block_state_simple(name)
    item_from_block(name)


# ================= FRAGMENT BLOCKS (tier) =================
def gen_fragment_block(name, tier):
    tc = TIER[tier]
    rng = rng_for(name)
    img = new_img()
    px = img.load()
    dark = mix(tc, (0, 0, 0, 255), 0.45)
    lite = mix(tc, (255, 255, 255, 255), 0.45)
    for y in range(S):
        for x in range(S):
            px[x, y] = rng.choice([tc, dark, lite, mix(tc, dark, 0.5)])
    # facetted crystalline seams
    for i in range(S):
        px[i, i % S] = lite if i % 2 else tc
    for k in range(4, S, 7):
        for i in range(S):
            xx = (i + k) % S
            px[xx, i] = dark
            px[i, xx] = mix(lite, tc, 0.5)
    bevel(img, light=lite, dark=dark, base=tc)
    for (cx, cy) in ((8, 8), (24, 10), (12, 24), (23, 23)):
        px[cx, cy] = G4 if tier == "territe" else mix(lite, (255, 255, 255, 255), 0.6)
    save_tex(img, "block", name)
    model_cube_all(name)
    block_state_simple(name)
    item_from_block(name)


# ================= STRUCTURAL =================
def gen_greenhouse_frame():
    name = "greenhouse_frame"
    img = new_img()
    px = img.load()
    noise_fill(img, LAB, rng_for(name))
    # girder cross
    for i in range(S):
        for w in (0, 1, 2):
            px[i, w] = LAB_LIGHT if w == 0 else LAB[1]
            px[i, S - 1 - w] = LAB_DARK if w == 0 else LAB[3]
            px[w, i] = LAB_LIGHT if w == 0 else LAB[1]
            px[S - 1 - w, i] = LAB_DARK if w == 0 else LAB[3]
    for i in range(S):
        px[15, i] = LAB_SEAM
        px[16, i] = LAB[3]
        px[i, 15] = LAB_SEAM
        px[i, 16] = LAB[3]
    for (cx, cy) in ((15, 3), (15, 28), (3, 15), (28, 15)):
        px[cx, cy] = G2
        px[cx + 1, cy] = G3
    save_tex(img, "block", name)
    model_cube_all(name)
    block_state_simple(name)
    item_from_block(name)


def gen_greenhouse_glass():
    name = "greenhouse_glass"
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            px[x, y] = GLASS_TINT
    # frame border + mullions
    for i in range(S):
        for w in (0, 1):
            px[i, w] = GLASS_FRAME
            px[i, S - 1 - w] = mix(GLASS_FRAME, LAB_DARK, 0.4)
            px[w, i] = GLASS_FRAME
            px[S - 1 - w, i] = mix(GLASS_FRAME, LAB_DARK, 0.4)
    for i in range(2, S - 2):
        px[15, i] = (210, 228, 232, 150)
        px[i, 15] = (210, 228, 232, 150)
    # specular streak
    for i in range(4, 14):
        px[i, 28 - i] = (245, 252, 255, 170)
    px[6, 7] = (255, 255, 255, 210)
    px[7, 8] = (255, 255, 255, 160)
    save_tex(img, "block", name)
    model_cube_all_cutout(name)
    block_state_simple(name)
    item_from_block(name)


def gen_crop_tower_frame():
    name = "crop_tower_frame"
    img = panel_base(name)
    px = img.load()
    # vertical rails + slot rungs
    for y in range(2, 30):
        px[7, y] = LAB_DARK
        px[8, y] = LAB[3]
        px[23, y] = LAB_DARK
        px[24, y] = LAB[3]
    for y in range(4, 30, 6):
        for x in range(8, 24):
            px[x, y] = LAB_SEAM
            px[x, y + 1] = mix(G2, LAB[0], 0.4)
    vglow(px, 15, 2, 30, GREEN_RAMP)
    vglow(px, 16, 2, 30, GREEN_RAMP)
    save_tex(img, "block", name)
    model_cube_all(name)
    block_state_simple(name)
    item_from_block(name)


def gen_fragment_decor():
    name = "fragment_decor"
    rng = rng_for(name)
    img = new_img()
    px = img.load()
    cols = [TIER[t] for t in TIER_ORDER]
    for y in range(S):
        band = cols[min(len(cols) - 1, y * len(cols) // S)]
        for x in range(S):
            px[x, y] = mix(band, rng.choice([(0, 0, 0, 255), (255, 255, 255, 255)]), 0.12)
    bevel(img, light=LAB_LIGHT, dark=LAB_DARK, base=cols[2])
    for i in range(S):
        px[i, i] = mix((255, 255, 255, 255), cols[i * len(cols) // S], 0.4)
    save_tex(img, "block", name)
    model_cube_all(name)
    block_state_simple(name)
    item_from_block(name)


# ================= CROPS (8 stages) =================
def _stalk(px, cx, top, bot, col, shadow):
    for y in range(top, bot):
        px[cx, y] = col
        px[cx - 1, y] = shadow


def gen_crop(name, family):
    """8 stage cross-sprites; blockstate maps age=N -> stageN model."""
    for age in range(8):
        img = new_img()
        px = img.load()
        rng = rng_for(name + str(age))
        t = age / 7.0
        base_y = 30
        height = int(6 + t * 20)
        top = base_y - height
        if family == "resource":
            stalk, leaf, glow = mix(G1, TIER["forgite"], 0.3), G2, LED_CORE
            for cx in (13, 16, 19):
                _stalk(px, cx, top, base_y, stalk, mix(stalk, (0, 0, 0, 255), 0.4))
            if age >= 3:
                for cx in (13, 16, 19):
                    for k in range(1 + age // 2):
                        yy = top + 2 + k * 4
                        if yy < base_y:
                            px[cx - 1, yy] = leaf
                            px[cx + 1, yy] = leaf
            if age >= 5:  # crystalline resource nodes near maturity
                for cx in (13, 16, 19):
                    px[cx, top] = glow
                    px[cx, top + 1] = mix(TIER["orbite"], glow, 0.4)
        elif family == "food":
            stalk, leaf, grain = G1, G2, (226, 208, 120, 255)
            for cx in (13, 16, 19):
                _stalk(px, cx, top, base_y, stalk, mix(stalk, (0, 0, 0, 255), 0.4))
                for k in range(age):
                    yy = base_y - 2 - k * 3
                    if yy > top:
                        px[cx - 1, yy] = leaf
                        px[cx + 1, yy] = leaf
            if age >= 5:  # grain heads
                for cx in (13, 16, 19):
                    for yy in range(top, top + 5):
                        px[cx, yy] = grain
                        if age >= 6:
                            px[cx - 1, yy] = mix(grain, (0, 0, 0, 255), 0.3)
                            px[cx + 1, yy] = mix(grain, (0, 0, 0, 255), 0.3)
        else:  # alien
            stalk, flesh, glow = (78, 54, 110, 255), (150, 70, 178, 255), (214, 150, 255, 255)
            main = 16
            _stalk(px, main, top, base_y, stalk, mix(stalk, (0, 0, 0, 255), 0.4))
            if age >= 2:
                for cx in (12, 20):
                    off = int((base_y - top) * 0.4)
                    _stalk(px, cx, top + off, base_y - 2, stalk, mix(stalk, (0, 0, 0, 255), 0.4))
            if age >= 4:  # bulbous pods
                r = 2 + age // 3
                for yy in range(S):
                    for xx in range(S):
                        d = math.hypot(xx - main, yy - top - 1)
                        if d < r:
                            px[xx, yy] = flesh if d > r - 1.4 else glow
            if age >= 6:
                px[main, top - 1] = glow
        save_tex(img, "block", f"{name}_stage{age}")
        write_json("block_model", f"{name}_stage{age}", {
            "parent": "minecraft:block/crop",
            "textures": {"crop": f"{NS}:block/{name}_stage{age}"}})
    write_json("blockstate", name, {
        "variants": {f"age={a}": {"model": f"{NS}:block/{name}_stage{a}"} for a in range(8)}})
    # crop block-item: a small planted sprite (mature-ish) rather than the 3D cross
    _gen_crop_item_sprite(name, family)
    item_generated(name)


def _gen_crop_item_sprite(name, family):
    img = new_img()
    px = img.load()
    # reuse mature stage look, centred, on a little soil mound
    rng = rng_for(name + "_item")
    for x in range(9, 24):
        px[x, 27] = rng.choice(SOIL)
        px[x, 28] = SOIL_DAMP
    if family == "resource":
        for cx in (13, 16, 19):
            _stalk(px, cx, 8, 27, mix(G1, TIER["forgite"], 0.3), G0)
            px[cx, 8] = LED_CORE
            px[cx - 1, 12] = G2
            px[cx + 1, 16] = G2
    elif family == "food":
        for cx in (13, 16, 19):
            _stalk(px, cx, 8, 27, G1, G0)
            for yy in range(8, 13):
                px[cx, yy] = (226, 208, 120, 255)
    else:
        _stalk(px, 16, 6, 27, (78, 54, 110, 255), (40, 26, 60, 255))
        for yy in range(S):
            for xx in range(S):
                if math.hypot(xx - 16, yy - 8) < 4:
                    px[xx, yy] = (150, 70, 178, 255)
        px[16, 6] = (214, 150, 255, 255)
    save_tex(img, "item", name)


# ================= FLUID BLOCK-ITEMS (placeable source) =================
def gen_fluid_block(name, ramp):
    # liquid blocks render via the fluid renderer; give the block-item a canister-ish sprite
    img = new_img()
    px = img.load()
    for y in range(6, 27):
        for x in range(9, 23):
            px[x, y] = ramp[1] if (x + y) % 2 else ramp[2]
    for x in range(9, 23):
        px[x, 6] = ramp[3]
        px[x, 26] = ramp[0]
    for y in range(6, 27):
        px[9, y] = ramp[0]
        px[22, y] = mix(ramp[2], (255, 255, 255, 255), 0.3)
    save_tex(img, "item", name)
    # keep a minimal block model (particle) so the BlockItem/renderer resolve
    write_json("block_model", name, {"textures": {"particle": f"{NS}:item/{name}"}})
    write_json("blockstate", name, {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    item_generated(name)


# ================= ITEMS =================
def _item_base(rng, body, edge):
    img = new_img()
    px = img.load()
    return img, px


def gen_seed_item(name, hull, core, glow=None):
    img = new_img()
    px = img.load()
    rng = rng_for(name)
    # teardrop seed
    for y in range(8, 25):
        w = int(6 * math.sin((y - 8) / 17 * math.pi))
        for x in range(16 - w, 16 + w):
            d = (x - 16) / max(1, w)
            px[x, y] = mix(hull, core, 0.5 - abs(d) * 0.5)
    for y in range(9, 24):
        px[16, y] = mix(core, (255, 255, 255, 255), 0.2)
    # seam
    for y in range(10, 23):
        px[13 + (y % 2), y] = mix(hull, (0, 0, 0, 255), 0.3)
    if glow:
        px[16, 11] = glow
        px[15, 12] = mix(glow, core, 0.4)
    save_tex(img, "item", name)
    item_generated(name)


def gen_fragment_item(name, tier):
    tc = TIER[tier]
    img = new_img()
    px = img.load()
    # a stoppered vial of tier fragment
    glass = (206, 224, 230, 255)
    for y in range(6, 27):
        for x in range(11, 21):
            px[x, y] = glass
    for y in range(12, 25):
        for x in range(12, 20):
            px[x, y] = mix(tc, WATER[1], 0.2) if (x + y) % 2 else tc
    for x in range(12, 20):
        px[x, 12] = mix(tc, (255, 255, 255, 255), 0.5)
    # cork + neck
    for y in range(4, 7):
        for x in range(13, 19):
            px[x, y] = (150, 112, 70, 255)
    for x in range(11, 21):
        px[x, 6] = mix(glass, LAB_DARK, 0.4)
    # highlight + tier spark
    for y in range(8, 25):
        px[13, y] = (245, 252, 255, 200)
    px[16, 16] = G4 if tier == "territe" else mix(tc, (255, 255, 255, 255), 0.7)
    save_tex(img, "item", name)
    item_generated(name)


def gen_canister(name, ramp):
    img = new_img()
    px = img.load()
    metal = LAB[1]
    for y in range(7, 27):
        for x in range(10, 22):
            px[x, y] = metal
    bevel_box(px, 10, 7, 21, 26, LAB_LIGHT, LAB_DARK)
    recess(px, 12, 12, 19, 22, ramp[1])
    for y in range(13, 22):
        for x in range(13, 19):
            px[x, y] = ramp[1] if (x + y) % 2 else ramp[2]
    for x in range(13, 19):
        px[x, 13] = ramp[3]
    for x in range(11, 21):   # cap
        px[x, 6] = LAB_DARK
        px[x, 5] = LAB[2]
    led(px, 20, 9)
    save_tex(img, "item", name)
    item_generated(name)


def gen_bucket(name, ramp):
    img = new_img()
    px = img.load()
    # vanilla-ish bucket silhouette with tinted contents
    for y in range(12, 27):
        w = 9 - (y - 12) // 3
        for x in range(16 - w, 16 + w):
            px[x, y] = LAB[1] if (x + y) % 2 else LAB[3]
    for x in range(9, 24):    # rim
        px[x, 12] = LAB_LIGHT
        px[x, 13] = LAB_DARK
    for x in range(11, 22):   # fluid surface
        px[x, 14] = ramp[2]
        px[x, 15] = ramp[1]
    # handle
    for i in range(6):
        px[9 + i, 11 - i // 2] = LAB_DARK
        px[23 - i, 11 - i // 2] = LAB_DARK
    save_tex(img, "item", name)
    item_generated(name)


def bevel_box(px, x0, y0, x1, y1, light, dark):
    for x in range(x0, x1 + 1):
        px[x, y0] = light
        px[x, y1] = dark
    for y in range(y0, y1 + 1):
        px[x0, y] = light
        px[x1, y] = dark


def gen_produce(name, flesh, hi, leaf=True):
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 16, y - 18)
            if d < 9:
                px[x, y] = flesh if d > 8 - 3 else mix(flesh, hi, 0.5)
            if d < 3:
                px[x, y] = hi
    # specular
    px[12, 13] = mix(hi, (255, 255, 255, 255), 0.6)
    px[13, 13] = mix(hi, (255, 255, 255, 255), 0.3)
    if leaf:
        px[16, 8] = G1
        px[16, 7] = G2
        px[15, 8] = G1
        px[17, 9] = G2
    save_tex(img, "item", name)
    item_generated(name)


def gen_fertiliser(name, accent):
    img = new_img()
    px = img.load()
    # sack with a tag
    sack = (206, 196, 168, 255)
    for y in range(9, 28):
        w = 8 if 12 < y < 25 else 6
        for x in range(16 - w, 16 + w):
            px[x, y] = sack if (x * y) % 5 else mix(sack, (0, 0, 0, 255), 0.2)
    for x in range(11, 22):   # tie
        px[x, 9] = (150, 120, 80, 255)
    px[16, 7] = (120, 96, 60, 255)
    px[16, 8] = (120, 96, 60, 255)
    # granule pile spilling
    for (gx, gy) in ((12, 26), (16, 27), (20, 26), (14, 25), (18, 25)):
        px[gx, gy] = accent
    # emblem
    px[16, 17] = accent
    px[15, 18] = mix(accent, (255, 255, 255, 255), 0.4)
    px[17, 18] = mix(accent, (255, 255, 255, 255), 0.4)
    px[16, 19] = accent
    save_tex(img, "item", name)
    item_generated(name)


def gen_lump(name, ramp):
    img = new_img()
    px = img.load()
    rng = rng_for(name)
    for y in range(S):
        for x in range(S):
            if math.hypot(x - 16, y - 17) < 8 + rng.random() * 2:
                px[x, y] = rng.choice(ramp)
    # a few highlights + dark pits
    for _ in range(10):
        x, y = rng.randint(10, 22), rng.randint(11, 24)
        px[x, y] = mix(ramp[-1], (255, 255, 255, 255), 0.4)
    save_tex(img, "item", name)
    item_generated(name)


def gen_module(name, glyph, accent):
    img = new_img()
    px = img.load()
    # chip / circuit board
    board = mix(accent, (0, 0, 0, 255), 0.45)
    for y in range(7, 26):
        for x in range(7, 26):
            px[x, y] = board if (x + y) % 2 else mix(board, (0, 0, 0, 255), 0.25)
    bevel_box(px, 7, 7, 25, 25, mix(accent, (255, 255, 255, 255), 0.3), (0, 0, 0, 255))
    # pins
    for i in range(9, 24, 3):
        px[i, 5] = LAB[2]
        px[i, 6] = LAB_DARK
        px[i, 26] = LAB[2]
        px[i, 27] = LAB_DARK
    # traces
    for i in range(9, 24):
        px[i, 16] = mix(accent, (255, 255, 255, 255), 0.2)
    # glyph
    if glyph == "speed":
        for i in range(6):
            px[13 + i, 12 + i] = G4
            px[13 + i, 13 + i] = accent
            px[12 + i, 18 - i] = accent
    elif glyph == "gear":
        for a in range(0, 360, 45):
            x = 16 + int(5 * math.cos(math.radians(a)))
            y = 16 + int(5 * math.sin(math.radians(a)))
            px[x, y] = G4
        px[16, 16] = accent
    save_tex(img, "item", name)
    item_generated(name)


def gen_variant_fragment():
    # resource_fragment — a component-tinted generic fragment mote (neutral green)
    name = "resource_fragment"
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 16, y - 16)
            if d < 8:
                px[x, y] = mix(G1, G2, 1 - d / 8)
            elif d < 10:
                px[x, y] = mix(G0, G1, (10 - d) / 2)
    for a in range(0, 360, 45):
        x = 16 + int(9 * math.cos(math.radians(a)))
        y = 16 + int(9 * math.sin(math.radians(a)))
        px[x, y] = G4
    px[16, 16] = G4
    save_tex(img, "item", name)
    item_generated(name)


def gen_module_wrappers():
    gen_module("speed_module", "speed", TIER["orbite"])
    gen_module("efficiency_module", "gear", G2)


# ================= GUI =================
def gen_gui():
    """176x150 lab machine panel in a 256x256 image; slot wells at menu slot coords."""
    W, H = 176, 150
    img = Image.new("RGBA", (256, 256), CLEAR)
    px = img.load()

    def fill(x0, y0, x1, y1, c):
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[x, y] = c

    def box(x0, y0, x1, y1, light, dark):
        for x in range(x0, x1):
            px[x, y0] = light
            px[x, y1 - 1] = dark
        for y in range(y0, y1):
            px[x0, y] = light
            px[x1 - 1, y] = dark

    fill(0, 0, W, H, (222, 230, 235, 255))
    box(0, 0, W, H, (248, 252, 255, 255), (140, 152, 162, 255))
    box(1, 1, W - 1, H - 1, (236, 242, 246, 255), (170, 182, 192, 255))
    # machine work-area inset (top)
    fill(6, 14, W - 6, 52, (206, 216, 222, 255))
    box(6, 14, W - 6, 52, (150, 162, 172, 255), (248, 252, 255, 255))

    def slot(x, y):
        for yy in range(y - 1, y + 17):
            for xx in range(x - 1, x + 17):
                px[xx, yy] = (58, 70, 78, 255)
        box(x - 1, y - 1, x + 17, y + 17, (40, 50, 56, 255), (200, 212, 220, 255))
        for yy in range(y, y + 16):
            for xx in range(x, x + 16):
                px[xx, yy] = (150, 162, 170, 255) if (xx + yy) % 2 else (140, 152, 160, 255)

    for (sx, sy) in ((26, 26), (48, 26), (70, 26), (116, 26), (138, 26), (92, 20), (92, 38)):
        slot(sx, sy)
    # green accent rails
    for x in range(6, W - 6):
        px[x, 12] = (120, 200, 96, 255)
        px[x, 53] = (120, 200, 96, 255)
    # player inventory + hotbar
    for row in range(3):
        for col in range(9):
            slot(8 + col * 18, 68 + row * 18)
    for col in range(9):
        slot(8 + col * 18, 126)
    PAINTED["gui"].add("machine.png")
    path = os.path.join(GUI_TEX, "machine.png")
    if not (os.path.exists(path) and not FORCE):
        img.save(path)


# ---------------- catalogues ----------------
MACHINES = [
    ("fragment_extractor", "extractor", G2, "grid"),
    ("fragment_infuser", "infuser", G2, "grid"),
    ("seed_synthesizer", "synthesizer", G2, "grid"),
    ("seed_research_bench", "research", TIER["orbite"], "grid"),
    ("planter", "leaf", G2, "vent"),
    ("harvester", "blade", G2, "vent"),
    ("fertiliser_applicator", "drop", G2, "vent"),
    ("fertiliser_processor", "drop", G2, "grid"),
    ("genetics_station", "dna", TIER["orbite"], "grid"),
    ("greenhouse_controller", "leaf", G2, "grid"),
    ("oxygen_plant", "drop", TIER["orbite"], "vent"),
    ("biofuel_converter", "flame", AMBER[2], "vent"),
    ("crop_tower_controller", "tower", G2, "grid"),
    ("terraforming_controller", "globe", TIER["colonite"], "grid"),
]
BEDS = [("territe_grow_bed", "territe"), ("forgite_grow_bed", "forgite"),
        ("orbite_grow_bed", "orbite"), ("colonite_grow_bed", "colonite"),
        ("voidite_grow_bed", "voidite")]
FRAGMENT_BLOCKS = [(f"{t}_fragment_block", t) for t in TIER_ORDER]
CROPS = [("resource_crop", "resource"), ("engineered_food_crop", "food"), ("alien_crop", "alien")]

# every registered block, for coverage
ALL_BLOCKS = ([m[0] for m in MACHINES] + [b[0] for b in BEDS] + [e[0] for e in FRAGMENT_BLOCKS]
              + [c[0] for c in CROPS] + ["pollination_beacon", "greenhouse_frame",
              "greenhouse_glass", "crop_tower_frame", "fragment_decor", "nutrient", "biofuel"])


def main():
    for (name, kind, accent, top) in MACHINES:
        gen_machine(name, kind, accent, top)
    gen_pollination_beacon()
    for (name, tier) in BEDS:
        gen_grow_bed(name, tier)
    for (name, tier) in FRAGMENT_BLOCKS:
        gen_fragment_block(name, tier)
    for (name, family) in CROPS:
        gen_crop(name, family)
    gen_greenhouse_frame()
    gen_greenhouse_glass()
    gen_crop_tower_frame()
    gen_fragment_decor()
    gen_fluid_block("nutrient", WATER + [mix(WATER[2], (255, 255, 255, 255), 0.4)])
    gen_fluid_block("biofuel", AMBER)

    # ---- items ----
    gen_seed_item("resource_seed", mix(G1, TIER["forgite"], 0.3), G2, LED_CORE)
    gen_seed_item("food_seed", (150, 120, 66, 255), G2, None)
    gen_seed_item("alien_seed", (78, 54, 110, 255), (150, 70, 178, 255), (214, 150, 255, 255))
    gen_seed_item("blank_seed", (196, 200, 190, 255), (168, 174, 160, 255), None)
    gen_seed_item("charged_seed", (60, 140, 100, 255), G3, G4)
    gen_seed_item("terraforming_seed", TIER["colonite"], (170, 210, 255, 255), (245, 252, 255, 255))
    gen_variant_fragment()
    for t in TIER_ORDER:
        gen_fragment_item(f"{t}_fragment", t)
    gen_canister("nutrient_canister", WATER + [mix(WATER[2], (255, 255, 255, 255), 0.4)])
    gen_canister("biofuel_canister", AMBER)
    gen_bucket("nutrient_bucket", WATER + [mix(WATER[2], (255, 255, 255, 255), 0.4)])
    gen_bucket("biofuel_bucket", AMBER)
    gen_produce("engineered_food", (210, 150, 70, 255), (245, 210, 120, 255), leaf=True)
    gen_produce("alien_produce", (150, 70, 178, 255), (214, 150, 255, 255), leaf=False)
    gen_fertiliser("fertiliser", G2)
    gen_fertiliser("speed_fertiliser", TIER["orbite"])
    gen_fertiliser("yield_fertiliser", (226, 208, 120, 255))
    gen_lump("biomass", [G0, G1, mix(G1, SOIL[0], 0.5)])
    gen_lump("crop_waste", [SOIL[1], SOIL[0], (96, 92, 60, 255)])
    gen_module_wrappers()

    greyscale_tint_base("resource_seed")
    greyscale_tint_base("resource_fragment")
    gen_gui()

    # ---- coverage ----
    missing_block_tex = []
    for b in ALL_BLOCKS:
        # crops/fluids paint item sprites; others paint block textures
        pass
    print("tiers:", {t: "#%02X%02X%02X" % TIER[t][:3] for t in TIER_ORDER})
    print("block textures:", len(PAINTED["block"]))
    print("item textures:", len(PAINTED["item"]))
    print("gui textures:", len(PAINTED["gui"]))


if __name__ == "__main__":
    main()
