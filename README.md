# Firstworks

Firstworks is a standalone NeoForge 1.21.1 mod about primitive, in-world processing. Its wooden Barrels and hand-operated Looms turn early materials into visible workshop projects without machine screens or instant conversions.

The included hide-tanning chain is the first complete use of this system, not its limit. Barrel processes are data-driven and can be expanded by modpacks through datapacks or KubeJS.

Firstworks has no dependency on Create or the Inventors modpack.

## Data-driven animal materials

Firstworks owns the reusable raw-hide and animal-bone rules. Datapacks and modpacks can extend the positive entity-type tags or add entries to the higher-priority exclusion tags:

- `#firstworks:drops_bones`
- `#firstworks:no_bone_drops`
- `#firstworks:leather_drops_as_raw_hide`
- `#firstworks:no_raw_hide_drops`
- `#firstworks:charcoal_igniters`

The default bone list contains vertebrate animals. Squid, glow squid, bees, allays, and undead horses are intentionally excluded. Bone drops and leather replacement can also be disabled independently in the common config.

## Primitive barrel processing

- Craft a Barrel from matching planks and slabs. Every vanilla wood family has a variant.
- Add ingredients and fluid directly to the open Barrel. Fluid you add (buckets, bottles, pipes, or rain) always goes to the shared **input** store; the **output** store only holds finished recipe fluids.
- Close or open the lid with an empty hand. Valid sealed recipes progress over time.
- See the stored item and fluid, including separate input and output fluid levels, without opening a menu.
- Collect completed output before toggling the lid again.
- Toggle the lid with a rising redstone pulse for simple early automation.
- Open Barrels collect rainwater by default; this can be disabled with `rainFillsBarrels` in `config/firstworks-common.toml`. Rain only fills the input side and never contaminates a non-water input or the output side. Because the Barrel now keeps separate input and output fluid stores that share one 4000 mB capacity, rain can leave any remainder (e.g. 317 mB) without breaking the exact fluid amounts Firstworks' recipes require; open a recipe from partial water by topping it up or starting from ingredients that only consume what they need. Collection is driven by Minecraft's precipitation ticks, so its effective rate scales with the `randomTickSpeed` game rule and is intentionally gradual.

Opening a working Barrel cancels its current progress without consuming or ejecting the contents. Standard NeoForge item and fluid capabilities are exposed for modded automation with clean sided routing: items enter from above and finished items leave below; fluid fills the input store from above and drains finished output fluid from below; side access allows combined item insertion/extraction and fluid input filling with output-first drainage. Sealing the lid locks all automated item and fluid insertion and extraction until the Barrel is opened again.

## Built-in cordage and leatherworking

Grass and ferns have a 30% chance to provide Plant Fibre when gathered normally; using a Bone or Flint Knife guarantees the fibre. Hand-twist it into Crude Cordage for primitive tools, then ret fibre in a water-filled Barrel. Craft early Bone and Flint tools (pickaxe, axe, shovel, hoe, sword, and knife) as accessible alternatives before metalworking. To spin fibre, hold the durable Hand Spindle in your main hand, place two Retted Fibre in your offhand, and hold use until they become two Twine. Releasing early cancels without consuming the fibre. Twine can then be woven into Cloth or combined into Rope. Wooden, stone, bone, and flint tools require primitive bindings; vanilla iron, gold, and diamond tools require Rope by default; netherite upgrades retain the bound diamond tool beneath them.

Bind a vanilla torch, stick, and Crude Cordage or Rope into a **Standing Torch**—a rustic, floor-supported torch stand (light level 14) with flame and smoke particles and solid collision. Combine flint, Plant Fibre, and Crude Cordage or Rope into a single-use **Fire Starter** that ignites or relights campfires and fire targets before breaking after one use.

Before metalworking, shape three clay balls into an Unfired Clay Bucket and fire it on a campfire. The finished vessel can collect and place water or Tannin Solution and move either fluid into and out of Barrels, but it rejects lava and unrelated fluids. Tannin Solution flows, forms sources, hydrates farmland, extinguishes fire, and otherwise behaves like tinted water in the world. Filled variants reuse one tintable fluid overlay rather than requiring a separate fluid sprite.

The vanilla tool binding changes are controlled by `bindVanillaToolRecipes` in `config/firstworks-common.toml`. Disabling the option restores vanilla tool recipes after a datapack reload while leaving every cordage material and process available.

Firstworks uses the Barrel to turn leather into a physical early-game production chain:

1. Animals that normally drop leather drop Raw Hide instead, and crafting 4 Rabbit Hides yields Raw Hide instead of finished leather.
2. Soak Raw Hides in water.
3. Scrape each Soaked Hide with a Bone or Flint Knife, consuming one durability.
4. Strip logs with an axe to collect one to three Tree Bark.
5. Seal Tree Bark in water to brew Tannin Solution.
6. Tan Scraped Hide in the solution.
7. Dry the Tannin-Soaked Hide in a furnace to produce vanilla leather.

Animal leather replacement is controlled by `config/firstworks-common.toml`. Packs that provide their own early-game progression can disable it while continuing to use the Barrel system.

## Fleece and wool

Sheep provide color-aware Raw Fleece instead of finished wool. Wash Raw Fleece with water in a sealed Barrel to make Clean Wool, then combine four matching pieces into the corresponding wool block. A single dye recolors a batch of Raw Fleece or Clean Wool. Beds require three Cloth, three matching Clean Wool, and three planks.

Cloth must be woven on a Firstworks Loom. Add four Twine or String directly to the frame, then use the Loom sixteen times with an empty hand to work the shuttle and finish one Cloth. The growing weave uses the output item's own sprite and tint, including custom recipe outputs. Sneak-use with an empty hand retrieves unfinished thread. Looms are available in every vanilla wood family and expose item input/output capabilities. Each stroke is a normal empty-hand block interaction, allowing automation tools such as a Create Deployer in use mode to operate the Loom without a hard Create dependency. Datapack and KubeJS recipes can set their own `strokes` value for easier or more demanding materials.

This progression is controlled by `enableTextileProgression` in `config/firstworks-common.toml`. Disabling it restores vanilla sheep drops, shearing, string-to-wool, and bed recipes after a datapack reload.

## Primitive masonry

Place one clay ball into the single-cavity Wooden Brick Mold (equipped with an integrated wooden molding board base for reliable center interaction), press it twice with an empty hand, and collect the unfired brick. Each press visibly spreads the material farther across the cavity until it becomes a fully compressed, clay-textured surface tinted from the recipe output. Fire unfired bricks over a campfire, then mix sand and water into Wet Mortar in a sealed Barrel to bind structural Brick Blocks. The Mold has no menu: hoppers can load material and remove completed output, and an empty-handed Create Deployer can automate each press.

Brick-molding recipes are data-driven, appear in JEI, and can be added through KubeJS. The `enableMasonryProgression` option controls whether this chain replaces vanilla brick smelting and block crafting.

## Charcoal and grinding

Build a charcoal mound from four or more connected log blocks, surround it with blocks in `#firstworks:charcoal_sealants`, and ignite one exposed opening with a Fire Starter or flint and steel. Seal the opening and wait for carbonization; the completed mound persists through chunk unloading and yields charcoal when opened. Wood and sealant tags are data-driven for modpack expansion.

Place the Mortar & Pestle as a workstation, insert a grindable item, and use an empty hand to start its visible pestle animation. Recipes are data-driven and support KubeJS and JEI.

## Datapacks and KubeJS

Barrel processes use the `firstworks:barrel_processing` recipe type. Loom recipes use `firstworks:loom_weaving`, while held-spindle recipes use `firstworks:spinning`. Both define their input, output, work required, and batch size.

KubeJS is optional. When installed, Firstworks registers typed helpers for both processing systems and cancellable start/completion events.

### Registering wood variants

Put a wood registration in a KubeJS startup script to create a matched, fully functional loom and barrel pair:

```js
StartupEvents.registry('block', event => {
  Firstworks.registerWoodType(event, 'kubejs:redwood', {
    planks: 'examplemod:redwood_planks',
    slab: 'examplemod:redwood_slab',
    log: 'examplemod:redwood_log',
    strippedLog: 'examplemod:stripped_redwood_log',
    displayName: 'Redwood'
  })
})
```

This registers `kubejs:redwood_loom` and `kubejs:redwood_barrel`, including their block items, names, blockstates, models, loot tables, axe-mineable tags, and shaped recipes. They use Firstworks' native block entities, automation capabilities, rendering, processing, Jade integration, and JEI catalysts.

Texture locations are inferred from the supplied block IDs. Woods with non-standard paths can override `plankTexture`, `logTexture`, `logTopTexture`, and `strippedLogTexture` with model texture IDs such as `examplemod:block/redwood_log_end`. Set `recipes: false` if the pack supplies its own recipes. Startup registry changes require a game restart.

### Recipes and process events

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

  event.recipes.firstworks.brick_molding(
    'firstworks:unfired_clay_brick',
    'minecraft:clay_ball'
  ).inputCount(1).presses(2).id('example:mold_clay_brick')
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

FirstworksEvents.brickMoldingStarting(event => {
  // event.cancel() prevents this molding attempt.
})

FirstworksEvents.brickMoldingCompleted(event => {
  console.info(`Molded ${event.result} at ${event.pos}`)
})

FirstworksEvents.mortarGrindingStarting(event => {
  // event.cancel() prevents this grinding attempt.
})

FirstworksEvents.mortarGrindingCompleted(event => {
  console.info(`Ground ${event.result} at ${event.pos}`)
})
```

## Optional integrations

- **Jade** shows live Barrel processing, Loom loading/weaving, and Mortar & Pestle grinding status.
- **JEI** provides dedicated Barrel Processing, Hand Spinning, and Loom Weaving categories with their tools and wood variants registered as catalysts.

## Credits

Firstworks is inspired by [TerraFirmaCraft](https://www.curseforge.com/minecraft/mc-mods/terrafirmacraft) and its hands-on approach to believable survival progression.

## Building

Run `./gradlew build` (`gradlew.bat build` on Windows). The distributable jar is created under `build/libs`.
