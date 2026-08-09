#!/usr/bin/env python3
"""Command line entry point for the Frosted Heart town simulator."""

from __future__ import annotations

import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import sys

from town_model.audit import run_audit
from town_model.config import load_scenario
from town_model.simulation import MonteCarloResult, run_monte_carlo, run_simulation


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
DEFAULT_SCENARIO = SCRIPT_DIR / "town_model/scenarios/reference.toml"


def _output_dir(value: str | None, command: str) -> Path:
    if value:
        return Path(value).resolve()
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    return REPO_ROOT / "build/town-sim" / f"{stamp}-{command}"


def _csv_ints(value: str) -> list[int]:
    return [int(item.strip()) for item in value.split(",") if item.strip()]


def _csv_strings(value: str) -> list[str]:
    return [item.strip() for item in value.split(",") if item.strip()]


def _scenario(args: argparse.Namespace):
    return load_scenario(args.scenario)


def command_audit(args: argparse.Namespace) -> int:
    scenario = _scenario(args)
    results = run_audit(scenario, REPO_ROOT)
    payload = [item.as_dict() for item in results]
    if args.json:
        print(json.dumps(payload, ensure_ascii=False, indent=2))
    else:
        for item in results:
            print(f"[{item.status.upper():7}] {item.name}: {item.expected}")
    return 0 if all(item.status == "pass" for item in results) else 2


def command_simulate(args: argparse.Namespace) -> int:
    from town_model.reporting import write_simulation_report

    scenario = _scenario(args)
    populations = _csv_ints(args.populations) if args.populations else list(scenario.simulation.populations)
    profiles = _csv_strings(args.profiles)
    policies = _csv_strings(args.policies)
    results: list[MonteCarloResult] = []
    if args.design:
        for profile in profiles:
            for policy in policies:
                for population in populations:
                    result = run_simulation(
                        scenario,
                        profile,
                        policy,
                        population,
                        days=args.days,
                        seed=args.seed,
                        climate_mode="design",
                        collect_timeseries=True,
                    )
                    results.append(MonteCarloResult([result.summary], result))
    else:
        for profile in profiles:
            for policy in policies:
                for population in populations:
                    results.append(
                        run_monte_carlo(
                            scenario,
                            profile,
                            policy,
                            population,
                            days=args.days,
                            runs=args.runs,
                            seed=args.seed,
                            workers=args.workers,
                        )
                    )
    output = _output_dir(args.out, "simulate")
    report = write_simulation_report(output, results, title=scenario.name)
    print(report)
    for result in results:
        print(json.dumps(result.aggregate(), ensure_ascii=False))
    return 0


def command_optimize(args: argparse.Namespace) -> int:
    from town_model.optimization import optimize_layout
    from town_model.reporting import write_layout_report

    scenario = _scenario(args)
    candidates = optimize_layout(
        scenario,
        args.population,
        seed=args.seed,
        restarts=args.restarts,
        iterations=args.iterations,
        verify_candidates=args.verify_candidates,
    )
    output = _output_dir(args.out, "layout")
    report = write_layout_report(output, candidates)
    print(report)
    if candidates:
        print(json.dumps(candidates[0].as_dict(), ensure_ascii=False, indent=2))
    return 0


def command_sensitivity(args: argparse.Namespace) -> int:
    from town_model.reporting import write_sensitivity_report
    from town_model.sensitivity import run_sensitivity

    scenario = _scenario(args)
    rows = run_sensitivity(
        scenario,
        args.profile,
        args.policy,
        args.population,
        fraction=args.fraction,
        days=args.days,
        seed=args.seed,
    )
    output = _output_dir(args.out, "sensitivity")
    report = write_sensitivity_report(output, rows)
    print(report)
    for row in rows:
        print(f"{row.parameter:32} {row.local_slope:12.3f}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Frosted Heart town critical self-sufficiency simulator"
    )
    parser.add_argument("--scenario", default=str(DEFAULT_SCENARIO), help="scenario TOML")
    subparsers = parser.add_subparsers(dest="command", required=True)

    audit = subparsers.add_parser("audit", help="check compatibility assumptions against Java")
    audit.add_argument("--json", action="store_true")
    audit.set_defaults(handler=command_audit)

    simulate = subparsers.add_parser("simulate", help="run deterministic or Monte Carlo simulations")
    simulate.add_argument("--profiles", default="current_compat,target_rc")
    simulate.add_argument("--policies", default="none,forecast,conservative")
    simulate.add_argument("--populations", default=None, help="comma-separated resident counts")
    simulate.add_argument("--days", type=int, default=None)
    simulate.add_argument("--runs", type=int, default=None)
    simulate.add_argument("--seed", type=int, default=None)
    simulate.add_argument(
        "--workers",
        type=int,
        default=min(8, os.cpu_count() or 1),
        help="parallel Monte Carlo worker processes",
    )
    simulate.add_argument("--design", action="store_true", help="run the deterministic design cold wave")
    simulate.add_argument("--out", default=None)
    simulate.set_defaults(handler=command_simulate)

    optimize = subparsers.add_parser("optimize-layout", help="search the layout Pareto front")
    optimize.add_argument("--population", type=int, default=24)
    optimize.add_argument("--seed", type=int, default=None)
    optimize.add_argument("--restarts", type=int, default=None)
    optimize.add_argument("--iterations", type=int, default=None)
    optimize.add_argument("--verify-candidates", type=int, default=3)
    optimize.add_argument("--out", default=None)
    optimize.set_defaults(handler=command_optimize)

    sensitivity = subparsers.add_parser("sensitivity", help="rank exposed designer parameters")
    sensitivity.add_argument("--profile", default="target_rc")
    sensitivity.add_argument("--policy", default="forecast")
    sensitivity.add_argument("--population", type=int, default=24)
    sensitivity.add_argument("--fraction", type=float, default=0.10)
    sensitivity.add_argument("--days", type=int, default=None)
    sensitivity.add_argument("--seed", type=int, default=None)
    sensitivity.add_argument("--out", default=None)
    sensitivity.set_defaults(handler=command_sensitivity)
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return int(args.handler(args))


if __name__ == "__main__":
    sys.exit(main())
