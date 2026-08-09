"""One-at-a-time sensitivity analysis of designer-facing knobs."""

from __future__ import annotations

from dataclasses import dataclass, replace
from typing import Callable

from .config import Scenario
from .simulation import SimulationSummary, run_simulation


@dataclass(frozen=True)
class SensitivityRow:
    parameter: str
    minus_score: float
    baseline_score: float
    plus_score: float
    local_slope: float

    def as_dict(self) -> dict[str, float | str]:
        return {
            "parameter": self.parameter,
            "minus_score": self.minus_score,
            "baseline_score": self.baseline_score,
            "plus_score": self.plus_score,
            "local_slope": self.local_slope,
        }


def pressure_score(summary: SimulationSummary) -> float:
    return (
        summary.deaths * 10000.0
        + int(summary.irreversible_collapse) * 5000.0
        + summary.fuel_stockout_days * 100.0
        + summary.food_stockout_days * 100.0
        + summary.load_shed_days * 20.0
        + summary.soft_pressure_days * 5.0
        + summary.housing_survival_degree_hours
        + summary.work_shortfall_degree_hours * 0.1
        + summary.fuel_used_fv / 100000.0
    )


def _scale_emitters(scenario: Scenario, attribute: str, factor: float) -> Scenario:
    return replace(
        scenario,
        emitters=tuple(
            replace(item, **{attribute: getattr(item, attribute) * factor})
            for item in scenario.emitters
        ),
    )


def _scale_process_output(scenario: Scenario, resource: str, factor: float) -> Scenario:
    processes = []
    for item in scenario.processes:
        outputs = dict(item.outputs_per_swe_day)
        if resource in outputs:
            outputs[resource] *= factor
        processes.append(replace(item, outputs_per_swe_day=outputs))
    return replace(scenario, processes=tuple(processes))


def _mutators() -> dict[str, Callable[[Scenario, float], Scenario]]:
    return {
        "climate.baseline_c": lambda s, f: replace(
            s, climate=replace(s.climate, baseline_c=s.climate.baseline_c * f)
        ),
        "heat_field.envelope_loss": lambda s, f: replace(
            s,
            heat_field=replace(
                s.heat_field,
                envelope_loss_fv_per_block2_c_hour=s.heat_field.envelope_loss_fv_per_block2_c_hour * f,
            ),
        ),
        "heat_field.volume_leak": lambda s, f: replace(
            s,
            heat_field=replace(
                s.heat_field,
                volume_leak_fv_per_block3_c_hour=s.heat_field.volume_leak_fv_per_block3_c_hour * f,
            ),
        ),
        "heat_field.capacitance": lambda s, f: replace(
            s,
            heat_field=replace(
                s.heat_field,
                capacitance_fv_per_block3_c=s.heat_field.capacitance_fv_per_block3_c * f,
            ),
        ),
        "network.capacity": lambda s, f: replace(
            s,
            network=replace(s.network, capacity_fv_per_hour=s.network.capacity_fv_per_hour * f),
        ),
        "emitters.radius": lambda s, f: _scale_emitters(s, "radius_blocks", f),
        "emitters.max_power": lambda s, f: _scale_emitters(s, "max_power_fv_per_hour", f),
        "emitters.efficiency": lambda s, f: replace(
            s,
            emitters=tuple(
                replace(item, efficiency=min(1.0, item.efficiency * f))
                for item in s.emitters
            ),
        ),
        "population.food_demand": lambda s, f: replace(
            s,
            population=replace(
                s.population,
                food_energy_per_fce_day=s.population.food_energy_per_fce_day * f,
            ),
        ),
        "population.cold_metabolism": lambda s, f: replace(
            s,
            population=replace(
                s.population,
                cold_food_per_fce_degree_hour=s.population.cold_food_per_fce_degree_hour * f,
            ),
        ),
        "process.fuel_output": lambda s, f: _scale_process_output(s, "fuel_fv", f),
        "process.food_output": lambda s, f: _scale_process_output(s, "food_energy", f),
    }


def run_sensitivity(
    scenario: Scenario,
    profile: str,
    policy: str,
    population: int,
    *,
    fraction: float = 0.1,
    days: int | None = None,
    seed: int | None = None,
) -> list[SensitivityRow]:
    baseline = run_simulation(
        scenario,
        profile,
        policy,
        population,
        days=days,
        seed=seed,
        collect_timeseries=False,
    )
    baseline_score = pressure_score(baseline.summary)
    rows: list[SensitivityRow] = []
    for name, mutate in _mutators().items():
        minus = run_simulation(
            mutate(scenario, 1.0 - fraction),
            profile,
            policy,
            population,
            days=days,
            seed=seed,
            collect_timeseries=False,
        )
        plus = run_simulation(
            mutate(scenario, 1.0 + fraction),
            profile,
            policy,
            population,
            days=days,
            seed=seed,
            collect_timeseries=False,
        )
        minus_score = pressure_score(minus.summary)
        plus_score = pressure_score(plus.summary)
        rows.append(
            SensitivityRow(
                parameter=name,
                minus_score=minus_score,
                baseline_score=baseline_score,
                plus_score=plus_score,
                local_slope=(plus_score - minus_score) / (2.0 * fraction),
            )
        )
    return sorted(rows, key=lambda item: abs(item.local_slope), reverse=True)
