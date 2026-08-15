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
            label="Theory: optimal split for standard-adult reference",
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


def plot_low_tail_risk(rows: list[dict[str, str]], output: Path) -> Path:
    population = values(rows, "population")
    figure, axes = plt.subplots(1, 2, figsize=(14.8, 5.5), sharey=True)
    for axis, average_prefix, tail_prefix, ylabel in (
        (
            axes[0], "minimum_average_health", "minimum_p10_health",
            "Lowest observed health over 120 days (points, P5–P95)",
        ),
        (
            axes[1], "minimum_average_mental", "minimum_p10_mental",
            "Lowest observed mental state over 120 days (points, P5–P95)",
        ),
    ):
        simulation_band(
            axis, population, rows, average_prefix, BLUE, "o",
            "Town mean on its worst day",
        )
        simulation_band(
            axis, population, rows, tail_prefix, ORANGE, "s",
            "Resident P10 on its worst day",
        )
        axis.axhline(
            5.0, color=NEUTRAL, linewidth=1.3, linestyle="--",
            label="Current removal threshold",
        )
        axis.set_xlim(1, 200)
        axis.set_ylim(0, 100)
        axis.set_xlabel("Resident population")
        axis.set_ylabel(ylabel)
        axis.legend(frameon=False, loc="upper right")
        finish_axis(axis)
    figure.tight_layout(w_pad=2.8)
    return save(figure, output, "stage4-observable-low-tail-risk.png")


def plot_reserve_trends(rows: list[dict[str, str]], output: Path) -> Path:
    selected = {11, 12, 13, 14, 16, 24}
    by_population: dict[int, list[dict[str, str]]] = defaultdict(list)
    for row in rows:
        population = int(row["population"])
        if population in selected:
            by_population[population].append(row)
    colors = list(plt.get_cmap("tab10").colors)
    figure, axes = plt.subplots(1, 2, figsize=(15.2, 6.0))
    for index, population in enumerate(sorted(by_population)):
        current = sorted(by_population[population], key=lambda row: int(row["day"]))
        day = [int(row["day"]) + 1 for row in current]
        color = colors[index]
        width = 3.0 if population == 13 else 2.0
        for axis, prefix in (
            (axes[0], "fuel_reserve_trend"),
            (axes[1], "food_reserve_trend"),
        ):
            axis.plot(
                day, values(current, f"{prefix}_p50"), color=color,
                linewidth=width, label=f"P={population}",
            )
            axis.fill_between(
                day, values(current, f"{prefix}_p05"),
                values(current, f"{prefix}_p95"), color=color,
                alpha=0.08 if population != 13 else 0.14, linewidth=0,
            )
    for axis, ylabel in (
        (axes[0], "Daily change in T1 fuel reserve (reserve-days/day, P5–P95)"),
        (axes[1], "Daily change in edible food reserve (reserve-days/day, P5–P95)"),
    ):
        axis.axhline(0.0, color=NEUTRAL, linewidth=1.3, linestyle="--")
        axis.set_xlim(1, 120)
        axis.set_yscale("symlog", linthresh=0.25, linscale=0.8)
        axis.set_xlabel("Simulation day")
        axis.set_ylabel(ylabel)
        finish_axis(axis)
    axes[1].legend(frameon=False, loc="upper left", ncol=2)
    figure.tight_layout(w_pad=2.7)
    return save(figure, output, "stage4-observable-reserve-trends.png")


def plot_work_and_exit_risk(rows: list[dict[str, str]], output: Path) -> Path:
    population = values(rows, "population")
    figure, axes = plt.subplots(1, 2, figsize=(14.6, 5.4), sharey=True)
    simulation_band(
        axes[0], population, rows, "maximum_unable_to_work_fraction",
        BLUE, "o", "Peak residents unable to work",
    )
    simulation_band(
        axes[0], population, rows, "maximum_exit_risk_fraction",
        ORANGE, "s", "Peak residents due to exit next morning",
    )
    axes[0].set_ylabel("Population exposed at the worst daily snapshot (fraction, P5–P95)")
    axes[0].legend(frameon=False, loc="upper right")

    simulation_band(
        axes[1], population, rows, "maximum_episode_affected_fraction",
        PURPLE, "D", "Largest crisis episode",
    )
    axes[1].set_ylabel("Residents losing work capacity or exiting (fraction, P5–P95)")
    axes[1].legend(frameon=False, loc="upper right")
    for axis in axes:
        axis.set_xlim(1, 200)
        axis.set_ylim(0, 1.02)
        axis.set_xlabel("Resident population")
        finish_axis(axis)
    figure.tight_layout(w_pad=3.0)
    return save(figure, output, "stage4-observable-work-and-exit-risk.png")


def plot_event_dynamics(rows: list[dict[str, str]], output: Path) -> Path:
    population = values(rows, "population")
    figure, axes = plt.subplots(1, 2, figsize=(14.8, 5.5))
    simulation_band(
        axes[0], population, rows, "adverse_signal_rate_per_30_days",
        BLUE, "o", "Adverse threshold crossings",
    )
    simulation_band(
        axes[0], population, rows, "resident_exit_rate_per_30_days",
        ORANGE, "s", "Irreversible resident exits",
    )
    axes[0].set_yscale("symlog", linthresh=0.25, linscale=0.8)
    axes[0].set_ylabel("Threshold-crossing event rate (events per 30 days, P5–P95)")
    axes[0].legend(frameon=False, loc="upper left")

    simulation_band(
        axes[1], population, rows, "adverse_signal_fano_factor",
        BLUE, "o", "Adverse threshold crossings",
    )
    simulation_band(
        axes[1], population, rows, "resident_exit_fano_factor",
        ORANGE, "s", "Irreversible resident exits",
    )
    axes[1].axhline(
        1.0, color=NEUTRAL, linewidth=1.3, linestyle="--",
        label="Poisson variability",
    )
    axes[1].set_yscale("symlog", linthresh=1.0, linscale=0.8)
    axes[1].set_ylabel("Daily count variance ÷ mean (Fano factor, P5–P95)")
    axes[1].legend(frameon=False, loc="upper left")
    for axis in axes:
        axis.set_xlim(1, 200)
        axis.set_xlabel("Resident population")
        finish_axis(axis)
    figure.tight_layout(w_pad=3.0)
    return save(figure, output, "stage4-observable-event-dynamics.png")


def plot_warning_and_recovery(rows: list[dict[str, str]], output: Path) -> Path:
    population = values(rows, "population")
    figure, axes = plt.subplots(1, 2, figsize=(14.8, 5.5))
    axes[0].scatter(
        population, values(rows, "crisis_probability"), s=20, marker="o",
        facecolor="white", edgecolor=BLUE, linewidth=1.15,
        label="At least one crisis episode",
    )
    axes[0].scatter(
        population, values(rows, "unrecovered_episode_probability"), s=20, marker="s",
        facecolor="white", edgecolor=ORANGE, linewidth=1.15,
        label="Episode still unrecovered at day 120",
    )
    axes[0].scatter(
        population, values(rows, "prior_warning_probability_among_exit_runs"),
        s=20, marker="^", facecolor="white", edgecolor=GREEN, linewidth=1.15,
        label="Exit runs warned at least 1 day earlier",
    )
    axes[0].set_ylim(0, 1.02)
    axes[0].set_ylabel("Monte Carlo runs (probability)")
    axes[0].legend(frameon=False, loc="center right")

    simulation_band(
        axes[1], population, rows, "first_exit_warning_lead_days",
        GREEN, "^", "First warning to first exit (exit runs only)",
    )
    simulation_band(
        axes[1], population, rows, "mean_recovery_days",
        PURPLE, "D", "Episode start to 7-day reserve recovery",
    )
    axes[1].set_ylabel("Episode timing (days, P5–P95; zero if no qualifying run)")
    axes[1].legend(frameon=False, loc="upper right")
    for axis in axes:
        axis.set_xlim(1, 200)
        axis.set_xlabel("Resident population")
        finish_axis(axis)
    figure.tight_layout(w_pad=3.0)
    return save(figure, output, "stage4-observable-warning-and-recovery.png")


def representative_trial(
    timeline_rows: list[dict[str, str]], event_rows: list[dict[str, str]]
) -> tuple[int, str]:
    exit_types = {
        "RESIDENT_EXIT_HEALTH", "RESIDENT_EXIT_MENTAL", "RESIDENT_EXIT_BOTH",
    }
    first_exit: dict[int, int] = {}
    for row in event_rows:
        if row["type"] in exit_types:
            trial = int(row["trial"])
            day = int(row["day"])
            first_exit[trial] = min(day, first_exit.get(trial, day))
    if first_exit:
        ordered_days = sorted(first_exit.values())
        median_day = ordered_days[len(ordered_days) // 2]
        trial = min(first_exit, key=lambda value: (abs(first_exit[value] - median_day), value))
        return trial, f"trial {trial}; first exit near the exit-trial median"

    event_count: dict[int, int] = defaultdict(int)
    for row in event_rows:
        if row["severity"] != "INFORMATION":
            event_count[int(row["trial"])] += 1
    trials = sorted({int(row["trial"]) for row in timeline_rows})
    ordered_counts = sorted(event_count.get(trial, 0) for trial in trials)
    median_count = ordered_counts[len(ordered_counts) // 2]
    trial = min(trials, key=lambda value: (abs(event_count.get(value, 0) - median_count), value))
    return trial, f"trial {trial}; adverse-event count near the trial median"


def plot_player_timeline(
    timeline_rows: list[dict[str, str]],
    event_rows: list[dict[str, str]],
    output: Path,
) -> tuple[Path, int, str]:
    trial, reason = representative_trial(timeline_rows, event_rows)
    rows = sorted(
        (row for row in timeline_rows if int(row["trial"]) == trial),
        key=lambda row: int(row["day"]),
    )
    day = [int(row["day"]) + 1 for row in rows]
    figure, axes = plt.subplots(4, 1, figsize=(14.6, 15.2), sharex=True)

    temperature = axes[0]
    temperature.plot(day, values(rows, "morning_climate_c"), color=SKY, label="Climate at tower")
    temperature.plot(day, values(rows, "house_temperature_c"), color=BLUE, label="House interior")
    temperature.plot(day, values(rows, "hunting_temperature_c"), color=ORANGE, label="Hunting interior")
    temperature.axhline(0.0, color=NEUTRAL, linewidth=1.3, linestyle="--", label="0 °C work/safety threshold")
    temperature.set_ylabel("Morning temperature (°C)")
    temperature.legend(frameon=False, loc="lower right", ncol=2)

    reserves = axes[1]
    reserves.plot(day, values(rows, "fuel_reserve_days"), color=BLUE, label="T1 fuel")
    reserves.plot(day, values(rows, "food_reserve_days"), color=ORANGE, label="Edible food")
    reserves.axhline(3.0, color=YELLOW, linewidth=1.3, linestyle=(0, (3, 2)), label="3-day warning")
    reserves.axhline(7.0, color=GREEN, linewidth=1.3, linestyle="--", label="7-day recovery")
    reserves.set_yscale("symlog", linthresh=1.0, linscale=0.8)
    reserves.set_ylim(bottom=0.0)
    reserves.set_ylabel("Warehouse reserve (days, symlog)")
    reserves.legend(frameon=False, loc="upper left", ncol=2)

    wellbeing = axes[2]
    wellbeing.plot(day, values(rows, "average_health"), color=BLUE, label="Mean health")
    wellbeing.plot(day, values(rows, "p10_health"), color=BLUE, linestyle="--", label="P10 health")
    wellbeing.plot(day, values(rows, "average_mental"), color=PURPLE, label="Mean mental")
    wellbeing.plot(day, values(rows, "p10_mental"), color=PURPLE, linestyle="--", label="P10 mental")
    wellbeing.axhline(5.0, color=NEUTRAL, linewidth=1.3, linestyle=(0, (3, 2)), label="Exit threshold")
    wellbeing.set_ylim(0.0, 100.0)
    wellbeing.set_ylabel("Resident state (points)")
    wellbeing.legend(frameon=False, loc="upper right", ncol=2)

    residents = axes[3]
    residents.step(day, values(rows, "population"), color=NEUTRAL, where="post", label="Residents")
    residents.plot(day, values(rows, "unable_to_work_count"), color=ORANGE, label="Unable to work")
    residents.plot(day, values(rows, "exit_risk_count"), color=PURPLE, label="Due to exit next morning")
    exit_days = [
        int(row["day"]) + 1 for row in event_rows
        if int(row["trial"]) == trial and row["type"].startswith("RESIDENT_EXIT_")
    ]
    if exit_days:
        maximum = max(values(rows, "population") or [1.0])
        residents.scatter(exit_days, [maximum] * len(exit_days), marker="x", s=65,
                          color="#000000", linewidth=1.8, label="Resident exit")
    residents.set_ylabel("Residents (count)")
    residents.set_xlabel("Simulation day")
    residents.legend(frameon=False, loc="upper right", ncol=2)

    for axis in axes:
        axis.set_xlim(1, max(day))
        finish_axis(axis)
    figure.tight_layout(h_pad=1.3)
    return save(figure, output, "stage4-player-town-history.png"), trial, reason


def event_category(event_type: str) -> str:
    if event_type.startswith("RESIDENT_EXIT_"):
        return "Resident exit"
    if event_type in {"WORK_CAPACITY_LOST", "EXIT_RISK_ENTERED"}:
        return "Resident threshold"
    if event_type.startswith("FOOD_") or event_type.startswith("FUEL_"):
        return "Resource threshold"
    return "Climate or thermal threshold"


def plot_event_raster(
    rows: list[dict[str, str]], trial_count: int, simulated_days: int, output: Path
) -> Path:
    styles = {
        "Climate or thermal threshold": (SKY, "|"),
        "Resource threshold": (YELLOW, "o"),
        "Resident threshold": (PURPLE, "s"),
        "Resident exit": ("#000000", "x"),
    }
    adverse = [row for row in rows if row["severity"] != "INFORMATION"]
    figure, axis = plt.subplots(figsize=(14.6, 8.2))
    for category, (color, marker) in styles.items():
        current = [row for row in adverse if event_category(row["type"]) == category]
        axis.scatter(
            [int(row["day"]) + 1 for row in current],
            [int(row["trial"]) + 1 for row in current],
            s=12 if marker != "|" else 22, marker=marker, color=color,
            linewidth=0.7, alpha=0.78, label=category,
        )
    axis.set_xlim(1, simulated_days)
    axis.set_ylim(0.5, trial_count + 0.5)
    axis.set_xlabel("Simulation day")
    axis.set_ylabel("Monte Carlo trial")
    axis.legend(frameon=False, loc="upper right", ncol=2)
    finish_axis(axis)
    figure.tight_layout()
    return save(figure, output, "stage4-player-event-raster.png")


def plot_initial_resident_heterogeneity(
    rows: list[dict[str, str]], summary: dict, output: Path
) -> Path:
    labels = ["Infant", "Child", "Adult", "Elder"]
    observed_counts = [0, 0, 0, 0]
    for row in rows:
        observed_counts[int(row["age_group"])] += 1
    total = max(1, sum(observed_counts))
    observed = [count / total for count in observed_counts]
    weights = summary["parameters"]["residents"]["generation"]["ageWeights"]
    expected_raw = [float(weights[key]) for key in ("infant", "child", "adult", "elder")]
    expected_total = sum(expected_raw)
    expected = [value / expected_total for value in expected_raw]

    figure, axes = plt.subplots(1, 2, figsize=(15.0, 5.8))
    age_axis, attribute_axis = axes
    positions = list(range(4))
    age_axis.bar([value - 0.18 for value in positions], expected, width=0.36,
                 color=SKY, label="Configured probability")
    age_axis.bar([value + 0.18 for value in positions], observed, width=0.36,
                 color=ORANGE, label="Simulated residents")
    age_axis.set_xticks(positions, labels)
    age_axis.set_ylim(0.0, 0.7)
    age_axis.set_ylabel("Initial residents (fraction)")
    age_axis.legend(frameon=False, loc="upper left")

    attribute_axis.hist(values(rows, "strength"), bins=35, density=True,
                        histtype="step", color=BLUE, linewidth=2.5, label="Strength")
    attribute_axis.hist(values(rows, "intelligence"), bins=35, density=True,
                        histtype="step", color=PURPLE, linewidth=2.5, label="Intelligence")
    attribute_axis.hist(values(rows, "mining_proficiency"), bins=35, density=True,
                        histtype="step", color=ORANGE, linewidth=2.2, label="Mining proficiency")
    attribute_axis.hist(values(rows, "hunting_proficiency"), bins=35, density=True,
                        histtype="step", color=GREEN, linewidth=2.2, label="Hunting proficiency")
    attribute_axis.set_xlim(0.0, 100.0)
    attribute_axis.set_xlabel("Initial resident value (points)")
    attribute_axis.set_ylabel("Probability density")
    attribute_axis.legend(frameon=False, loc="upper right")
    for axis in axes:
        finish_axis(axis)
    figure.tight_layout(w_pad=3.0)
    return save(figure, output, "stage4-player-initial-residents.png")


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
    timeline_rows = read_csv(arguments.input / "player-timeline-trials.csv")
    event_rows = read_csv(arguments.input / "event-raster.csv")
    initial_resident_rows = read_csv(arguments.input / "initial-residents.csv")
    summary = read_json(arguments.input / "summary.json")
    configure_style()
    timeline_path, trial, reason = plot_player_timeline(
        timeline_rows, event_rows, arguments.output,
    )
    print(f"{timeline_path} ({reason})")
    for path in (
        plot_event_raster(
            event_rows,
            int(summary["runsPerPopulation"]),
            int(summary["days"]),
            arguments.output,
        ),
        plot_initial_resident_heterogeneity(initial_resident_rows, summary, arguments.output),
        plot_capacity_and_temperature(population_rows, arguments.output),
        plot_self_supply(population_rows, arguments.output),
        plot_probabilities(population_rows, arguments.output),
        plot_reserves(reserve_rows, arguments.output),
        plot_thermal_limit(summary, population_rows, arguments.output),
        plot_low_tail_risk(population_rows, arguments.output),
        plot_reserve_trends(reserve_rows, arguments.output),
        plot_work_and_exit_risk(population_rows, arguments.output),
        plot_event_dynamics(population_rows, arguments.output),
        plot_warning_and_recovery(population_rows, arguments.output),
    ):
        print(path)


if __name__ == "__main__":
    main()
