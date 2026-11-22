#!/usr/bin/env python3
"""Convert the approved Generic Blockbench drill to Forge OBJ modules and two item descriptors.

Usage: python Scripts/export-stratadrill.py path/to/model.bbmodel [--resources src/main/resources]
The editable Blockbench project is the source of truth and is opened read-only.
"""
import argparse
import base64
from collections import Counter
import hashlib
import json
import math
from pathlib import Path
import struct

NS = "frostedheart"
MODEL_DIR = "models/item/stratadrill"
TEXTURE_DIR = "item/stratadrill"
# Ship one OBJ per physical module. Animation roles are inline Forge visibility
# views of that geometry, not separate files.
MODULE_PARTS = {
    "body": ("body",),
    "steam": ("steam_fixed", "steam_crank", "steam_rod", "steam_piston"),
    "diesel": ("diesel_fixed", "diesel_flywheel"),
    "spiral": ("spiral",),
    "wheel": ("wheel_fixed", "wheel_rotor"),
}
# Map the named Blockbench outliner groups, rather than relying on layer
# visibility or element list order. The wheel's rotor group is nested in its head.
PART_GROUPS = {
    "body": ("01 机架 · 铆接前箱", "02 夹头 · 传动轴"),
    "steam_fixed": ("S 蒸汽动力盒", "S 压力表针"),
    "steam_crank": ("S 曲柄盘",),
    "steam_rod": ("S 外连杆",),
    "steam_piston": ("S 活塞",),
    "diesel_fixed": ("D 柴油动力盒",),
    "diesel_flywheel": ("D 飞轮",),
    "spiral": ("01 螺旋切削头 B · 宽双槽",),
    "wheel_fixed": ("03 竖轮掘进头 · 1×3",),
    "wheel_rotor": ("旋转总成 · 横轴 Z",),
}
ATLAS = ("cast_iron", "spiral_steel", "wheel_steel")
CUTTER_TINT_MARKER = 31 # consumed as a face role by StrataDrillModels, not rendered by ItemColors
SPIRAL_ELEMENT = "螺旋切削头 · 连续双槽低面试样"
# Approved B mesh: the two helical raised lands (three strips on each half after the
# shank transition) and the outer lip facets at the split point. Do not tint the
# recess/flute floor or mounting shank. Named Blockbench faces, not OBJ line positions.
SPIRAL_CUTTER_FACES = frozenset(
    {f"f{14 * ring + half + strip:04x}" for ring in range(3, 12) for half in (0, 7) for strip in (4, 5, 6)}
    | {"f00a9", "f00b2", "f00b4", "f00b6", "f00b8", "f00c1", "f00c3", "f00c5"}
)
WHEEL_TOOTH_NAMES = frozenset(f"可换截齿 {n:02d}" for n in range(1, 9))
# Blockbench OBJ exporter corner and winding order, including the downward and
# upward face UV orientation. Indices are zero-based here.
CUBE_FACES = {
    "north": ((3, 6, 4, 1), (0, 0, -1)),
    "east": ((2, 3, 1, 0), (1, 0, 0)),
    "south": ((7, 2, 0, 5), (0, 0, 1)),
    "west": ((6, 7, 5, 4), (-1, 0, 0)),
    "up": ((5, 0, 1, 4), (0, 1, 0)),
    "down": ((6, 3, 2, 7), (0, -1, 0)),
}


def add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


def scale(a, factor):
    return tuple(x * factor for x in a)


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def cross(a, b):
    x, y, z = a
    u, v, w = b
    return y * w - z * v, z * u - x * w, x * v - y * u


def unit(a):
    return scale(a, 1 / math.sqrt(dot(a, a)))


def rotate(point, angles):
    # Three.js XYZ Euler (intrinsic), Generic Blockbench project's default.
    x, y, z = point
    rx, ry, rz = (math.radians(v) for v in angles)
    cz, sz = math.cos(rz), math.sin(rz)
    x, y = cz * x - sz * y, sz * x + cz * y
    cy, sy = math.cos(ry), math.sin(ry)
    x, z = cy * x + sy * z, -sy * x + cy * z
    cx, sx = math.cos(rx), math.sin(rx)
    return x, cx * y - sx * z, sx * y + cx * z


def cross_side(a, b, top, check):
    # Compare top and check across the line through a,b, as MeshFace.getSortedVertices.
    dif = sub(b, a)
    proj = add(a, scale(dif, dot(sub(top, a), dif) / dot(dif, dif)))
    return dot(sub(proj, top), sub(check, b)) > 0


def sorted_face(vertices, ids):
    if len(ids) == 3:
        return ids
    a, b, c, d = (vertices[k] for k in ids)
    if cross_side(b, c, a, d):
        return [ids[i] for i in (2, 0, 1, 3)]
    if cross_side(a, b, c, d):
        return [ids[i] for i in (0, 2, 1, 3)]
    return ids


def element_geometry(e):
    origin = e["origin"]
    angles = e.get("rotation", (0, 0, 0))
    geom = []
    if e["type"] == "cube":
        a, b = e["from"], e["to"]
        vertices = [
            (b[0], b[1], b[2]), (b[0], b[1], a[2]), (b[0], a[1], b[2]), (b[0], a[1], a[2]),
            (a[0], b[1], a[2]), (a[0], b[1], b[2]), (a[0], a[1], a[2]), (a[0], a[1], b[2]),
        ]
        positions = [scale(add(origin, rotate(sub(v, origin), angles)), 1 / 16) for v in vertices]
        for key, face in e["faces"].items():
            texture = face.get("texture")
            if texture is None:
                continue
            vertices, normal = CUBE_FACES[key]
            u0, v0, u1, v1 = face["uv"]
            uv = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
            steps = (face.get("rotation", 0) // 90) % 4
            uv = uv[-steps:] + uv[:-steps] if steps else uv
            geom.append((key, texture, vertices, uv[::-1], rotate(normal, angles)))
    elif e["type"] == "mesh":
        vertices = e["vertices"]
        index = {key: i for i, key in enumerate(vertices)}
        positions = [scale(add(origin, rotate(v, angles)), 1 / 16) for v in vertices.values()]
        for key, face in e["faces"].items():
            texture = face.get("texture")
            if texture is None:
                continue
            ids = sorted_face(vertices, face["vertices"])
            a, b, c = (vertices[k] for k in ids[:3])
            normal = rotate(unit(cross(sub(b, a), sub(c, a))), angles)
            geom.append((key, texture, tuple(index[k] for k in ids), [face["uv"][k] for k in ids], normal))
    return positions, geom


def nums(seq):
    return " ".join(format(0 if abs(x) < 1e-14 else x, ".12g") for x in seq)


def write_obj(path, parts, elements, members, sizes):
    rows = ["# Generated by Scripts/export-stratadrill.py from the editable S04 project.", "mtllib materials.mtl"]
    stats = {}
    vindex = uvindex = normindex = 1
    for part in parts:
        if len(parts) > 1:
            # Forge makes the first entity type top-level. Start with g so the
            # element objects below it are children of this visibility group.
            rows.append(f"g {part}")
        counts = Counter()
        marked = 0
        spiral_faces = set()
        wheel_teeth = set()
        for i in members[part]:
            element = elements[i]
            position, faces = element_geometry(element)
            is_spiral = part == "spiral" and element["name"] == SPIRAL_ELEMENT
            is_tooth = part == "wheel_rotor" and element["name"] in WHEEL_TOOTH_NAMES
            if is_tooth:
                wheel_teeth.add(element["name"])
            rows.append(f"o element_{i:02}")
            rows.extend("v " + nums(v) for v in position)
            last = None
            for key, tex, ids, uv, normal in faces:
                assert len(ids) == len(uv) and len(ids) in (3, 4)
                is_spiral_cutter = is_spiral and key in SPIRAL_CUTTER_FACES
                material = ATLAS[tex]
                if is_spiral_cutter or is_tooth:
                    assert tex == (1 if is_spiral_cutter else 2)
                    if is_spiral_cutter:
                        spiral_faces.add(key)
                    marked += 1
                    material += "_cutter"
                if material != last:
                    rows.append("usemtl " + material)
                    last = material
                w, h = sizes[tex]
                rows.extend("vt " + nums((u / w, 1 - v / h)) for u, v in uv)
                rows.append("vn " + nums(normal))
                rows.append("f " + " ".join(f"{vindex + v}/{uvindex + j}/{normindex}" for j, v in enumerate(ids)))
                normindex += 1
                uvindex += len(ids)
                counts[len(ids)] += 1
            vindex += len(position)
        if part == "spiral":
            assert spiral_faces == SPIRAL_CUTTER_FACES and marked == len(SPIRAL_CUTTER_FACES), "Spiral cutter face map needs review"
        elif part == "wheel_rotor":
            assert wheel_teeth == WHEEL_TOOTH_NAMES and marked == 6 * len(WHEEL_TOOTH_NAMES), "Wheel tooth map needs review"
        stats[part] = counts, marked
    path.write_text("\n".join(rows) + "\n", encoding="ascii")
    return stats


def save_json(path, data):
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="ascii")


def obj_json(module, part):
    data = {
        "loader": "forge:obj",
        "model": f"{NS}:{MODEL_DIR}/{module}.obj",
        "automatic_culling": False,
        "flip_v": True,
        "emissive_ambient": False,
        "render_type": "minecraft:solid",
        "textures": {"particle": f"{NS}:{TEXTURE_DIR}/cast_iron"},
    }
    siblings = MODULE_PARTS[module]
    if len(siblings) > 1:
        # Forge treats omitted groups as visible, so explicitly hide siblings.
        data["visibility"] = {role: role == part for role in siblings}
    return data


def box_center(element):
    return scale(add(element["from"], element["to"]), .5)


def part_membership(model):
    names = {group["uuid"]: group["name"] for group in model["groups"]}
    elements = {element["uuid"]: i for i, element in enumerate(model["elements"])}
    outliner = {}

    def index(nodes):
        for node in nodes:
            if isinstance(node, dict):
                name = names[node["uuid"]]
                assert name not in outliner, f"Duplicate outliner group {name}"
                outliner[name] = node["children"]
                index(node["children"])

    def aggregate(nodes):
        for node in nodes:
            if isinstance(node, dict):
                yield from aggregate(node["children"])
            else:
                yield elements[node]

    index(model["outliner"])
    result = {
        role: [i for group in groups for i in aggregate(outliner[group])]
        for role, groups in PART_GROUPS.items()
    }
    rotor = set(result["wheel_rotor"])
    assert rotor.issubset(result["wheel_fixed"]), "Rotor must be part of wheel assembly"
    result["wheel_fixed"] = [i for i in result["wheel_fixed"] if i not in rotor]
    assert sorted(i for values in result.values() for i in values) == list(range(len(elements))), "Roles must cover all elements exactly once"
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("input", type=Path)
    parser.add_argument("--resources", type=Path, default=Path("src/main/resources"))
    args = parser.parse_args()
    model = json.loads(args.input.read_bytes())
    elements = model["elements"]
    parts = part_membership(model)
    assert sorted(role for roles in MODULE_PARTS.values() for role in roles) == sorted(parts)
    assert all(g["rotation"] == [0, 0, 0] for g in model["groups"]), "Expected rest pose in all groups"
    for e in elements:
        assert e["type"] in ("cube", "mesh")
        assert e.get("export", True) and not e.get("inflate") and not e.get("rescale") and not e.get("stretch")
        assert (not e.get("box_uv") if e["type"] == "cube" else e["shading"] == "flat")
    textures = model["textures"]
    assert len(textures) == len(ATLAS)
    assert [(t["uv_width"], t["uv_height"]) for t in textures] == [(128, 64), (32, 32), (32, 32)]
    assets = args.resources / "assets" / NS
    models = assets / MODEL_DIR
    texture_dir = assets / "textures" / TEXTURE_DIR
    models.mkdir(parents=True, exist_ok=True)
    texture_dir.mkdir(parents=True, exist_ok=True)
    material = []
    sizes = []
    for name, t in zip(ATLAS, textures):
        prefix, png = t["source"].split(",", 1)
        assert prefix == "data:image/png;base64", prefix
        png = base64.b64decode(png, validate=True)
        size = t["uv_width"], t["uv_height"]
        assert struct.unpack(">II", png[16:24]) == size
        sizes.append(size)
        (texture_dir / f"{name}.png").write_bytes(png)
        material.append(f"newmtl {name}\nKd 1 1 1\nforge_TintIndex -1\nmap_Kd {NS}:{TEXTURE_DIR}/{name}\n")
        if name in ("spiral_steel", "wheel_steel"):
            material.append(f"newmtl {name}_cutter\nKd 1 1 1\nforge_TintIndex {CUTTER_TINT_MARKER}\n"
                            f"map_Kd {NS}:{TEXTURE_DIR}/{name}\n")
        print(f"{name:16s} PNG {size} SHA256 {hashlib.sha256(png).hexdigest()}")
    (models / "materials.mtl").write_text("\n".join(material), encoding="ascii")
    totals = Counter()
    by_part = {}
    for module, roles in MODULE_PARTS.items():
        stats = write_obj(models / f"{module}.obj", roles, elements, parts, sizes)
        for part, (counts, marked) in stats.items():
            totals += counts
            by_part[part] = counts.total()
            print(f"{part:16s} {counts[4]:3d} quads + {counts[3]:2d} triangles, {marked} cutter faces")
    base = obj_json("body", "body")
    base["gui_light"] = "front" if model["front_gui_light"] else "side"
    base["ambientocclusion"] = model["ambientocclusion"]
    base["display"] = model["display"]
    save_json(models.with_suffix(".json"), base)
    save_json(models / "parts.json", {
        "loader": "forge:composite",
        "textures": base["textures"],
        # This model is a bake-time parts catalogue, not an item render pass.
        "item_render_order": [],
        "children": {
            part: obj_json(module, part)
            for module, roles in MODULE_PARTS.items() if module != "body"
            for part in roles
        },
    })
    print(f"Total textured outputs: {totals.total()} faces ({totals[4]} quads, {totals[3]} tris)")
    for power in ("steam", "diesel"):
        active = by_part["body"] + sum(v for k, v in by_part.items() if k.startswith(power + "_"))
        print(f"{power}: spiral {active + by_part['spiral']}, wheel {active + by_part['wheel_fixed'] + by_part['wheel_rotor']}")
    print("Rear, front grip centers (Blockbench units):", box_center(elements[7]), box_center(elements[9]))


if __name__ == "__main__":
    main()
