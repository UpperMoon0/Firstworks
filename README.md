# Firstworks

Firstworks is a standalone NeoForge 1.21.1 mod about primitive, in-world processing. Its wooden Barrels and hand-operated Looms turn early materials into visible workshop projects without machine screens or instant conversions.

The included hide-tanning chain is the first complete use of this system, not its limit. Barrel processes are data-driven and can be expanded by modpacks through datapacks or KubeJS.

Firstworks has no dependency on Create or the Inventors modpack.

## Primitive barrel processing

- Craft a Barrel from matching planks and slabs. Every vanilla wood family has a variant.
- Add ingredients and fluid directly to the open Barrel.
- Close or open the lid with an empty hand. Valid sealed recipes progress over time.
- See the stored item and fluid without opening a menu.
- Collect completed output before toggling the lid again.
- Toggle the lid with a rising redstone pulse for simple early automation.

Opening a working Barrel cancels its current progress without consuming or ejecting the contents. Standard NeoForge item and fluid capabilities are also exposed for modded automation: items enter from above, finished items leave below, and side access can inspect both item slots. Sealing the lid locks all automated item and fluid insertion and extraction until the Barrel is opened again.

## Built-in cordage and leatherworking

Grass and ferns have a 30% chance to provide Plant Fibre when gathered normally; using any sword guarantees the fibre. Hand-twist it into Crude Cordage for wooden and stone tools, then ret fibre in a water-filled Barrel. To spin it, hold the durable Hand Spindle in your main hand, place two Retted Fibre in your offhand, and hold use until they become two Twine. Releasing early cancels without consuming the fibre. Twine can then be woven into Cloth or combined into Rope. Vanilla iron, gold, and diamond tools require Rope by default; netherite upgrades retain the bound diamond tool beneath them.

The vanilla tool binding changes are controlled by `bindVanillaToolRecipes` in `config/firstworks-common.toml`. Disabling the option restores vanilla tool recipes after a datapack reload while leaving every cordage material and process available.

Firstworks uses the Barrel to turn leather into a physical early-game production chain:

1. Animals that normally drop leather drop Raw Hide instead.
2. Soak Raw Hides in water.
3. Scrape each Soaked Hide with any sword, consuming one durability.
4. Strip logs with an axe to collect one to three Tree Bark.
5. Seal Tree Bark in water to brew Tannin Solution.
6. Tan Scraped Hide in the solution.
7. Dry the Tannin-Soaked Hide in a furnace to produce vanilla leather.

Animal leather replacement is controlled by `config/firstworks-common.toml`. Packs that provide their own early-game progression can disable it while continuing to use the Barrel system.

## Fleece and wool

Sheep provide color-aware Raw Fleece instead of finished wool. Wash Raw Fleece with water in a sealed Barrel to make Clean Wool, then combine four matching pieces into the corresponding wool block. A single dye recolors a batch of Raw Fleece or Clean Wool. Beds require three Cloth, three matching Clean Wool, and three planks.

Cloth must be woven on a Firstworks Loom. Add four Twine or String directly to the frame, then use the Loom sixteen times with an empty hand to work the shuttle and finish one Cloth. The growing weave uses the output item's own sprite and tint, including custom recipe outputs. Sneak-use with an empty hand retrieves unfinished thread. Looms are available in every vanilla wood family and expose item input/output capabilities. Each stroke is a normal empty-hand block interaction, allowing automation tools such as a Create Deployer in use mode to operate the Loom without a hard Create dependency. Datapack and KubeJS recipes can set their own `strokes` value for easier or more demanding materials.

This progression is controlled by `enableTextileProgression` in `config/firstworks-common.toml`. Disabling it restores vanilla sheep drops, shearing, string-to-wool, and bed recipes after a datapack reload.

## Datapacks and KubeJS

Barrel processes use the `firstworks:barrel_processing` recipe type. Loom recipes use `firstworks:loom_weaving`, while held-spindle recipes use `firstworks:spinning`. Both define their input, output, work required, and batch size.

KubeJS is optional. When installed, Firstworks registers typed helpers for both processing systems and cancellable start/completion events.

```js
ServerEvents.recipes(event => {
  event.recipes.firstworks.barrel_processing(
    'minecraft:leather',
    'firstworks:scraped_hide',
    'firstworks:tannin_solution',
    250
  ).time(1200).id('example:fast_leather')

  event.recipes.firstworks.barrel_processing(
    'firstworks:tree_bark',
    'minecraft:water',
    1000,
    'firstworks:tannin_solution',
    1000
  ).time(24000).id('example:tannin')

  event.recipes.firstworks.loom_weaving(
    'minecraft:white_banner',
    'minecraft:string'
  ).inputCount(6).strokes(6).id('example:woven_banner')

  event.recipes.firstworks.spinning(
    'minecraft:string',
    'minecraft:cobweb'
  ).inputCount(1).time(40).id('example:spin_cobweb')
})

FirstworksEvents.barrelProcessStarting(event => {
  // event.cancel() prevents this process until the contents or lid changes.
})

FirstworksEvents.barrelProcessCompleted(event => {
  console.info(`Completed ${event.recipeId} at ${event.pos}`)
})

FirstworksEvents.loomWeavingStarting(event => {
  // event.cancel() prevents weaving until the Loom's input changes.
})

FirstworksEvents.loomWeavingCompleted(event => {
  console.info(`Wove ${event.result} at ${event.pos}`)
})

FirstworksEvents.spindleSpinningStarting(event => {
  // event.cancel() prevents this spinning attempt.
})

FirstworksEvents.spindleSpinningCompleted(event => {
  console.info(`${event.player.name.string} spun ${event.result}`)
})
```

## Optional integrations

- **Jade** shows live Barrel processing and Loom loading, stroke progress, cancellation, and completed output.
- **JEI** provides dedicated Barrel Processing, Hand Spinning, and Loom Weaving categories with their tools and wood variants registered as catalysts.

## Credits

Firstworks is inspired by [TerraFirmaCraft](https://www.curseforge.com/minecraft/mc-mods/terrafirmacraft) and its hands-on approach to believable survival progression.

## Building

Run `./gradlew build` (`gradlew.bat build` on Windows). The distributable jar is created under `build/libs`.
