# Figures Guidelines

The goal of figures is game development communication in publication standard. In general, the philosophy is that **every pixel should be meaningful**.

### Text and fonts
- Font: sans-serif (Arial). Consistent across all panels and figures.
- Font size: Consistent and large. The goal is always to ensure readability over compactness. Go for larger font sizes always, even if it means adjusting figure dimensions to accommodate the text. The figure should be readable without zooming in.
- LaTeX symbols: all mathematical notation should be properly formatted. Mathematical symbols should always be accompanied by a concise English meaning and followed by physical units, unless noted otherwise.
- Avoid unncessary text. Only include essential information in axis labels, legends.
- Titles should only be used when necessary to clarify the content of the panel. If a title is used, it should be concise and informative, not redundant with axis labels or legends.

### Colors
- Colors should be visually guiding, not misleading.
- Use colorblind-accessible palettes. Avoid red-green only distinctions; ensure all color pairs are distinguishable under common color vision deficiencies (deuteranopia, protanopia).

### Spines, ticks, and grids
- Remove top and right spines (no box frames). Keep only left and bottom spines.
- Tick direction: outward, consistent across all panels.
- Comparable panels when arranged horizontally should use consistent Y-axis limits. Same for panels arranged vertically with X-axis limits.
- Legends should not overlap with data. Unnecessary or repeated axis labels should be omitted.
- No grids, unless explicitly needed for data interpretation.

### Line and dot styles
- Line width and style should be consistent with the type of data being represented.
- Theory: Use thick, solid lines. They imply continuity and mathematical certainty. Color represent the underlying theory framework.
- Simulation: dots with error bars. Use distinct markers (e.g., circles, squares, crosses) without connecting lines. Color representing the underlying model.
- Asymptotic limits and other reference lines: dashed lines.

### Output
- Figures should be saved as png with dpi=300 at least.