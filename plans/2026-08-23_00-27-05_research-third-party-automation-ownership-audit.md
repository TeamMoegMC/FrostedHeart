# Third-Party Research Automation Ownership Audit

- Time: `2026-08-23 00:27:05 +0800`
- Authors: `Codex; OpenAI; implementation planning role`
- Status: `ready`
- Scope: Installed third-party machines, recipe executors, automated placers, multiblock formation, and capability paths that can consume or produce research-locked content
- Related: [`docs/research/gameplay-and-integrations.md`](../docs/research/gameplay-and-integrations.md), [`docs/research/known-risks.md`](../docs/research/known-risks.md), `ResearchHooks`, `IOwnerTile`

## Goal

Inventory every execution path in the supported companion pack that can bypass a player crafting/use check, decide whether research should control it, and either integrate the explicit player/team-or-machine-owner contract or document an intentional bypass.

## Verified Current State

- Player crafting, block use, campfire cooking, IE multiblock formation, IE assembler patterns, Create mechanical crafting, and generator upgrades have explicit research checks.
- Player paths resolve the real player's current Chorda team. Machine paths resolve a persisted owner. Restricted content fails closed for FakePlayer, null player, ownerless machine, or unresolved owner; unrestricted content remains available.
- The framework does not intercept unknown recipe-manager, capability, scripted, or third-party execution paths automatically.

## Steps

1. Freeze the companion mod list and enumerate machines or scripts that execute crafting, smelting/cooking, assembly, multiblock formation, block interaction, placement, or direct capability transfers.
2. Trace each candidate from its actual execution/mutation site, not only its preview or recipe lookup UI.
3. Classify the path as player-owned, machine-owned, globally intentional, or out of research scope.
4. For controlled paths, identify a persistent owner source and call the shared authorization predicate at the last side-effect-free point before execution. Never infer ownership from the nearest online player.
5. Specify legacy ownerless behavior. Default to unrestricted recipes allowed and research-locked recipes denied unless a real-player interaction can safely claim the machine.
6. Add focused tests for unlocked owner, locked owner, other team, ownerless state, FakePlayer, nested/adjacent machines, reload, and save/reload owner persistence.
7. Update the companion/mod living documentation with every integrated path and every explicitly accepted bypass.

## Validation

- A searchable matrix names every audited mod, class/method or script identifier, action, owner source, lock type, and outcome.
- Every controlled path has a test that exercises its real execution point and proves no output/input mutation on denial.
- Two adjacent machines owned by different teams cannot exchange authorization context.
- Ownerless and FakePlayer execution is fail-closed only for restricted content.
- Full runtime, no-FTB/no-JEI supported runtime, `./gradlew test`, relevant GameTests, both repositories' catalogue validation, and `git diff --check` pass.

## Documentation Impact

Update `docs/research/gameplay-and-integrations.md` and `docs/research/known-risks.md`. Add a completion diary entry and mark this plan `completed`, `superseded`, or `abandoned` with its outcome.

## Open Questions

- Which script-only transformations are intended to respect research locks versus remain infrastructure-level exemptions?
- Which installed machines already persist a trustworthy team/owner, and which need an explicit first-real-player claim or placement owner field?
