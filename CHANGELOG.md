# Changelog

## 0.0.8
- Added optional rain collection for open Barrels (`rainFillsBarrels`, default `false`; rate via `rainFillAmount`).
  Rain only fills when the lid is open and the Barrel holds no fluid or only water, and never contaminates a
  non-water fluid. Water is committed in 250 mB steps so it never leaves a residue that would silently break
  fluid-exact recipes (e.g. tannin brewing). Filling is driven by `Block#handlePrecipitation` rather than a
  per-tick check, so it only runs during actual precipitation and naturally respects roof coverage.
- Persisted the KubeJS barrel process cancellation flag so a cancelled process survives chunk unload / restart
  (previously only `progress` was saved).
- The `Firstworks` KubeJS global is unchanged: it is registered as a class binding (same mechanism KubeJS uses
  for `ID`/`Math`/`PlatformWrapper`), which is correct on 0.0.7+. The earlier "Firstworks is not defined" report
  was against 0.0.6, which predated the binding entirely.
