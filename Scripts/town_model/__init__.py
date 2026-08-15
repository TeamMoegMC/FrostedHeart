"""Frosted Heart town sustainability simulator.

The package intentionally keeps the compatibility model and the proposed
RC model separate.  Importing it has no side effects; use ``town_sim.py`` for
the command line interface.
"""

from .config import Scenario, load_scenario
from .simulation import SimulationResult, run_monte_carlo, run_simulation

__all__ = [
    "Scenario",
    "SimulationResult",
    "load_scenario",
    "run_monte_carlo",
    "run_simulation",
]

