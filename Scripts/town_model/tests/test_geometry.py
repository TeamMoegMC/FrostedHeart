from dataclasses import replace
import math

import pytest

from Scripts.town_model.geometry import (
    Box,
    boxes_overlap,
    coverage_patterns,
    covered_fraction,
    heat_footprint,
    sphere_intersection_volume,
    sphere_volume,
)


def test_box_measures_and_overlap():
    left = Box("left", (0, 0, 0), (2, 3, 4))
    touching = Box("touching", (2, 0, 0), (1, 1, 1))
    overlapping = Box("overlap", (1, 0, 0), (2, 1, 1))
    assert left.volume == 24
    assert left.floor_area == 8
    assert left.envelope_area == 52
    assert not boxes_overlap(left, touching)
    assert boxes_overlap(left, overlapping)


def test_building_coverage_uses_discrete_voxel_centres(reference_scenario):
    building = reference_scenario.buildings[0]
    tower = reference_scenario.emitters[0]
    patterns = coverage_patterns(building, (tower,))
    assert patterns.sum() == pytest.approx(1.0)
    assert covered_fraction(patterns) == 1.0
    far_tower = replace(tower, position=(100.0, 0.0, 100.0))
    assert covered_fraction(coverage_patterns(building, (far_tower,))) == 0.0


def test_sphere_overlap_is_zero_for_tangent_spheres(reference_scenario):
    assert sphere_intersection_volume(4.0, 6.0, 10.0) == 0.0
    assert sphere_intersection_volume(4.0, 8.0, 1.0) == pytest.approx(sphere_volume(4.0))
    tower = reference_scenario.emitters[0]
    same = replace(tower, name="same")
    footprint = heat_footprint((tower, same))
    assert footprint.overlap_ratio == pytest.approx(0.5)

