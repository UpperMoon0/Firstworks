# Firstworks — Packmaker & Datapack Developer Guide

This document is the authoritative technical reference for modpack developers and datapack authors configuring, scripting, or extending Firstworks (NeoForge 1.21.1).

---

## Table of Contents
1. [Configuration](#1-configuration)
2. [Public Tags Reference](#2-public-tags-reference)
3. [Animal Material Profiles](#3-animal-material-profiles)
4. [Datapack Recipe Types & JSON Schemas](#4-datapack-recipe-types--json-schemas)
5. [KubeJS Integration](#5-kubejs-integration)
   - [Custom Wood Type Registration](#custom-wood-type-registration)
   - [Custom Recipe Registration via KubeJS](#kubejs-recipe-registration)
   - [Event Handlers & Payload Fields](#kubejs-event-handlers--payload-fields)
6. [Automation Capabilities & Sided Behavior](#6-automation-capabilities--sided-behavior)
7. [Food Recipe Overrides & Interop IDs](#7-food-recipe-overrides--interop-ids)
8. [In-World Charcoal Mound System](#8-in-world-charcoal-mound-system)
9. [Jade & JEI Integration](#9-jade--jei-integration)
10. [Migration Notes (0.0.10 → 0.0.11)](#10-migration-notes-0010--0011)
11. [Migration Notes (0.0.11 → 0.0.12)](#11-migration-notes-0011--0012)
12. [Migration Notes (0.0.12 → 0.0.13)](#12-migration-notes-0012--0013)
13. [Migration Notes (0.0.13 → 0.0.14)](#13-migration-notes-0013--0014)

---

## 1. Configuration

Starting in **0.0.11**, Firstworks registers all gameplay options as a **`SERVER`** configuration. This ensures that gameplay-authoritative rules, progression toggles, durations, and yield multipliers are automatically synchronized from dedicated servers to connected clients (and reflected accurately in client guides such as JEI and Jade).

- **Modpack Default Path**: `defaultconfigs/firstworks-server.toml`
- **Per-World Path**: `saves/<world>/serverconfig/firstworks-server.toml`

### Configuration Options Table

| Option Key | Type | Default | Range | Description |
| :--- | :--- | :--- | :--- | :--- |
| `replaceAnimalLeatherDrops` | Boolean | `true` | `true / false` | Replaces vanilla leather drops with `firstworks:raw_hide` for tagged entities and rewrites 4-rabbit-hide crafting. |
| `addAnimalBoneDrops` | Boolean | `true` | `true / false` | Adds 1–2 bones to vertebrate entities in `#firstworks:drops_bones`. |
| `bindVanillaToolRecipes` | Boolean | `true` | `true / false` | Master toggle requiring bindings for vanilla wooden/stone/metal/diamond tools. |
| `bindPrimitiveVanillaTools` | Boolean | `true` | `true / false` | Requires `#firstworks:primitive_bindings` for wooden and stone tools. |
| `bindMetalVanillaTools` | Boolean | `true` | `true / false` | Requires `#firstworks:strong_bindings` for iron, gold, and diamond tools. |
| `enableTextileProgression` | Boolean | `true` | `true / false` | Replaces wool drops with raw fleece, disables String-to-Wool, and requires Cloth/Clean Wool for beds. |
| `enableMasonryProgression` | Boolean | `true` | `true / false` | Requires brick molding, firing, wet mortar mixing, and mortar-bound brick blocks. |
| `enableGrainProgression` | Boolean | `true` | `true / false` | When enabled, rewrites the winning `minecraft:bread`, `minecraft:cookie`, and `minecraft:cake` recipes to require `#c:doughs/wheat` / `#c:flours/wheat`. When disabled, Firstworks leaves those recipe IDs untouched so vanilla or another datapack/mod can own them. |
| `enablePrimitiveCopperProgression` | Boolean | `true` | `true / false` | When enabled, removes the six vanilla raw/ore copper smelting and blasting recipe IDs. When disabled, Firstworks leaves the winning vanilla/datapack recipes untouched while keeping its copper workshop mechanics available. |
| `rainFillsBarrels` | Boolean | `true` | `true / false` | Allows rain to gradually fill open barrels with water during precipitation events. |
| `rainFillAmount` | Integer | `100` | `1 – 4000` | Millibuckets of water gathered per precipitation event. |
| `charcoalCarbonizeDuration` | Integer | `6000` | `20 – 72000` | Ticks required for a sealed mound to carbonize (default: 5 minutes / 6000 ticks). |
| `charcoalMinLogs` | Integer | `4` | `4 – 64` | Minimum number of connected logs required for a valid charcoal mound (default: 4). |
| `charcoalMaxLogs` | Integer | `64` | `4 – 256` | Maximum connected logs allowed in a single charcoal mound (default: 64). |
| `charcoalSealWindow` | Integer | `1200` | `20 – 6000` | Ticks allowed to seal the opening after ignition before charge fails (default: 60s / 1200 ticks). |
| `charcoalNormalYield` | Double | `0.75` | `0.05 – 1.0` | Charcoal multiplier when mound finishes without breach (default: 75% yield). |
| `charcoalBreachedYield` | Double | `0.25` | `0.0 – 1.0` | Charcoal multiplier if mound is breached during carbonization (default: 25% yield). |
| `plantFibreHandChance` | Double | `0.30` | `0.0 – 1.0` | Chance to gather Plant Fibre with an empty hand or non-knife tool (default: 30%). |
| `rawOchreGatherChance` | Double | `0.20` | `0.0 – 1.0` | Chance to gather Raw Ochre without a primitive knife (default: 20%). |
| `quernManualWorkPerCrank` | Integer | `5` | `1 – 100` | Work progress added per manual empty-hand crank of the Quern. |

---

## 2. Public Tags Reference

Firstworks exposes data-driven tags for extensible pack integration. Below are the exact shipped default contents for each tag:

### Item Tags (`data/firstworks/tags/item/`)

| Tag | Shipped Default Items | Purpose |
| :--- | :--- | :--- |
| `#firstworks:primitive_knives` | `firstworks:bone_knife`, `firstworks:flint_knife`, `firstworks:copper_knife` | Tools recognized for hide scraping, guaranteed fibre harvesting, ochre extraction, and other primitive knife interactions. |
| `#firstworks:primitive_bindings` | `firstworks:crude_cordage`, `firstworks:rope` | Items accepted as bindings for wooden, stone, bone, and flint tools. |
| `#firstworks:strong_bindings` | `firstworks:rope`, `firstworks:hafting_compound` | Strong binding role used by higher-tier tools and Stone/Copper workshop recipes. |
| `#firstworks:resin_tapping_tools` | `firstworks:resin_tap` | Additional tools accepted for renewable resin tapping; primitive knives are accepted separately. |
| `#firstworks:hammers` | `firstworks:stone_hammer` | Hammer-role tools that advance Stone Anvil work. |
| `#firstworks:refractory_materials` | `firstworks:grog`, `firstworks:refractory_clay`, `firstworks:refractory_brick` | Shared refractory-material classification for workshop extension. |
| `#firstworks:primitive_copper` | `firstworks:cast_copper_billet`, `firstworks:annealed_copper_billet`, `minecraft:copper_ingot`, `firstworks:copper_fasteners`, `firstworks:copper_knife` | Firstworks primitive-copper chain and products that precede mature metallurgy. |
| `#firstworks:crucible_furnace_fuels` | `minecraft:coal`, `minecraft:charcoal` | Items accepted as Crucible Furnace reserve fuel. Add coke, peat, charcoal variants, or other pack fuels here. |
| `#firstworks:charcoal_igniters` | `firstworks:fire_starter`, `minecraft:flint_and_steel` | Items capable of igniting charcoal mounds. |
| `#firstworks:raw_hides` | `firstworks:raw_hide` | Raw hide items removed during animal drop normalization before adding `firstworks:raw_hide`. Packs integrating third-party animal mods should add items like `naturalist:hide` here. |
| `#firstworks:tree_bark` | `firstworks:tree_bark` | Stripped bark items used for brewing tannin solution in barrels. |
| `#firstworks:barrels` | All barrel item variants | Item classification for barrel workstations. |
| `#firstworks:looms` | All loom item variants | Item classification for loom workstations. |

### Common NeoForge Tags (`data/c/tags/item/`)

Firstworks participates in the `c` (Common Tags) interoperability namespace so compatible third-party materials plug into Firstworks progression without recipe edits:

| Tag | Shipped Default Items | Purpose |
| :--- | :--- | :--- |
| `#c:flours` | `firstworks:flour` | Any ground-grain flour accepted by flour consumers. |
| `#c:flours/wheat` | `firstworks:flour` | Wheat flour accepted by dough recipes and the runtime Cake progression rewrite. |
| `#c:doughs` | `firstworks:dough` | Any raw dough accepted by dough consumers. |
| `#c:doughs/wheat` | `firstworks:dough` | Wheat dough accepted by Firstworks bread cooking recipes and the runtime Bread/Cookie progression rewrites. |
| `#c:strings` | `minecraft:string`, `firstworks:twine` | String-compatible materials accepted by the Basket, Rope, and Cloth recipes. Add compatible third-party string materials here. |

**Intended rule:** third-party mods and datapacks that add their own flour or wheat dough should add those items to these common tags rather than hardcoding `firstworks:flour` / `firstworks:dough` into replacement recipes. Firstworks recipes and enabled runtime rewrites match by tag, so tagged foreign items work automatically (e.g. a rice flour mod adds its item to `#c:flours`, a modpack reroutes a mod's wheat dough through `#c:doughs/wheat` to bake with Firstworks bread recipes).

### Block Tags (`data/firstworks/tags/block/`)

| Tag | Shipped Default Blocks | Purpose |
| :--- | :--- | :--- |
| `#firstworks:resin_trees` | `#minecraft:spruce_logs`, `#minecraft:jungle_logs` | Living/log blocks that can receive renewable resin scars. |
| `#firstworks:charcoal_woods` | `#minecraft:logs_that_burn` | Blocks accepted as fuel logs in charcoal mounds. Non-burning woods are excluded. |
| `#firstworks:charcoal_sealants` | `minecraft:dirt`, `minecraft:grass_block`, `minecraft:coarse_dirt`, `minecraft:rooted_dirt`, `minecraft:podzol`, `minecraft:mud`, `minecraft:clay` | Airtight casing blocks required to seal charcoal mounds. |
| `#firstworks:ochre_sources` | `minecraft:clay`, `minecraft:coarse_dirt`, `minecraft:red_sand`, `minecraft:terracotta` | World blocks capable of yielding raw ochre when broken. |
| `#firstworks:plant_fibre_sources` | `minecraft:short_grass`, `minecraft:fern` | Single-tall vegetation providing plant fibre (30% hand chance, 100% knife chance). |
| `#firstworks:double_plant_fibre_sources` | `minecraft:tall_grass`, `minecraft:large_fern` | Double-tall vegetation providing double plant fibre. |
| `#firstworks:barrels` | All barrel block variants | Block classification for barrel workstations. |
| `#firstworks:looms` | All loom block variants | Block classification for loom workstations. |

### Entity Type Tags (`data/firstworks/tags/entity_type/`)

| Tag | Shipped Default Entities | Purpose |
| :--- | :--- | :--- |
| `#firstworks:drops_bones` | Vertebrate animals (cow, sheep, pig, chicken, horse, donkey, mule, wolf, cat, etc.) | Entities that drop 1–2 bones on death when `addAnimalBoneDrops` is enabled. |
| `#firstworks:no_bone_drops` | `minecraft:squid`, `minecraft:glow_squid`, `minecraft:bee`, `minecraft:allay`, `minecraft:skeleton_horse`, `minecraft:zombie_horse` | Packmaker veto tag excluding specific entities from bone drops regardless of fallback rules. |
| `#firstworks:leather_drops_as_raw_hide` | `minecraft:cow`, `minecraft:mooshroom`, `minecraft:horse`, `minecraft:donkey`, `minecraft:mule`, `minecraft:llama`, `minecraft:trader_llama` | Entities whose vanilla leather drops are replaced with raw hide. |
| `#firstworks:no_raw_hide_drops` | *(Empty by default)* | Packmaker veto tag excluding specific entities from hide replacement. |

---

## 3. Animal Material Profiles

Fine-grained animal drop configurations can be defined under `data/<namespace>/firstworks/animal_materials/<name>.json`.

### Precedence Order
1. **Exclusion Tags (`#firstworks:no_*`)**: Highest priority veto. If an entity is present in `#firstworks:no_bone_drops` or `#firstworks:no_raw_hide_drops`, Firstworks will not modify those drops.
2. **Animal Material Profiles (`animal_materials/*.json`)**: Per-entity configured min/max bone and hide counts with Looting bonus.
3. **Fallback Tags (`#firstworks:drops_bones`, `#firstworks:leather_drops_as_raw_hide`)**: Generic 1–2 drop rules.
4. **Unmodified Vanilla/Modded**: No changes applied.

### Profile Schema Example
`data/examplemod/firstworks/animal_materials/bison.json`:
```json
{
  "entity": "naturalist:bison",
  "bones": {
    "min": 2,
    "max": 5,
    "looting_bonus": 1
  },
  "hide": {
    "min": 2,
    "max": 4,
    "looting_bonus": 1
  }
}
```
*Note: When a profile defines `hide`, Firstworks automatically removes any vanilla `minecraft:leather` or modded raw hides matching `#firstworks:raw_hides` before adding the profile-defined amount, preventing duplicate drops.*

---

## 4. Datapack Recipe Types & JSON Schemas

### 1. Barrel Processing (`firstworks:barrel_processing`)
Processes items and/or fluids over time in a sealed or open barrel.
`data/example/recipe/tan_hide.json`:
```json
{
  "type": "firstworks:barrel_processing",
  "ingredient": { "item": "firstworks:scraped_hide" },
  "input_count": 1,
  "fluid": "firstworks:tannin_solution",
  "fluid_amount": 250,
  "result": { "id": "firstworks:tannin_soaked_hide", "count": 1 },
  "output_fluid": "minecraft:water",
  "output_fluid_amount": 250,
  "duration": 1200,
  "sealed": true
}
```

### 2. Loom Weaving (`firstworks:loom_weaving`)
`data/example/recipe/weave_cloth.json`:
```json
{
  "type": "firstworks:loom_weaving",
  "ingredient": { "tag": "c:strings" },
  "input_count": 4,
  "result": { "id": "firstworks:cloth", "count": 1 },
  "strokes": 8,
  "weaving": { "pattern": ["A", "A", "B", "B"], "passes": 8 }
}
```

`weaving` is optional. Its `pattern` accepts 1–16 entries (`"A"` or `"B"`) and repeats for successive successful shuttle passes. `weaving.passes` optionally overrides `strokes` (1–64); without it, the existing `strokes` value is preserved. Recipes without `weaving` use alternating A/B sheds, starting with A. Built-in cloth uses four passes to keep basic weaving short.

Empty-hand use on the side post holding the shuttle throws it across. Use the lower crossbar to change shed; the beater operates automatically after a valid pass. Wrong-side or wrong-shed input does not advance work. Sneak-use returns loaded thread; ordinary use collects ready output. Partial work, shuttle side, and shed persist across saves. Adding thread during partial work is rejected so it cannot erase progress. Hints are available without Jade; Jade and JEI also show controls and pattern requirements. Existing starting/completed KubeJS events remain available; `event.loom.getShed()` and `event.loom.isShuttleRight()` expose the current mechanism state.

### 3. Hand Spinning (`firstworks:spinning`)
Held in main hand with Hand Spindle and input in offhand.
`data/example/recipe/spin_twine.json`:
```json
{
  "type": "firstworks:spinning",
  "ingredient": { "item": "firstworks:retted_fibre" },
  "input_count": 2,
  "result": { "id": "firstworks:twine", "count": 2 },
  "duration": 40
}
```

### 4. Brick Molding (`firstworks:brick_molding`)
`data/example/recipe/mold_unfired_brick.json`:
```json
{
  "type": "firstworks:brick_molding",
  "ingredient": { "item": "minecraft:clay_ball" },
  "input_count": 1,
  "result": { "id": "firstworks:unfired_clay_brick", "count": 1 },
  "presses": 2
}
```

### 5. Mortar Grinding (`firstworks:mortar_grinding`)
In-world grinding workstation.
`data/example/recipe/grind_ochre.json`:
```json
{
  "type": "firstworks:mortar_grinding",
  "ingredient": { "item": "firstworks:raw_ochre" },
  "input_count": 1,
  "result": { "id": "firstworks:ground_ochre", "count": 2 },
  "duration": 48
}
```

### 6. Quern Grinding (`firstworks:quern_grinding`)
Hand-operated bulk grinding workstation.
`data/firstworks/recipe/quern_wheat_flour.json`:
```json
{
  "type": "firstworks:quern_grinding",
  "ingredient": { "item": "minecraft:wheat" },
  "input_count": 4,
  "result": { "id": "firstworks:flour", "count": 4 },
  "work": 60
}
```

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `ingredient` | Ingredient | *(required)* | Input item matcher, matched independently of batch size. |
| `input_count` | Int `1–64` | `1` | Batch size consumed per completion. Items are inserted one at a time until the batch is full. |
| `result` | Item Stack | *(required)* | Output produced when the batch completes. |
| `work` | Int `1–72000` | `60` | Total processing effort required per batch. |
| `priority` | Int | `0` | When several quern recipes match the same ingredient, the highest `priority` wins. Ties break by recipe id, so overlapping matchers resolve deterministically without requiring mutually exclusive ingredients. |

**Work-based manual model.** Recipe `work` is the hand-labor required for one batch. The global config controls how much work each player crank contributes:

```
recipe.work ÷ quernManualWorkPerCrank = required player cranks
```

For the shipped Wheat recipe, `60 work ÷ 5 work per crank = 12 cranks` at the default config. Packs can change the labor rate without rewriting every recipe.

The Quern is intentionally player-operated. Item transfer can be automated, but grinding cannot: hoppers and pipes may load a valid batch and extract its finished result, while a player must still turn the crank to provide every unit of processing work.

**Ingredient Exclusivity:** because the quern matches ingredients independently of stack size to allow incremental 1-by-1 loading, recipes historically had to define mutually exclusive ingredient sets. That restriction is lifted: when multiple quern recipes match the same input, the one with the highest `priority` is selected (ties break by recipe id). Packmakers can now intentionally layer quern recipes that share an ingredient (for example a high-`priority` special output over a low-`priority` fallback) without a pack bug.

**Visual speed:** the grinding stone's rotation advances in proportion to the work applied per crank, so a higher `quernManualWorkPerCrank` (more work per turn) visibly spins the quern faster. The balance is unchanged — the `work` field on each recipe is still the single universal labor measure, and there are no per-source `manual_work` / `animal_duration` / `mechanical_duration` fields. (Per-source drive metadata such as a mechanical drive spinning faster than an animal wheel is not part of this release; the rate-to-rotation scaling above is the supported knob.)

### 7. Workshop Processing (`firstworks:workshop_processing`)
Stone/Copper workshop stations use one shared recipe type with a station selector. Full interaction and extension details live in [`STONE_COPPER_WORKSHOP.md`](STONE_COPPER_WORKSHOP.md).

```json
{
  "type": "firstworks:workshop_processing",
  "station": "crucible_furnace",
  "ingredient": { "item": "minecraft:raw_copper" },
  "input_count": 3,
  "catalyst": { "item": "firstworks:casting_mold" },
  "catalyst_count": 1,
  "consume_catalyst": false,
  "result": { "id": "firstworks:cast_copper_billet" },
  "work": 240,
  "priority": 0
}
```

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `station` | String | *(required)* | `pottery_wheel`, `stone_anvil`, or `crucible_furnace`. Unknown values fail recipe loading. |
| `ingredient` | Ingredient | *(required)* | Primary input matcher. |
| `input_count` | Int `1–64` | `1` | Required/consumed primary-input count. |
| `catalyst` | Ingredient | *(omitted)* | Optional tool/mold/catalyst. Declared-empty ingredients remain required and therefore match nothing. |
| `catalyst_count` | Int `1–64` | `1` | Required catalyst count. Non-default values require a declared catalyst. |
| `consume_catalyst` | Boolean | `false` | Whether the declared catalyst is consumed. `true` requires a catalyst. |
| `result` | Item Stack | *(required)* | Output produced on completion. |
| `work` | Int `1–72000` | `20` | Manual actions for manual stations or ticks for the Crucible Furnace. |
| `priority` | Int | `0` | Explicit overlap priority. Highest value wins; ties then use larger `input_count`, catalyst-specific over catalyst-free, then recipe id. |

This same contract is enforced by the KubeJS recipe schema, including the upper bounds above. `priority` should be preferred for intentional overrides instead of relying on lexicographical recipe-id naming.

---

## 5. KubeJS Integration

### Custom Wood Type Registration
In a **startup script** (`kubejs/startup_scripts/wood.js`):
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
*Automatically registers matching `kubejs:redwood_barrel` and `kubejs:redwood_loom` blocks, items, blockstates, models, loot tables, recipes, and JEI/Jade integration.*

### KubeJS Recipe Registration
In a **server script** (`kubejs/server_scripts/recipes.js`), register Firstworks recipes using standard `event.custom({...})`:

```js
ServerEvents.recipes(event => {
  // 1. Barrel Processing
  event.custom({
    type: 'firstworks:barrel_processing',
    ingredient: { item: 'firstworks:scraped_hide' },
    input_count: 1,
    fluid: 'firstworks:tannin_solution',
    fluid_amount: 250,
    result: { id: 'firstworks:tannin_soaked_hide', count: 1 },
    output_fluid: 'minecraft:water',
    output_fluid_amount: 250,
    duration: 1200,
    sealed: true
  }).id('custom:tan_hide')

  // 2. Loom Weaving
  event.custom({
    type: 'firstworks:loom_weaving',
    ingredient: { item: 'firstworks:twine' },
    input_count: 4,
    result: { id: 'firstworks:cloth', count: 1 },
    strokes: 16
  }).id('custom:weave_cloth')

  // 3. Hand Spinning
  event.custom({
    type: 'firstworks:spinning',
    ingredient: { item: 'firstworks:retted_fibre' },
    input_count: 2,
    result: { id: 'firstworks:twine', count: 2 },
    duration: 40
  }).id('custom:spin_twine')

  // 4. Brick Molding
  event.custom({
    type: 'firstworks:brick_molding',
    ingredient: { item: 'minecraft:clay_ball' },
    input_count: 1,
    result: { id: 'firstworks:unfired_clay_brick', count: 1 },
    presses: 2
  }).id('custom:mold_brick')

  // 5. Mortar Grinding
  event.custom({
    type: 'firstworks:mortar_grinding',
    ingredient: { item: 'firstworks:raw_ochre' },
    input_count: 1,
    result: { id: 'firstworks:ground_ochre', count: 2 },
    duration: 48
  }).id('custom:grind_ochre')

  // 6. Quern Grinding
  event.custom({
    type: 'firstworks:quern_grinding',
    ingredient: { item: 'minecraft:wheat' },
    input_count: 4,
    result: {
      id: 'firstworks:flour',
      count: 4
    },
    work: 60
  }).id('example:wheat_flour')

  // 7. Workshop Processing
  event.custom({
    type: 'firstworks:workshop_processing',
    station: 'stone_anvil',
    ingredient: { item: 'firstworks:annealed_copper_billet' },
    result: { id: 'minecraft:copper_ingot' },
    work: 8,
    priority: 0
  }).id('example:work_copper')
})
```

### KubeJS Event Handlers & Payload Fields

All process events support `event.cancel()` on `*Starting` handlers to dynamically veto an operation.

```js
// 1. Barrel Processing
FirstworksEvents.barrelProcessStarting(event => {
  // Guaranteed properties:
  // event.level        (ServerLevel)
  // event.pos          (BlockPos)
  // event.barrel       (BarrelBlockEntity)
  // event.recipeId     (ResourceLocation)
  // event.recipe       (BarrelRecipe)
  // event.input        (ItemStack copy)
  // event.inputFluid   (FluidStack copy)
  // event.result       (ItemStack copy)
  // event.outputFluid  (FluidStack copy)
  // event.cancel()
})
FirstworksEvents.barrelProcessCompleted(event => {
  console.info(`Barrel completed ${event.recipeId} at ${event.pos}`)
})

// 2. Loom Weaving
FirstworksEvents.loomWeavingStarting(event => {
  // Guaranteed properties:
  // event.level     (ServerLevel)
  // event.pos       (BlockPos)
  // event.loom      (LoomBlockEntity)
  // event.recipeId  (ResourceLocation)
  // event.recipe    (LoomRecipe)
  // event.input     (ItemStack copy)
  // event.result    (ItemStack copy)
  // event.cancel()
})
FirstworksEvents.loomWeavingCompleted(event => {
  console.info(`Wove ${event.result} at ${event.pos}`)
})

// 3. Spindle Spinning
FirstworksEvents.spindleSpinningStarting(event => {
  // Guaranteed properties:
  // event.level     (ServerLevel)
  // event.player    (Player)
  // event.recipeId  (ResourceLocation)
  // event.recipe    (SpinningRecipe)
  // event.input     (ItemStack copy)
  // event.result    (ItemStack copy)
  // event.cancel()
})
FirstworksEvents.spindleSpinningCompleted(event => {
  console.info(`${event.player.name.string} spun ${event.result}`)
})

// 4. Brick Molding
FirstworksEvents.brickMoldingStarting(event => {
  // Guaranteed properties:
  // event.level     (ServerLevel)
  // event.pos       (BlockPos)
  // event.mold      (BrickMoldBlockEntity)
  // event.recipeId  (ResourceLocation)
  // event.recipe    (BrickMoldingRecipe)
  // event.input     (ItemStack copy)
  // event.result    (ItemStack copy)
  // event.cancel()
})
FirstworksEvents.brickMoldingCompleted(event => {
  console.info(`Molded ${event.result} at ${event.pos}`)
})

// 5. Mortar Grinding
FirstworksEvents.mortarGrindingStarting(event => {
  // Guaranteed properties:
  // event.level     (ServerLevel)
  // event.pos       (BlockPos)
  // event.mortar    (MortarBlockEntity)
  // event.recipeId  (ResourceLocation)
  // event.recipe    (MortarGrindingRecipe)
  // event.input     (ItemStack copy)
  // event.result    (ItemStack copy)
  // event.cancel()
})
FirstworksEvents.mortarGrindingCompleted(event => {
  console.info(`Mortar ground ${event.result} at ${event.pos}`)
})

// 6. Quern Grinding
FirstworksEvents.quernGrindingStarting(event => {
  // Guaranteed properties:
  // event.level     (ServerLevel)
  // event.pos       (BlockPos)
  // event.quern     (QuernBlockEntity)
  // event.recipeId  (ResourceLocation)
  // event.recipe    (QuernGrindingRecipe)
  // event.input     (ItemStack copy)
  // event.result    (ItemStack copy)
  // event.cancel()
  //
  // Fires on the first manual crank that begins a fresh batch. Cancelling
  // prevents that crank from adding progress.
})
FirstworksEvents.quernGrindingCompleted(event => {
  console.info(`Quern completed ${event.recipeId} at ${event.pos}`)
})

// 7. Workshop Processing
FirstworksEvents.workshopProcessingStarting(event => {
  // Guaranteed properties:
  // event.level      (ServerLevel)
  // event.pos        (BlockPos)
  // event.workshop   (WorkshopBlockEntity)
  // event.station    (String: pottery_wheel / stone_anvil / crucible_furnace)
  // event.recipeId   (ResourceLocation)
  // event.recipe     (WorkshopRecipe)
  // event.input      (ItemStack copy)
  // event.catalyst   (ItemStack copy)
  // event.result     (ItemStack copy)
  // event.cancel()
  //
  // Fires before the first manual work unit, or before a Crucible Furnace
  // consumes reserve fuel. Cancelling therefore has no fuel/progress side effect.
})
FirstworksEvents.workshopProcessingCompleted(event => {
  console.info(`Workshop ${event.station} completed ${event.recipeId} at ${event.pos}`)
})
```

For ticking Crucible Furnaces, a cancelled workshop start is latched until relevant workshop state changes or the furnace is restoked, avoiding an event every server tick. Manual stations may retry on the next explicit player work action.

---

## 6. Automation Capabilities & Sided Behavior

- **Barrel (`firstworks:barrel`)**:
  - **Top Face**: Inserts items into input store; fills input fluid store.
  - **Bottom Face**: Extracts completed result items; drains output fluid store.
  - **Side Faces**: Bi-directional item transfer (input insertion / output extraction); fluid insertion into input store, output-first fluid drainage.
  - **Lid Lock**: Closing the lid locks all automated item/fluid transfer until opened.
  - **Redstone**: Rising redstone edge toggles the lid state.
- **Loom (`firstworks:loom`)**:
  - Exposes item input/output capabilities.
  - Mechanism operation must target the correct side post and the lower shed crossbar. Generic repeated clicks from a fixed deployer no longer complete weaving; Create-specific layouts have not been validated.
- **Brick Mold (`firstworks:brick_mold`)**:
  - All faces expose the same two-slot item handler: slot 0 accepts valid mold ingredients and slot 1 exposes completed output.
  - Create Deployer in empty-hand "Use" mode performs presses.
- **Woven Basket (`firstworks:basket`)**:
  - 9-slot primitive storage container with full automation and hopper support.
- **Mortar & Pestle (`firstworks:mortar_and_pestle`)**:
  - Exposes standard NeoForge `IItemHandler` capability (extracts output only).
- **Quern (`firstworks:quern`)**:
  - **Top Face**: Inserts raw ingredients into input slot.
  - **Bottom Face**: Extracts completed result from output slot (raw input cannot be extracted by automation).
  - **Side Faces / Unsided**: Accepts input insertion and output extraction.
  - **Processing**: Has no powered processing capability. Automated transfer does not advance work; only a player's empty-hand crank does.
- **Workshop stations (`pottery_wheel`, `stone_anvil`, `crucible_furnace`)**:
  - All faces expose the same four-slot handler: slot 0 input, slot 1 catalyst, slot 2 fuel, slot 3 output.
  - Automation may insert into the first three valid slots and may extract only completed output from slot 3.
  - Normal player right-click favors recipe input/catalyst roles. On heated stations, sneak-right-click any item in `#firstworks:crucible_furnace_fuels` forces it into slot 2, so fuel remains reachable even when a pack recipe also uses that item as input or catalyst.
  - Adding recipe input or catalyst preserves active progress and running state when the selected recipe id remains unchanged; if the insertion changes the selected recipe, processing resets before the new recipe begins. Reserve-fuel top-ups also preserve progress and do not consume another fuel item while the current batch is already running.

---

## 7. Food Recipe Overrides & Interop IDs

Firstworks can rewrite vanilla wheat-food progression at datapack sync time while keeping the recipe IDs interoperable.

**Runtime progression rewrites** (enabled by `enableGrainProgression`, default `true`):
- `minecraft:bread` — 3× `#c:doughs/wheat`
- `minecraft:cookie` — `#c:doughs/wheat` + cocoa beans → 8 cookies
- `minecraft:cake` — 3× `#c:flours/wheat` + milk buckets + sugar + egg

Firstworks **does not ship `data/minecraft/recipe/bread.json`, `cookie.json`, or `cake.json` in 0.0.14**. Instead, when grain progression is enabled, it replaces the winning recipes for those IDs after datapacks load. When the toggle is disabled and datapacks are reloaded/restarted, Firstworks performs no rewrite, so the vanilla recipe or whichever datapack/mod won that ID remains intact. The Quern, Flour, Dough, and `firstworks:bread_from_*` cooking recipes remain available either way.

**Firstworks dough recipes** (`data/firstworks/recipe/`):
- `firstworks:dough_from_water_bucket` — 3× `#c:flours/wheat` + water bucket → 3 dough
- `firstworks:dough_from_clay_water` — 3× `#c:flours/wheat` + `firstworks:water_clay_bucket` → 3 dough
- `firstworks:dough_from_water_bottle` — `#c:flours/wheat` + `minecraft:potion` whose potion contents are water. `strict: false` permits additional components on that Minecraft potion item; it does not match arbitrary modded water-bottle items.

**Bread cooking recipes** (input is `#c:doughs/wheat`, so tagged foreign wheat dough bakes too):
- `firstworks:bread_from_campfire_dough`
- `firstworks:bread_from_smelting_dough`
- `firstworks:bread_from_smoking_dough`

**Packmaker pattern** — when another mod owns dough production, remove its conflicting route rather than Firstworks' outputs:

```js
ServerEvents.recipes(event => {
    // Example: another mod owns dough production.
    event.remove({ id: 'somefoodmod:wheat_dough_from_water' })
})
```

Because all Firstworks routes match by common tag (`#c:flours/wheat`, `#c:doughs/wheat`), adding your own flour or dough items to those tags is usually enough — no recipe removal required. See [Common NeoForge Tags](#common-neoforge-tags-datactagsitem).

---

## 8. In-World Charcoal Mound System

- **Construction**: `charcoalMinLogs`–`charcoalMaxLogs` connected fuel blocks (`#firstworks:charcoal_woods`; defaults to 4–64 logs), encased in an airtight shell (`#firstworks:charcoal_sealants`; defaults to dirt, grass, mud, clay) leaving 1 exposed face open for ignition.
- **Candidate Detection**: Calculates shell coverage ($\ge 50\%$). Ordinary standing trees ($< 10\%$ coverage) are ignored and pass through to vanilla flint-and-steel behavior.
- **Ignition & Sealing**:
  - Right-click the exposed log opening with `#firstworks:charcoal_igniters`.
  - Diegetic feedback: flame & smoke burst from opening; no action-bar popup.
  - Place a sealant block into the opening within `charcoalSealWindow` (default: 60s / 1200 ticks).
  - Instant transition with gravel "whumpf" sound (protected by a 1-tick deferred check against claim cancellation races).
- **Carbonization**:
  - Carbonizes for `charcoalCarbonizeDuration` (default: 5 minutes / 6000 ticks).
  - Subtle, capped smoke leaks through exterior sealant blocks (1–3 sources/sec).
- **Materialization**:
  - Consumed logs are replaced bottom-up directly in the world with physical `firstworks:charcoal_pile` blocks storing 1–4 charcoal each.
  - Default normal yield: 75% (`charcoalNormalYield`).
  - Default breached yield: 25% (`charcoalBreachedYield`).

---

## 9. Jade & JEI Integration

- **Jade Tooltips**:
  - **Barrel**: Recipe progress, time remaining, output preview, input/output fluid stores.
  - **Loom**: Loaded threads, stroke progress, output preview.
  - **Brick Mold**: Loaded ingredient, press progress bar.
  - **Mortar & Pestle**: Empty status, loaded materials, grinding progress bar with seconds remaining, output ready alert.
  - **Quern**: Loaded batch, work progress bar, completed output alert.
  - **Charcoal Mound**: Active log count, seal countdown progress bar, carbonization progress bar, remaining time, and expected yield (resolving through the visible sealant shell).
  - **Charcoal Pile**: Stored charcoal count.
- **JEI Categories**:
  - Barrel Processing, Hand Spinning, Loom Weaving, Brick Molding, Mortar Grinding, Quern Grinding, **Workshop Processing**, and dynamic Charcoal Mound Information guide.
  - The base Hand Spindle is a catalyst for Hand Spinning, the Quern is a catalyst for Quern Grinding, the Loom block family is discovered for Loom Weaving, and the Pottery Wheel, Stone Anvil, Crucible Furnace, and Bellows are catalysts for their Workshop Processing views.
  - Crucible Furnace recipe views enumerate `#firstworks:crucible_furnace_fuels`, so pack-added fuels appear in JEI without code changes.

---

## 10. Migration Notes (0.0.10 → 0.0.11)

1. **Config Path Changed**: Move customizations from `config/firstworks-common.toml` to `defaultconfigs/firstworks-server.toml` (for modpacks) or `saves/<world>/serverconfig/firstworks-server.toml` (for existing worlds).
2. **Charcoal Piles in World**: Charcoal mounds now materialize physical `firstworks:charcoal_pile` blocks in the world upon completion instead of dropping item entities on uncover.
3. **Legacy Mound Migration**: Existing worlds with completed `"READY"` mounds automatically deserialize as `Phase.LEGACY_READY` and safely convert to physical charcoal piles when chunks load.

---

## 11. Migration Notes (0.0.11 → 0.0.12)

1. **Quern Introduced**: `firstworks:quern` is a new hand-operated bulk workstation, and `firstworks:quern_grinding` recipes use a `work` field (default `60`). Each empty-hand crank contributes `quernManualWorkPerCrank` work (default `5`).
2. **Flour/Dough Common Tags**: `firstworks:flour` is now tagged `#c:flours` + `#c:flours/wheat` and `firstworks:dough` is tagged `#c:doughs` + `#c:doughs/wheat`. Recipe-matching is tag-based, so foreign flour/dough items can join progression by editing tags only.
3. **Vanilla Food Overrides**: 0.0.12 introduced hard datapack overrides for `minecraft:bread`, `minecraft:cookie`, and `minecraft:cake`. In 0.0.14 those files are removed and the same progression is applied at datapack sync time only while `enableGrainProgression=true`; see [Food Recipe Overrides & Interop IDs](#7-food-recipe-overrides--interop-ids).
4. **New Materials and Recipes**: Flour, Wheat Dough, water-container dough recipes, dough cooking recipes, and the Quern recipe category are new in 0.0.12.

## 12. Migration Notes (0.0.12 → 0.0.13)

1. **Common String Inputs**: Basket, Rope, and Cloth now consume `#c:strings`, which includes vanilla String and Firstworks Twine by default. Add compatible third-party string materials to that tag.
2. **Removed Recipe IDs**: `firstworks:rope_from_string` and `firstworks:weave_cloth_from_twine` were removed because their replacements now use `#c:strings`. Datapacks and KubeJS scripts that remove or replace these IDs should be updated.
3. **Knife Behavior**: `firstworks:bone_knife` and `firstworks:flint_knife` are now dedicated knife items rather than sword items. Their registry IDs and durability values remain unchanged; ordinary block breaking no longer applies sword-style durability damage.

---

## 13. Migration Notes (0.0.13 → 0.0.14)

1. **Grain Progression Toggle**: `enableGrainProgression` (default `true`) rewrites the winning Bread/Cookie/Cake recipes to require the Firstworks flour/dough pipeline. Firstworks no longer ships hard `data/minecraft` replacements for those IDs; disabling the toggle leaves the winning datapack/mod recipes untouched after reload/restart. The Quern, Flour, and Dough items remain available either way.
2. **Quern Recipe Priority**: `firstworks:quern_grinding` gains an optional `priority` field (default `0`). When multiple quern recipes match the same ingredient, the highest `priority` wins (ties break by recipe id). The previous requirement that quern ingredient matchers be mutually exclusive is relaxed — overlapping matchers now resolve deterministically.
3. **Quern Visual Speed**: the grinding stone's rotation advances with the work applied per crank (`quernManualWorkPerCrank`); there are still no per-source labor or duration fields on recipes.
4. **Workshop Catalyst Semantics**: `firstworks:workshop_processing` distinguishes an omitted catalyst from a declared catalyst whose ingredient/tag resolves empty. Declared-empty catalysts match nothing instead of becoming catalyst-free recipes.
5. **Workshop Role Routing**: player insertion now resolves recipe roles before fuel; sneak-right-click a `#firstworks:crucible_furnace_fuels` item on a heated workshop station explicitly targets the fuel reserve. Automation retains fixed slots 0=input, 1=catalyst, 2=fuel, 3=output. Same-recipe input/catalyst top-ups preserve heated progress.
6. **Primitive Copper Progression Toggle**: `enablePrimitiveCopperProgression` defaults to `true`. Disabling it leaves the winning vanilla/datapack copper smelting and blasting recipes untouched while retaining Firstworks copper mechanics.
7. **Crucible Fuel Tag**: Crucible Furnace reserve fuel is now controlled by `#firstworks:crucible_furnace_fuels`, which contains Coal and Charcoal by default and is shared by runtime insertion and JEI.
8. **Workshop Priority and Bounds**: `firstworks:workshop_processing` adds optional integer `priority` (default `0`). `input_count` / `catalyst_count` are `1–64`, `work` is `1–72000`, and the KubeJS schema exposes the same limits.
9. **Workshop KubeJS Lifecycle**: `workshopProcessingStarting` is cancellable before progress/fuel consumption and `workshopProcessingCompleted` fires after output production, with level/position/station/recipe/input/catalyst/result context.


## 0.0.15 workstation interactions

### Stone Anvil: ordered forging and workability

Existing `workshop_processing` Stone Anvil recipes without `forge` retain their cold hammer-smashing behavior and `work` count. Adding `forge` replaces the generic work count with an ordered action sequence:

```json
{
  "type": "firstworks:workshop_processing",
  "station": "stone_anvil",
  "ingredient": { "item": "firstworks:annealed_copper_billet" },
  "result": { "id": "minecraft:copper_ingot" },
  "forge": {
    "actions": ["flatten", "draw", "bend", "flatten"],
    "heat_ticks": 1200,
    "visual": {
      "type": "deformable",
      "initial_profile": "billet",
      "length": 0.32,
      "width": 0.16,
      "height": 0.12
    }
  }
}
```

- `actions`: 1–64 entries, each `flatten`, `draw`, or `bend`. The next entry advances only when the matching working-surface zone is struck with an item in `#firstworks:hammers`. A wrong action does not consume work or tool durability.
- The broad center selects Flatten, outer edges/far side select Draw, and the projecting horn selects Bend. Coordinates rotate with the block; input item geometry never changes the controls. A subtle outline and contextual hint identify the targeted zone.
- `heat_ticks`: the workability window in loaded server ticks, default 1200 (60 seconds), range 0–72000. Zero enables cold working for custom recipes. This is separate from progress and tool durability.
- For heat-requiring recipes, load the input and sneak-use a hammer on the anvil beside a lit campfire or currently hot Crucible Furnace (one face-adjacent block, including below). This heats or reheats the workpiece without resetting its sequence. Cooling pauses work; it does not consume extra input or erase progress. Stored heat and actions survive save/reload; unloaded stations do not tick.
- Empty-hand use collects output. Sneak-empty-hand use retrieves stored items and resets work. Adding input/catalysts during partial forging is rejected to preserve progress.
- `visual` is optional. Without it, the original input item/model is rendered until completion. Built-in metalworking uses a solid workpiece; drawing lengthens it, flattening spreads/thins it, and bending raises its profile.
- `visual.type: "deformable"` supports `initial_profile: "billet"` or `"plate"`; dimensions are block units with length 0.05–0.6, width 0.05–0.4, and height 0.02–0.3. The generic solid visual uses copper material.
- `visual.type: "stages"` optionally uses `models`, a list of up to 65 model ids such as `example:forge_workpieces/billet_0`. Put those JSON models under `assets/example/models/forge_workpieces/`. Index zero is the untouched shape, subsequent indices follow progress, and the last model is retained when the list is shorter than the sequence. Author stages in anvil-local block coordinates; the surface is Y=10.6/16. Missing models fall back to the generic deformable workpiece. Completed recipes always display their result item.
- Forge metadata is valid only on Stone Anvil recipes. The existing workshop starting/completed hooks remain intact. `event.workshop.getForgeHeat()` and `getLastForgeAction()` expose the current state.

### Mortar: crush and held grinding

The mortar never completes work on a timer alone. Old recipes without `processing` become a single held-grind stage using their existing `duration`; no new fields are required.

```json
{
  "type": "firstworks:mortar_grinding",
  "ingredient": { "item": "minecraft:brick" },
  "result": { "id": "firstworks:grog" },
  "processing": [
    { "action": "crush", "count": 3 },
    { "action": "grind", "duration": 40 }
  ]
}
```

- `processing`: up to 16 ordered stages; an omitted/empty list uses the legacy-duration grind fallback.
- `crush` requires `count` 1–64 (no duration). Empty-hand use aimed at the bowl center performs one discrete pestle strike. Strikes have a short four-tick debounce.
- `grind` requires `duration` 1–72000 (no count). Hold use with an empty main hand while looking at the inner bowl/rim. Each validated server input tick earns one work tick; releasing, looking away, moving out of reach, opening a screen, changing items, or disconnecting stops further progress. Multiple players or duplicate packets cannot advance the same mortar twice in a server tick. Mouse-circle tracing is unnecessary.
- Stages may be crush-only, grind-only, or mixed. Wrong actions never advance a stage. The pestle strikes vertically for crushing and moves in a circle during actual grinding. Effects stop when grinding pauses.
- Stage index, stage work, recipe identity, and lifecycle state survive save/reload. A saved mortar resumes paused until a player operates it again. Reloading does not replay the starting hook for an unchanged partial recipe.
- Empty-hand use collects output; sneak-empty-hand use retrieves input and resets work. Automation still inserts recipe inputs and extracts results, but does not supply manual work.
- Starting/completed KubeJS hooks are preserved. A cancelled start stays blocked until input is retrieved/reloaded, avoiding one callback per held-input tick. `event.mortar.getStageIndex()` (zero-based), `getStageProgress()`, and `getStage()` expose stage state. The legacy no-player `startGrinding()` method remains callable but returns false; scripts cannot start autonomous grinding through it.

Use `ServerEvents.recipes(event => event.custom({...}))` for these optional metadata objects, including `forge`, `processing`, and loom `weaving`. Existing recipe builders and recipes without metadata remain valid. The examples above can be passed directly to `event.custom`.

### Crucible heat visuals

Hot contents, molten fill, and fire/spark effects require a paid, running batch with Bellows air remaining, input present, and no ready output. The one fuel item paid at batch start continues to cover that batch after an air pause; an empty reserve slot does not mean the paid batch has run out of fuel. Retained recipe progress or Bellows air alone never implies heat. Air starvation removes the hot appearance immediately under this rule; resuming air restores it for the paid batch. Completed output appears cooled. No new temperature simulation, yield change, or mold consumption rule is introduced.

### Controls and feedback coverage

Overlapping loom and mortar recipes use the largest input count that the loaded stack can satisfy, with recipe id as the tie-breaker. Processing, hints, and previews share that selection. Before enough input is loaded, hints show the smallest matching requirement (also ordered by id). Mortars accept material up to the largest matching batch, capped by the item's stack limit; choose the batch by loading it before starting work.

The 0.0.15 changes cover Stone Anvil, Loom, and Mortar controls through contextual hints, item tooltips, Jade, and JEI. Missing input counts, recipe catalysts, hammers, heat, wrong action/shed, and ready output have distinct feedback. JEI exposes ordered actions and stages, with complete long sequences in the recipe tooltip. The broader audit of unchanged stations in issue #21 and the pottery visuals in issue #19 remain outside this PR.
