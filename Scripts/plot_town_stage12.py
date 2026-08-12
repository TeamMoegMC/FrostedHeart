#!/usr/bin/env python3
"""Plot stage-1/2 Java simulator diagnostics using repository figure rules."""

from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt
from matplotlib import font_manager


BLUE = "#0072B2"
ORANGE = "#D55E00"
GREEN = "#009E73"
PURPLE = "#CC79A7"
NEUTRAL = "#4D4D4D"


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


def finish_axis(axis: plt.Axes) -> None:
    axis.spines["top"].set_visible(False)
    axis.spines["right"].set_visible(False)
    axis.tick_params(direction="out", width=1.0, length=5)
    axis.grid(False)


def plot_production(input_dir: Path, output_dir: Path) -> Path:
    mining_rows = read_csv(input_dir / "mining-t1-sweep.csv")
    hunting_rows = read_csv(input_dir / "hunting-processing-sweep.csv")
    grouped: dict[str, list[dict[str, str]]] = defaultdict(list)
    for row in mining_rows:
        grouped[row["fuel"]].append(row)

    figure, axes = plt.subplots(1, 2, figsize=(13.2, 5.4))
    left, right = axes

    fuel_styles = {
        "coal": (BLUE, "o", "Coal"),
        "coke": (ORANGE, "s", "Coke (1 coal → 1 coke)"),
    }
    for fuel in ("coal", "coke"):
        rows = grouped[fuel]
        x = [float(row["base_output_items_per_swe_day"]) for row in rows]
        theory = [float(row["theory_required_mining_swe"]) for row in rows]
        simulation = [float(row["simulation_required_mining_swe"]) for row in rows]
        color, marker, label = fuel_styles[fuel]
        left.plot(x, theory, color=color, label=f"Theory — {label}")
        left.errorbar(
            x,
            simulation,
            yerr=[0.0] * len(x),
            fmt=marker,
            linestyle="none",
            markersize=6.5,
            markerfacecolor="white",
            markeredgewidth=1.6,
            color=color,
            label=f"Simulation — {label}",
        )
    left.axvline(3.5, color=NEUTRAL, linewidth=1.5, linestyle="--", label="Current 3.5")
    left.set_xlabel("Total mine output rate (items per mining SWE-day)")
    left.set_ylabel("Mining labor required (SWE)")
    left.set_title("T1 fuel labor")
    left.legend(frameon=False, loc="upper right")
    finish_axis(left)

    capacity = [float(row["processing_capacity_meat_per_day"]) for row in hunting_rows]
    theory_support = [float(row["theory_supported_residents_per_hunting_swe"]) for row in hunting_rows]
    simulation_support = [
        float(row["simulation_mean_supported_residents_per_hunting_swe"]) for row in hunting_rows
    ]
    ci95 = [float(row["simulation_ci95_supported_residents"]) for row in hunting_rows]
    right.plot(capacity, theory_support, color=GREEN, label="Theory")
    right.errorbar(
        capacity,
        simulation_support,
        yerr=ci95,
        fmt="o",
        linestyle="none",
        markersize=6.5,
        markerfacecolor="white",
        markeredgewidth=1.6,
        capsize=3.5,
        color=PURPLE,
        label="Simulation (95% CI)",
    )
    right.axhline(theory_support[0], color=NEUTRAL, linewidth=1.4, linestyle="--", label="All raw")
    right.axhline(theory_support[-1], color=ORANGE, linewidth=1.4, linestyle="--", label="All cooked")
    right.set_xlabel("Raw-meat processing capacity (items/day)")
    right.set_ylabel("Residents supported per hunting SWE")
    right.set_title("Meat-processing leverage")
    right.legend(frameon=False, loc="upper left")
    finish_axis(right)

    figure.tight_layout(w_pad=3.0)
    output = output_dir / "stage12-production-balance.png"
    figure.savefig(output, bbox_inches="tight")
    plt.close(figure)
    return output


def plot_house(input_dir: Path, output_dir: Path) -> Path:
    temperature_rows = read_csv(input_dir / "house-temperature-sweep.csv")
    food_rows = read_csv(input_dir / "house-food-sweep.csv")
    figure, axes = plt.subplots(1, 2, figsize=(13.2, 5.4), sharey=True)
    left, right = axes

    temperature = [float(row["temperature_celsius"]) for row in temperature_rows]
    safe_minimum = float(temperature_rows[0]["safe_minimum_temperature_celsius"])
    safe_maximum = float(temperature_rows[0]["safe_maximum_temperature_celsius"])
    for metric, color, marker, label in (
        ("health", BLUE, "o", "Health"),
        ("mental", ORANGE, "s", "Mental"),
    ):
        theory = [float(row[f"theory_{metric}_delta"]) for row in temperature_rows]
        simulation = [float(row[f"simulation_{metric}_delta"]) for row in temperature_rows]
        left.plot(temperature, theory, color=color, label=f"Theory — {label}")
        left.errorbar(
            temperature,
            simulation,
            yerr=[0.0] * len(temperature),
            fmt=marker,
            linestyle="none",
            markersize=6.0,
            markerfacecolor="white",
            markeredgewidth=1.5,
            color=color,
            label=f"Simulation — {label}",
        )
    left.axhline(0.0, color=NEUTRAL, linewidth=1.4, linestyle="--")
    left.axvline(
        safe_minimum,
        color=NEUTRAL,
        linewidth=1.4,
        linestyle="--",
        label=f"Direct-stress bounds ({safe_minimum:g}–{safe_maximum:g} °C)",
    )
    left.axvline(safe_maximum, color=NEUTRAL, linewidth=1.4, linestyle="--")
    left.axvline(
        24.0,
        color=NEUTRAL,
        linewidth=1.3,
        linestyle=(0, (2, 2)),
        label="Comfort point (24 °C)",
    )
    left.set_xlabel("House temperature (°C)")
    left.set_ylabel("Resident change per day (points/day)")
    left.set_title("Temperature response at full food")
    left.legend(
        frameon=False,
        loc="upper center",
        bbox_to_anchor=(0.5, -0.20),
        ncol=3,
    )
    finish_axis(left)

    satisfaction = [float(row["food_satisfaction"]) for row in food_rows]
    food_penalty_exponent = float(food_rows[0]["food_deficit_penalty_exponent"])
    for metric, color, marker, label in (
        ("health", BLUE, "o", "Health"),
        ("mental", ORANGE, "s", "Mental"),
    ):
        theory = [float(row[f"theory_{metric}_delta"]) for row in food_rows]
        simulation = [float(row[f"simulation_{metric}_delta"]) for row in food_rows]
        right.plot(satisfaction, theory, color=color, label=f"Theory — {label}")
        right.errorbar(
            satisfaction,
            simulation,
            yerr=[0.0] * len(satisfaction),
            fmt=marker,
            linestyle="none",
            markersize=6.0,
            markerfacecolor="white",
            markeredgewidth=1.5,
            color=color,
            label=f"Simulation — {label}",
        )
    right.axhline(0.0, color=NEUTRAL, linewidth=1.4, linestyle="--")
    right.set_xlabel("Food satisfaction (fraction of daily requirement)")
    right.set_title(f"Food response at 24 °C (penalty exponent {food_penalty_exponent:g})")
    right.legend(frameon=False, loc="lower right")
    finish_axis(right)

    figure.tight_layout(w_pad=3.0)
    output = output_dir / "stage12-house-response.png"
    figure.savefig(output, bbox_inches="tight")
    plt.close(figure)
    return output


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, required=True, help="Java simulation report directory")
    parser.add_argument("--output", type=Path, required=True, help="Directory for PNG figures")
    arguments = parser.parse_args()
    arguments.output.mkdir(parents=True, exist_ok=True)
    configure_style()
    for output in (
        plot_production(arguments.input, arguments.output),
        plot_house(arguments.input, arguments.output),
    ):
        print(output)


if __name__ == "__main__":
    main()
