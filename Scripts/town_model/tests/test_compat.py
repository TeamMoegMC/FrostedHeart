from pathlib import Path

import pytest

from Scripts.town_model.audit import run_audit
from Scripts.town_model.compat import (
    current_block_temperature,
    current_heat_at_point,
    current_heat_ceiling,
    current_heating_radius,
    current_house_settles,
    current_network_extra_fuel_per_hour,
    current_radiator_radius,
    legacy_temperature_modifier,
)
from Scripts.town_model.config import load_scenario


ROOT = Path(__file__).resolve().parents[3]
SCENARIO = ROOT / "Scripts/town_model/scenarios/reference.toml"


def test_current_block_temperature_uses_ceiling_not_addition():
    assert current_block_temperature(-30.0, 20.0) == 10.0
    assert current_block_temperature(-5.0, 20.0) == 20.0
    assert current_block_temperature(25.0, 20.0) == 25.0


def test_current_heat_areas_take_maximum():
    assert current_heat_at_point(10.0, 20.0, 15.0) == 20.0
    assert current_heat_at_point(-4.0) == 0.0


def test_current_generator_and_radiator_levels():
    assert current_heating_radius(1.0) == 16
    assert current_heating_radius(2.0) == 24
    assert current_radiator_radius(1.0) == 8
    assert current_radiator_radius(2.0) == 16
    assert current_heat_ceiling(3.0) == 30


def test_current_network_fuel_is_quantised_per_tick():
    assert current_network_extra_fuel_per_hour(3.0) == 0.0
    assert current_network_extra_fuel_per_hour(4.0) == 1000.0
    assert current_network_extra_fuel_per_hour(25.0) == 6000.0


def test_legacy_building_modifier_and_cold_stasis_fixture():
    assert legacy_temperature_modifier(True, 1.0) == 24.0
    assert legacy_temperature_modifier(True, 3.0) == 30.0
    assert legacy_temperature_modifier(False, 3.0) == 0.0
    assert current_house_settles(True, 0.0)
    assert current_house_settles(True, 50.0)
    assert not current_house_settles(True, -0.01)
    assert not current_house_settles(True, 50.01)


def test_audit_matches_checked_in_java_sources():
    scenario = load_scenario(SCENARIO)
    results = run_audit(scenario, ROOT)
    assert results
    assert {result.status for result in results} == {"pass"}

