"""Drift audit between the compatibility model and selected Java facts."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re

from .config import Scenario


@dataclass(frozen=True)
class AuditCheck:
    name: str
    path: str
    pattern: str
    expected: str


@dataclass(frozen=True)
class AuditResult:
    name: str
    status: str
    expected: str
    path: str

    def as_dict(self) -> dict[str, str]:
        return {
            "name": self.name,
            "status": self.status,
            "expected": self.expected,
            "path": self.path,
        }


CHECKS = (
    AuditCheck(
        "block_temperature_ceiling",
        "src/main/java/com/teammoeg/frostedheart/content/climate/WorldTemperature.java",
        r"Math\.min\(nature\s*\+\s*heat\s*\*\s*2\s*,\s*heat\)",
        "nature + heat*2 capped by heat",
    ),
    AuditCheck(
        "heat_overlap_max",
        "src/main/java/com/teammoeg/frostedheart/content/climate/gamedata/chunkheat/ChunkHeatData.java",
        r"if\s*\(tmp\s*>\s*ret\)\s*ret\s*=\s*tmp",
        "overlapping heat areas take the greatest value",
    ),
    AuditCheck(
        "tower_radius_formula",
        "src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/HeatingState.java",
        r"16\s*\+\s*\(rlevel\s*-\s*1\)\s*\*\s*8",
        "radius 16 at level 1, +8 per level",
    ),
    AuditCheck(
        "radiator_radius_formula",
        "src/main/java/com/teammoeg/frostedheart/content/climate/block/radiator/RadiatorState.java",
        r"8\s*\+\s*\(rlevel\s*-\s*1\)\s*\*\s*8",
        "radiator radius 8 at level 1, 16 at level 2",
    ),
    AuditCheck(
        "radiator_heat_endpoint",
        "src/main/java/com/teammoeg/frostedheart/content/climate/block/radiator/RadiatorState.java",
        r"HeatEndpoint\.consumer\(100,\s*4\)",
        "radiator consumes 4 heat/t",
    ),
    AuditCheck(
        "house_legacy_endpoint",
        "src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseBlockEntity.java",
        r"HeatEndpoint\.consumer\(99,\s*1\)",
        "house still directly consumes 1 heat/t",
    ),
    AuditCheck(
        "house_minimum_heat_modifier",
        "src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseBlockEntity.java",
        r"Math\.max\(endpoint\.getTempLevel\(\)\s*\*\s*10,\s*TownMathFunctions\.COMFORTABLE_TEMP_HOUSE\)",
        "direct endpoint adds at least the 24 C comfort value",
    ),
    AuditCheck(
        "network_fuel_quantisation",
        "src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/GeneratorData.java",
        r"powerRemain\s*/\s*efficiency\s*/\s*25f\s*\*\s*8f",
        "network heat fuel is floored per tick after /efficiency/25*8",
    ),
    AuditCheck(
        "forecast_horizon",
        "src/main/java/com/teammoeg/frostedheart/content/climate/gamedata/climate/WorldClimate.java",
        r"120\s*-\s*clockSource\.getHours\(\)\s*%\s*3",
        "forecast horizon is approximately 120 game hours",
    ),
)


def _find_repo_root(start: Path) -> Path:
    for candidate in (start, *start.parents):
        if (candidate / "src/main/java").exists():
            return candidate
    raise FileNotFoundError("could not locate FrostedHeart repository root")


def run_audit(scenario: Scenario, repo_root: Path | None = None) -> list[AuditResult]:
    root = _find_repo_root(scenario.source_path.parent) if repo_root is None else repo_root
    results: list[AuditResult] = []
    for check in CHECKS:
        path = root / check.path
        if not path.exists():
            status = "missing"
        else:
            text = path.read_text(encoding="utf-8")
            status = "pass" if re.search(check.pattern, text, flags=re.MULTILINE) else "drift"
        results.append(AuditResult(check.name, status, check.expected, check.path))
    return results
