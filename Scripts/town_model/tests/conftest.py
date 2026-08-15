from pathlib import Path
import sys

import pytest

ROOT = Path(__file__).resolve().parents[3]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from Scripts.town_model.config import load_scenario


@pytest.fixture(scope="session")
def reference_scenario():
    return load_scenario(ROOT / "Scripts/town_model/scenarios/reference.toml")
