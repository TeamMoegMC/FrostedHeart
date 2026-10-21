"""Prepare/restore ignored runtime assets for the opt-in ice-cave noise sampler.

Run after processGameTestResources and before launching the test JVM:
  python Scripts/ice_cave_sample_assets.py prepare
  python Scripts/ice_cave_sample_assets.py cleanup
No source resources, pack files, build configuration, or ordinary run worlds change.
"""
import argparse
import base64
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT.parent / "TheWinterRescue"
DATA = PACK / "kubejs/data"
OUTPUT = ROOT / "build/resources/gameTest"
MANIFEST = ROOT / "build/ice-cave-sample-assets.json"
BIOMES = ("the_winter_rescue:ice_caves", "the_winter_rescue:hydrothermal_deposits")
CATEGORIES = ("density_function", "noise", "placed_feature", "configured_feature", "configured_carver")
RESOURCE = re.compile(r"^#?([a-z0-9_.-]+):([a-z0-9_./-]+)$")


def resource_path(resource, category):
    namespace, name = resource.lstrip("#").split(":", 1)
    return DATA / namespace / category / f"{name}.json"


def strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from strings(child)


def tag_members(tag, visiting=()):
    if tag in visiting:
        return set()
    path = resource_path(tag, "tags/worldgen/biome")
    if not path.exists():
        return set()
    result = set()
    for value in json.loads(path.read_text(encoding="utf-8-sig"))["values"]:
        name = value if isinstance(value, str) else value["id"]
        result.update(tag_members(name, (*visiting, tag)) if name.startswith("#") else (name,))
    return result


def collect():
    files = {}
    pending = []
    visited = set()

    def add(path):
        if path not in visited:
            visited.add(path)
            pending.append(path)

    for biome in BIOMES:
        add(resource_path(biome, "worldgen/biome"))
    settings = DATA / "minecraft/worldgen/noise_settings/overworld.json"
    files[OUTPUT / "data/frostedheart_ice_cave_sample/worldgen/noise_settings/overworld.json"] = settings.read_bytes()
    pending.append(settings)
    visited.add(settings)
    # Keep exact vanilla overrides as well as the pack's density/noise graph.
    for namespace in ("minecraft", "the_winter_rescue"):
        for category in ("density_function", "noise"):
            for path in (DATA / namespace / "worldgen" / category).rglob("*.json"):
                add(path)

    # Project pack tag membership onto the two tested biomes. Other biome registries
    # (and their unrelated entity mods) are unnecessary for fixed-biome dimensions.
    for path in DATA.glob("*/tags/worldgen/biome/**/*.json"):
        relative = path.relative_to(DATA)
        tag = f"#{relative.parts[0]}:{path.relative_to(DATA / relative.parts[0] / 'tags/worldgen/biome').with_suffix('').as_posix()}"
        members = sorted(set(BIOMES) & tag_members(tag))
        if members:
            original = json.loads(path.read_text(encoding="utf-8-sig"))
            files[OUTPUT / "data" / relative] = json.dumps({"replace": original.get("replace", False), "values": members}).encode()

    for path in DATA.glob("*/forge/biome_modifier/*.json"):
        config = json.loads(path.read_text(encoding="utf-8-sig"))
        selectors = config.get("biomes", [])
        selectors = [selectors] if isinstance(selectors, str) else selectors
        matches = set()
        for selector in selectors:
            if isinstance(selector, str):
                matches.update(tag_members(selector) if selector.startswith("#") else (selector,))
        if matches & set(BIOMES):
            add(path)

    while pending:
        path = pending.pop()
        if path != settings:
            files[OUTPUT / "data" / path.relative_to(DATA)] = path.read_bytes()
        config = json.loads(path.read_text(encoding="utf-8-sig"))
        for value in strings(config):
            if not RESOURCE.fullmatch(value):
                continue
            if value.startswith("#"):
                candidate = resource_path(value, "tags/blocks")
                if candidate.exists():
                    add(candidate)
            else:
                for category in CATEGORIES:
                    candidate = resource_path(value, f"worldgen/{category}")
                    if candidate.exists():
                        add(candidate)

    for name, biome in zip(("ice", "hydrothermal"), BIOMES):
        dimension = {"type": "minecraft:overworld", "generator": {"type": "minecraft:noise",
                     "biome_source": {"type": "minecraft:fixed", "biome": biome},
                     "settings": "frostedheart_ice_cave_sample:overworld"}}
        files[OUTPUT / f"data/frostedheart_ice_cave_sample/dimension/{name}.json"] = json.dumps(dimension).encode()

    for name in ("immersiveengineering-server.toml", "create-server.toml"):
        files[ROOT / "run-gametest/defaultconfigs" / name] = (PACK / "defaultconfigs" / name).read_bytes()
    files[ROOT / "run-gametest/config/create-common.toml"] = (PACK / "config/create-common.toml").read_bytes()
    return files


def cleanup():
    if not MANIFEST.exists():
        print("No prepared sampler assets to restore.")
        return
    for entry in json.loads(MANIFEST.read_text()):
        path = ROOT / entry["path"]
        if entry["before"] is None:
            path.unlink(missing_ok=True)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(base64.b64decode(entry["before"]))
    MANIFEST.unlink()
    print("Restored all sampler runtime files; source resources were untouched.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("prepare", "cleanup", "list"))
    action = parser.parse_args().action
    if action == "cleanup":
        cleanup()
        return
    files = collect()
    if action == "list":
        print(json.dumps([path.relative_to(ROOT).as_posix() for path in sorted(files)], indent=2))
        return
    if MANIFEST.exists():
        cleanup()
    backup = [{"path": path.relative_to(ROOT).as_posix(),
               "before": base64.b64encode(path.read_bytes()).decode() if path.exists() else None}
              for path in files]
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(backup))
    for path, contents in files.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(contents)
    print(f"Prepared {len(files)} runtime files for conditional fixed-biome sampling.")
    print("Run namespace frostedheart_ice_cave_sample; afterward run this script with cleanup.")


if __name__ == "__main__":
    main()
