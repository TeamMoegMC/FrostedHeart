#!/usr/bin/env python3
"""Compare fixed-population resident nutrition experiments."""

from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt


ATTRIBUTE_SERIES = [
    ("average_health", "Health"),
    ("average_mental", "Mental"),
    ("average_strength", "Strength"),
    ("average_intelligence", "Intelligence"),
]

NUTRITION_SERIES = [
    ("average_fat", "Fat"),
    ("average_carbohydrate", "Carbohydrate"),
    ("average_protein", "Protein"),
    ("average_vegetable", "Vegetable"),
]

SEVERE_SERIES = [
    ("severe_fat_count", "Fat", "#e6ab02"),
    ("severe_carbohydrate_count", "Carbohydrate", "#66a61e"),
    ("severe_protein_count", "Protein", "#a6761d"),
    ("severe_vegetable_count", "Vegetable", "#1b9e77"),
]

SCENARIO_COLORS = ["#d73027", "#7570b3", "#1b9e77", "#e6ab02", "#66a61e"]


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "datasets", nargs="+", metavar="LABEL=SIMULATION_DIR",
        help="Display label and Stage 4 output directory.",
    )
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--dpi", type=int, default=180)
    return parser.parse_args()


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as stream:
        return list(csv.DictReader(stream))


def percentile(values: list[float], probability: float) -> float:
    ordered = sorted(values)
    if not ordered:
        return 0.0
    position = probability * (len(ordered) - 1)
    lower = int(position)
    upper = min(len(ordered) - 1, lower + 1)
    fraction = position - lower
    return ordered[lower] * (1.0 - fraction) + ordered[upper] * fraction


def median_by_day(rows: list[dict[str, str]], column: str) -> tuple[list[int], list[float]]:
    grouped: dict[int, list[float]] = defaultdict(list)
    for row in rows:
        grouped[int(row["day"])].append(float(row[column]))
    days = sorted(grouped)
    return days, [percentile(grouped[day], 0.5) for day in days]


def parse_datasets(values: list[str]) -> list[tuple[str, list[dict[str, str]]]]:
    result = []
    for value in values:
        if "=" not in value:
            raise ValueError(f"Dataset must use LABEL=DIR: {value}")
        label, raw_path = value.split("=", 1)
        result.append((label, read_csv(
            Path(raw_path).resolve() / "player-timeline-trials.csv")))
    return result


def plot_metric_facets(
    datasets: list[tuple[str, list[dict[str, str]]]],
    metrics: list[tuple[str, str]],
    title: str,
    y_label: str,
    output: Path,
    dpi: int,
    nutrition_guides: bool = False,
) -> None:
    fig, axes = plt.subplots(2, 2, figsize=(12.5, 7.8), sharex=True, sharey=True)
    for axis, (column, metric_label) in zip(axes.flat, metrics):
        for index, (label, rows) in enumerate(datasets):
            days, values = median_by_day(rows, column)
            axis.plot(days, values, color=SCENARIO_COLORS[index],
                      linewidth=2.0, label=label)
        if nutrition_guides:
            axis.axhline(20, color="#666666", linewidth=0.8, linestyle=":")
            axis.axhline(70, color="#666666", linewidth=0.8, linestyle="--")
        axis.set_title(metric_label)
        axis.set_ylim(0, 100)
        axis.grid(alpha=0.22)
    for axis in axes[1, :]:
        axis.set_xlabel("Simulation day")
    axes[0, 0].set_ylabel(y_label)
    axes[1, 0].set_ylabel(y_label)
    handles, labels = axes[0, 0].get_legend_handles_labels()
    fig.legend(handles, labels, loc="upper center", bbox_to_anchor=(0.5, 0.955),
               ncol=2, frameon=False)
    fig.suptitle(title, fontsize=15, y=0.995)
    fig.tight_layout(rect=(0, 0, 1, 0.88))
    fig.savefig(output, dpi=dpi, bbox_inches="tight")
    plt.close(fig)


def plot_severe_states(
    datasets: list[tuple[str, list[dict[str, str]]]],
    output: Path,
    dpi: int,
) -> None:
    fig, axes = plt.subplots(2, 2, figsize=(12.5, 7.8), sharex=True, sharey=True)
    for axis, (scenario_label, rows) in zip(axes.flat, datasets):
        for column, label, color in SEVERE_SERIES:
            derived = []
            for row in rows:
                value = float(row[column]) / max(1.0, float(row["population"]))
                derived.append(dict(row, severe_fraction=str(value)))
            days, values = median_by_day(derived, "severe_fraction")
            axis.plot(days, values, color=color, linewidth=1.8, label=label)
        axis.set_title(scenario_label)
        axis.set_ylim(0, 1.02)
        axis.grid(alpha=0.22)
    for axis in axes[1, :]:
        axis.set_xlabel("Simulation day")
    axes[0, 0].set_ylabel("Severe resident fraction")
    axes[1, 0].set_ylabel("Severe resident fraction")
    handles, labels = axes[0, 0].get_legend_handles_labels()
    fig.legend(handles, labels, loc="upper center", bbox_to_anchor=(0.5, 0.955),
               ncol=4, frameon=False)
    fig.suptitle("Severe nutrition states vs time", fontsize=15, y=0.995)
    fig.tight_layout(rect=(0, 0, 1, 0.90))
    fig.savefig(output, dpi=dpi, bbox_inches="tight")
    plt.close(fig)


def main() -> None:
    args = arguments()
    datasets = parse_datasets(args.datasets)
    if len(datasets) > len(SCENARIO_COLORS):
        raise ValueError(f"At most {len(SCENARIO_COLORS)} datasets are supported.")
    args.output.mkdir(parents=True, exist_ok=True)
    plot_metric_facets(
        datasets, ATTRIBUTE_SERIES, "Resident attributes: P50 nutrition comparison",
        "Median resident mean attribute",
        args.output / "resident-attributes-vs-time-comparison.png", args.dpi,
    )
    plot_metric_facets(
        datasets, NUTRITION_SERIES, "Nutrition reserves: P50 model comparison",
        "Median resident mean reserve",
        args.output / "resident-nutrition-vs-time-comparison.png", args.dpi, True,
    )
    plot_severe_states(
        datasets, args.output / "resident-severe-states-vs-time-comparison.png", args.dpi,
    )
    print(args.output.resolve())


if __name__ == "__main__":
    main()
