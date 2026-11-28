# Ice halo datasets

Tables for the ice crystal halos of the calculated sky (`content/climate/render/sky`, see `docs/climate/calculated-sky.md`). They are loaded by `IceCrystalData`.

## Source

All tables are calculated, not measured. They were written by the `datasets` mode of the offline sky preview, a standalone Java program kept outside this repository in the contributor workspace (`sky-preview`, run as `run.ps1 -Mode datasets`). The preview ray traces light through hexagonal ice prisms with Fresnel weights per color band:

- Ice refractive index `n = 1.3005 + 0.0030 / lambda^2` with lambda in micrometers, for bands at 680, 550, and 440 nm, each 25 nm wide.
- Crystal shapes, shares, and tilts as in `crystals.csv`. They were chosen for a rich halo display in the game, not measured. Real cirrus usually shows fewer oriented crystals.
- Effective index of a skew ray through a prism, `n' = sqrt(n^2 - a^2) / sqrt(1 - a^2)` with `a` the cosine between ray and prism axis, after Bravais.

Random numbers come from SplitMix64 seeded per ray, so a rerun writes the same files.

## Files

- `crystals.csv`: one row per crystal kind. `orientation` is `random` (tumbling), `plate` (axis near vertical), `column` (axis near horizontal), or `parry` (axis horizontal, two side faces horizontal). `half_height_over_radius` is the prism's half height over the radius of its hexagon. `tilt_degrees` is the Gaussian spread of the axis, and `rotation_degrees` the spread of a Parry column's turn about its axis.
- `side_faces.csv`: 2D grid. Rows are the effective index `n'` (1.300 to 2.000 in steps of 0.005, then to 4.00 in steps of 0.02), columns are the absolute in-plane deviation in degrees (bin centers, 0.5 degree bins). Values are the probability density per radian of deviation for light that enters and leaves through side faces, including external reflection, from 1200 prism rotations by 48 entry heights. Each row integrates to 1.
- `paths_<crystal>.csv`: for each oriented crystal kind, rows by sun elevation in whole degrees from -6 to 90. Columns are the share of light per color band on each path class: `side` (in and out through side faces, even number of end face reflections), `side_mirrored` (odd number), `end_to_side` (in through an end face, out through a side face), `end_to_end` (in through one end face, out through the other), `end_mirror` (reflected off an end face from outside, or in and out through the same end face touching only end faces, which leaves in the mirror direction), and `other`. Each band's six shares add up to 1, less the light still inside the crystal after 40 interactions, which is at most 0.5% (Parry columns under a sun at the zenith). From 1,000,000 traced rays per row.
- `scattering_<crystal>.csv`: for each tumbling crystal kind, the phase function per steradian by angle from the sun in 0.05 degree bins (bin centers), per color band, from 8,000,000 traced rays. Diffraction is not included. Each band integrates to 1.

## Changing the crystals

The path shares and scattering tables depend on the crystal shapes, so change `crystals.csv` only together with a rerun of the generator. The GameTests in `CalculatedSkyGameTests` compare the sky against reference values from the same run.
