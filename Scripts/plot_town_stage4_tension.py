#!/usr/bin/env python3
"""Plot the 24-resident stage-4 tension experiment using Figure_Guidelines.md."""

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
SKY = "#56B4E9"
YELLOW = "#E69F00"
NEUTRAL = "#4D4D4D"
STRATEGY_COLORS = {"fixed": ORANGE, "forecast": BLUE}


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
            "lines.linewidth": 2.2,
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


def save(figure: plt.Figure, output: Path, name: str) -> Path:
    output.mkdir(parents=True, exist_ok=True)
    path = output / name
    figure.savefig(path, bbox_inches="tight")
    plt.close(figure)
    return path


def select_trial(run_rows: list[dict[str, str]]) -> int:
    paired: dict[int, dict[str, dict[str, str]]] = defaultdict(dict)
    for row in run_rows:
        paired[int(row["trial"])][row["strategy"]] = row
    rescued: list[tuple[int, int]] = []
    stable: list[tuple[float, int]] = []
    for trial, strategies in paired.items():
        if set(strategies) != {"fixed", "forecast"}:
            continue
        fixed = strategies["fixed"]
        forecast = strategies["forecast"]
        both_started = fixed["burn_in_survived"] == "true" and forecast["burn_in_survived"] == "true"
        if not both_started:
            continue
        fixed_exits = int(fixed["measured_exits"])
        forecast_exits = int(forecast["measured_exits"])
        if fixed_exits > 0 and forecast_exits == 0:
            lead = int(fixed["warning_lead_days"])
            rescued.append((lead if lead >= 0 else 10_000, trial))
        difference = float(fixed["danger_zone_fraction"]) - float(
            forecast["danger_zone_fraction"]
        )
        stable.append((difference, trial))
    if rescued:
        rescued.sort()
        return rescued[len(rescued) // 2][1]
    if stable:
        return max(stable)[1]
    return min(paired)


def spans(rows: list[dict[str, str]], key: str) -> list[tuple[int, int]]:
    days = sorted(int(row["day"]) for row in rows if row[key] == "true")
    if not days:
        return []
    result: list[tuple[int, int]] = []
    start = previous = days[0]
    for day in days[1:]:
        if day != previous + 1:
            result.append((start, previous + 1))
            start = day
        previous = day
    result.append((start, previous + 1))
    return result


def plot_player_history(
    run_rows: list[dict[str, str]],
    daily_rows: list[dict[str, str]],
    output: Path,
) -> Path:
    trial = select_trial(run_rows)
    selected = [row for row in daily_rows if int(row["trial"]) == trial]
    by_strategy = {
        strategy: sorted(
            (row for row in selected if row["strategy"] == strategy),
            key=lambda row: int(row["day"]),
        )
        for strategy in ("fixed", "forecast")
    }
    figure, axes = plt.subplots(5, 1, figsize=(13.5, 15.5), sharex=True)
    temperature_axis, food_axis, fuel_axis, resident_axis, population_axis = axes

    fixed = by_strategy["fixed"]
    days = [int(row["day"]) + 1 for row in fixed]
    temperature_axis.plot(
        days,
        [float(row["morning_climate_c"]) for row in fixed],
        color=NEUTRAL,
        linewidth=1.4,
        linestyle="--",
        label="Morning climate",
    )
    for strategy in ("fixed", "forecast"):
        rows = by_strategy[strategy]
        x = [int(row["day"]) + 1 for row in rows]
        color = STRATEGY_COLORS[strategy]
        label = "Fixed T1" if strategy == "fixed" else "Forecast-driven T1"
        temperature_axis.plot(
            x,
            [float(row["house_temperature_c"]) for row in rows],
            color=color,
            label=f"{label}: house",
        )
        temperature_axis.plot(
            x,
            [float(row["hunting_temperature_c"]) for row in rows],
            color=color,
            linewidth=1.3,
            linestyle=":" if strategy == "fixed" else "-.",
            label=f"{label}: hunting",
        )
        food_axis.plot(
            x,
            [float(row["food_reserve_days"]) for row in rows],
            color=color,
            label=label,
        )
        fuel_axis.plot(
            x,
            [float(row["fuel_reserve_days"]) for row in rows],
            color=color,
            label=label,
        )
        resident_axis.plot(
            x,
            [float(row["average_health"]) for row in rows],
            color=color,
            label=f"{label}: mean health",
        )
        resident_axis.plot(
            x,
            [float(row["average_mental"]) for row in rows],
            color=color,
            linestyle="--",
            label=f"{label}: mean mental",
        )
        population_axis.step(
            x,
            [int(row["population"]) for row in rows],
            where="post",
            color=color,
            label=label,
        )
    for start, end in spans(by_strategy["forecast"], "overdrive"):
        for axis in axes:
            axis.axvspan(start + 1, end + 1, color=SKY, alpha=0.08, linewidth=0)

    temperature_axis.axhline(0.0, color=NEUTRAL, linewidth=1.1, linestyle=(0, (3, 2)))
    temperature_axis.set_ylabel("Temperature (°C)")
    temperature_axis.legend(frameon=False, loc="lower right", ncol=2)
    for axis, ylabel in (
        (food_axis, "Food reserve (days)"),
        (fuel_axis, "T1 reserve at current mode (days)"),
    ):
        axis.axhspan(3.0, 7.0, color=YELLOW, alpha=0.11, linewidth=0)
        axis.axhline(3.0, color=NEUTRAL, linewidth=1.0, linestyle=":")
        axis.axhline(7.0, color=NEUTRAL, linewidth=1.0, linestyle="--")
        axis.set_ylabel(ylabel)
        axis.set_ylim(bottom=0.0)
        axis.legend(frameon=False, loc="best")
    resident_axis.axhline(10.0, color=NEUTRAL, linewidth=1.0, linestyle="--")
    resident_axis.set_ylabel("Resident state (0–100)")
    resident_axis.set_ylim(0.0, 100.0)
    resident_axis.legend(frameon=False, loc="best", ncol=2)
    population_axis.set_ylabel("Residents")
    population_axis.set_xlabel("Measured simulation day")
    population_axis.set_ylim(0.0, 25.0)
    population_axis.legend(frameon=False, loc="best")
    for axis in axes:
        axis.set_xlim(1, 120)
        finish_axis(axis)
    figure.tight_layout(h_pad=0.8)
    return save(figure, output, "stage4-t1-24-player-history.png")


def plot_event_raster(rows: list[dict[str, str]], output: Path) -> Path:
    categories = {
        "FORECAST_SEVERE": (SKY, "|", "Severe forecast begins"),
        "FOOD_RESERVE_BELOW_7_DAYS": (YELLOW, "s", "Food reserve <7 days"),
        "FUEL_RESERVE_BELOW_7_DAYS": (PURPLE, "D", "T1 reserve <7 days"),
        "HUNTING_TEMPERATURE_STOP": (ORANGE, "x", "Hunting stops"),
        "EXIT_RISK_ENTERED": (NEUTRAL, "^", "Exit risk begins"),
        "RESIDENT_EXIT": (BLUE, "o", "Resident exits"),
    }
    normalized: list[dict[str, object]] = []
    for row in rows:
        event_type = row["type"]
        if event_type.startswith("RESIDENT_EXIT_"):
            event_type = "RESIDENT_EXIT"
        if event_type not in categories:
            continue
        normalized.append(
            {
                "strategy": row["strategy"],
                "trial": int(row["trial"]),
                "day": int(row["day"]) + 1,
                "type": event_type,
            }
        )
    all_trials = sorted({int(row["trial"]) for row in rows})
    displayed_trials = all_trials[:: max(1, len(all_trials) // 200)]
    displayed_set = set(displayed_trials)
    trial_lane = {trial: index + 1 for index, trial in enumerate(displayed_trials)}
    normalized = [row for row in normalized if int(row["trial"]) in displayed_set]
    lanes = len(displayed_trials)
    figure, axes = plt.subplots(1, 2, figsize=(15.0, 7.0), sharex=True, sharey=True)
    for axis, strategy in zip(axes, ("fixed", "forecast")):
        for event_type, (color, marker, label) in categories.items():
            current = [
                row
                for row in normalized
                if row["strategy"] == strategy and row["type"] == event_type
            ]
            if not current:
                continue
            axis.scatter(
                [int(row["day"]) for row in current],
                [trial_lane[int(row["trial"])] for row in current],
                s=18 if marker != "|" else 28,
                marker=marker,
                color=color,
                linewidth=0.9,
                alpha=0.78,
                label=label,
            )
        axis.set_xlim(1, 120)
        axis.set_ylim(0, lanes + 1)
        axis.set_xlabel("Measured simulation day")
        axis.set_title("Fixed T1" if strategy == "fixed" else "Forecast-driven T1")
        finish_axis(axis)
    axes[0].set_ylabel("Every fifth paired trial")
    handles, labels = axes[0].get_legend_handles_labels()
    second_handles, second_labels = axes[1].get_legend_handles_labels()
    for handle, label in zip(second_handles, second_labels):
        if label not in labels:
            handles.append(handle)
            labels.append(label)
    figure.legend(
        handles,
        labels,
        frameon=False,
        loc="lower center",
        bbox_to_anchor=(0.5, -0.01),
        ncol=3,
    )
    figure.tight_layout(w_pad=2.0, rect=(0, 0.10, 1, 1))
    return save(figure, output, "stage4-t1-24-event-raster.png")


def grid(rows: list[dict[str, str]], strategy: str, key: str) -> tuple[list[int], list[int], list[list[float]]]:
    selected = [row for row in rows if row["strategy"] == strategy]
    mines = sorted({int(row["requested_mine_capacity"]) for row in selected})
    hunts = sorted({int(row["actual_hunt_capacity"]) for row in selected})
    lookup = {
        (int(row["requested_mine_capacity"]), int(row["actual_hunt_capacity"])): float(row[key])
        for row in selected
    }
    matrix = [[lookup[(mine, hunt)] for mine in mines] for hunt in hunts]
    return mines, hunts, matrix


def plot_capacity_map(rows: list[dict[str, str]], output: Path) -> Path:
    figure, axes = plt.subplots(2, 2, figsize=(12.5, 10.5), sharex=True, sharey=True)
    panels = (
        (axes[0, 0], "fixed", "full_survival_probability", "Fixed: no exits", 0.0, 1.0),
        (axes[0, 1], "forecast", "full_survival_probability", "Forecast: no exits", 0.0, 1.0),
        (axes[1, 0], "fixed", "danger_zone_fraction_mean", "Fixed: days in 3–7 day reserve zone", 0.0, None),
        (axes[1, 1], "forecast", "danger_zone_fraction_mean", "Forecast: days in 3–7 day reserve zone", 0.0, None),
    )
    danger_max = max(float(row["danger_zone_fraction_mean"]) for row in rows)
    for axis, strategy, key, title, minimum, maximum in panels:
        mines, hunts, matrix = grid(rows, strategy, key)
        upper = maximum if maximum is not None else max(0.01, danger_max)
        image = axis.imshow(
            matrix,
            origin="lower",
            aspect="auto",
            interpolation="nearest",
            cmap="cividis",
            vmin=minimum,
            vmax=upper,
        )
        axis.set_xticks(range(len(mines)), labels=mines)
        axis.set_yticks(range(len(hunts)), labels=hunts)
        axis.set_title(title)
        for y, current in enumerate(matrix):
            for x, value in enumerate(current):
                axis.text(
                    x,
                    y,
                    f"{value:.2f}",
                    ha="center",
                    va="center",
                    color="white" if value > 0.55 * upper else "black",
                    fontsize=11,
                )
        colorbar = figure.colorbar(image, ax=axis, fraction=0.046, pad=0.04)
        colorbar.ax.tick_params(direction="out", labelsize=11)
        finish_axis(axis)
    for axis in axes[1, :]:
        axis.set_xlabel("Mine resident capacity")
    for axis in axes[:, 0]:
        axis.set_ylabel("Hunting resident capacity")
    figure.tight_layout(h_pad=2.2, w_pad=1.8)
    return save(figure, output, "stage4-t1-24-capacity-map.png")


def plot_strategy_tradeoff(
    rows: list[dict[str, str]], output: Path, mine_capacity: int, hunt_capacity: int
) -> Path:
    selected = [
        row
        for row in rows
        if int(row["requested_mine_capacity"]) == mine_capacity
        and int(row["actual_hunt_capacity"]) == hunt_capacity
    ]
    selected.sort(key=lambda row: row["strategy"])
    strategies = [row["strategy"] for row in selected]
    labels = ["Fixed T1" if value == "fixed" else "Forecast-driven T1" for value in strategies]
    x = list(range(len(selected)))
    figure, axes = plt.subplots(1, 2, figsize=(14.5, 5.8))
    probability_axis, cost_axis = axes
    probability_series = (
        ("full_survival_probability", BLUE, "o", "No exits"),
        ("food_shortage_probability", ORANGE, "s", "Food shortage"),
        ("fuel_shortage_probability", PURPLE, "D", "T1 shortage"),
        ("soft_instability_probability", GREEN, "^", "Any soft instability"),
    )
    for key, color, marker, label in probability_series:
        probability_axis.scatter(
            x,
            [float(row[key]) for row in selected],
            s=78,
            marker=marker,
            facecolor="white",
            edgecolor=color,
            linewidth=1.8,
            label=label,
            zorder=3,
        )
    probability_axis.axhline(0.95, color=NEUTRAL, linewidth=1.2, linestyle="--")
    probability_axis.set_xticks(x, labels=labels)
    probability_axis.set_ylim(0.0, 1.03)
    probability_axis.set_ylabel("Paired trials (probability)")
    probability_axis.legend(frameon=False, loc="center left")

    fuel = [float(row["loaded_fuel_items_mean"]) for row in selected]
    baseline = fuel[0] if fuel and fuel[0] > 0 else 1.0
    cost_axis.scatter(
        x,
        [value / baseline for value in fuel],
        s=84,
        marker="o",
        facecolor="white",
        edgecolor=BLUE,
        linewidth=1.8,
        label="T1 fuel use ÷ fixed fuel use",
    )
    cost_axis.scatter(
        x,
        [float(row["overdrive_days_mean"]) / 120.0 for row in selected],
        s=84,
        marker="s",
        facecolor="white",
        edgecolor=ORANGE,
        linewidth=1.8,
        label="Days overdriven (fraction)",
    )
    cost_axis.scatter(
        x,
        [float(row["danger_zone_fraction_mean"]) for row in selected],
        s=84,
        marker="D",
        facecolor="white",
        edgecolor=PURPLE,
        linewidth=1.8,
        label="Days in 3–7 day reserve zone (fraction)",
    )
    cost_axis.axhline(1.0, color=NEUTRAL, linewidth=1.2, linestyle="--")
    cost_axis.set_xticks(x, labels=labels)
    cost_axis.set_ylim(bottom=0.0)
    cost_axis.set_ylabel("Relative cost or measured-day fraction")
    cost_axis.legend(frameon=False, loc="best")
    for axis in axes:
        finish_axis(axis)
    figure.tight_layout(w_pad=3.0)
    return save(figure, output, "stage4-t1-24-strategy-tradeoff.png")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--mine-capacity", type=int, default=8)
    parser.add_argument("--hunt-capacity", type=int, default=4)
    args = parser.parse_args()
    configure_style()
    capacity_rows = read_csv(args.input / "capacity-grid.csv")
    run_rows = read_csv(args.input / "detailed-runs.csv")
    daily_rows = read_csv(args.input / "player-timeline-trials.csv")
    event_rows = read_csv(args.input / "event-raster.csv")
    generated = [
        plot_player_history(run_rows, daily_rows, args.output),
        plot_event_raster(event_rows, args.output),
        plot_capacity_map(capacity_rows, args.output),
        plot_strategy_tradeoff(
            capacity_rows, args.output, args.mine_capacity, args.hunt_capacity
        ),
    ]
    for path in generated:
        print(path)


if __name__ == "__main__":
    main()
