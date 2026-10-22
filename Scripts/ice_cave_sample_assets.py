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
CATEGORIES = ("density_function", "noise", "placed_feature", "configured_feature", "configured_carver",
              "biome", "noise_settings", "structure", "structure_set", "processor_list", "template_pool")
RESOURCE = re.compile(r"^#?([a-z0-9_.-]+):([a-z0-9_./-]+)$")
BIOME_TAG_SCRIPT = PACK / "kubejs/server_scripts/src/tags/biome_tags.js"


def literal_biome_tag_additions():
    """Read the pack's literal KubeJS biome tag callback without needing KubeJS on a GameTest server."""
    source = BIOME_TAG_SCRIPT.read_text(encoding="utf-8-sig")
    source = re.sub(r"//[^\n]*|/\*.*?\*/", "", source, flags=re.S)
    wrapper = re.fullmatch(r'''\s*ServerEvents\.tags\(\s*(["'])worldgen/biome\1\s*,\s*\(\s*event\s*\)\s*=>\s*\{(.*)\}\s*\)\s*;?\s*''', source, re.S)
    if not wrapper:
        raise ValueError(f"Unsupported biome tag script wrapper in {BIOME_TAG_SCRIPT}; expected one worldgen/biome literal callback")
    body = wrapper.group(2)
    statement = re.compile(r'''event\.add\(\s*(["'])([a-z0-9_.-]+:[a-z0-9_./-]+)\1\s*,\s*\[([^\]]*)\]\s*\)\s*;?''', re.S)
    additions = []
    end = 0
    for match in statement.finditer(body):
        if body[end:match.start()].strip():
            raise ValueError(f"Unsupported statement in {BIOME_TAG_SCRIPT}: {body[end:match.start()].strip()[:120]}")
        members = []
        for value in match.group(3).split(","):
            value = value.strip()
            if not value:
                continue
            literal = re.fullmatch(r'''(["'])([a-z0-9_.-]+:[a-z0-9_./-]+)\1''', value)
            if not literal:
                raise ValueError(f"Unsupported nonliteral biome member in {BIOME_TAG_SCRIPT}: {value}")
            members.append(literal.group(2))
        additions.append((match.group(2), members))
        end = match.end()
    if body[end:].strip() or not additions:
        raise ValueError(f"Unsupported or empty biome tag callback in {BIOME_TAG_SCRIPT}")
    return additions


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


def collect(normal_preset=False, client=False, keep_stone_age_spawns=False, runtime=None, client_mods=()):
    files = {}
    pending = []
    visited = set()

    def add(path):
        if path not in visited:
            visited.add(path)
            pending.append(path)

    active_biomes = set(BIOMES)

    def project_biome_tag(tag):
        path = resource_path(tag, "tags/worldgen/biome")
        original = json.loads(path.read_text(encoding="utf-8-sig")) if path.exists() else {}
        members = sorted(biome for biome in tag_members(tag)
                         if biome in active_biomes or normal_preset and biome.startswith("minecraft:"))
        return {"replace": original.get("replace", False), "values": members}

    preset = DATA / "minecraft/worldgen/world_preset/normal.json"
    if normal_preset:
        preset_contents = preset.read_bytes()
        preset_data = json.loads(preset_contents.decode("utf-8-sig"))
        active_biomes.update(entry["biome"] for entry in preset_data["dimensions"]["minecraft:overworld"]
                             ["generator"]["biome_source"]["biomes"])
        files[OUTPUT / "data/minecraft/worldgen/world_preset/normal.json"] = preset_contents
    for biome in active_biomes:
        path = resource_path(biome, "worldgen/biome")
        if path.exists():
            add(path)
    settings = DATA / "minecraft/worldgen/noise_settings/overworld.json"
    settings_contents = settings.read_bytes()
    files[OUTPUT / "data/frostedheart_ice_cave_sample/worldgen/noise_settings/overworld.json"] = settings_contents
    if normal_preset:
        files[OUTPUT / "data/minecraft/worldgen/noise_settings/overworld.json"] = settings_contents
    add(settings)
    # Keep exact vanilla overrides as well as the pack's density/noise graph.
    for namespace in ("minecraft", "the_winter_rescue"):
        for category in ("density_function", "noise"):
            for path in (DATA / namespace / "worldgen" / category).rglob("*.json"):
                add(path)

    # Host/carver tags are also queried by Java and by OreConfiguration.tag without a '#' prefix.
    # Include the pack's actual block-tag overrides, rather than relying on JSON string reachability.
    for path in DATA.glob("*/tags/blocks/**/*.json"):
        add(path)

    # Project pack tags onto the selected registries. Normal mode retains every
    # preset biome and all reachable pack resources, rather than two fixed biomes.
    for path in DATA.glob("*/tags/worldgen/biome/**/*.json"):
        relative = path.relative_to(DATA)
        tag = f"#{relative.parts[0]}:{path.relative_to(DATA / relative.parts[0] / 'tags/worldgen/biome').with_suffix('').as_posix()}"
        config = project_biome_tag(tag)
        # Empty replacement tags intentionally disable a mod's default membership.
        # Vanilla biome holders are registered even when absent from this preset.
        if config["values"] or config["replace"]:
            files[OUTPUT / "data" / relative] = json.dumps(config).encode()

    if not normal_preset:
        materialized = []
        for tag, additions in literal_biome_tag_additions():
            original_path = resource_path(tag, "tags/worldgen/biome")
            destination = OUTPUT / "data" / original_path.relative_to(DATA)
            if destination in files:
                config = json.loads(files[destination])
            else:
                config = project_biome_tag(tag)
            config["values"] = list(dict.fromkeys([*config["values"], *(biome for biome in additions if biome in active_biomes)]))
            files[destination] = json.dumps(config).encode()
            materialized.append({"tag": tag, "file": destination.relative_to(ROOT).as_posix(), "result": config,
                                 "original_script_member_count": len(additions)})
        files[ROOT / "build/ice-cave-sample-biome-tag-adaptations.json"] = json.dumps({
            "source": str(BIOME_TAG_SCRIPT.resolve()), "scope": "Literal event.add biome tags projected to the two fixed sample biomes; original additions and static replace semantics retained",
            "selected_biomes": sorted(active_biomes), "materialized": materialized}, indent=2).encode()

    for path in DATA.glob("*/forge/biome_modifier/*.json"):
        config = json.loads(path.read_text(encoding="utf-8-sig"))
        selectors = config.get("biomes", [])
        selectors = [selectors] if isinstance(selectors, str) else selectors
        matches = set()
        for selector in selectors:
            if isinstance(selector, str):
                matches.update(tag_members(selector) if selector.startswith("#") else (selector,))
        if matches & active_biomes:
            add(path)

    removed_spawns = []
    while pending:
        path = pending.pop()
        contents = path.read_bytes()
        config = json.loads(contents.decode("utf-8-sig"))
        if path != settings:
            if normal_preset and not keep_stone_age_spawns and "spawners" in config:
                for category, entries in config["spawners"].items():
                    removed_spawns.extend({"biome_file": path.relative_to(DATA).as_posix(), "category": category,
                                           "entry": entry} for entry in entries if entry["type"].startswith("stone_age:"))
                    config["spawners"][category] = [entry for entry in entries if not entry["type"].startswith("stone_age:")]
                contents = json.dumps(config).encode()
            files[OUTPUT / "data" / path.relative_to(DATA)] = contents
        for value in strings(config):
            if not RESOURCE.fullmatch(value):
                continue
            if value.startswith("#"):
                candidate = resource_path(value, "tags/blocks")
                if candidate.exists():
                    add(candidate)
            else:
                for category in CATEGORIES:
                    if category == "biome" and not normal_preset:
                        continue
                    candidate = resource_path(value, f"worldgen/{category}")
                    if candidate.exists():
                        add(candidate)

    for name, biome in zip(("ice", "hydrothermal"), BIOMES):
        dimension = {"type": "minecraft:overworld", "generator": {"type": "minecraft:noise",
                     "biome_source": {"type": "minecraft:fixed", "biome": biome},
                     "settings": "frostedheart_ice_cave_sample:overworld"}}
        files[OUTPUT / f"data/frostedheart_ice_cave_sample/dimension/{name}.json"] = json.dumps(dimension).encode()

    if normal_preset:
        files[OUTPUT / "data/frostedheart_ice_cave_sample/dimension/normal.json"] = json.dumps(
            preset_data["dimensions"]["minecraft:overworld"]).encode()
        files[ROOT / "build/ice-cave-normal-assets-adaptations.json"] = json.dumps({
            "worldgen": "Original TWR normal preset, noise settings, referenced biomes/features and applicable modifiers",
            "dependency_adaptation": "Only absent stone_age mob spawn references removed; world generation unchanged",
            "removed_spawns": removed_spawns}, indent=2).encode()

    runtime = Path(runtime) if runtime is not None else Path("build/ice-cave-client-runtime" if client else "run-gametest")
    runtime = (runtime if runtime.is_absolute() else ROOT / runtime).resolve()
    for name in ("immersiveengineering-server.toml", "create-server.toml"):
        files[runtime / "defaultconfigs" / name] = (PACK / "defaultconfigs" / name).read_bytes()
    files[runtime / "config/create-common.toml"] = (PACK / "config/create-common.toml").read_bytes()
    if not client:
        # Same minimal research fixture as the ordinary GameTest runtime; no pack gameplay is exercised here.
        # It avoids adding client-only KubeJS dependencies to the conditional terrain sampler.
        files[runtime / "config/fhresearches/root.json"] = (ROOT / "src/gametest/resources/fixtures/ice_cave_research/root.json").read_bytes()
        files[runtime / "gameteststructures/phase0a_empty.snbt"] = (ROOT / "src/gametest/resources/phase0a_empty.snbt").read_bytes()
    if client:
        # The integrated server validates the real pack research catalogue before world startup.
        # Preserve every definition verbatim; an empty placeholder would not represent this pack.
        for path in (PACK / "config/fhresearches").rglob("*"):
            if path.is_file():
                files[runtime / "config/fhresearches" / path.relative_to(PACK / "config/fhresearches")] = path.read_bytes()
        # Catalogue icons/ingredients refer to the pack's real KubeJS items (e.g. servo_core/motor).
        # Load the original registry script rather than changing research definitions or inventing placeholders.
        item_registry = Path("kubejs/startup_scripts/src/registries/item.js")
        files[runtime / item_registry] = (PACK / item_registry).read_bytes()
        # The biome exclusions stop FH's global modifier from appending a second
        # freeze_top_layer to pack biomes that already define it. Block tags also
        # carry the pack's actual moss-replacement terrain contract.
        for name in ("biome_tags.js", "block_tags.js"):
            script = Path("kubejs/server_scripts/src/tags") / name
            files[runtime / script] = (PACK / script).read_bytes()
        for mod in client_mods:
            path = Path(mod).resolve()
            files[runtime / "mods" / path.name] = path.read_bytes()
    return files


def manifest_path(path):
    """Allow an explicit runtime outside this repository without changing source destinations."""
    try:
        return path.relative_to(ROOT).as_posix()
    except ValueError:
        return str(path)


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
    parser.add_argument("--normal-preset", action="store_true", help="Include exact TWR multi-noise normal preset and referenced biomes")
    parser.add_argument("--client", action="store_true", help="Prepare isolated build/ice-cave-client-runtime configs")
    parser.add_argument("--runtime", type=Path, help="Runtime directory, absolute or relative to this repository; overrides the default")
    parser.add_argument("--keep-stone-age-spawns", action="store_true", help="Use only when stone_age is installed in the development client")
    parser.add_argument("--client-mod", action="append", default=[], help="Existing local dependency JAR to load in the isolated client only")
    args = parser.parse_args()
    action = args.action
    if action == "cleanup":
        cleanup()
        return
    files = collect(args.normal_preset, args.client, args.keep_stone_age_spawns, args.runtime, args.client_mod)
    if action == "list":
        print(json.dumps([manifest_path(path) for path in sorted(files)], indent=2))
        return
    if MANIFEST.exists():
        cleanup()
    backup = [{"path": manifest_path(path),
               "before": base64.b64encode(path.read_bytes()).decode() if path.exists() else None}
              for path in files if path not in (ROOT / "build/ice-cave-normal-assets-adaptations.json",
                                                ROOT / "build/ice-cave-sample-biome-tag-adaptations.json")]
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(backup))
    for path, contents in files.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(contents)
    print(f"Prepared {len(files)} runtime files; normal preset={args.normal_preset}, client={args.client}.")
    print("Run namespace frostedheart_ice_cave_sample; afterward run this script with cleanup.")


if __name__ == "__main__":
    main()
