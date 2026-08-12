#!/usr/bin/env python3
"""Plot the Java stage-4 T1 population sweep using Figure_Guidelines.md."""

from __future__ import annotations

import argparse
import csv
import json
from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt
from matplotlib import font_manager


BLUE = "#0072B2"
ORANGE = "#D55E00"
GREEN = "#009E73"
PURPLE = "#CC79A7"
SKY = "#56B4E9"
YELLOW = "#E69F00"
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
            "legend.fontsize": 10.5,
            "xtick.labelsize": 12,
            "ytick.labelsize": 12,
            "axes.linewidth": 1.1,
            "lines.linewidth": 2.5,
            "savefig.dpi": 320,
        }
    )


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as stream:
        return list(csv.DictReader(stream))


def read_json(path: Path) -> dict:
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def values(rows: list[dict[str, str]], key: str) -> list[float]:
    return [float(row[key]) for row in rows]


def finish_axis(axis: plt.Axes) -> None:
    axis.spines["top"].set_visible(False)
    axis.spines["right"].set_visible(False)
    axis.tick_params(direction="out", width=1.0, length=5)
    axis.grid(False)


def save(figure: plt.Figure, output: Path, name: str) -> Path:
    output.mkdir(parents=True, exist_ok=True)
    path = output / name
    figure.savefig(path, bbox_inches="tight")
    plt.close(figure)
    return path


def simulation_band(
    axis: plt.Axes,
    x: list[float],
    rows: list[dict[str, str]],
    prefix: str,
    color: str,
    marker: str,
    label: str,
) -> None:
    middle = values(rows, f"{prefix}_p50")
    lower = values(rows, f"{prefix}_p05")
    upper = values(rows, f"{prefix}_p95")
    axis.fill_between(x, lower, upper, color=color, alpha=0.14, linewidth=0)
    axis.scatter(
        x, middle, s=20, marker=marker, facecolor="white",
        edgecolor=color, linewidth=1.15, label=label, zorder=3,
    )


def probability_band(
    axis: plt.Axes,
    x: list[float],
    rows: list[dict[str, str]],
    prefix: str,
    color: str,
    marker: str,
    label: str,
) -> None:
    probability = values(rows, f"{prefix}_probability")
    lower = values(rows, f"{prefix}_wilson_lower")
    upper = values(rows, f"{prefix}_wilson_upper")
    axis.fill_between(x, lower, upper, color=color, alpha=0.13, linewidth=0)
    axis.scatter(
        x, probability, s=20, marker=marker, facecolor="white",
        edgecolor=color, linewidth=1.15, label=label, zorder=3,
    )


def plot_capacity_and_temperature(
    rows: list[dict[str, str]], output: Path
) -> Path:
    population = values(rows, "population")
    figure, axes = plt.subplots(1, 2, figsize=(15.2, 5.4))
    geometry_axis, temperature_axis = axes

    geometry_axis.plot(
        population, values(rows, "house_coverage_fraction"),
        color=BLUE, label="House interior",
    )
    geometry_axis.plot(
        population, values(rows, "hunting_coverage_fraction"),
        color=ORANGE, linestyle="--", label="Hunting interior",
    )
    geometry_axis.axhline(
        1.0, color=NEUTRAL, linewidth=1.3, linestyle=(0, (3, 2)),
        label="Complete coverage",
    )
    geometry_axis.set_xlim(1, 200)
    geometry_axis.set_ylim(0.0, 1.03)
    geometry_axis.set_xlabel("Resident population")
    geometry_axis.set_ylabel("Interior voxels inside T1 field (fraction)")
    geometry_axis.legend(frameon=False, loc="lower left")

    simulation_band(
        temperature_axis, population, rows, "climate_serviceable",
        GREEN, "^", "Climate permits ≥0 °C at complete coverage",
    )
    simulation_band(
        temperature_axis, population, rows, "house_workable",
        BLUE, "o", "House interior within 0–40 °C",
    )
    simulation_band(
        temperature_axis, population, rows, "hunting_workable",
        ORANGE, "s", "Hunting interior at ≥0 °C",
    )
    temperature_axis.set_xlim(1, 200)
    temperature_axis.set_ylim(0.0, 1.02)
    temperature_axis.set_xlabel("Resident population")
    temperature_axis.set_ylabel("Hours meeting temperature rule (fraction, P5–P95)")
    temperature_axis.legend(frameon=False, loc="lower left")

    for axis in axes:
        finish_axis(axis)
    figure.tight_layout(w_pad=2.5)
    return save(figure, output, "stage4-t1-capacity-and-temperature.png")


def plot_self_supply(rows: list[dict[str, str]], output: Path) -> Path:
    population = values(rows, "population")
    figure, axes = plt.subplots(1, 2, figsize=(15.2, 5.4))
    fuel_axis, food_axis = axes
    panels = (
        (
            fuel_axis, "fuel_self_supply", "theory_fuel_self_supply_ratio",
            BLUE, "o", "T1 fuel potential production ÷ demand",
        ),
        (
            food_axis, "food_self_supply", "theory_food_self_supply_ratio",
            ORANGE, "s", "Edible food potential production ÷ demand",
        ),
    )
    for axis, simulation_prefix, theory_key, color, marker, ylabel in panels:
        axis.plot(
            population, values(rows, theory_key), color=color,
            label="Theory: optimal initial worker split, no cold stoppage",
        )
        simulation_band(
            axis, population, rows, simulation_prefix, color, marker,
            "Simulation: current assignment and climate",
        )
        axis.axhline(
            1.0, color=NEUTRAL, linewidth=1.4, linestyle="--",
            label="Production equals demand",
        )
        axis.axvline(
            13, color=NEUTRAL, linewidth=1.1, linestyle=(0, (2, 2)),
            label="Stage-3 continuous threshold: 13 residents",
        )
        axis.set_xlim(1, 200)
        axis.set_ylim(bottom=0.0)
        axis.set_xlabel("Resident population")
        axis.set_ylabel(ylabel)
        finish_axis(axis)
    fuel_axis.legend(frameon=False, loc="upper right")
    food_axis.legend(frameon=False, loc="upper right")
    figure.tight_layout(w_pad=4.8)
    return save(figure, output, "stage4-t1-self-supply.png")


def plot_probabilities(rows: list[dict[str, str]], output: Path) -> Path:
    population = values(rows, "population")
    figure, axes = plt.subplots(1, 2, figsize=(13.8, 5.4), sharey=True)
    shortage_axis, success_axis = axes

    probability_band(
        shortage_axis, population, rows, "fuel_shortage",
        BLUE, "o", "At least one T1 fuel shortage day",
    )
    probability_band(
        shortage_axis, population, rows, "food_shortage",
        ORANGE, "s", "At least one food shortage day",
    )
    shortage_axis.set_ylabel("Runs with shortage (probability, 95% Wilson)")
    shortage_axis.legend(frameon=False, loc="center right")

    probability_band(
        success_axis, population, rows, "survival",
        PURPLE, "D", "No resident deaths over 120 days",
    )
    probability_band(
        success_axis, population, rows, "no_shortage",
        GREEN, "^", "No deaths and no food or fuel shortage",
    )
    success_axis.axhline(
        0.95, color=NEUTRAL, linewidth=1.4, linestyle="--",
        label="95% reliability target",
    )
    success_axis.set_ylabel("Successful runs (probability, 95% Wilson)")
    success_axis.legend(frameon=False, loc="center right")

    for axis in axes:
        axis.set_xlim(1, 200)
        axis.set_ylim(0.0, 1.02)
        axis.set_xlabel("Resident population")
        finish_axis(axis)
    figure.tight_layout(w_pad=2.7)
    return save(figure, output, "stage4-t1-outcome-probabilities.png")


def plot_reserves(rows: list[dict[str, str]], output: Path) -> Path:
    by_population: dict[int, list[dict[str, str]]] = defaultdict(list)
    for row in rows:
        by_population[int(row["population"])].append(row)
    populations = sorted(by_population)
    colors = list(plt.get_cmap("tab10").colors)
    line_styles = ("-", "--", "-.", ":", "-", "--", "-.", ":", "-", "--")

    figure, axes = plt.subplots(1, 2, figsize=(15.2, 6.3))
    fuel_axis, food_axis = axes
    handles = []
    labels = []
    for index, population in enumerate(populations):
        current = sorted(by_population[population], key=lambda row: int(row["day"]))
        days = [int(row["day"]) + 1 for row in current]
        color = colors[index % len(colors)]
        width = 3.2 if population == 13 else 1.9
        alpha = 0.14 if population == 13 else 0.055
        style = line_styles[index % len(line_styles)]
        fuel_line, = fuel_axis.plot(
            days, values(current, "fuel_reserve_p50"), color=color,
            linestyle=style, linewidth=width,
        )
        fuel_axis.fill_between(
            days, values(current, "fuel_reserve_p05"),
            values(current, "fuel_reserve_p95"), color=color,
            alpha=alpha, linewidth=0,
        )
        food_axis.plot(
            days, values(current, "food_reserve_p50"), color=color,
            linestyle=style, linewidth=width,
        )
        food_axis.fill_between(
            days, values(current, "food_reserve_p05"),
            values(current, "food_reserve_p95"), color=color,
            alpha=alpha, linewidth=0,
        )
        handles.append(fuel_line)
        labels.append(f"P={population}" + (" (stage-3 threshold)" if population == 13 else ""))

    for axis, ylabel in (
        (fuel_axis, "T1 fuel reserve (days, symlog)"),
        (food_axis, "Edible food reserve (days, symlog)"),
    ):
        axis.axhline(7.0, color=NEUTRAL, linewidth=1.2, linestyle=(0, (3, 2)))
        axis.set_yscale("symlog", linthresh=1.0, linscale=0.8)
        axis.set_ylim(bottom=0.0)
        axis.set_xlim(1, 120)
        axis.set_xlabel("Simulation day")
        axis.set_ylabel(ylabel)
        finish_axis(axis)
    figure.legend(
        handles, labels, frameon=False, loc="lower center",
        bbox_to_anchor=(0.5, -0.005), ncol=5,
    )
    figure.tight_layout(w_pad=2.7, rect=(0, 0.13, 1, 1))
    return save(figure, output, "stage4-t1-reserve-trajectories.png")


def plot_thermal_limit(
    summary: dict, rows: list[dict[str, str]], output: Path
) -> Path:
    parameters = summary["parameters"]
    climate = parameters["climate"]
    generator = parameters["generatorT1"]
    location = summary["baseScenario"]["location"]
    dimension_plus_biome = (
        float(location["dimensionTemperatureCelsius"])
        + float(location["biomeTemperatureCelsius"])
    )
    heat = float(generator["temperaturePerLevelCelsius"])
    heat_multiplier = float(climate["blockHeatApplicationMultiplier"])
    alpha = float(climate["blockMaximumClimateAffection"])
    coverage = [0.0 + index / 400.0 for index in range(401)]
    climate_limit = [
        (0.0 - dimension_plus_biome - heat_multiplier * heat * value) / alpha
        for value in coverage
    ]

    figure, axis = plt.subplots(figsize=(8.1, 5.8))
    axis.plot(
        coverage, climate_limit, color=BLUE,
        label="Active normal T1 theory at 0 °C threshold",
    )
    axis.scatter(
        values(rows, "house_coverage_fraction"),
        values(rows, "house_minimum_climate_c"), s=22, marker="o",
        facecolor="white", edgecolor=ORANGE, linewidth=1.15,
        label="Compact house layouts",
    )
    axis.scatter(
        values(rows, "hunting_coverage_fraction"),
        values(rows, "hunting_minimum_climate_c"), s=22, marker="s",
        facecolor="white", edgecolor=GREEN, linewidth=1.15,
        label="Compact hunting layouts",
    )
    axis.axhline(
        -20.0, color=NEUTRAL, linewidth=1.4, linestyle="--",
        label="Complete-coverage climate limit: −20 °C",
    )
    axis.set_xlim(0.80, 1.005)
    axis.set_xlabel("Interior voxels inside T1 field (fraction)")
    axis.set_ylabel("Minimum climate temperature for 0 °C interior (°C)")
    axis.legend(frameon=False, loc="upper right")
    finish_axis(axis)
    figure.tight_layout()
    return save(figure, output, "stage4-t1-coverage-thermal-limit.png")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--input", type=Path, required=True,
        help="Stage-4 Java population-sweep report directory",
    )
    parser.add_argument("--output", type=Path, required=True)
    arguments = parser.parse_args()
    population_rows = read_csv(arguments.input / "population.csv")
    reserve_rows = read_csv(arguments.input / "reserve-trajectories.csv")
    summary = read_json(arguments.input / "summary.json")
    configure_style()
    for path in (
        plot_capacity_and_temperature(population_rows, arguments.output),
        plot_self_supply(population_rows, arguments.output),
        plot_probabilities(population_rows, arguments.output),
        plot_reserves(reserve_rows, arguments.output),
        plot_thermal_limit(summary, population_rows, arguments.output),
    ):
        print(path)


if __name__ == "__main__":
    main()
