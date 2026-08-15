"""Hourly climate traces and player-visible forecast categories."""

from __future__ import annotations

import math
import numpy as np

from .config import ClimateSpec


BOTTOMS_C = np.asarray([-10, -20, -30, -40, -50, -60, -70, -80, -90], dtype=float)


def forecast_temperature_level(temperature_c: float) -> int:
    """Mirror ``WeatherForecast.getTemperatureLevel`` from Frosted Heart."""
    if temperature_c >= 4.0:
        return 1
    if temperature_c <= -2.0:
        for index in range(len(BOTTOMS_C) - 1, -1, -1):
            if temperature_c < BOTTOMS_C[index] + 2.0:
                return -index - 1
    return 0


def forecast_levels(temperatures_c: np.ndarray) -> np.ndarray:
    return np.fromiter(
        (forecast_temperature_level(float(value)) for value in temperatures_c),
        dtype=np.int16,
        count=len(temperatures_c),
    )


def _daily_cycle(hours: np.ndarray, amplitude: float) -> np.ndarray:
    # Coldest shortly before dawn, warmest shortly after noon.
    return amplitude * np.sin(2.0 * math.pi * (hours - 8.0) / 24.0)


def _add_event(track: np.ndarray, start: int, length: int, amplitude: float) -> None:
    stop = min(len(track), start + max(1, length))
    if stop <= start:
        return
    phase = np.linspace(0.0, math.pi, stop - start, endpoint=True)
    track[start:stop] = amplitude * np.sin(phase)


def generate_stochastic_climate(
    spec: ClimateSpec,
    days: int,
    dt_hours: float,
    seed: int,
) -> np.ndarray:
    """Generate three-track cold/warm events with hourly-scale output.

    Frosted Heart combines the strongest negative and strongest positive track.
    The configurable generator intentionally mirrors that property while keeping
    event amplitude distributions visible and editable in TOML.
    """
    steps = int(round(days * 24 / dt_hours))
    rng = np.random.default_rng(seed)
    tracks = np.zeros((max(1, spec.tracks), steps), dtype=float)
    steps_per_day = 24.0 / dt_hours
    for track in tracks:
        cursor = int(rng.uniform(0.0, spec.calm_days_max) * steps_per_day)
        while cursor < steps:
            duration = int(rng.uniform(spec.event_days_min, spec.event_days_max) * steps_per_day)
            if rng.random() < spec.warm_probability:
                amplitude = rng.uniform(spec.warm_amplitude_min_c, spec.warm_amplitude_max_c)
            else:
                # Squaring biases ordinary events toward the mild end while
                # retaining rare, very deep cold waves.
                fraction = rng.random() ** max(0.01, spec.cold_amplitude_shape)
                amplitude = -(
                    spec.cold_amplitude_min_c
                    + fraction * (spec.cold_amplitude_max_c - spec.cold_amplitude_min_c)
                )
            _add_event(track, cursor, duration, amplitude)
            calm = int(rng.uniform(spec.calm_days_min, spec.calm_days_max) * steps_per_day)
            cursor += max(1, duration + calm)

    negative = np.min(np.minimum(tracks, 0.0), axis=0)
    positive = np.max(np.maximum(tracks, 0.0), axis=0)
    hours = np.arange(steps, dtype=float) * dt_hours
    return (
        spec.baseline_c
        + spec.local_offset_c
        + negative
        + positive
        + _daily_cycle(hours, spec.daily_swing_c)
    )


def generate_design_climate(spec: ClimateSpec, days: int, dt_hours: float) -> np.ndarray:
    steps = int(round(days * 24 / dt_hours))
    hours = np.arange(steps, dtype=float) * dt_hours
    trace = spec.baseline_c + spec.local_offset_c + _daily_cycle(hours, spec.daily_swing_c)
    start = int(round(spec.design_cold_start_day * 24 / dt_hours))
    duration = int(round(spec.design_cold_duration_days * 24 / dt_hours))
    pulse = np.zeros(steps, dtype=float)
    _add_event(pulse, start, duration, -abs(spec.design_cold_amplitude_c))
    return trace + pulse
