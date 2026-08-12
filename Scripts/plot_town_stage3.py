#!/usr/bin/env python3
"""Plot stage-3 Java multi-day outputs using Figure_Guidelines.md."""

from __future__ import annotations

import argparse
import csv
import json
from collections import defaultdict
from pathlib import Path
from statistics import median

import matplotlib.pyplot as plt
from matplotlib import font_manager


BLUE = "#0072B2"
ORANGE = "#D55E00"
GREEN = "#009E73"
PURPLE = "#CC79A7"
SKY = "#56B4E9"
NEUTRAL = "#4D4D4D"
COLORS = (BLUE, ORANGE, GREEN, PURPLE)


def configure_style() -> None:
    for candidate in (
        Path("/System/Library/Fonts/Supplemental/Arial.ttf"),
        Path("/Library/Fonts/Arial.ttf"),
    ):
        if candidate.is_file():
            font_manager.fontManager.addfont(candidate)
            break
    plt.rcParams.update(
        {
            "font.family": "sans-serif",
            "font.sans-serif": ["Arial", "DejaVu Sans"],
            "font.size": 13,
            "axes.labelsize": 14,
            "axes.titlesize": 14,
            "legend.fontsize": 11,
            "xtick.labelsize": 12,
            "ytick.labelsize": 12,
            "axes.linewidth": 1.1,
            "lines.linewidth": 2.6,
            "savefig.dpi": 320,
        }
    )


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as stream:
        return list(csv.DictReader(stream))


def read_json(path: Path) -> dict:
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def percentile(values: list[float], probability: float) -> float:
    ordered = sorted(values)
    if not ordered:
        return 0.0
    position = probability * (len(ordered) - 1)
    lower = int(position)
    upper = min(lower + 1, len(ordered) - 1)
    fraction = position - lower
    return ordered[lower] * (1.0 - fraction) + ordered[upper] * fraction


def finish_axis(axis: plt.Axes) -> None:
    axis.spines["top"].set_visible(False)
    axis.spines["right"].set_visible(False)
    axis.tick_params(direction="out", width=1.0, length=5)
    axis.grid(False)


def population(report: Path) -> int:
    summary = read_json(report / "summary.json")
    return int(summary["scenario"]["population"]["standardAdults"])


def plot_feasibility(reports: list[Path], output_dir: Path) -> Path:
    frontier_rows = read_csv(reports[-1] / "frontier.csv")
    best_by_population: dict[int, float] = defaultdict(float)
    for row in frontier_rows:
        count = int(row["population"])
        best_by_population[count] = max(
            best_by_population[count], float(row["joint_coverage"])
        )

    theory_x = sorted(best_by_population)
    theory_y = [best_by_population[value] for value in theory_x]
    simulation_x: list[int] = []
    simulation_mid: list[float] = []
    simulation_low: list[float] = []
    simulation_high: list[float] = []
    for report in reports:
        count = population(report)
        rows = read_csv(report / "runs.csv")
        joint = [
            min(float(row["fuel_potential_coverage"]), float(row["food_potential_coverage"]))
            for row in rows
        ]
        simulation_x.append(count)
        simulation_mid.append(median(joint))
        simulation_low.append(percentile(joint, 0.05))
        simulation_high.append(percentile(joint, 0.95))

    figure, axis = plt.subplots(figsize=(7.8, 5.7))
    axis.plot(
        theory_x,
        theory_y,
        color=BLUE,
        label="Theory — optimal initial worker split",
    )
    axis.errorbar(
        simulation_x,
        simulation_mid,
        yerr=[
            [middle - low for middle, low in zip(simulation_mid, simulation_low)],
            [high - middle for middle, high in zip(simulation_mid, simulation_high)],
        ],
        fmt="o",
        linestyle="none",
        markersize=7.5,
        markerfacecolor="white",
        markeredgewidth=1.8,
        capsize=4,
        color=ORANGE,
        label="Simulation — current assignment, 120-day P5–P95",
    )
    axis.axhline(1.0, color=NEUTRAL, linewidth=1.5, linestyle="--", label="Self-supply boundary")
    axis.axvline(13, color=NEUTRAL, linewidth=1.3, linestyle=(0, (2, 2)), label="Continuous theory minimum (13)")
    axis.set_xlim(0, max(theory_x))
    axis.set_ylim(bottom=0)
    axis.set_xlabel("Resident population")
    axis.set_ylabel("Joint structural coverage (min of fuel and food)")
    axis.set_title("T1 coke-loop feasibility at constant 24 °C")
    axis.legend(frameon=False, loc="upper left")
    finish_axis(axis)
    figure.tight_layout()
    output = output_dir / "stage3-feasibility-frontier.png"
    figure.savefig(output, bbox_inches="tight")
    plt.close(figure)
    return output


def plot_reserves(reports: list[Path], output_dir: Path) -> Path:
    figure, axes = plt.subplots(1, 2, figsize=(13.2, 5.5))
    fuel_axis, food_axis = axes
    for color, report in zip(COLORS, reports):
        count = population(report)
        rows = read_csv(report / "daily.csv")
        days = [int(row["day"]) + 1 for row in rows]
        fuel_median = [float(row["fuel_reserve_p50"]) for row in rows]
        fuel_low = [float(row["fuel_reserve_p05"]) for row in rows]
        fuel_high = [float(row["fuel_reserve_p95"]) for row in rows]
        food_median = [float(row["food_reserve_p50"]) for row in rows]
        food_low = [float(row["food_reserve_p05"]) for row in rows]
        food_high = [float(row["food_reserve_p95"]) for row in rows]

        label = f"{count} resident" if count == 1 else f"{count} residents"
        fuel_axis.plot(days, fuel_median, color=color, label=label)
        fuel_axis.fill_between(days, fuel_low, fuel_high, color=color, alpha=0.14, linewidth=0)
        food_axis.plot(days, food_median, color=color, label=label)
        food_axis.fill_between(days, food_low, food_high, color=color, alpha=0.14, linewidth=0)

    for axis, title in ((fuel_axis, "Operational T1 fuel reserve"), (food_axis, "Edible food reserve")):
        axis.axhline(7.0, color=NEUTRAL, linewidth=1.4, linestyle="--", label="Initial/target reserve (7 days)")
        axis.set_yscale("symlog", linthresh=1.0, linscale=0.8)
        axis.set_ylim(bottom=0.0)
        axis.set_xlabel("Simulation day")
        axis.set_ylabel("Reserve (days, symlog scale)")
        axis.set_title(title)
        finish_axis(axis)
    fuel_axis.legend(frameon=False, loc="upper left", ncol=2)
    food_axis.legend(frameon=False, loc="upper left", ncol=2)
    figure.tight_layout(w_pad=3.0)
    output = output_dir / "stage3-reserve-trajectories.png"
    figure.savefig(output, bbox_inches="tight")
    plt.close(figure)
    return output


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--input",
        type=Path,
        action="append",
        required=True,
        help="Stage-3 Java report directory; repeat for each population",
    )
    parser.add_argument("--output", type=Path, required=True)
    arguments = parser.parse_args()
    reports = sorted(arguments.input, key=population)
    arguments.output.mkdir(parents=True, exist_ok=True)
    configure_style()
    for output in (
        plot_feasibility(reports, arguments.output),
        plot_reserves(reports, arguments.output),
    ):
        print(output)


if __name__ == "__main__":
    main()
