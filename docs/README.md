# Frosted Heart System Documentation

This directory explains implemented, player-facing game systems. Source, configuration, datapacks, and companion-pack data remain authoritative.

| System                        | Start here                                               | Status       |
|-------------------------------|----------------------------------------------------------|--------------|
| Town management               | [town/README.md](town/README.md)                         | Transitional |
| Curiosity encounter           | [boss/README.md](boss/README.md)                         | Transitional |
| Nutrition                     | [nutrition/README.md](nutrition/README.md)               | Current      |
| Frosted Research              | [research/README.md](research/README.md)                 | Current      |
| Knowledge archive and linking | [knowledge/README.md](knowledge/README.md)               | Current      |
| Climate and temperature       | [climate/README.md](climate/README.md)                   | Transitional      |
| Space station                 | [space/README.md](space/README.md)                       | Current      |
| Scenario font rendering       | [scenario-font-rendering.md](scenario-font-rendering.md) | Current      |
| Ice cave world generation     | [worldgen/README.md](worldgen/README.md)                   | Partial      |

`Current` means verified against implementation. `Partial` omits known areas. `Transitional` mixes verified material with legacy, experimental, or proposed content. [`deprecated/`](deprecated/) is historical only.

## Library Guides

- [Chorda UI (CUI) 指南](chorda-cui.md): element tree, lifecycle, layout, input, themes, screen adapters, widgets, content panels, pan/zoom viewport, and editors.

Read the system README, then follow its document and code anchors. When behavior, formulas, persistence, networking, configuration, data formats, commands, or cross-system contracts change, update the owning document and record the impact in [`diary/`](../diary/).

Repository-wide authority and writing rules live in [`AGENTS.md`](../AGENTS.md). Creative intent is under read-only [`design/`](../design/); intended work belongs in [`plans/`](../plans/), and open ideas in [`discussion/`](../discussion/).
