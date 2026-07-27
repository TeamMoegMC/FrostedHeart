# Hunting worker-slot config merge fix

- Time: `2026-07-27 15:38:45 +0800`
- Author: `Codex; OpenAI GPT-5; primary agent /root`
- Status: `completed`
- Scope: `HuntingBaseBlockEntity worker-slot calculation`

## Completed

- Restored the hunting-base worker-slot calculation lost during the scope merge.
- Effective floor area is once again divided by `HUNTING.floorBlocksPerWorkerSlot`, and the result is bounded by `HUNTING.minimumWorkerSlots`.

## Decisions

- Preserve the existing effective-floor-area formula and integer truncation while replacing only the regressed hard-coded values.
- Leave the unrelated `SimpleItemKey` merge error unchanged because it is outside this requested fix.

## Validation

- `git diff --check` passed.
- `./gradlew compileJava --offline` succeeded in the existing temporary review copy after removing its known `SimpleItemKey` extra-brace blocker; only the repository's existing 20 deprecation warnings remained.

## Remaining

- The main working tree still cannot compile until the unrelated extra closing brace in `SimpleItemKey.java` is fixed.
