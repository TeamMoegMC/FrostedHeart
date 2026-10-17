# Research JEI revoke investigation paused

- Time: `2026-08-25 22:50:00 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `partial`
- Scope: `V2 Design revoke and JEI recipe visibility`

## Completed

- Confirmed that `frostedresearch_test:smoke_design` targets the exact recipe ID `minecraft:stick`, whose ingredient is the wooden-planks tag.
- Confirmed through `/research result info` that the server-side team state no longer contains the Design result after revoke.
- Replaced JEI recipe-object identity matching with stable recipe-ID indexing of JEI runtime registrations.
- Routed JEI synchronization requests onto the Minecraft client thread after the runtime log showed JEI rejecting calls made from `Server thread`.
- Made idempotent grant/revoke of a known result resend the full knowledge snapshot to reconcile stale client projections.

## Decisions

- Investigation is intentionally stopped at the user's request. The current behavior must not be documented as fixed.
- The access model remains recipe-ID-specific; this report concerns the exact `minecraft:stick` recipe rather than unrelated recipes that share the stick output.

## Validation

- `./gradlew compileJava` passed after the attempted fixes.
- `git diff --check` passed.
- Manual in-game verification still failed: after `smoke_design` was revoked, JEI continued to display the `minecraft:stick` wooden-planks-tag recipe.

## Remaining

- Determine why JEI still presents `minecraft:stick` despite the client projection refresh and stable-ID visibility update.
- On resumption, inspect the post-fix client log and the actual JEI hidden-recipe state for `RecipeTypes.CRAFTING`; do not assume the previous object-identity or thread-affinity hypotheses fully explain the remaining behavior.
- Reconcile or revert the two attempted JEI fixes if further investigation shows they are unnecessary or incorrect.
