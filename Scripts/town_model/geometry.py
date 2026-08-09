"""Discrete building geometry and spherical heat-field coverage."""

from __future__ import annotations

from dataclasses import dataclass
import math
from typing import Iterable

import numpy as np

from .config import BuildingSpec, EmitterSpec


@dataclass(frozen=True)
class Box:
    name: str
    position: tuple[int, int, int]
    size: tuple[int, int, int]

    @property
    def volume(self) -> int:
        return self.size[0] * self.size[1] * self.size[2]

    @property
    def floor_area(self) -> int:
        return self.size[0] * self.size[2]

    @property
    def envelope_area(self) -> int:
        x, y, z = self.size
        return 2 * (x * y + x * z + y * z)

    def voxel_centers(self) -> np.ndarray:
        x, y, z = self.size
        grid = np.indices((x, y, z), dtype=float).reshape(3, -1).T
        origin = np.asarray(self.position, dtype=float)
        return grid + origin + 0.5


def box_from_spec(spec: BuildingSpec) -> Box:
    return Box(spec.name, spec.position, spec.size)


def boxes_overlap(left: Box, right: Box) -> bool:
    for axis in range(3):
        left_min = left.position[axis]
        left_max = left_min + left.size[axis]
        right_min = right.position[axis]
        right_max = right_min + right.size[axis]
        if left_max <= right_min or right_max <= left_min:
            return False
    return True


def sphere_volume(radius: float) -> float:
    return 4.0 * math.pi * radius**3 / 3.0


def sphere_surface(radius: float) -> float:
    return 4.0 * math.pi * radius**2


def sphere_intersection_volume(radius_a: float, radius_b: float, distance: float) -> float:
    if distance >= radius_a + radius_b:
        return 0.0
    if distance <= abs(radius_a - radius_b):
        return sphere_volume(min(radius_a, radius_b))
    numerator = math.pi * (radius_a + radius_b - distance) ** 2
    polynomial = (
        distance**2
        + 2.0 * distance * (radius_a + radius_b)
        - 3.0 * (radius_a - radius_b) ** 2
    )
    return numerator * polynomial / (12.0 * distance)


def emitter_membership(points: np.ndarray, emitter: EmitterSpec) -> np.ndarray:
    center = np.asarray(emitter.position, dtype=float) + 0.5
    return np.sum((points - center) ** 2, axis=1) <= emitter.radius_blocks**2


def coverage_patterns(building: BuildingSpec, emitters: Iterable[EmitterSpec]) -> np.ndarray:
    """Return fractions indexed by emitter-membership bit mask.

    Storing membership patterns, instead of just one covered fraction, lets the
    simulator take the maximum of emitters whose temperatures change over time.
    """
    emitter_list = tuple(emitters)
    points = box_from_spec(building).voxel_centers()
    patterns = np.zeros(len(points), dtype=np.int64)
    for index, emitter in enumerate(emitter_list):
        patterns |= emitter_membership(points, emitter).astype(np.int64) << index
    counts = np.bincount(patterns, minlength=1 << len(emitter_list)).astype(float)
    return counts / max(1, len(points))


def covered_fraction(patterns: np.ndarray) -> float:
    return float(1.0 - patterns[0])


def mean_environment_temperature(
    patterns: np.ndarray,
    ambient_c: float,
    emitter_temperatures_c: np.ndarray,
) -> float:
    values = np.full(len(patterns), ambient_c, dtype=float)
    for mask in range(1, len(patterns)):
        active = [
            emitter_temperatures_c[index]
            for index in range(len(emitter_temperatures_c))
            if mask & (1 << index)
        ]
        values[mask] = max(ambient_c, max(active, default=ambient_c))
    return float(np.dot(patterns, values))


@dataclass(frozen=True)
class HeatFootprint:
    gross_volume: float
    pair_overlap_volume: float
    overlap_ratio: float


def heat_footprint(emitters: Iterable[EmitterSpec]) -> HeatFootprint:
    emitter_list = tuple(emitters)
    gross = sum(sphere_volume(item.radius_blocks) for item in emitter_list)
    overlap = 0.0
    for left_index, left in enumerate(emitter_list):
        for right in emitter_list[left_index + 1 :]:
            distance = math.dist(left.position, right.position)
            overlap += sphere_intersection_volume(
                left.radius_blocks, right.radius_blocks, distance
            )
    # Pairwise overlap is deliberately a waste proxy; triple overlap can make
    # the unbounded sum exceed the true union, so cap the ratio for reporting.
    ratio = min(1.0, overlap / gross) if gross else 0.0
    return HeatFootprint(gross, overlap, ratio)

