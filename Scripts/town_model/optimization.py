"""Seeded layout search for buildings and networked heaters."""

from __future__ import annotations

from dataclasses import dataclass, replace
import math
from typing import Sequence

import numpy as np

from .config import BuildingSpec, EmitterSpec, Scenario
from .geometry import (
    box_from_spec,
    boxes_overlap,
    coverage_patterns,
    covered_fraction,
    heat_footprint,
    sphere_surface,
    sphere_volume,
)


@dataclass(frozen=True)
class LayoutCandidate:
    score: float
    coverage: float
    minimum_coverage: float
    overlap_ratio: float
    footprint_blocks2: float
    steady_heat_loss_fv_per_hour: float
    buildings: tuple[BuildingSpec, ...]
    emitters: tuple[EmitterSpec, ...]
    design_deaths: int | None = None
    design_minimum_health: float | None = None
    design_fuel_used_fv: float | None = None

    def as_dict(self) -> dict[str, object]:
        return {
            "score": self.score,
            "coverage": self.coverage,
            "minimum_coverage": self.minimum_coverage,
            "overlap_ratio": self.overlap_ratio,
            "footprint_blocks2": self.footprint_blocks2,
            "steady_heat_loss_fv_per_hour": self.steady_heat_loss_fv_per_hour,
            "buildings": {
                item.name: list(item.position) for item in self.buildings
            },
            "emitters": {item.name: list(item.position) for item in self.emitters},
            "design_deaths": self.design_deaths,
            "design_minimum_health": self.design_minimum_health,
            "design_fuel_used_fv": self.design_fuel_used_fv,
        }


def _layout_footprint(buildings: Sequence[BuildingSpec]) -> float:
    min_x = min(item.position[0] for item in buildings)
    min_z = min(item.position[2] for item in buildings)
    max_x = max(item.position[0] + item.size[0] for item in buildings)
    max_z = max(item.position[2] + item.size[2] for item in buildings)
    return float((max_x - min_x) * (max_z - min_z))


def evaluate_layout(
    scenario: Scenario,
    buildings: Sequence[BuildingSpec],
    emitters: Sequence[EmitterSpec],
) -> LayoutCandidate:
    invalid_overlaps = 0
    for left_index, left in enumerate(buildings):
        for right in buildings[left_index + 1 :]:
            invalid_overlaps += int(boxes_overlap(box_from_spec(left), box_from_spec(right)))
    coverages = [covered_fraction(coverage_patterns(item, emitters)) for item in buildings]
    weights = [max(1, item.capacity) for item in buildings]
    weighted_coverage = float(np.average(coverages, weights=weights))
    minimum_coverage = min(coverages, default=0.0)
    heat = heat_footprint(emitters)
    footprint = _layout_footprint(buildings)
    ambient = scenario.climate.baseline_c + scenario.climate.local_offset_c
    field = scenario.heat_field
    steady_loss = 0.0
    for emitter in emitters:
        conductance = (
            field.envelope_loss_fv_per_block2_c_hour * sphere_surface(emitter.radius_blocks)
            + field.volume_leak_fv_per_block3_c_hour * sphere_volume(emitter.radius_blocks)
        )
        steady_loss += conductance * max(0.0, emitter.setpoint_c - ambient)
    uncovered_penalty = sum(
        max(1, item.capacity) * (1.0 - coverage)
        for item, coverage in zip(buildings, coverages)
    )
    score = (
        invalid_overlaps * 1e9
        + uncovered_penalty * 1e6
        + heat.overlap_ratio * 1e5
        + steady_loss
        + footprint
    )
    return LayoutCandidate(
        score=score,
        coverage=weighted_coverage,
        minimum_coverage=minimum_coverage,
        overlap_ratio=heat.overlap_ratio,
        footprint_blocks2=footprint,
        steady_heat_loss_fv_per_hour=steady_loss,
        buildings=tuple(buildings),
        emitters=tuple(emitters),
    )


def _mutate(
    rng: np.random.Generator,
    buildings: tuple[BuildingSpec, ...],
    emitters: tuple[EmitterSpec, ...],
    radius: int,
) -> tuple[tuple[BuildingSpec, ...], tuple[EmitterSpec, ...]]:
    movable_buildings = [index for index, item in enumerate(buildings) if item.movable]
    movable_emitters = [index for index, item in enumerate(emitters) if item.movable]
    total = len(movable_buildings) + len(movable_emitters)
    if total == 0:
        return buildings, emitters
    choice = int(rng.integers(total))
    dx, dz = rng.integers(-3, 4, size=2)
    if dx == 0 and dz == 0:
        dx = 1
    if choice < len(movable_buildings):
        index = movable_buildings[choice]
        item = buildings[index]
        x = int(np.clip(item.position[0] + dx, -radius, radius))
        z = int(np.clip(item.position[2] + dz, -radius, radius))
        mutable = list(buildings)
        mutable[index] = replace(item, position=(x, item.position[1], z))
        return tuple(mutable), emitters
    index = movable_emitters[choice - len(movable_buildings)]
    item = emitters[index]
    x = float(np.clip(item.position[0] + dx, -radius, radius))
    z = float(np.clip(item.position[2] + dz, -radius, radius))
    mutable_emitters = list(emitters)
    mutable_emitters[index] = replace(item, position=(x, item.position[1], z))
    return buildings, tuple(mutable_emitters)


def _dominates(left: LayoutCandidate, right: LayoutCandidate) -> bool:
    left_values = (
        1.0 - left.minimum_coverage,
        left.overlap_ratio,
        left.footprint_blocks2,
        left.steady_heat_loss_fv_per_hour,
    )
    right_values = (
        1.0 - right.minimum_coverage,
        right.overlap_ratio,
        right.footprint_blocks2,
        right.steady_heat_loss_fv_per_hour,
    )
    return all(a <= b for a, b in zip(left_values, right_values)) and any(
        a < b for a, b in zip(left_values, right_values)
    )


def pareto_front(candidates: Sequence[LayoutCandidate]) -> list[LayoutCandidate]:
    front = [
        candidate
        for candidate in candidates
        if not any(
            _dominates(other, candidate) for other in candidates if other is not candidate
        )
    ]
    return sorted(front, key=lambda item: item.score)


def optimize_layout(
    scenario: Scenario,
    population: int,
    *,
    seed: int | None = None,
    restarts: int | None = None,
    iterations: int | None = None,
    verify_candidates: int = 3,
) -> list[LayoutCandidate]:
    rng = np.random.default_rng(
        scenario.simulation.seed if seed is None else seed
    )
    restart_count = scenario.layout.restarts if restarts is None else restarts
    iteration_count = scenario.layout.iterations if iterations is None else iterations
    base_buildings = scenario.active_buildings(population)
    base_emitters = scenario.active_emitters(population)
    collected: list[LayoutCandidate] = []
    for restart in range(max(1, restart_count)):
        buildings = base_buildings
        emitters = base_emitters
        # Diversify restarts without throwing away a known-feasible seed layout.
        for _ in range(restart * 4):
            buildings, emitters = _mutate(
                rng, buildings, emitters, scenario.layout.search_radius_blocks
            )
        current = evaluate_layout(scenario, buildings, emitters)
        best = current
        for iteration in range(max(1, iteration_count)):
            trial_buildings, trial_emitters = _mutate(
                rng, buildings, emitters, scenario.layout.search_radius_blocks
            )
            trial = evaluate_layout(scenario, trial_buildings, trial_emitters)
            fraction = iteration / max(1, iteration_count - 1)
            temperature = max(1.0, 50000.0 * (1.0 - fraction) ** 2)
            accept = trial.score <= current.score or rng.random() < math.exp(
                min(0.0, (current.score - trial.score) / temperature)
            )
            if accept:
                buildings, emitters, current = trial_buildings, trial_emitters, trial
            if current.score < best.score:
                best = current
        collected.extend((best, current))
    front = pareto_front(collected)
    if verify_candidates <= 0:
        return front
    from .simulation import run_simulation

    verified: list[LayoutCandidate] = []
    for index, candidate in enumerate(front):
        if index >= verify_candidates:
            verified.append(candidate)
            continue
        candidate_scenario = replace(
            scenario,
            buildings=candidate.buildings,
            emitters=candidate.emitters,
        )
        result = run_simulation(
            candidate_scenario,
            "target_rc",
            "forecast",
            population,
            days=scenario.simulation.days,
            seed=scenario.simulation.seed if seed is None else seed,
            climate_mode="design",
            collect_timeseries=False,
        )
        verified.append(
            replace(
                candidate,
                design_deaths=result.summary.deaths,
                design_minimum_health=result.summary.minimum_health,
                design_fuel_used_fv=result.summary.fuel_used_fv,
            )
        )
    return verified
