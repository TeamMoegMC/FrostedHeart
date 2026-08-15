from dataclasses import replace

import pytest

from Scripts.town_model.optimization import optimize_layout
from Scripts.town_model.simulation import run_simulation


def test_seeded_simulation_is_reproducible(reference_scenario):
    first = run_simulation(
        reference_scenario, "target_rc", "forecast", 8,
        days=2, seed=77, collect_timeseries=False,
    )
    second = run_simulation(
        reference_scenario, "target_rc", "forecast", 8,
        days=2, seed=77, collect_timeseries=False,
    )
    assert first.summary == second.summary


def test_fuel_stock_obeys_whole_system_conservation(reference_scenario):
    result = run_simulation(
        reference_scenario, "target_rc", "none", 8,
        days=1, seed=2, climate_mode="design", collect_timeseries=False,
    )
    initial = (
        reference_scenario.inventory.initial.get("fuel_fv", 0.0)
        + 8 * reference_scenario.inventory.initial_per_person["fuel_fv"]
    )
    assert result.summary.final_fuel_fv == pytest.approx(
        initial + result.summary.fuel_produced_fv - result.summary.fuel_used_fv
    )


def test_target_cold_house_still_consumes_food_but_current_house_stalls(reference_scenario):
    climate = replace(
        reference_scenario.climate,
        baseline_c=-100.0,
        local_offset_c=0.0,
        daily_swing_c=0.0,
        design_cold_amplitude_c=0.0,
    )
    inventory = replace(
        reference_scenario.inventory,
        initial={"biomass": 0.0},
        initial_per_person={"fuel_fv": 0.0, "food_energy": 100.0, "nutrition": 20.0},
    )
    scenario = replace(reference_scenario, climate=climate, inventory=inventory)
    current = run_simulation(
        scenario, "current_compat", "none", 8,
        days=1, seed=1, climate_mode="design", collect_timeseries=False,
    )
    target = run_simulation(
        scenario, "target_rc", "none", 8,
        days=1, seed=1, climate_mode="design", collect_timeseries=False,
    )
    assert current.summary.final_food_energy == pytest.approx(800.0)
    assert target.summary.final_food_energy < current.summary.final_food_energy


def test_small_layout_search_is_seeded(reference_scenario):
    first = optimize_layout(
        reference_scenario, 8, seed=9, restarts=1, iterations=5, verify_candidates=0
    )
    second = optimize_layout(
        reference_scenario, 8, seed=9, restarts=1, iterations=5, verify_candidates=0
    )
    assert [item.as_dict() for item in first] == [item.as_dict() for item in second]
