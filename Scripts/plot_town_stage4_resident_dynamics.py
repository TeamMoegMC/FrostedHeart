#!/usr/bin/env python3
"""Plot resident attribute, threshold-event, and equilibrium Monte Carlo results."""

from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt


ATTRIBUTE_SERIES = [
    ("average_health", "Health", "#d95f02"),
    ("average_mental", "Mental", "#1b9e77"),
    ("average_strength", "Strength", "#7570b3"),
    ("average_intelligence", "Intelligence", "#e7298a"),
]

NUTRITION_SERIES = [
    ("average_fat", "Fat", "#e6ab02"),
    ("average_carbohydrate", "Carbohydrate", "#66a61e"),
    ("average_protein", "Protein", "#a6761d"),
    ("average_vegetable", "Vegetable", "#1b9e77"),
]

EVENT_GROUPS = {
    "WORK_CAPACITY_LOST": ("Lost work capacity", "#7570b3"),
    "EXIT_RISK_ENTERED": ("Entered exit risk", "#e7298a"),
    "RESIDENT_EXIT": ("Resident exit", "#d73027"),
    "NUTRITION_FAT_SEVERE": ("Fat severe", "#e6ab02"),
    "NUTRITION_CARBOHYDRATE_SEVERE": ("Carbohydrate severe", "#66a61e"),
    "NUTRITION_PROTEIN_SEVERE": ("Protein severe", "#a6761d"),
    "NUTRITION_VEGETABLE_SEVERE": ("Vegetable severe", "#1b9e77"),
}


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("simulation_dir", type=Path)
    parser.add_argument("--output", type=Path)
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


def interval_by_day(
    data: list[dict[str, str]], column: str
) -> tuple[list[int], list[float], list[float], list[float]]:
    grouped: dict[int, list[float]] = defaultdict(list)
    for row in data:
        grouped[int(row["day"])].append(float(row[column]))
    days = sorted(grouped)
    return (
        days,
        [percentile(grouped[day], 0.05) for day in days],
        [percentile(grouped[day], 0.50) for day in days],
        [percentile(grouped[day], 0.95) for day in days],
    )


def plot_time_facets(
    timeline: list[dict[str, str]],
    series: list[tuple[str, str, str]],
    title: str,
    y_label: str,
    path: Path,
    dpi: int,
) -> None:
    fig, axes = plt.subplots(2, 2, figsize=(12, 7.5), sharex=True, sharey=True)
    for axis, (column, label, color) in zip(axes.flat, series):
        x, lower, middle, upper = interval_by_day(timeline, column)
        axis.fill_between(x, lower, upper, color=color, alpha=0.18,
                          label="Monte Carlo P05–P95")
        axis.plot(x, middle, color=color, linewidth=2.0, label="Median")
        axis.set_title(label)
        axis.set_ylim(0, 100)
        axis.grid(alpha=0.22)
    axes[1, 0].set_xlabel("Simulation day")
    axes[1, 1].set_xlabel("Simulation day")
    axes[0, 0].set_ylabel(y_label)
    axes[1, 0].set_ylabel(y_label)
    handles, labels = axes[0, 0].get_legend_handles_labels()
    fig.legend(handles, labels, loc="upper center", bbox_to_anchor=(0.5, 0.955),
               ncol=2, frameon=False)
    fig.suptitle(title, fontsize=15, y=0.995)
    fig.tight_layout(rect=(0, 0, 1, 0.90))
    fig.savefig(path, dpi=dpi, bbox_inches="tight")
    plt.close(fig)


def event_group(value: str) -> str | None:
    if value.startswith("RESIDENT_EXIT_"):
        return "RESIDENT_EXIT"
    return value if value in EVENT_GROUPS else None


def plot_thresholds(
    timeline: list[dict[str, str]],
    events: list[dict[str, str]],
    path: Path,
    dpi: int,
) -> None:
    trials = max(1, len({row["trial"] for row in timeline}))
    days = list(range(max(int(row["day"]) for row in timeline) + 1))
    daily: dict[str, dict[int, float]] = defaultdict(lambda: defaultdict(float))
    for row in events:
        group = event_group(row["type"])
        if group is not None:
            daily[group][int(row["day"])] += float(row["affected_count"]) / trials

    fig, (event_axis, state_axis) = plt.subplots(2, 1, figsize=(13, 8), sharex=True)
    for key, (label, color) in EVENT_GROUPS.items():
        if key in daily:
            event_axis.plot(days, [daily[key].get(day, 0.0) for day in days],
                            label=label, color=color, linewidth=1.6)
    event_axis.set_ylabel("Mean crossings / trial / day")
    event_axis.set_title("Threshold-entry events")
    event_axis.grid(alpha=0.22)
    event_axis.legend(ncol=4, frameon=False, fontsize=8)

    state_columns = [
        ("unable_to_work_count", "Unable to work", "#7570b3"),
        ("exit_risk_count", "At exit risk", "#e7298a"),
        ("severe_fat_count", "Fat severe", "#e6ab02"),
        ("severe_carbohydrate_count", "Carbohydrate severe", "#66a61e"),
        ("severe_protein_count", "Protein severe", "#a6761d"),
        ("severe_vegetable_count", "Vegetable severe", "#1b9e77"),
    ]
    for column, label, color in state_columns:
        fraction_column = f"{column}_fraction"
        state = [dict(row, **{fraction_column: str(
            float(row[column]) / max(1.0, float(row["population"])))}) for row in timeline]
        x, lower, middle, upper = interval_by_day(state, fraction_column)
        state_axis.fill_between(x, lower, upper, color=color, alpha=0.08)
        state_axis.plot(x, middle, label=label, color=color, linewidth=1.6)
    state_axis.set_ylabel("Affected resident fraction")
    state_axis.set_xlabel("Simulation day")
    state_axis.set_ylim(bottom=0)
    state_axis.set_title("Residents currently beyond thresholds")
    state_axis.grid(alpha=0.22)
    state_axis.legend(ncol=3, frameon=False, fontsize=8)
    fig.suptitle("Resident threshold events and states vs time", fontsize=15)
    fig.tight_layout(rect=(0, 0, 1, 0.96))
    fig.savefig(path, dpi=dpi, bbox_inches="tight")
    plt.close(fig)


def plot_equilibrium(population: list[dict[str, str]], path: Path, dpi: int) -> None:
    metrics = ATTRIBUTE_SERIES + NUTRITION_SERIES
    fig, axes = plt.subplots(2, 4, figsize=(16, 7.5), sharex=True, sharey=True)
    population = sorted(population, key=lambda row: int(row["population"]))
    x = [int(row["population"]) for row in population]
    for axis, (_, label, color) in zip(axes.flat, metrics):
        stem = f"equilibrium_{label.lower()}"
        lower = [float(row[f"{stem}_p05"]) for row in population]
        middle = [float(row[f"{stem}_p50"]) for row in population]
        upper = [float(row[f"{stem}_p95"]) for row in population]
        axis.fill_between(x, lower, upper,
                          color=color, alpha=0.18)
        axis.plot(x, middle, color=color, linewidth=2.0)
        axis.set_title(label)
        axis.set_ylim(0, 100)
        axis.grid(alpha=0.22)
    for axis in axes[1, :]:
        axis.set_xlabel("Initial population")
    axes[0, 0].set_ylabel("Trailing-window mean")
    axes[1, 0].set_ylabel("Trailing-window mean")
    window = int(float(population[0]["equilibrium_window_days"]))
    fig.suptitle(
        f"Resident equilibrium attributes vs population ({window}-day trailing mean; P05–P95)",
        fontsize=15,
    )
    fig.tight_layout(rect=(0, 0, 1, 0.96))
    fig.savefig(path, dpi=dpi, bbox_inches="tight")
    plt.close(fig)


def main() -> None:
    args = arguments()
    source = args.simulation_dir.resolve()
    output = (args.output or source / "plots").resolve()
    output.mkdir(parents=True, exist_ok=True)
    timeline = read_csv(source / "player-timeline-trials.csv")
    events = read_csv(source / "event-raster.csv")
    population = read_csv(source / "population.csv")

    plot_time_facets(
        timeline, ATTRIBUTE_SERIES, "Resident mean attributes vs time",
        "Resident mean attribute",
        output / "resident-attributes-vs-time.png", args.dpi,
    )
    plot_time_facets(
        timeline, NUTRITION_SERIES, "Resident mean nutrition reserves vs time",
        "Resident mean reserve",
        output / "resident-nutrition-vs-time.png", args.dpi,
    )
    plot_thresholds(
        timeline, events, output / "resident-threshold-events-vs-time.png", args.dpi,
    )
    plot_equilibrium(
        population, output / "resident-equilibrium-vs-population.png", args.dpi,
    )
    print(output)


if __name__ == "__main__":
    main()
