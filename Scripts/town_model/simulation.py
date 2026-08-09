"""Coupled climate, thermal, population, inventory, and production simulation."""

from __future__ import annotations

from dataclasses import asdict, dataclass, field
import math
from typing import Iterable, Mapping, Sequence

import numpy as np

from .climate import (
    forecast_levels,
    generate_design_climate,
    generate_stochastic_climate,
)
from .compat import current_block_temperature, current_house_settles
from .config import BuildingSpec, PolicySpec, ProcessSpec, Scenario
from .geometry import (
    coverage_patterns,
    heat_footprint,
    mean_environment_temperature,
)
from .thermal import (
    ThermalControl,
    building_rc_step,
    current_compat_step,
    target_rc_step,
)


VALID_PROFILES = {"current_compat", "target_rc"}


@dataclass(frozen=True)
class SimulationSummary:
    profile: str
    policy: str
    population_start: int
    population_end: int
    days: int
    seed: int
    deaths: int
    irreversible_collapse: bool
    soft_pressure: bool
    soft_pressure_days: int
    load_shed_days: int
    fuel_stockout_days: int
    food_stockout_days: int
    minimum_fuel_fv: float
    minimum_food_energy: float
    minimum_health: float
    minimum_mental: float
    fuel_used_fv: float
    fuel_produced_fv: float
    food_produced: float
    mean_power_headroom: float
    mean_normal_power_headroom: float
    mean_fuel_headroom: float
    mean_food_headroom: float
    mean_normal_fuel_headroom: float
    mean_normal_food_headroom: float
    housing_comfort_degree_hours: float
    housing_survival_degree_hours: float
    work_shortfall_degree_hours: float
    mean_heat_coverage: float
    heat_overlap_ratio: float
    final_fuel_reserve_days: float
    final_food_reserve_days: float
    final_fuel_fv: float
    final_food_energy: float
    final_nutrition: float
    recovery_days_after_design_cold: float | None


@dataclass
class SimulationResult:
    summary: SimulationSummary
    timeseries: dict[str, np.ndarray] = field(default_factory=dict)


@dataclass
class MonteCarloResult:
    summaries: list[SimulationSummary]
    representative: SimulationResult | None

    def aggregate(self) -> dict[str, float | int | str]:
        if not self.summaries:
            return {"runs": 0}
        values = self.summaries
        return {
            "profile": values[0].profile,
            "policy": values[0].policy,
            "population": values[0].population_start,
            "days": values[0].days,
            "runs": len(values),
            "survival_rate": float(np.mean([item.deaths == 0 for item in values])),
            "collapse_rate": float(np.mean([item.irreversible_collapse for item in values])),
            "stockout_or_unsafe_rate": float(
                np.mean(
                    [
                        item.fuel_stockout_days > 0
                        or item.food_stockout_days > 0
                        or item.housing_survival_degree_hours > 0.0
                        for item in values
                    ]
                )
            ),
            "soft_pressure_rate": float(np.mean([item.soft_pressure for item in values])),
            "mean_deaths": float(np.mean([item.deaths for item in values])),
            "median_minimum_fuel_fv": float(np.median([item.minimum_fuel_fv for item in values])),
            "p05_minimum_fuel_fv": float(np.quantile([item.minimum_fuel_fv for item in values], 0.05)),
            "median_minimum_health": float(np.median([item.minimum_health for item in values])),
            "mean_fuel_used_fv": float(np.mean([item.fuel_used_fv for item in values])),
            "mean_soft_pressure_days": float(np.mean([item.soft_pressure_days for item in values])),
            "mean_load_shed_days": float(np.mean([item.load_shed_days for item in values])),
            "mean_power_headroom": float(np.mean([item.mean_power_headroom for item in values])),
            "mean_normal_power_headroom": float(
                np.mean([item.mean_normal_power_headroom for item in values])
            ),
            "mean_fuel_headroom": float(np.mean([item.mean_fuel_headroom for item in values])),
            "mean_food_headroom": float(np.mean([item.mean_food_headroom for item in values])),
            "mean_normal_fuel_headroom": float(
                np.mean([item.mean_normal_fuel_headroom for item in values])
            ),
            "mean_normal_food_headroom": float(
                np.mean([item.mean_normal_food_headroom for item in values])
            ),
        }


@dataclass
class _DailyAccumulator:
    temperature_hours: dict[str, float]
    comfort_degree_hours: dict[str, float]
    survival_degree_hours: dict[str, float]
    work_degree_hours: dict[str, float]

    @classmethod
    def create(cls, buildings: Sequence[BuildingSpec]) -> "_DailyAccumulator":
        zeros = {item.name: 0.0 for item in buildings}
        return cls(zeros.copy(), zeros.copy(), zeros.copy(), zeros.copy())

    def reset(self) -> None:
        for mapping in (
            self.temperature_hours,
            self.comfort_degree_hours,
            self.survival_degree_hours,
            self.work_degree_hours,
        ):
            for key in mapping:
                mapping[key] = 0.0


def _future_window_min(values: np.ndarray, window: int) -> np.ndarray:
    result = np.empty_like(values)
    from collections import deque

    queue: deque[int] = deque()
    for index in range(len(values) - 1, -1, -1):
        while queue and queue[0] >= index + window:
            queue.popleft()
        while queue and values[queue[-1]] >= values[index]:
            queue.pop()
        queue.append(index)
        result[index] = values[queue[0]]
    return result


def _policy_control(
    policy: PolicySpec,
    emitters: Sequence,
    current_level: int,
    future_min_level: int,
) -> ThermalControl:
    cold_expected = future_min_level <= policy.cold_trigger_level
    if policy.name == "none":
        active = tuple(bool(item.active_by_default or item.kind == "tower") for item in emitters)
        return ThermalControl(active, False, policy.setpoint_offset_c)
    if policy.name == "conservative":
        active = tuple(True for _ in emitters)
        overdrive = policy.allow_overdrive and cold_expected
        return ThermalControl(active, overdrive, policy.setpoint_offset_c)
    active = tuple(
        bool(item.kind == "tower" or item.active_by_default or cold_expected)
        for item in emitters
    )
    return ThermalControl(
        active,
        policy.allow_overdrive and cold_expected,
        policy.setpoint_offset_c,
    )


def _current_building_temperature(
    patterns: np.ndarray,
    ambient_c: float,
    heat_ceilings_c: np.ndarray,
) -> float:
    values = np.empty(len(patterns), dtype=float)
    values[0] = current_block_temperature(ambient_c, 0.0)
    for mask in range(1, len(patterns)):
        heat = max(
            (
                heat_ceilings_c[index]
                for index in range(len(heat_ceilings_c))
                if mask & (1 << index)
            ),
            default=0.0,
        )
        values[mask] = current_block_temperature(ambient_c, heat)
    return float(np.dot(patterns, values))


def _work_factor(profile: str, building: BuildingSpec, mean_temperature_c: float) -> float:
    if profile == "current_compat":
        if building.kind == "mine":
            return 1.0
        return 1.0 if building.work_min_c <= mean_temperature_c <= building.maximum_c else 0.0
    if mean_temperature_c <= building.survival_min_c or mean_temperature_c >= building.maximum_c:
        return 0.0
    if mean_temperature_c < building.work_min_c:
        span = max(1e-9, building.work_min_c - building.survival_min_c)
        return 0.25 * (mean_temperature_c - building.survival_min_c) / span
    if mean_temperature_c < building.comfort_c:
        span = max(1e-9, building.comfort_c - building.work_min_c)
        return 0.25 + 0.75 * (mean_temperature_c - building.work_min_c) / span
    return 1.0


def _initial_inventory(scenario: Scenario, population: int) -> tuple[dict[str, float], dict[str, float]]:
    names = set(scenario.inventory.initial) | set(scenario.inventory.initial_per_person)
    stocks = {
        name: scenario.inventory.initial.get(name, 0.0)
        + scenario.inventory.initial_per_person.get(name, 0.0) * population
        for name in names
    }
    capacity_names = set(scenario.inventory.capacity) | set(scenario.inventory.capacity_per_person)
    capacities = {
        name: scenario.inventory.capacity.get(name, math.inf)
        + scenario.inventory.capacity_per_person.get(name, 0.0) * population
        for name in capacity_names
    }
    for name in stocks:
        capacities.setdefault(name, math.inf)
    return stocks, capacities


def _process_day(
    scenario: Scenario,
    processes: Sequence[ProcessSpec],
    buildings: Sequence[BuildingSpec],
    daily: _DailyAccumulator,
    stocks: dict[str, float],
    capacities: Mapping[str, float],
    policy: PolicySpec,
    population: int,
    health: float,
    mental: float,
    fuel_demand_estimate: float,
    food_demand_estimate: float,
    *,
    profile: str = "target_rc",
) -> tuple[dict[str, float], dict[str, float], bool, float]:
    available_swe = (
        population
        * scenario.population.worker_fraction
        * scenario.population.swe_per_worker
        * max(0.0, health / 100.0)
        * max(0.0, mental / 100.0)
    )
    by_kind: dict[str, list[tuple[BuildingSpec, float]]] = {}
    for building in buildings:
        mean_temp = daily.temperature_hours[building.name] / 24.0
        by_kind.setdefault(building.kind, []).append(
            (building, _work_factor("target_rc", building, mean_temp))
        )

    fuel_reserve = stocks.get("fuel_fv", 0.0) / max(1.0, fuel_demand_estimate)
    food_reserve = stocks.get("food_energy", 0.0) / max(1.0, food_demand_estimate)
    category_base: dict[str, float] = {}
    category_count: dict[str, int] = {}
    for process in processes:
        category_base[process.category] = category_base.get(process.category, 0.0) + max(
            0.0, process.base_labor_share
        )
        category_count[process.category] = category_count.get(process.category, 0) + 1
    weights: dict[str, float] = {}
    load_shed = False
    for process in processes:
        weight = max(0.0, process.base_labor_share)
        category_total = category_base.get(process.category, 0.0)
        category_fraction = (
            max(0.0, process.base_labor_share) / category_total
            if category_total > 0.0
            else 1.0 / max(1, category_count.get(process.category, 1))
        )
        if process.category == "fuel" and fuel_reserve < policy.reserve_days:
            weight += policy.fuel_labor_bonus * category_fraction
        if process.category == "food" and food_reserve < policy.reserve_days:
            weight += policy.food_labor_bonus * category_fraction
        if (
            process.category == "industry"
            and policy.allow_load_shedding
            and fuel_reserve < policy.critical_reserve_days
        ):
            weight = 0.0
            load_shed = True
        weights[process.name] = weight
    total_weight = sum(weights.values())
    if total_weight > 1.0:
        weights = {key: value / total_weight for key, value in weights.items()}

    produced: dict[str, float] = {}
    consumed: dict[str, float] = {}
    swe_used = 0.0
    for process in sorted(processes, key=lambda item: item.priority):
        building_options = by_kind.get(process.building_kind, [])
        if not building_options:
            continue
        temperature_factor = float(np.mean([factor for _, factor in building_options]))
        capacity_multiplier = (
            len(building_options) if process.capacity_scales_with_buildings else 1
        )
        assigned = min(
            process.max_swe * capacity_multiplier,
            available_swe * weights.get(process.name, 0.0),
        )
        assigned *= max(0.0, temperature_factor)
        if assigned <= 0.0:
            continue
        input_scale = 1.0
        process_inputs = (
            process.current_inputs_per_swe_day
            if profile == "current_compat"
            else process.inputs_per_swe_day
        )
        process_outputs = (
            process.current_outputs_per_swe_day
            if profile == "current_compat"
            else process.outputs_per_swe_day
        )
        for resource, amount in process_inputs.items():
            required = amount * assigned
            if required > 0:
                input_scale = min(input_scale, stocks.get(resource, 0.0) / required)
        assigned *= max(0.0, min(1.0, input_scale))
        for resource, amount in process_inputs.items():
            quantity = amount * assigned
            stocks[resource] = max(0.0, stocks.get(resource, 0.0) - quantity)
            consumed[resource] = consumed.get(resource, 0.0) + quantity
        for resource, amount in process_outputs.items():
            quantity = amount * assigned
            stocks[resource] = min(
                capacities.get(resource, math.inf), stocks.get(resource, 0.0) + quantity
            )
            produced[resource] = produced.get(resource, 0.0) + quantity
        swe_used += assigned
    return produced, consumed, load_shed, swe_used


def _target_process_day(
    profile: str,
    *args,
    **kwargs,
) -> tuple[dict[str, float], dict[str, float], bool, float]:
    # Compatibility uses the same data-driven peripheral process graph, but its
    # binary building eligibility is applied below before invoking the shared
    # resource settlement.
    scenario: Scenario = args[0]
    buildings: Sequence[BuildingSpec] = args[2]
    daily: _DailyAccumulator = args[3]
    if profile == "current_compat":
        shadow = _DailyAccumulator.create(buildings)
        shadow.temperature_hours.update(daily.temperature_hours)
        shadow.comfort_degree_hours.update(daily.comfort_degree_hours)
        shadow.survival_degree_hours.update(daily.survival_degree_hours)
        shadow.work_degree_hours.update(daily.work_degree_hours)
        # _process_day calls the target curve, so make ineligible current
        # buildings land at their survival minimum; eligible ones at comfort.
        for building in buildings:
            current_mean = daily.temperature_hours[building.name] / 24.0
            eligible = _work_factor(profile, building, current_mean) > 0.0
            shadow.temperature_hours[building.name] = 24.0 * (
                building.comfort_c if eligible else building.survival_min_c
            )
        args = (scenario, args[1], buildings, shadow, *args[4:])
    return _process_day(*args, profile=profile, **kwargs)


def run_simulation(
    scenario: Scenario,
    profile: str,
    policy_name: str,
    population: int,
    *,
    days: int | None = None,
    seed: int | None = None,
    climate_mode: str = "stochastic",
    collect_timeseries: bool = True,
) -> SimulationResult:
    if profile not in VALID_PROFILES:
        raise ValueError(f"unknown profile {profile!r}; choose from {sorted(VALID_PROFILES)}")
    if policy_name not in scenario.policies:
        raise ValueError(f"unknown policy {policy_name!r}")
    days = int(days if days is not None else scenario.simulation.days)
    seed = int(seed if seed is not None else scenario.simulation.seed)
    dt = scenario.simulation.dt_hours
    steps_per_day = int(round(24.0 / dt))
    policy = scenario.policies[policy_name]
    emitters = scenario.active_emitters(population)
    buildings = scenario.active_buildings(population)
    processes = scenario.active_processes(population)
    if sum(item.capacity for item in buildings if item.kind == "house") < population:
        raise ValueError(f"reference layout has insufficient beds for population {population}")

    ambient = (
        generate_design_climate(scenario.climate, days, dt)
        if climate_mode == "design"
        else generate_stochastic_climate(scenario.climate, days, dt, seed)
    )
    levels = forecast_levels(ambient)
    forecast_steps = max(1, int(round(policy.preheat_hours / dt)))
    future_min = _future_window_min(levels, forecast_steps)

    patterns = {item.name: coverage_patterns(item, emitters) for item in buildings}
    coverage = {
        item.name: 1.0 - float(patterns[item.name][0]) for item in buildings
    }
    footprint = heat_footprint(emitters)
    stocks, capacities = _initial_inventory(scenario, population)
    stocks.setdefault("fuel_fv", 0.0)
    stocks.setdefault("food_energy", 0.0)
    stocks.setdefault("nutrition", 0.0)

    emitter_temperatures = np.full(len(emitters), scenario.layout.initial_temperature_c)
    building_temperatures = {
        item.name: scenario.layout.initial_temperature_c for item in buildings
    }
    daily = _DailyAccumulator.create(buildings)
    health = 100.0
    mental = 100.0
    living_population = population
    deaths = 0
    total_fuel_used = 0.0
    total_fuel_produced = 0.0
    total_food_produced = 0.0
    total_food_demand = 0.0
    normal_fuel_produced = 0.0
    normal_fuel_used = 0.0
    normal_food_produced = 0.0
    normal_food_demand = 0.0
    total_housing_comfort_dh = 0.0
    total_housing_survival_dh = 0.0
    total_work_shortfall_dh = 0.0
    soft_pressure_days = 0
    load_shed_days = 0
    fuel_stockout_days = 0
    food_stockout_days = 0
    zero_stock_streak = 0
    irreversible = False
    min_fuel = stocks["fuel_fv"]
    min_food = stocks["food_energy"]
    min_health = health
    min_mental = mental
    power_headrooms: list[float] = []
    normal_power_headrooms: list[float] = []
    last_daily_fuel_demand = sum(
        item.base_burn_fv_per_hour * 24.0 for item in emitters if item.kind == "tower"
    )
    fuel_used_at_last_settlement = 0.0
    daily_min_ambient = math.inf
    last_daily_food_demand = (
        living_population
        * scenario.population.fce_per_person
        * scenario.population.food_energy_per_fce_day
    )
    cold_end_day = scenario.climate.design_cold_start_day + scenario.climate.design_cold_duration_days
    recovery_day: float | None = None

    series: dict[str, list[float]] = {
        "hour": [],
        "ambient_c": [],
        "housing_c": [],
        "work_c": [],
        "fuel_fv": [],
        "food_energy": [],
        "health": [],
        "mental": [],
        "thermal_power_fv_per_hour": [],
        "power_headroom": [],
        "population": [],
    }
    for emitter in emitters:
        series[f"field_{emitter.name}_c"] = []

    for step, ambient_c in enumerate(ambient):
        control = _policy_control(
            policy,
            emitters,
            int(levels[step]),
            int(future_min[step]),
        )
        if profile == "target_rc":
            thermal = target_rc_step(
                scenario,
                emitters,
                emitter_temperatures,
                float(ambient_c),
                stocks["fuel_fv"],
                dt,
                control,
            )
        else:
            thermal = current_compat_step(
                scenario,
                emitters,
                buildings,
                emitter_temperatures,
                stocks["fuel_fv"],
                dt,
                control,
            )
        emitter_temperatures = thermal.emitter_temperatures_c
        daily_min_ambient = min(daily_min_ambient, float(ambient_c))
        stocks["fuel_fv"] = max(0.0, stocks["fuel_fv"] - thermal.fuel_used_fv)
        total_fuel_used += thermal.fuel_used_fv
        min_fuel = min(min_fuel, stocks["fuel_fv"])
        power_headrooms.append(thermal.power_headroom)
        if (
            ambient_c >= scenario.climate.baseline_c + scenario.climate.local_offset_c - 10.0
            and not control.overdrive
        ):
            normal_power_headrooms.append(thermal.power_headroom)

        housing_temperatures: list[float] = []
        work_temperatures: list[float] = []
        for building in buildings:
            if profile == "target_rc":
                environment = mean_environment_temperature(
                    patterns[building.name],
                    float(ambient_c),
                    emitter_temperatures,
                )
                temperature = building_rc_step(
                    building,
                    building_temperatures[building.name],
                    environment,
                    dt,
                )
            else:
                temperature = _current_building_temperature(
                    patterns[building.name],
                    float(ambient_c),
                    emitter_temperatures,
                )
                if building.legacy_direct_heat and thermal.network_served.get(building.name, False):
                    temperature += building.legacy_heat_modifier_c
            building_temperatures[building.name] = temperature
            daily.temperature_hours[building.name] += temperature * dt
            daily.comfort_degree_hours[building.name] += max(0.0, building.comfort_c - temperature) * dt
            daily.survival_degree_hours[building.name] += max(0.0, building.survival_min_c - temperature) * dt
            daily.work_degree_hours[building.name] += max(0.0, building.work_min_c - temperature) * dt
            if building.kind == "house":
                housing_temperatures.append(temperature)
            else:
                work_temperatures.append(temperature)

        if collect_timeseries:
            series["hour"].append(step * dt)
            series["ambient_c"].append(float(ambient_c))
            series["housing_c"].append(float(np.mean(housing_temperatures)))
            series["work_c"].append(float(np.mean(work_temperatures)) if work_temperatures else math.nan)
            series["fuel_fv"].append(stocks["fuel_fv"])
            series["food_energy"].append(stocks["food_energy"])
            series["health"].append(health)
            series["mental"].append(mental)
            series["thermal_power_fv_per_hour"].append(float(np.sum(thermal.delivered_power_fv_per_hour)))
            series["power_headroom"].append(thermal.power_headroom)
            series["population"].append(float(living_population))
            for index, emitter in enumerate(emitters):
                series[f"field_{emitter.name}_c"].append(emitter_temperatures[index])

        if (step + 1) % steps_per_day != 0:
            continue

        current_day = (step + 1) // steps_per_day
        daily_fuel_used = total_fuel_used - fuel_used_at_last_settlement
        housing = [item for item in buildings if item.kind == "house"]
        housing_comfort_dh = sum(
            daily.comfort_degree_hours[item.name] * item.capacity for item in housing
        ) / max(1, sum(item.capacity for item in housing))
        housing_survival_dh = sum(
            daily.survival_degree_hours[item.name] * item.capacity for item in housing
        ) / max(1, sum(item.capacity for item in housing))
        work_shortfall_dh = sum(
            daily.work_degree_hours[item.name] for item in buildings if item.kind != "house"
        ) / max(1, len([item for item in buildings if item.kind != "house"]))
        total_housing_comfort_dh += housing_comfort_dh
        total_housing_survival_dh += housing_survival_dh
        total_work_shortfall_dh += work_shortfall_dh

        produced, consumed, load_shed, _ = _target_process_day(
            profile,
            scenario,
            processes,
            buildings,
            daily,
            stocks,
            capacities,
            policy,
            living_population,
            health,
            mental,
            last_daily_fuel_demand,
            last_daily_food_demand,
        )
        fuel_produced = produced.get("fuel_fv", 0.0)
        food_produced = produced.get("food_energy", 0.0)
        process_fuel_used = consumed.get("fuel_fv", 0.0)
        total_fuel_used += process_fuel_used
        fuel_used_at_last_settlement = total_fuel_used
        total_fuel_produced += fuel_produced
        total_food_produced += food_produced
        load_shed_days += int(load_shed)

        settles_fraction = 1.0
        if profile == "current_compat":
            valid_capacity = sum(
                item.capacity
                for item in housing
                if current_house_settles(
                    True, daily.temperature_hours[item.name] / 24.0
                )
            )
            settles_fraction = min(1.0, valid_capacity / max(1, living_population))
        fce = living_population * scenario.population.fce_per_person * settles_fraction
        food_need = fce * scenario.population.food_energy_per_fce_day
        if profile == "target_rc":
            food_need += (
                fce
                * scenario.population.cold_food_per_fce_degree_hour
                * housing_comfort_dh
            )
        nutrition_need = fce * scenario.population.nutrition_per_fce_day
        last_daily_food_demand = max(1.0, food_need)
        total_food_demand += food_need

        food_taken = min(food_need, stocks.get("food_energy", 0.0))
        nutrition_taken = min(nutrition_need, stocks.get("nutrition", 0.0))
        stocks["food_energy"] = max(0.0, stocks.get("food_energy", 0.0) - food_taken)
        stocks["nutrition"] = max(0.0, stocks.get("nutrition", 0.0) - nutrition_taken)
        food_satisfaction = food_taken / food_need if food_need > 0 else 1.0
        nutrition_satisfaction = nutrition_taken / nutrition_need if nutrition_need > 0 else 1.0

        if settles_fraction > 0:
            health += scenario.population.health_recovery_per_day * min(food_satisfaction, nutrition_satisfaction)
            mental += scenario.population.mental_recovery_per_day * min(food_satisfaction, nutrition_satisfaction)
            health -= scenario.population.health_loss_no_food_per_day * (1.0 - food_satisfaction)
            mental -= scenario.population.mental_loss_no_food_per_day * (1.0 - food_satisfaction)
            if profile == "target_rc":
                health -= scenario.population.health_loss_per_survival_degree_hour * housing_survival_dh
                mental -= scenario.population.mental_loss_per_comfort_degree_hour * housing_comfort_dh
        health = min(100.0, max(0.0, health))
        mental = min(100.0, max(0.0, mental))

        if health <= scenario.population.death_threshold and living_population > 0:
            casualties = min(living_population, max(1, math.ceil(living_population * 0.1)))
            living_population -= casualties
            deaths += casualties
            health = max(15.0, health)

        fuel_stockout = stocks["fuel_fv"] <= 1e-9
        food_stockout = stocks["food_energy"] <= 1e-9
        fuel_stockout_days += int(fuel_stockout)
        food_stockout_days += int(food_stockout)
        zero_stock_streak = zero_stock_streak + 1 if fuel_stockout and food_stockout else 0
        irreversible = irreversible or (zero_stock_streak >= 3 and health < 20.0)
        fuel_reserve = stocks["fuel_fv"] / max(1.0, last_daily_fuel_demand)
        food_reserve = stocks["food_energy"] / max(1.0, last_daily_food_demand)
        pressure_today = (
            fuel_reserve < 3.0
            or food_reserve < 3.0
            or load_shed
            or housing_comfort_dh >= scenario.simulation.soft_comfort_degree_hours
        )
        soft_pressure_days += int(pressure_today)
        daily_fuel_used += process_fuel_used
        normal_day = daily_min_ambient >= (
            scenario.climate.baseline_c + scenario.climate.local_offset_c - 10.0
        )
        if normal_day:
            normal_fuel_produced += fuel_produced
            normal_fuel_used += daily_fuel_used
            normal_food_produced += food_produced
            normal_food_demand += food_need
        last_daily_fuel_demand = max(1.0, daily_fuel_used)
        if (
            climate_mode == "design"
            and current_day >= cold_end_day
            and recovery_day is None
            and fuel_reserve >= 7.0
            and food_reserve >= 7.0
        ):
            recovery_day = current_day - cold_end_day

        min_fuel = min(min_fuel, stocks["fuel_fv"])
        min_food = min(min_food, stocks["food_energy"])
        min_health = min(min_health, health)
        min_mental = min(min_mental, mental)
        daily.reset()
        daily_min_ambient = math.inf

    final_fuel_reserve = stocks["fuel_fv"] / max(1.0, last_daily_fuel_demand)
    final_food_reserve = stocks["food_energy"] / max(1.0, last_daily_food_demand)
    summary = SimulationSummary(
        profile=profile,
        policy=policy_name,
        population_start=population,
        population_end=living_population,
        days=days,
        seed=seed,
        deaths=deaths,
        irreversible_collapse=irreversible,
        soft_pressure=soft_pressure_days > 0,
        soft_pressure_days=soft_pressure_days,
        load_shed_days=load_shed_days,
        fuel_stockout_days=fuel_stockout_days,
        food_stockout_days=food_stockout_days,
        minimum_fuel_fv=min_fuel,
        minimum_food_energy=min_food,
        minimum_health=min_health,
        minimum_mental=min_mental,
        fuel_used_fv=total_fuel_used,
        fuel_produced_fv=total_fuel_produced,
        food_produced=total_food_produced,
        mean_power_headroom=float(np.mean(power_headrooms)) if power_headrooms else 0.0,
        mean_normal_power_headroom=(
            float(np.mean(normal_power_headrooms)) if normal_power_headrooms else 0.0
        ),
        mean_fuel_headroom=(total_fuel_produced - total_fuel_used) / max(1.0, total_fuel_used),
        mean_food_headroom=(total_food_produced - total_food_demand) / max(1.0, total_food_demand),
        mean_normal_fuel_headroom=(normal_fuel_produced - normal_fuel_used) / max(1.0, normal_fuel_used),
        mean_normal_food_headroom=(normal_food_produced - normal_food_demand) / max(1.0, normal_food_demand),
        housing_comfort_degree_hours=total_housing_comfort_dh,
        housing_survival_degree_hours=total_housing_survival_dh,
        work_shortfall_degree_hours=total_work_shortfall_dh,
        mean_heat_coverage=float(np.mean(list(coverage.values()))),
        heat_overlap_ratio=footprint.overlap_ratio,
        final_fuel_reserve_days=final_fuel_reserve,
        final_food_reserve_days=final_food_reserve,
        final_fuel_fv=stocks["fuel_fv"],
        final_food_energy=stocks["food_energy"],
        final_nutrition=stocks["nutrition"],
        recovery_days_after_design_cold=recovery_day,
    )
    arrays = {key: np.asarray(value, dtype=float) for key, value in series.items()} if collect_timeseries else {}
    return SimulationResult(summary, arrays)


def run_monte_carlo(
    scenario: Scenario,
    profile: str,
    policy_name: str,
    population: int,
    *,
    days: int | None = None,
    runs: int | None = None,
    seed: int | None = None,
    keep_representative: bool = True,
    workers: int = 1,
) -> MonteCarloResult:
    run_count = int(runs if runs is not None else scenario.simulation.runs)
    base_seed = int(seed if seed is not None else scenario.simulation.seed)
    representative = (
        run_simulation(
            scenario,
            profile,
            policy_name,
            population,
            days=days,
            seed=base_seed,
            climate_mode="stochastic",
            collect_timeseries=True,
        )
        if keep_representative and run_count > 0
        else None
    )
    tasks = [
        (scenario, profile, policy_name, population, days, base_seed + run_index)
        for run_index in range(run_count)
    ]
    if workers > 1 and run_count > 1:
        from concurrent.futures import ProcessPoolExecutor
        import warnings

        chunk_size = max(1, run_count // (workers * 4))
        try:
            with ProcessPoolExecutor(max_workers=workers) as executor:
                summaries = list(executor.map(_run_summary_task, tasks, chunksize=chunk_size))
        except (OSError, PermissionError) as error:
            warnings.warn(
                f"parallel workers unavailable ({error}); falling back to serial execution",
                RuntimeWarning,
                stacklevel=2,
            )
            summaries = [_run_summary_task(task) for task in tasks]
    else:
        summaries = [_run_summary_task(task) for task in tasks]
    return MonteCarloResult(summaries, representative)


def _run_summary_task(
    task: tuple[Scenario, str, str, int, int | None, int]
) -> SimulationSummary:
    scenario, profile, policy_name, population, days, seed = task
    return run_simulation(
        scenario,
        profile,
        policy_name,
        population,
        days=days,
        seed=seed,
        climate_mode="stochastic",
        collect_timeseries=False,
    ).summary


def summaries_as_dicts(summaries: Iterable[SimulationSummary]) -> list[dict[str, object]]:
    return [asdict(summary) for summary in summaries]
