"""TOML configuration schema for the town simulator.

All values are expressed in game-facing units rather than SI units.  This is
deliberate: ``fuel value`` is the common conserved energy stock shared by the
existing generator recipes and the proposed balance model.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Mapping
import tomllib


Vec3 = tuple[float, float, float]
Int3 = tuple[int, int, int]


def _tuple3(value: list[Any] | tuple[Any, ...], cast: type) -> tuple[Any, Any, Any]:
    if len(value) != 3:
        raise ValueError(f"expected a three-element coordinate, got {value!r}")
    return cast(value[0]), cast(value[1]), cast(value[2])


@dataclass(frozen=True)
class SimulationSpec:
    days: int = 120
    dt_hours: float = 0.25
    runs: int = 1000
    seed: int = 20260810
    populations: tuple[int, ...] = (8, 24, 48)
    soft_comfort_degree_hours: float = 110.0


@dataclass(frozen=True)
class ClimateSpec:
    baseline_c: float
    local_offset_c: float
    daily_swing_c: float
    tracks: int
    calm_days_min: float
    calm_days_max: float
    event_days_min: float
    event_days_max: float
    warm_probability: float
    cold_amplitude_min_c: float
    cold_amplitude_max_c: float
    cold_amplitude_shape: float
    warm_amplitude_min_c: float
    warm_amplitude_max_c: float
    design_cold_start_day: float
    design_cold_duration_days: float
    design_cold_amplitude_c: float
    forecast_hours: int = 120


@dataclass(frozen=True)
class HeatFieldSpec:
    envelope_loss_fv_per_block2_c_hour: float
    volume_leak_fv_per_block3_c_hour: float
    capacitance_fv_per_block3_c: float


@dataclass(frozen=True)
class NetworkSpec:
    capacity_fv_per_hour: float
    efficiency: float
    current_efficiency: float
    current_heat_efficiency: float
    current_capacity_heat_per_tick: float


@dataclass(frozen=True)
class EmitterSpec:
    name: str
    kind: str
    position: Vec3
    radius_blocks: float
    setpoint_c: float
    overdrive_setpoint_c: float
    max_power_fv_per_hour: float
    overdrive_power_multiplier: float
    base_burn_fv_per_hour: float
    overdrive_burn_fv_per_hour: float
    efficiency: float
    ramp_c_per_hour: float
    networked: bool
    priority: int
    min_population: int
    active_by_default: bool
    movable: bool
    current_heat_ceiling_c: float
    current_overdrive_heat_ceiling_c: float
    current_network_heat_per_tick: float


@dataclass(frozen=True)
class BuildingSpec:
    name: str
    kind: str
    position: Int3
    size: Int3
    capacity: int
    min_population: int
    envelope_u_fv_per_block2_c_hour: float
    leakage_fv_per_block3_c_hour: float
    capacitance_fv_per_block3_c: float
    comfort_c: float
    work_min_c: float
    survival_min_c: float
    maximum_c: float
    legacy_direct_heat: bool
    legacy_heat_modifier_c: float
    legacy_network_heat_per_tick: float
    movable: bool


@dataclass(frozen=True)
class PopulationSpec:
    worker_fraction: float
    swe_per_worker: float
    fce_per_person: float
    food_energy_per_fce_day: float
    nutrition_per_fce_day: float
    cold_food_per_fce_degree_hour: float
    health_loss_no_food_per_day: float
    mental_loss_no_food_per_day: float
    health_loss_per_survival_degree_hour: float
    mental_loss_per_comfort_degree_hour: float
    health_recovery_per_day: float
    mental_recovery_per_day: float
    death_threshold: float


@dataclass(frozen=True)
class ProcessSpec:
    name: str
    category: str
    building_kind: str
    priority: int
    base_labor_share: float
    max_swe: float
    capacity_scales_with_buildings: bool
    min_population: int
    outputs_per_swe_day: Mapping[str, float]
    inputs_per_swe_day: Mapping[str, float]
    current_outputs_per_swe_day: Mapping[str, float]
    current_inputs_per_swe_day: Mapping[str, float]


@dataclass(frozen=True)
class InventorySpec:
    initial: Mapping[str, float]
    initial_per_person: Mapping[str, float]
    capacity: Mapping[str, float]
    capacity_per_person: Mapping[str, float]


@dataclass(frozen=True)
class PolicySpec:
    name: str
    reserve_days: float
    critical_reserve_days: float
    cold_trigger_level: int
    preheat_hours: int
    setpoint_offset_c: float
    allow_overdrive: bool
    allow_load_shedding: bool
    fuel_labor_bonus: float
    food_labor_bonus: float


@dataclass(frozen=True)
class LayoutSpec:
    search_radius_blocks: int
    restarts: int
    iterations: int
    initial_temperature_c: float


@dataclass(frozen=True)
class Scenario:
    name: str
    description: str
    sources: Mapping[str, str]
    simulation: SimulationSpec
    climate: ClimateSpec
    heat_field: HeatFieldSpec
    network: NetworkSpec
    emitters: tuple[EmitterSpec, ...]
    buildings: tuple[BuildingSpec, ...]
    population: PopulationSpec
    processes: tuple[ProcessSpec, ...]
    inventory: InventorySpec
    policies: Mapping[str, PolicySpec]
    layout: LayoutSpec
    source_path: Path = field(compare=False)

    def active_emitters(self, population: int) -> tuple[EmitterSpec, ...]:
        return tuple(item for item in self.emitters if item.min_population <= population)

    def active_buildings(self, population: int) -> tuple[BuildingSpec, ...]:
        return tuple(item for item in self.buildings if item.min_population <= population)

    def active_processes(self, population: int) -> tuple[ProcessSpec, ...]:
        return tuple(item for item in self.processes if item.min_population <= population)


def _require(mapping: Mapping[str, Any], key: str, section: str) -> Any:
    if key not in mapping:
        raise ValueError(f"missing required key {section}.{key}")
    return mapping[key]


def load_scenario(path: str | Path) -> Scenario:
    source_path = Path(path).resolve()
    with source_path.open("rb") as handle:
        raw = tomllib.load(handle)

    meta = raw.get("meta", {})
    sim = raw.get("simulation", {})
    climate = raw.get("climate", {})
    field_raw = raw.get("heat_field", {})
    network = raw.get("network", {})
    pop = raw.get("population", {})
    inv = raw.get("inventory", {})
    layout = raw.get("layout", {})

    emitters = tuple(
        EmitterSpec(
            name=str(_require(item, "name", "emitter")),
            kind=str(item.get("kind", "heater")),
            position=_tuple3(item.get("position", [0, 0, 0]), float),
            radius_blocks=float(_require(item, "radius_blocks", "emitter")),
            setpoint_c=float(_require(item, "setpoint_c", "emitter")),
            overdrive_setpoint_c=float(item.get("overdrive_setpoint_c", item["setpoint_c"])),
            max_power_fv_per_hour=float(_require(item, "max_power_fv_per_hour", "emitter")),
            overdrive_power_multiplier=float(item.get("overdrive_power_multiplier", 1.0)),
            base_burn_fv_per_hour=float(item.get("base_burn_fv_per_hour", 0.0)),
            overdrive_burn_fv_per_hour=float(item.get("overdrive_burn_fv_per_hour", 0.0)),
            efficiency=float(item.get("efficiency", 1.0)),
            ramp_c_per_hour=float(item.get("ramp_c_per_hour", 10.0)),
            networked=bool(item.get("networked", False)),
            priority=int(item.get("priority", 100)),
            min_population=int(item.get("min_population", 0)),
            active_by_default=bool(item.get("active_by_default", True)),
            movable=bool(item.get("movable", False)),
            current_heat_ceiling_c=float(item.get("current_heat_ceiling_c", item["setpoint_c"])),
            current_overdrive_heat_ceiling_c=float(item.get("current_overdrive_heat_ceiling_c", item.get("overdrive_setpoint_c", item["setpoint_c"]))),
            current_network_heat_per_tick=float(item.get("current_network_heat_per_tick", 0.0)),
        )
        for item in raw.get("emitter", [])
    )
    buildings = tuple(
        BuildingSpec(
            name=str(_require(item, "name", "building")),
            kind=str(_require(item, "kind", "building")),
            position=_tuple3(item.get("position", [0, 0, 0]), int),
            size=_tuple3(_require(item, "size", "building"), int),
            capacity=int(item.get("capacity", 0)),
            min_population=int(item.get("min_population", 0)),
            envelope_u_fv_per_block2_c_hour=float(item.get("envelope_u_fv_per_block2_c_hour", 0.02)),
            leakage_fv_per_block3_c_hour=float(item.get("leakage_fv_per_block3_c_hour", 0.002)),
            capacitance_fv_per_block3_c=float(item.get("capacitance_fv_per_block3_c", 0.2)),
            comfort_c=float(item.get("comfort_c", 18.0)),
            work_min_c=float(item.get("work_min_c", 0.0)),
            survival_min_c=float(item.get("survival_min_c", -5.0)),
            maximum_c=float(item.get("maximum_c", 50.0)),
            legacy_direct_heat=bool(item.get("legacy_direct_heat", False)),
            legacy_heat_modifier_c=float(item.get("legacy_heat_modifier_c", 24.0)),
            legacy_network_heat_per_tick=float(item.get("legacy_network_heat_per_tick", 0.0)),
            movable=bool(item.get("movable", True)),
        )
        for item in raw.get("building", [])
    )
    processes = tuple(
        ProcessSpec(
            name=str(_require(item, "name", "process")),
            category=str(_require(item, "category", "process")),
            building_kind=str(_require(item, "building_kind", "process")),
            priority=int(item.get("priority", 100)),
            base_labor_share=float(item.get("base_labor_share", 0.0)),
            max_swe=float(item.get("max_swe", 1e9)),
            capacity_scales_with_buildings=bool(item.get("capacity_scales_with_buildings", False)),
            min_population=int(item.get("min_population", 0)),
            outputs_per_swe_day={str(k): float(v) for k, v in item.get("outputs_per_swe_day", {}).items()},
            inputs_per_swe_day={str(k): float(v) for k, v in item.get("inputs_per_swe_day", {}).items()},
            current_outputs_per_swe_day={
                str(k): float(v)
                for k, v in item.get("current_outputs_per_swe_day", item.get("outputs_per_swe_day", {})).items()
            },
            current_inputs_per_swe_day={
                str(k): float(v)
                for k, v in item.get("current_inputs_per_swe_day", item.get("inputs_per_swe_day", {})).items()
            },
        )
        for item in raw.get("process", [])
    )
    policies = {
        str(item["name"]): PolicySpec(
            name=str(item["name"]),
            reserve_days=float(item.get("reserve_days", 7.0)),
            critical_reserve_days=float(item.get("critical_reserve_days", 2.0)),
            cold_trigger_level=int(item.get("cold_trigger_level", -3)),
            preheat_hours=int(item.get("preheat_hours", 24)),
            setpoint_offset_c=float(item.get("setpoint_offset_c", 0.0)),
            allow_overdrive=bool(item.get("allow_overdrive", False)),
            allow_load_shedding=bool(item.get("allow_load_shedding", False)),
            fuel_labor_bonus=float(item.get("fuel_labor_bonus", 0.0)),
            food_labor_bonus=float(item.get("food_labor_bonus", 0.0)),
        )
        for item in raw.get("policy", [])
    }

    scenario = Scenario(
        name=str(meta.get("name", source_path.stem)),
        description=str(meta.get("description", "")),
        sources={str(k): str(v) for k, v in raw.get("sources", {}).items()},
        simulation=SimulationSpec(
            days=int(sim.get("days", 120)),
            dt_hours=float(sim.get("dt_hours", 0.25)),
            runs=int(sim.get("runs", 1000)),
            seed=int(sim.get("seed", 20260810)),
            populations=tuple(int(v) for v in sim.get("populations", [8, 24, 48])),
            soft_comfort_degree_hours=float(sim.get("soft_comfort_degree_hours", 110.0)),
        ),
        climate=ClimateSpec(
            baseline_c=float(_require(climate, "baseline_c", "climate")),
            local_offset_c=float(climate.get("local_offset_c", 0.0)),
            daily_swing_c=float(climate.get("daily_swing_c", 0.0)),
            tracks=int(climate.get("tracks", 3)),
            calm_days_min=float(climate.get("calm_days_min", 2.0)),
            calm_days_max=float(climate.get("calm_days_max", 7.0)),
            event_days_min=float(climate.get("event_days_min", 2.0)),
            event_days_max=float(climate.get("event_days_max", 7.0)),
            warm_probability=float(climate.get("warm_probability", 0.2)),
            cold_amplitude_min_c=float(climate.get("cold_amplitude_min_c", 8.0)),
            cold_amplitude_max_c=float(climate.get("cold_amplitude_max_c", 80.0)),
            cold_amplitude_shape=float(climate.get("cold_amplitude_shape", 2.0)),
            warm_amplitude_min_c=float(climate.get("warm_amplitude_min_c", 4.0)),
            warm_amplitude_max_c=float(climate.get("warm_amplitude_max_c", 18.0)),
            design_cold_start_day=float(climate.get("design_cold_start_day", 30.0)),
            design_cold_duration_days=float(climate.get("design_cold_duration_days", 5.0)),
            design_cold_amplitude_c=float(climate.get("design_cold_amplitude_c", 55.0)),
            forecast_hours=int(climate.get("forecast_hours", 120)),
        ),
        heat_field=HeatFieldSpec(
            envelope_loss_fv_per_block2_c_hour=float(_require(field_raw, "envelope_loss_fv_per_block2_c_hour", "heat_field")),
            volume_leak_fv_per_block3_c_hour=float(_require(field_raw, "volume_leak_fv_per_block3_c_hour", "heat_field")),
            capacitance_fv_per_block3_c=float(_require(field_raw, "capacitance_fv_per_block3_c", "heat_field")),
        ),
        network=NetworkSpec(
            capacity_fv_per_hour=float(_require(network, "capacity_fv_per_hour", "network")),
            efficiency=float(network.get("efficiency", 1.0)),
            current_efficiency=float(network.get("current_efficiency", 1.0)),
            current_heat_efficiency=float(network.get("current_heat_efficiency", 1.2)),
            current_capacity_heat_per_tick=float(network.get("current_capacity_heat_per_tick", 25.0)),
        ),
        emitters=emitters,
        buildings=buildings,
        population=PopulationSpec(**{field_name: float(_require(pop, field_name, "population")) for field_name in PopulationSpec.__dataclass_fields__}),
        processes=processes,
        inventory=InventorySpec(
            initial={str(k): float(v) for k, v in inv.get("initial", {}).items()},
            initial_per_person={str(k): float(v) for k, v in inv.get("initial_per_person", {}).items()},
            capacity={str(k): float(v) for k, v in inv.get("capacity", {}).items()},
            capacity_per_person={str(k): float(v) for k, v in inv.get("capacity_per_person", {}).items()},
        ),
        policies=policies,
        layout=LayoutSpec(
            search_radius_blocks=int(layout.get("search_radius_blocks", 32)),
            restarts=int(layout.get("restarts", 8)),
            iterations=int(layout.get("iterations", 1500)),
            initial_temperature_c=float(layout.get("initial_temperature_c", 5.0)),
        ),
        source_path=source_path,
    )
    validate_scenario(scenario)
    return scenario


def validate_scenario(scenario: Scenario) -> None:
    if scenario.simulation.dt_hours <= 0 or 24 / scenario.simulation.dt_hours % 1:
        raise ValueError("simulation.dt_hours must be a positive divisor of 24")
    if not 0 < scenario.network.efficiency <= 1:
        raise ValueError("network.efficiency must be in (0, 1]")
    if not scenario.emitters:
        raise ValueError("scenario must define at least one emitter")
    if not scenario.buildings:
        raise ValueError("scenario must define at least one building")
    if "forecast" not in scenario.policies:
        raise ValueError("scenario must define a policy named 'forecast'")
    for emitter in scenario.emitters:
        if emitter.radius_blocks <= 0 or emitter.max_power_fv_per_hour < 0:
            raise ValueError(f"invalid emitter geometry or power: {emitter.name}")
        if not 0 < emitter.efficiency <= 1:
            raise ValueError(f"emitter efficiency must be in (0, 1]: {emitter.name}")
    for building in scenario.buildings:
        if any(value <= 0 for value in building.size):
            raise ValueError(f"building dimensions must be positive: {building.name}")
