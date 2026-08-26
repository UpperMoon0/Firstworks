# Changelog

## 0.0.8
- Added optional rain collection for open Barrels (`rainFillsBarrels`, default `false`; rate via `rainFillAmount`,
  granularity via `rainFillQuantum` default 250).
  Rain only fills when the lid is open and the Barrel holds no fluid or only water, and never contaminates a
  non-water fluid. Water is collected into an internal accumulator and committed in `rainFillQuantum` mB steps, so it
  never leaves a residue that would silently break Firstworks' built-in fluid-exact recipes (e.g. tannin brewing).
  The quantum is configurable so packs can match their own recipe `fluid_amount` values; output-fluid recipes require
  an exact amount, so they are only startable from rain water when their `fluid_amount` is compatible with
  `rainFillQuantum` (within the 4000 mB capacity), while item-output recipes are unaffected. Filling is driven by
  `Block#handlePrecipitation` rather than a per-tick check, so it only runs during actual precipitation and naturally
  respects roof coverage and rain/snow distinction.
  - The `rainAccumulator` (pending, not-yet-committed rain) is marked dirty on every change, so partial progress
    survives chunk unload / restart. A full Barrel does not churn dirty state once the accumulator is empty.
  - Rate note: precipitation events scale with the `randomTickSpeed` game rule, so effective fill speed scales with
    that gamerule too. This matches vanilla cauldron behaviour; lower `rainFillAmount` if it feels too fast.
- Persisted the KubeJS barrel process cancellation flag so a cancelled process survives chunk unload / restart
  (previously only `progress` was saved).
- The `Firstworks` KubeJS global is unchanged: it is registered as a class binding (same mechanism KubeJS uses
  for `ID`/`Math`/`PlatformWrapper`), which is correct on 0.0.7+. The earlier "Firstworks is not defined" report
  was against 0.0.6, which predated the binding entirely.
