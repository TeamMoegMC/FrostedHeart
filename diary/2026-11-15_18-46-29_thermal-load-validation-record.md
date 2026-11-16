# Thermal: the 2026-11-15 100-player runs saved as a validation record

- Time: `2026-11-15 18:46:29 -0600`
- Author: `Claude (Anthropic Opus 5.5); implementer, decisions by the project owner`
- Status: `documentation only; no code changed`
- Scope: `docs/climate/validation/thermal-load-100p-2026-11-15.json`, `docs/climate/thermal-runtime-control.md`
- Follows: [storage pressure and open admissions](2026-11-15_15-25-49_thermal-storage-pressure-open-admission.md)

## Why

The six 100-player runs behind the storage-pressure fix were only summarised in the diary; their result files lived in
untracked `build/` directories. The owner asked for them to be kept in `docs/`.

## Completed

- `docs/climate/validation/thermal-load-100p-2026-11-15.json` (about 79 KB):
  - shared setup: machine, JVM, server properties, workload, launch and bot commands;
  - a summary table of r4 (branch before the fix), r5 (master control), r6 (unfixed, with stall dumps), r7 (superseded
    fix), r8 (final rule before the scan reorder) and r9 (committed code `a9ac2f83f`);
  - per run: code, purpose, finding, `measuring.json`, the full `measurement.json` and `recovery.json`, the 200-tick
    windows with age violations (with refusals, coordinator rounds and worker CPU), and the recovery timeline;
  - the Section checkpoint ticks of each base in r4's and r5's saved worlds;
  - one trimmed row of r6's `recovery-stall.jsonl`: the 748-Section batch at stage 1 waiting for memory and the watched
    base Sections held at 21260.
- The "扩容与内存压力" section of `docs/climate/thermal-runtime-control.md` links the record.

## Correction

The earlier entry says one chunk of base 5 stopped at 21460 in r4. The saved world shows three of its four interior
chunks at 21460 and one at 21920; bases 3 and 4 stopped in all four, at 21380 and 21480.

## Left out

`metrics.jsonl` (about 250 KB per run), the full stall dumps (up to 560 KB), `comparison-points.jsonl`, logs, GC logs
and JFR recordings stay in the build directories, which are not tracked.

## Documentation

- As above; no other living document changed.
