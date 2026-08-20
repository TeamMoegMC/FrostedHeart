# Frosted Research

- Status: `Current`
- Last verified: `2026-08-21`
- Scope: Implemented research definitions, progress, clues, drawing-desk archive, and player-visible graph GUI boundary
- Code anchors: `Research`, `ResearchData`, `TeamResearchData#checkResearchComplete`, `DrawDeskScreen`, `ResearchArchiveLayer`, `ResearchGraphViewport`, `ResearchProjectWorkspace`, `ResearchWorkspaceState`, `ResearchGraphLayoutEngine`

## What Is This System?

Frosted Research owns research definitions, team progress, clue triggers, experiment points, effects, and the drawing-desk research interface. The drawing desk now switches between its unchanged `387 x 203` work surface and a responsive research archive with a player-visible dependency graph, project index, concise right summary, and centered project-file dialog.

## What Is Authoritative?

Source and synchronized `ResearchData` are authoritative for implemented behavior. `Research#getClues()` defines clue order and properties, while `TeamResearchData#checkResearchComplete` decides completion. The GUI must only present or route to these operations; it must not create another progress model.

## Where Next?

Read [research-ui.md](research-ui.md) for current completion rules, graph navigation, project dialog, clue routing, refresh contracts, and the remaining implementation limits. Intended later phases remain in the [modern research GUI plan](../../plans/2026-08-20_17-23-50_frostedresearch-modern-tech-tree-gui.md).

## Minimum Contribution Step

Before changing research UI or progress behavior, run `./gradlew test --tests "com.teammoeg.frostedresearch.*"` and update [research-ui.md](research-ui.md) when its documented contracts change.
