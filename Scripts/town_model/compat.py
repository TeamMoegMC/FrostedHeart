"""Pure functions that lock down notable current-code behavior."""

from __future__ import annotations

import math


TICKS_PER_GAME_HOUR = 1000


def current_block_temperature(nature_c: float, heat_ceiling_c: float) -> float:
    """Mirror ``WorldTemperature.block`` after nature and heat are known."""
    if nature_c > heat_ceiling_c:
        result = nature_c
    else:
        result = min(nature_c + heat_ceiling_c * 2.0, heat_ceiling_c)
    return max(-273.0, result)


def current_heat_at_point(*heat_values_c: float) -> float:
    """Current chunk heat areas use the greatest positive adjustment."""
    return max(0.0, *heat_values_c)


def current_heating_radius(range_level: float) -> int:
    if range_level <= 1.0:
        return int(16.0 * range_level)
    return int(16.0 + (range_level - 1.0) * 8.0)


def current_radiator_radius(range_level: float) -> int:
    if range_level <= 1.0:
        return int(8.0 * range_level)
    return int(8.0 + (range_level - 1.0) * 8.0)


def current_heat_ceiling(temp_level: float) -> int:
    return int(10.0 * temp_level)


def current_network_extra_fuel_per_hour(
    heat_used_per_tick: float,
    heat_efficiency: float = 1.2,
) -> float:
    """Mirror the generator's per-tick integer fuel quantisation."""
    per_tick = math.floor(heat_used_per_tick / heat_efficiency / 25.0 * 8.0)
    return float(max(0, per_tick) * TICKS_PER_GAME_HOUR)


def legacy_temperature_modifier(
    drained: bool,
    temperature_level: float,
    scale_c: float = 10.0,
    minimum_c: float = 24.0,
) -> float:
    if not drained:
        return 0.0
    return max(temperature_level * scale_c, minimum_c)


def current_house_settles(structure_valid: bool, temperature_c: float) -> bool:
    """Expose the current cold-house stasis bug as a regression fixture."""
    return structure_valid and 0.0 <= temperature_c <= 50.0

