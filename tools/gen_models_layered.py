#!/usr/bin/env python3
"""Rewrite machine / grow-bed / beacon block models as layered multi-element models
(plinth + inset body + console + corner posts; tray walls; beacon column+lens), matching
NeroTech/NeroSpace. Textures are unchanged — only models/block/<name>.json is rewritten.
These models are non-full-cube, so the corresponding blocks are registered noOcclusion.

Run: python3 tools/gen_models_layered.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLOCK_MODEL = os.path.join(ROOT, "common/src/main/resources/assets/neroagriculture/models/block")
NS = "neroagriculture"

ALLS = {"down": "side", "up": "side", "north": "side", "south": "side", "west": "side", "east": "side"}


def faces(m):
    return {d: {"texture": f"#{t}"} for d, t in m.items()}


def el(frm, to, f):
    return {"from": frm, "to": to, "faces": f}


def write(name, obj):
    with open(os.path.join(BLOCK_MODEL, name + ".json"), "w") as fh:
        json.dump(obj, fh, separators=(",", ":"))
    print("wrote", name)


def tex(top, side, bottom):
    return {"top": f"{NS}:block/{top}", "side": f"{NS}:block/{side}",
            "bottom": f"{NS}:block/{bottom}", "particle": f"{NS}:block/{side}"}


def machine(name):
    t = name + "_top"
    s = name + "_side"
    b = name + "_bottom"
    plinth = faces({**ALLS, "down": "bottom"})
    body = faces({**ALLS, "up": "top"})
    console = faces({**ALLS, "up": "top"})
    post = faces(ALLS)
    els = [
        el([0, 0, 0], [16, 3, 16], plinth),
        el([1, 3, 1], [15, 14, 15], body),
        el([3, 14, 3], [13, 16, 13], console),
        el([0, 3, 0], [2, 15, 2], post),
        el([14, 3, 0], [16, 15, 2], post),
        el([0, 3, 14], [2, 15, 16], post),
        el([14, 3, 14], [16, 15, 16], post),
    ]
    write(name, {"parent": "minecraft:block/block", "textures": tex(t, s, b), "elements": els})


def growbed(name):
    t = name + "_top"
    s = name + "_side"
    b = name + "_bottom"
    wall = faces(ALLS)
    plinth = faces({**ALLS, "down": "bottom"})
    soil = faces({"up": "top", "down": "bottom", "north": "top", "south": "top", "west": "top", "east": "top"})
    els = [
        el([0, 0, 0], [16, 4, 16], plinth),
        el([0, 4, 0], [16, 10, 2], wall),
        el([0, 4, 14], [16, 10, 16], wall),
        el([0, 4, 2], [2, 10, 14], wall),
        el([14, 4, 2], [16, 10, 14], wall),
        el([2, 4, 2], [14, 8, 14], soil),
    ]
    write(name, {"parent": "minecraft:block/block", "textures": tex(t, s, b), "elements": els})


def beacon(name):
    t = name + "_top"
    s = name + "_side"
    b = name + "_bottom"
    base = faces({**ALLS, "down": "bottom"})
    col = faces(ALLS)
    lens = faces({**ALLS, "up": "top"})
    els = [
        el([2, 0, 2], [14, 3, 14], base),
        el([5, 3, 5], [11, 11, 11], col),
        el([3, 11, 3], [13, 16, 13], lens),
    ]
    write(name, {"parent": "minecraft:block/block", "textures": tex(t, s, b), "elements": els})


MACHINES = ["essence_extractor", "essence_infuser", "seed_synthesizer", "seed_research_bench",
            "planter", "harvester", "fertiliser_applicator", "fertiliser_processor",
            "genetics_station", "greenhouse_controller", "oxygen_plant", "biofuel_converter",
            "crop_tower_controller", "terraforming_controller"]
BEDS = ["terran_grow_bed", "industrial_grow_bed", "orbital_grow_bed", "colonial_grow_bed", "deepvoid_grow_bed"]

for m in MACHINES:
    machine(m)
for bd in BEDS:
    growbed(bd)
beacon("pollination_beacon")
print("done:", len(MACHINES) + len(BEDS) + 1, "layered models")
