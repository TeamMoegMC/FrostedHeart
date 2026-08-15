from dataclasses import replace

import numpy as np
import pytest

from Scripts.town_model.config import BuildingSpec
from Scripts.town_model.thermal import (
    ThermalControl,
    allocate_current_network,
    building_rc_step,
    current_compat_step,
    target_rc_step,
)


def test_building_rc_matches_exponential_solution():
    building = BuildingSpec(
        name="test",
        kind="house",
        position=(0, 0, 0),
        size=(1, 1, 1),
        capacity=1,
        min_population=0,
        envelope_u_fv_per_block2_c_hour=1.0,
        leakage_fv_per_block3_c_hour=0.0,
        capacitance_fv_per_block3_c=6.0,
        comfort_c=18.0,
        work_min_c=0.0,
        survival_min_c=-5.0,
        maximum_c=50.0,
        legacy_direct_heat=False,
        legacy_heat_modifier_c=0.0,
        legacy_network_heat_per_tick=0.0,
        movable=False,
    )
    # G = 6 and C = 6, so one hour leaves exp(-1) of the initial delta.
    assert building_rc_step(building, 20.0, 0.0, 1.0) == pytest.approx(20.0 * np.exp(-1.0))


def test_better_insulation_retains_more_heat():
    base = BuildingSpec(
        "test", "house", (0, 0, 0), (2, 2, 2), 1, 0,
        0.1, 0.01, 1.0, 18.0, 0.0, -5.0, 50.0,
        False, 0.0, 0.0, False,
    )
    insulated = replace(base, envelope_u_fv_per_block2_c_hour=0.02)
    assert building_rc_step(insulated, 20.0, 0.0, 1.0) > building_rc_step(base, 20.0, 0.0, 1.0)


def test_target_field_holds_temperature_when_power_covers_loss(reference_scenario):
    emitter = reference_scenario.emitters[0]
    control = ThermalControl((True,), False, 0.0)
    step = target_rc_step(
        reference_scenario,
        (emitter,),
        np.asarray([emitter.setpoint_c]),
        -10.0,
        fuel_available_fv=1_000_000.0,
        dt_hours=0.25,
        control=control,
    )
    assert step.emitter_temperatures_c[0] == pytest.approx(emitter.setpoint_c)
    assert step.fuel_used_fv > emitter.base_burn_fv_per_hour * 0.25
    assert step.delivered_power_fv_per_hour[0] == pytest.approx(step.requested_power_fv_per_hour[0])


def test_current_network_allocation_respects_capacity(reference_scenario):
    emitters = reference_scenario.active_emitters(48)
    buildings = reference_scenario.active_buildings(48)
    control = ThermalControl(tuple(True for _ in emitters), False, 0.0)
    served, used = allocate_current_network(reference_scenario, emitters, buildings, control)
    assert used <= reference_scenario.network.current_capacity_heat_per_tick
    assert served["east_heater"]
    assert served["west_heater"]
    assert served["house_1"]


def test_current_endpoints_are_not_served_when_generator_has_no_fuel(reference_scenario):
    emitters = reference_scenario.active_emitters(8)
    buildings = reference_scenario.active_buildings(8)
    control = ThermalControl(tuple(True for _ in emitters), False, 0.0)
    step = current_compat_step(
        reference_scenario,
        emitters,
        buildings,
        np.zeros(len(emitters)),
        fuel_available_fv=0.0,
        dt_hours=0.25,
        control=control,
    )
    assert not step.network_served["house_1"]
    assert not step.network_served["hunting_base"]
