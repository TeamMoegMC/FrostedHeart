"""Compatibility and target-RC thermal engines."""

from __future__ import annotations

from dataclasses import dataclass
import math
from typing import Mapping, Sequence

import numpy as np

from .compat import current_network_extra_fuel_per_hour
from .config import BuildingSpec, EmitterSpec, Scenario
from .geometry import sphere_surface, sphere_volume


@dataclass(frozen=True)
class ThermalControl:
    active_emitters: tuple[bool, ...]
    overdrive: bool
    setpoint_offset_c: float = 0.0


@dataclass(frozen=True)
class ThermalStep:
    emitter_temperatures_c: np.ndarray
    delivered_power_fv_per_hour: np.ndarray
    requested_power_fv_per_hour: np.ndarray
    fuel_used_fv: float
    network_used_fv_per_hour: float
    network_served: Mapping[str, bool]
    power_headroom: float


def _field_loss(scenario: Scenario, emitter: EmitterSpec, temperature_c: float, ambient_c: float) -> float:
    delta = max(0.0, temperature_c - ambient_c)
    field = scenario.heat_field
    conductance = (
        field.envelope_loss_fv_per_block2_c_hour * sphere_surface(emitter.radius_blocks)
        + field.volume_leak_fv_per_block3_c_hour * sphere_volume(emitter.radius_blocks)
    )
    return conductance * delta


def target_rc_step(
    scenario: Scenario,
    emitters: Sequence[EmitterSpec],
    temperatures_c: np.ndarray,
    ambient_c: float,
    fuel_available_fv: float,
    dt_hours: float,
    control: ThermalControl,
) -> ThermalStep:
    count = len(emitters)
    requested = np.zeros(count, dtype=float)
    capacity = np.zeros(count, dtype=float)
    targets = np.full(count, ambient_c, dtype=float)
    losses = np.zeros(count, dtype=float)
    overhead = np.zeros(count, dtype=float)

    for index, emitter in enumerate(emitters):
        active = control.active_emitters[index]
        overdrive = active and control.overdrive
        target = (
            (emitter.overdrive_setpoint_c if overdrive else emitter.setpoint_c)
            + control.setpoint_offset_c
            if active
            else ambient_c
        )
        targets[index] = max(ambient_c, target)
        power_cap = emitter.max_power_fv_per_hour * (
            emitter.overdrive_power_multiplier if overdrive else 1.0
        )
        capacity[index] = max(0.0, power_cap if active else 0.0)
        losses[index] = _field_loss(scenario, emitter, temperatures_c[index], ambient_c)
        thermal_mass = (
            scenario.heat_field.capacitance_fv_per_block3_c
            * sphere_volume(emitter.radius_blocks)
        )
        desired_rise = min(
            max(0.0, targets[index] - temperatures_c[index]),
            emitter.ramp_c_per_hour * dt_hours,
        )
        catch_up = desired_rise * thermal_mass / dt_hours
        requested[index] = min(capacity[index], losses[index] + catch_up) if active else 0.0
        if active:
            overhead[index] = emitter.base_burn_fv_per_hour + (
                emitter.overdrive_burn_fv_per_hour if overdrive else 0.0
            )

    delivered = requested.copy()
    networked = [index for index, emitter in enumerate(emitters) if emitter.networked]
    remaining_network = scenario.network.capacity_fv_per_hour * scenario.network.efficiency
    for index in sorted(networked, key=lambda item: emitters[item].priority):
        allocation = min(delivered[index], max(0.0, remaining_network))
        delivered[index] = allocation
        remaining_network -= allocation

    variable_fuel_per_hour = 0.0
    for index, emitter in enumerate(emitters):
        if delivered[index] <= 0.0:
            continue
        network_efficiency = scenario.network.efficiency if emitter.networked else 1.0
        variable_fuel_per_hour += delivered[index] / (emitter.efficiency * network_efficiency)
    overhead_per_hour = float(np.sum(overhead))
    available_per_hour = fuel_available_fv / dt_hours if dt_hours else 0.0
    if available_per_hour < overhead_per_hour:
        delivered[:] = 0.0
        actual_fuel_per_hour = available_per_hour
    else:
        variable_budget = max(0.0, available_per_hour - overhead_per_hour)
        scale = min(1.0, variable_budget / variable_fuel_per_hour) if variable_fuel_per_hour else 1.0
        delivered *= scale
        actual_fuel_per_hour = overhead_per_hour + variable_fuel_per_hour * scale

    new_temperatures = temperatures_c.astype(float, copy=True)
    for index, emitter in enumerate(emitters):
        thermal_mass = max(
            1e-9,
            scenario.heat_field.capacitance_fv_per_block3_c
            * sphere_volume(emitter.radius_blocks),
        )
        actual_loss = _field_loss(scenario, emitter, temperatures_c[index], ambient_c)
        delta = (delivered[index] - actual_loss) * dt_hours / thermal_mass
        new_temperatures[index] = temperatures_c[index] + delta
        if delivered[index] > 0.0:
            new_temperatures[index] = min(new_temperatures[index], targets[index])
        new_temperatures[index] = max(ambient_c, new_temperatures[index])

    requested_total = float(np.sum(requested))
    capacity_total = float(np.sum(capacity))
    headroom = (capacity_total - requested_total) / requested_total if requested_total > 0 else 1.0
    network_used = float(sum(delivered[index] for index in networked))
    served = {
        emitters[index].name: bool(delivered[index] + 1e-9 >= requested[index])
        for index in networked
    }
    return ThermalStep(
        emitter_temperatures_c=new_temperatures,
        delivered_power_fv_per_hour=delivered,
        requested_power_fv_per_hour=requested,
        fuel_used_fv=min(fuel_available_fv, actual_fuel_per_hour * dt_hours),
        network_used_fv_per_hour=network_used,
        network_served=served,
        power_headroom=headroom,
    )


def allocate_current_network(
    scenario: Scenario,
    emitters: Sequence[EmitterSpec],
    buildings: Sequence[BuildingSpec],
    control: ThermalControl,
) -> tuple[dict[str, bool], float]:
    endpoints: list[tuple[int, str, float]] = []
    for index, emitter in enumerate(emitters):
        if emitter.networked and control.active_emitters[index]:
            endpoints.append((emitter.priority, emitter.name, emitter.current_network_heat_per_tick))
    for building in buildings:
        if building.legacy_direct_heat and building.legacy_network_heat_per_tick > 0:
            endpoints.append((99, building.name, building.legacy_network_heat_per_tick))
    effective_capacity = (
        scenario.network.current_capacity_heat_per_tick
        * scenario.network.current_efficiency
    )
    remaining = effective_capacity
    served: dict[str, bool] = {}
    used = 0.0
    for _, name, demand in sorted(endpoints):
        ok = demand <= remaining + 1e-9
        served[name] = ok
        if ok:
            remaining -= demand
            used += demand
    return served, used


def current_compat_step(
    scenario: Scenario,
    emitters: Sequence[EmitterSpec],
    buildings: Sequence[BuildingSpec],
    heat_ceilings_c: np.ndarray,
    fuel_available_fv: float,
    dt_hours: float,
    control: ThermalControl,
) -> ThermalStep:
    served, network_heat_per_tick = allocate_current_network(
        scenario, emitters, buildings, control
    )
    target = np.zeros(len(emitters), dtype=float)
    overhead_per_hour = 0.0
    for index, emitter in enumerate(emitters):
        active = control.active_emitters[index]
        if emitter.networked:
            active = active and served.get(emitter.name, False)
        if active:
            target[index] = (
                emitter.current_overdrive_heat_ceiling_c
                if control.overdrive
                else emitter.current_heat_ceiling_c
            )
            overhead_per_hour += emitter.base_burn_fv_per_hour
            if control.overdrive:
                overhead_per_hour += emitter.overdrive_burn_fv_per_hour
    extra = current_network_extra_fuel_per_hour(
        network_heat_per_tick,
        scenario.network.current_heat_efficiency,
    )
    requested_fuel_per_hour = overhead_per_hour + extra
    available_per_hour = fuel_available_fv / dt_hours if dt_hours else 0.0
    fuel_scale = min(1.0, available_per_hour / requested_fuel_per_hour) if requested_fuel_per_hour else 1.0
    if fuel_scale < 1.0:
        served = {name: False for name in served}
    actual_target = target if fuel_scale >= 1.0 else np.zeros_like(target)
    new_heat = heat_ceilings_c.astype(float, copy=True)
    for index, emitter in enumerate(emitters):
        max_change = emitter.ramp_c_per_hour * dt_hours
        difference = actual_target[index] - new_heat[index]
        new_heat[index] += max(-max_change, min(max_change, difference))
        new_heat[index] = max(0.0, new_heat[index])
    delivered = np.asarray(
        [
            emitter.current_network_heat_per_tick * 1000.0
            if emitter.networked and served.get(emitter.name, False)
            else 0.0
            for emitter in emitters
        ],
        dtype=float,
    )
    return ThermalStep(
        emitter_temperatures_c=new_heat,
        delivered_power_fv_per_hour=delivered,
        requested_power_fv_per_hour=delivered.copy(),
        fuel_used_fv=min(fuel_available_fv, requested_fuel_per_hour * dt_hours),
        network_used_fv_per_hour=network_heat_per_tick * 1000.0,
        network_served=served,
        power_headroom=(
            scenario.network.current_capacity_heat_per_tick
            * scenario.network.current_efficiency
            - network_heat_per_tick
        )
        / max(1e-9, network_heat_per_tick),
    )


def building_rc_step(
    building: BuildingSpec,
    indoor_c: float,
    environment_c: float,
    dt_hours: float,
) -> float:
    x, y, z = building.size
    volume = x * y * z
    area = 2 * (x * y + x * z + y * z)
    conductance = (
        building.envelope_u_fv_per_block2_c_hour * area
        + building.leakage_fv_per_block3_c_hour * volume
    )
    thermal_mass = max(1e-9, building.capacitance_fv_per_block3_c * volume)
    decay = math.exp(-conductance * dt_hours / thermal_mass)
    return environment_c + (indoor_c - environment_c) * decay
