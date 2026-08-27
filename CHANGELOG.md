# Changelog

## 0.0.8
- Refactored Barrel fluid storage into separate **input** and **output** tanks that share one 4000 mB capacity
  (`inputFluid + outputFluid <= 4000`). Recipes now consume only what they require: leftovers of input items or input
  fluid remain in the input store instead of forcing an all-or-nothing match, and recipe fluid outputs are written only
  to the output store and never mixed with or overwrite the input fluid. This removes the previous exact-fluid-amount
  brittleness for both built-in and custom (datapack / KubeJS) recipes.
  - External fluid insertion (buckets, bottles, pipes, rain) always targets the input store; the output store can never
    be externally filled. Bucket / clay-bucket removal is output-first and strict: when the output store holds fluid,
    only that fluid can be bucketed out, and a non-bucketable output never secretly drains the input. An empty output
    store drains input, so manually added water stays fully recoverable.
  - Fluid automation exposes two tanks: tank 0 = input (fill + drain), tank 1 = output (drain only). Targeted
    (`drain(FluidStack)`) automation drains the matching tank (output preferred), generic (`drain(int)`) drains
    output-first. Sealed Barrels reject all automated fluid insertion and extraction.
  - The renderer now shows the combined fluid level (input + output) using the output fluid's texture as the top layer;
    Jade shows the separate input/output fluid amounts plus the total against capacity.
  - Shift + empty-hand on an open Barrel retrieves the input ingredient stack, mirroring how extra input fluid can be
    bucketed back out, so both resource classes are reversible.
  - Legacy worlds that stored a single `Tank` tag migrate it into the input store (the output store starts empty), so no
    stored fluid is lost. Loaded fluid totals above 4000 mB are clamped by spilling input first.
- Added optional rain collection for open Barrels (`rainFillsBarrels`, default `false`; rate via `rainFillAmount`).
  Rain fills only the input store, only when the lid is open and the input holds no fluid or only water, and never
  contaminates a non-water input or the output store. Rain no longer uses the (now removed) accumulator / quantum
  workaround: any leftover amount is valid because input and output are distinct stores. Rain is also blocked while a
  process is running or while a KubeJS cancellation is pending, so environmental water never silently clears a scripted
  cancellation. Filling is driven by `Block#handlePrecipitation`, so it only runs during actual precipitation and
  respects roof coverage and rain/snow distinction; effective rate still scales with the `randomTickSpeed` game rule.
- Persisted the KubeJS barrel process cancellation flag so a cancelled process survives chunk unload / restart
  (previously only `progress` was saved).
- The `Firstworks` KubeJS global is unchanged: it is registered as a class binding (same mechanism KubeJS uses
  for `ID`/`Math`/`PlatformWrapper`), which is correct on 0.0.7+. The earlier "Firstworks is not defined" report
  was against 0.0.6, which predated the binding entirely.
- Review fixes on the dual-tank refactor:
  - Fluid mutations (input or output) now reset `progress`, so adding/removing fluid near completion can no longer
    inherit an almost-finished timer and instant-finish extra batches (consistent with how ingredient changes already
    reset progress).
  - Jade now always shows the stored input/output fluid breakdown and total/capacity, in every barrel state
    (open, waiting, processing, cancelled).
  - `currentProcess()` no longer reports a running recipe when item output is already present, matching `tick()`'s
    "collect output before the next cycle" rule, so Jade no longer shows a process stuck at zero.
  - `rainFillsBarrels` config wording corrected to describe the input-store check; existing output fluid is never
    modified by rain.
- Tree Bark is now genuinely mod-compatible. `Firstworks.registerWoodType()` registers its `log` (and `strippedLog`)
  as the authoritative source for bark: stripping that log yields `WOOD_TYPE` equal to the registration's wood type
  (e.g. `kubejs:chestnut`) and the bark texture is generated from the explicitly registered `logTexture` (a custom
  `logTexture` is now honored). For logs not explicitly registered, the wood type is still inferred conventionally
  from the registry name (`namespace:type` for modded `*_log`/`*_stem`, `oak` fallback), so discovered modded woods
  produce correctly named, textured bark instead of always dropping Oak. A central `WoodTypeRegistry` is shared between
  the server-side bark drop and the client-side texture generator.
- Bark fixes following re-review:
  - Explicit `logTexture` now loads: the registered block/model id (`windswept:block/chestnut_log`) is normalized to the
    real resource path `windswept:textures/block/chestnut_log.png` (adds `textures/` prefix and `.png`).
  - Conventional inference now also covers `_wood`, `_hyphae`, and `bamboo_block`, so stripping vanilla wood/hyphae/bamboo
    blocks yields the correct bark instead of always Oak.
  - Automatic discovery preserves the actual source block (`mymod:blue_stem`), so the fallback texture resolves to
    `mymod:textures/block/blue_stem.png` instead of a guessed `_log.png`; explicit registration still takes precedence.
  - Barrel automation `IFluidHandler.isFluidValid()` now reports static validity (`tankIndex == 0 && inputTank.isFluidValid(stack)`);
    sealing/current-fluid checks moved to `fill()`/`drain()`, fixing the capability contract.
- Added **Standing Torch**: a primitive standing torch block with flame and smoke particles, light level 14, floor survival checks, full custom model/textures, shaped crafting recipe (Torch + Stick + Primitive Binding), and `#minecraft:mineable/axe` integration.
- Replaced the vanilla $2 \times 2$ 4-rabbit-hide $\rightarrow$ Leather crafting recipe with `firstworks:raw_hide`, integrating with `replaceAnimalLeatherDrops` in configuration so vanilla leather crafting can be restored if the option is disabled.
