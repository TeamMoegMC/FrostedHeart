"""Compare final ore blocks from paired conditional samples, including all globally added ore/geodes.

python Scripts/ice_cave_sample_report.py build/ice-cave-natural-samples.json
The equal-volume fixed-biome comparison is separate from normal-preset landscape screenshots.
"""
import argparse
import json
from pathlib import Path

ORES = ("pyrite", "pyrolusite", "zinc", "lead", "silver")


def compare(samples):
    paired = {}
    for sample in samples:
        paired.setdefault(sample["seed"], {})[sample["fixed_biome"]] = sample
    results = []
    totals = {biome: {ore: 0 for ore in ORES} for biome in ("ice", "hydrothermal")}
    for seed, pair in paired.items():
        if not all(biome in pair for biome in totals):
            continue
        ice, hydro = pair["ice"], pair["hydrothermal"]
        volume_keys = ("interior_chunks", "min_y_inclusive", "max_y_exclusive")
        if any(ice[key] != hydro[key] for key in volume_keys):
            raise ValueError(f"Seed {seed} does not compare equal volume/heights")
        row = {"seed": seed, "ores": {}}
        for ore in ORES:
            left, right = ice["counts"][ore], hydro["counts"][ore]
            for biome, count in (("ice", left), ("hydrothermal", right)):
                totals[biome][ore] += count
            row["ores"][ore] = {"ice_final_blocks": left, "hydrothermal_final_blocks": right,
                                "ratio": left / right if right else None,
                                "within_two_times": left <= 2 * right if ore != "pyrolusite" and right else None}
        results.append(row)
    aggregate = {}
    for ore in ORES:
        left, right = totals["ice"][ore], totals["hydrothermal"][ore]
        aggregate[ore] = {"ice_final_blocks": left, "hydrothermal_final_blocks": right,
                          "ratio": left / right if right else None,
                          "within_two_times": left <= 2 * right if ore != "pyrolusite" and right else None}
    return {"scope": "Paired same-volume, same-height fixed-biome final blocks; includes global veins and hydrothermal geodes. "
                     "Not normal-world biome occurrence. Pyrolusite has no hydrothermal counterpart.",
            "paired_seeds": len(results), "per_seed": results, "aggregate": aggregate}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    args = parser.parse_args()
    result = compare(json.loads(args.input.read_text(encoding="utf-8-sig")))
    output = args.input.with_name(args.input.stem + "-ore-comparison.json")
    output.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps(result, indent=2))
    print(f"Wrote {output}")


if __name__ == "__main__":
    main()
