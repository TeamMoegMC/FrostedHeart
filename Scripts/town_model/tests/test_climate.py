import numpy as np

from Scripts.town_model.climate import (
    forecast_temperature_level,
    generate_stochastic_climate,
)
from Scripts.town_model.config import load_scenario


def test_forecast_temperature_levels_match_thresholds():
    assert forecast_temperature_level(4.0) == 1
    assert forecast_temperature_level(0.0) == 0
    assert forecast_temperature_level(-7.9) == 0
    assert forecast_temperature_level(-8.1) == -1
    assert forecast_temperature_level(-88.1) == -9


def test_stochastic_climate_is_seeded(reference_scenario):
    spec = reference_scenario.climate
    first = generate_stochastic_climate(spec, 10, 0.25, 123)
    second = generate_stochastic_climate(spec, 10, 0.25, 123)
    third = generate_stochastic_climate(spec, 10, 0.25, 124)
    np.testing.assert_array_equal(first, second)
    assert not np.array_equal(first, third)

