"""CSV, JSON, and self-contained HTML reports."""

from __future__ import annotations

import base64
import csv
from dataclasses import asdict
from io import BytesIO
import json
import os
from pathlib import Path
import tempfile
from typing import Iterable, Mapping, Sequence

_MATPLOTLIB_CACHE = Path(tempfile.gettempdir()) / "frostedheart-town-matplotlib"
_MATPLOTLIB_CACHE.mkdir(parents=True, exist_ok=True)
os.environ.setdefault("MPLCONFIGDIR", str(_MATPLOTLIB_CACHE))
os.environ.setdefault("XDG_CACHE_HOME", str(_MATPLOTLIB_CACHE))

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

from .optimization import LayoutCandidate
from .sensitivity import SensitivityRow
from .simulation import MonteCarloResult, SimulationResult


def _write_csv(path: Path, rows: Sequence[Mapping[str, object]]) -> None:
    if not rows:
        path.write_text("", encoding="utf-8")
        return
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def _figure_data_url(figure: plt.Figure) -> str:
    buffer = BytesIO()
    figure.savefig(buffer, format="png", dpi=140, bbox_inches="tight")
    plt.close(figure)
    encoded = base64.b64encode(buffer.getvalue()).decode("ascii")
    return f"data:image/png;base64,{encoded}"


def _timeseries_figure(result: SimulationResult) -> str | None:
    series = result.timeseries
    if not series:
        return None
    days = series["hour"] / 24.0
    figure, axes = plt.subplots(3, 1, figsize=(11, 9), sharex=True)
    axes[0].plot(days, series["ambient_c"], label="ambient", color="#5b8ff9")
    axes[0].plot(days, series["housing_c"], label="housing", color="#f6bd16")
    for key, values in series.items():
        if key.startswith("field_"):
            axes[0].plot(days, values, label=key.removeprefix("field_"), alpha=0.55)
    axes[0].axhline(0.0, color="#999", linewidth=0.8)
    axes[0].set_ylabel("temperature / C")
    axes[0].legend(ncol=4, fontsize=8)

    axes[1].plot(days, series["fuel_fv"], label="fuel FV", color="#e8684a")
    axes[1].plot(days, series["food_energy"], label="food energy", color="#6dc8ec")
    axes[1].set_ylabel("inventory")
    axes[1].legend()

    axes[2].plot(days, series["health"], label="health", color="#5ad8a6")
    axes[2].plot(days, series["mental"], label="mental", color="#9270ca")
    axes[2].set_ylim(0, 105)
    axes[2].set_ylabel("resident state")
    axes[2].set_xlabel("game day")
    axes[2].legend()
    figure.suptitle(
        f"{result.summary.profile} / {result.summary.policy} / "
        f"population {result.summary.population_start} / seed {result.summary.seed}"
    )
    figure.tight_layout()
    return _figure_data_url(figure)


def _aggregate_figure(rows: Sequence[Mapping[str, object]]) -> str | None:
    if not rows:
        return None
    labels = [
        f"{row['profile']}\n{row['policy']}\nP{row['population']}" for row in rows
    ]
    survival = [float(row["survival_rate"]) for row in rows]
    pressure = [float(row["soft_pressure_rate"]) for row in rows]
    x = np.arange(len(rows))
    figure, axis = plt.subplots(figsize=(max(9, len(rows) * 1.1), 4.5))
    axis.bar(x - 0.2, survival, width=0.4, label="survival rate", color="#5ad8a6")
    axis.bar(x + 0.2, pressure, width=0.4, label="soft-pressure rate", color="#f6bd16")
    axis.set_ylim(0, 1.05)
    axis.set_xticks(x, labels, fontsize=8)
    axis.set_ylabel("run fraction")
    axis.legend()
    figure.tight_layout()
    return _figure_data_url(figure)


def write_simulation_report(
    output_dir: Path,
    results: Sequence[MonteCarloResult],
    *,
    title: str,
) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)
    aggregate = [result.aggregate() for result in results]
    all_runs = [asdict(summary) for result in results for summary in result.summaries]
    (output_dir / "summary.json").write_text(
        json.dumps(aggregate, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    _write_csv(output_dir / "runs.csv", all_runs)
    _write_csv(output_dir / "aggregate.csv", aggregate)

    representative = next(
        (result.representative for result in results if result.representative is not None),
        None,
    )
    if representative is not None:
        keys = list(representative.timeseries)
        time_rows = [
            {key: representative.timeseries[key][index] for key in keys}
            for index in range(len(representative.timeseries[keys[0]]))
        ]
        _write_csv(output_dir / "timeseries.csv", time_rows)
    aggregate_plot = _aggregate_figure(aggregate)
    time_plot = _timeseries_figure(representative) if representative else None
    table_rows = "".join(
        "<tr>"
        f"<td>{row['profile']}</td><td>{row['policy']}</td><td>{row['population']}</td>"
        f"<td>{float(row['survival_rate']):.1%}</td>"
        f"<td>{float(row['soft_pressure_rate']):.1%}</td>"
        f"<td>{float(row['collapse_rate']):.1%}</td>"
        f"<td>{float(row['stockout_or_unsafe_rate']):.1%}</td>"
        f"<td>{float(row['mean_fuel_headroom']):.1%}</td>"
        f"<td>{float(row['mean_food_headroom']):.1%}</td>"
        "</tr>"
        for row in aggregate
    )
    images = "".join(
        f'<img src="{image}" alt="simulation chart">'
        for image in (aggregate_plot, time_plot)
        if image
    )
    html = f"""<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>{title}</title>
<style>
body{{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;max-width:1200px;margin:2rem auto;padding:0 1rem;color:#20242a}}
table{{border-collapse:collapse;width:100%;font-size:14px}}th,td{{border:1px solid #d8dde6;padding:.45rem;text-align:right}}th:first-child,td:first-child{{text-align:left}}img{{max-width:100%;display:block;margin:1.5rem auto}}code{{background:#f3f5f7;padding:.15rem .3rem}}
</style></head><body><h1>{title}</h1>
<p>模型输出使用游戏单位。原始逐次结果见 <code>runs.csv</code>，聚合值见 <code>summary.json</code>。</p>
<table><thead><tr><th>profile</th><th>policy</th><th>population</th><th>survival</th><th>soft pressure</th><th>collapse</th><th>stockout / unsafe</th><th>fuel headroom</th><th>food headroom</th></tr></thead>
<tbody>{table_rows}</tbody></table>{images}</body></html>"""
    report_path = output_dir / "report.html"
    report_path.write_text(html, encoding="utf-8")
    return report_path


def write_layout_report(output_dir: Path, candidates: Sequence[LayoutCandidate]) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)
    rows = [item.as_dict() for item in candidates]
    (output_dir / "layouts.json").write_text(
        json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    flat = [
        {key: value for key, value in row.items() if key not in {"buildings", "emitters"}}
        for row in rows
    ]
    _write_csv(output_dir / "layout_pareto.csv", flat)
    if candidates:
        figure, axis = plt.subplots(figsize=(7, 5))
        axis.scatter(
            [item.steady_heat_loss_fv_per_hour for item in candidates],
            [item.minimum_coverage for item in candidates],
            c=[item.overlap_ratio for item in candidates],
            s=[max(30.0, item.footprint_blocks2 / 10.0) for item in candidates],
            cmap="viridis_r",
        )
        axis.set_xlabel("steady heat-loss proxy / FV per hour")
        axis.set_ylabel("minimum building coverage")
        axis.set_ylim(0, 1.05)
        axis.set_title("Layout Pareto front (colour = overlap, size = footprint)")
        figure.tight_layout()
        image = _figure_data_url(figure)
    else:
        image = ""
    report = output_dir / "layout_report.html"
    report.write_text(
        f"<!doctype html><meta charset='utf-8'><title>Layout Pareto front</title>"
        f"<h1>Layout Pareto front</h1><p>{len(candidates)} non-dominated candidates.</p>"
        + (f"<img style='max-width:100%' src='{image}'>" if image else ""),
        encoding="utf-8",
    )
    return report


def write_sensitivity_report(output_dir: Path, rows: Sequence[SensitivityRow]) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)
    data = [item.as_dict() for item in rows]
    _write_csv(output_dir / "sensitivity.csv", data)
    (output_dir / "sensitivity.json").write_text(
        json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    figure, axis = plt.subplots(figsize=(9, max(4, len(rows) * 0.45)))
    ordered = list(reversed(rows))
    axis.barh(
        [item.parameter for item in ordered],
        [item.local_slope for item in ordered],
        color=["#e8684a" if item.local_slope > 0 else "#5ad8a6" for item in ordered],
    )
    axis.axvline(0, color="#555", linewidth=0.8)
    axis.set_xlabel("pressure-score change for a 100% parameter change")
    axis.set_title("Local one-at-a-time sensitivity")
    figure.tight_layout()
    image = _figure_data_url(figure)
    report = output_dir / "sensitivity_report.html"
    report.write_text(
        f"<!doctype html><meta charset='utf-8'><title>Sensitivity</title>"
        f"<h1>Sensitivity</h1><img style='max-width:100%' src='{image}'>",
        encoding="utf-8",
    )
    return report
