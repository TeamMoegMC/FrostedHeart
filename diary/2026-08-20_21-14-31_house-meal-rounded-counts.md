# House meal rounded item counts

- Time: `2026-08-20 21:14:31 +0800`
- Author: `Codex; OpenAI; implementation collaborator`
- Status: `completed`
- Scope: `house daily meal UI`

## Completed

- Changed the item-grid count overlay in `HouseMealElement` to round the actual consumed amount to an integer.
- Kept the authoritative fractional `MealEntry.amount` unchanged and exposed it in the item tooltip.
- Updated nutrition and town living docs to distinguish rounded display counts from exact settlement data.

## Decisions

- Rounding is presentation-only and never changes warehouse consumption, resident nutrition, persistence, or synchronization.

## Validation

- Java compilation and `git diff --check` passed.

## Remaining

- None.
