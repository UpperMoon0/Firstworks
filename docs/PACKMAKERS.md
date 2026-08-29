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
8. [QuernDriveable External Drive API (Java)](#8-querndriveable-external-drive-api-java)
9. [In-World Charcoal Mound System](#9-in-world-charcoal-mound-system)
10. [Jade & JEI Integration](#10-jade--jei-integration)
11. [Migration Notes (0.0.10 → 0.0.11)](#11-migration-notes-0010--0011)
12. [Migration Notes (0.0.11 → 0.0.12)](#12-migration-notes-0011--0012)

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
| `bindMetalVanillaTools` | Boolean | `true` | `true / false` | Requires `#firstworks:strong_bindings` (Rope) for iron, gold, and diamond tools. |
| `enableTextileProgression` | Boolean | `true` | `true / false` | Replaces wool drops with raw fleece, disables String-to-Wool, and requires Cloth/Clean Wool for beds. |
| `enableMasonryProgression` | Boolean | `true` | `true / false` | Requires brick molding, firing, wet mortar mixing, and mortar-bound brick blocks. |
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
| `quernDefaultDrivenWorkPerTick` | Integer | `1` | `1 – 100` | Work progress per tick applied when an external drive engages the Quern through `setDriven(true)` without an explicit rate. |

---

## 2. Public Tags Reference

Firstworks exposes data-driven tags for extensible pack integration. Below are the exact shipped default contents for each tag:

### Item Tags (`data/firstworks/tags/item/`)

| Tag | Shipped Default Items | Purpose |
| :--- | :--- | :--- |
| `#firstworks:primitive_knives` | `firstworks:bone_knife`, `firstworks:flint_knife` | Tools recognized for hide scraping, guaranteed fibre harvesting, and ochre extraction. |
| `#firstworks:primitive_bindings` | `firstworks:crude_cordage`, `firstworks:rope` | Items accepted as bindings for wooden, stone, bone, and flint tools. |
| `#firstworks:strong_bindings` | `firstworks:rope` | High-tier binding items required for iron, gold, and diamond tools. |
| `#firstworks:charcoal_igniters` | `firstworks:fire_starter`, `minecraft:flint_and_steel` | Items capable of igniting charcoal mounds. |
| `#firstworks:raw_hides` | `firstworks:raw_hide` | Raw hide items removed during animal drop normalization before adding `firstworks:raw_hide`. Packs integrating third-party animal mods should add items like `naturalist:hide` here. |
| `#firstworks:tree_bark` | `firstworks:tree_bark` | Stripped bark items used for brewing tannin solution in barrels. |
| `#firstworks:barrels` | All barrel item variants | Item classification for barrel workstations. |
| `#firstworks:looms` | All loom item variants | Item classification for loom workstations. |

### Common NeoForge Tags (`data/c/tags/item/`)

Firstworks participates in the `c` (Common Tags) interoperability namespace so third-party flours and doughs plug into Firstworks progression without recipe edits:

| Tag | Shipped Default Items | Purpose |
| :--- | :--- | :--- |
| `#c:flours` | `firstworks:flour` | Any ground-grain flour accepted by flour consumers. |
| `#c:flours/wheat` | `firstworks:flour` | Wheat flour accepted by dough recipes and the vanilla Cake override. |
| `#c:doughs` | `firstworks:dough` | Any raw dough accepted by dough consumers. |
| `#c:doughs/wheat` | `firstworks:dough` | Wheat dough accepted by Firstworks bread cooking recipes and the vanilla Bread/Cookie overrides. |

**Intended rule:** third-party mods and datapacks that add their own flour or wheat dough should add those items to these common tags rather than hardcoding `firstworks:flour` / `firstworks:dough` into replacement recipes. Firstworks recipes and overrides match by tag, so tagged foreign items work automatically (e.g. a rice flour mod adds its item to `#c:flours`, a modpack reroutes a mod's wheat dough through `#c:doughs/wheat` to bake with Firstworks bread recipes).

### Block Tags (`data/firstworks/tags/block/`)

| Tag | Shipped Default Blocks | Purpose |
| :--- | :--- | :--- |
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
  "ingredient": { "item": "firstworks:twine" },
  "input_count": 4,
  "result": { "id": "firstworks:cloth", "count": 1 },
  "strokes": 16
}
```

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
Single-block grinding workstation with manual and externally driven operation.
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

**Work-based throughput model.** Recipe `work` is source-agnostic; labor sources contribute progress at their own rates:

```
recipe.work ÷ labor source work rate = processing effort/time
```

- **Manual cranking** (right-click, empty hand): adds `quernManualWorkPerCrank` work per crank. Wheat at defaults: 60 work ÷ 5/crank = 12 cranks.
- **External drive**: adds the drive rate per tick (see [QuernDriveable External Drive API](#8-querndriveable-external-drive-api-java)). An animal wheel at 1 work/tick finishes wheat in 60 ticks (3 seconds); a stronger mechanical drive at 3 work/tick finishes in 1 second.

**Ingredient Exclusivity:** because the quern matches ingredients independently of stack size to allow incremental 1-by-1 loading, quern recipes must define mutually exclusive ingredient sets (never register multiple quern recipes with overlapping ingredient matchers).

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
  // Fires for BOTH operation modes: the first manual crank of a batch and
  // the moment an external drive engages a complete batch (e.g. an animal
  // or mechanical wheel starting to rotate the quern).
})
FirstworksEvents.quernGrindingCompleted(event => {
  console.info(`Quern completed ${event.recipeId} at ${event.pos}`)
})
```

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
  - Can be operated by automated deployers (e.g. Create Deployer in empty-hand "Use" mode).
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

---

## 7. Food Recipe Overrides & Interop IDs

Firstworks rewrites vanilla wheat-food progression and adds its own dough pipeline. Target these exact recipe IDs when rerouting or removing routes:

**Vanilla recipe overrides** (shipped under `data/minecraft/recipe/`; these replace the vanilla files):
- `minecraft:bread` — 3× `#c:doughs/wheat`
- `minecraft:cookie` — `#c:doughs/wheat` + cocoa beans
- `minecraft:cake` — 3× `#c:flours/wheat` + milk buckets + sugar + egg

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

## 8. QuernDriveable External Drive API (Java)

`com.nstut.firstworks.content.quern.QuernDriveable` is the public contract for addons that externally rotate a Quern (animal wheels, wind gimmicks, mechanical power, CHP-CE integrations, etc.):

```java
public interface QuernDriveable {
    boolean canDrive();                                  // complete batch present?
    int getDriveRate();                                  // current work/tick (0 = disconnected)
    void setDriveRate(int workPerTick);                  // engage/adjust/disconnect

    default boolean isDriven() { return getDriveRate() > 0; }
    default void setDriven(boolean driven) {
        setDriveRate(driven ? FirstworksConfig.QUERN_DEFAULT_DRIVEN_WORK_PER_TICK.get() : 0);
    }
}
```

Usage:

```java
if (level.getBlockEntity(pos) instanceof QuernDriveable quern) {
    if (quern.canDrive()) {
        quern.setDriveRate(1);
    }
}
```

**Lifecycle semantics:**

- **Drive updates happen server-side.** Rates set on the client are ignored; engage drives from your server tick or server-side interaction handlers.
- **`0` work/tick means disconnected.** The quern stops rotating but preserves accumulated progress on the loaded batch, so re-engaging resumes where it left off.
- **External providers must stop the drive when detached or unloaded** — call `setDriveRate(0)` when your power source moves away, breaks, or its chunk unloads.
- **`canDrive()` indicates whether a valid complete batch is present.** A drive applied while `canDrive()` is `false` is accepted but does no work; the quern disengages itself (`stop()`) if the active recipe becomes invalid, the batch disappears, or the output slot is blocked.
- **Recipe `work` determines required processing effort.** Progress accumulates as `rate × ticks`; the batch completes when accumulated progress reaches `recipe.work`. Rates above the remaining work simply finish sooner the same tick.
- **Manual and external progress share one accumulator.** A half-cranked batch can be finished by an external drive and vice versa.
- **Persistence:** progress and visual rotation survive chunk unload/reload, but active external drive does not. The Quern reloads stopped at `0` work/tick; your provider must confirm it is still attached and reapply `setDriveRate(rate)` on its own load.

**Recommended rate semantics** (conventions, not enforced): `1` = basic animal drive, `2` = stronger/faster animal, `4` = mechanical system. Alternatively compute the rate yourself — the balance equation is simply `recipe.work ÷ rate = ticks`. `setDriven(true)` uses `quernDefaultDrivenWorkPerTick` (default 1) for backward compatibility.

---

## 9. In-World Charcoal Mound System

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

## 10. Jade & JEI Integration

- **Jade Tooltips**:
  - **Barrel**: Recipe progress, time remaining, output preview, input/output fluid stores.
  - **Loom**: Loaded threads, stroke progress, output preview.
  - **Brick Mold**: Loaded ingredient, press progress bar.
  - **Mortar & Pestle**: Empty status, loaded materials, grinding progress bar with seconds remaining, output ready alert.
  - **Quern**: Loaded batch, work progress bar, completed output alert.
  - **Charcoal Mound**: Active log count, seal countdown progress bar, carbonization progress bar, remaining time, and expected yield (resolving through the visible sealant shell).
  - **Charcoal Pile**: Stored charcoal count.
- **JEI Categories**:
  - Barrel Processing, Hand Spinning, Loom Weaving, Brick Molding, Mortar Grinding, **Quern Grinding**, and dynamic Charcoal Mound Information guide.

---

## 11. Migration Notes (0.0.10 → 0.0.11)

1. **Config Path Changed**: Move customizations from `config/firstworks-common.toml` to `defaultconfigs/firstworks-server.toml` (for modpacks) or `saves/<world>/serverconfig/firstworks-server.toml` (for existing worlds).
2. **Charcoal Piles in World**: Charcoal mounds now materialize physical `firstworks:charcoal_pile` blocks in the world upon completion instead of dropping item entities on uncover.
3. **Legacy Mound Migration**: Existing worlds with completed `"READY"` mounds automatically deserialize as `Phase.LEGACY_READY` and safely convert to physical charcoal piles when chunks load.

---

## 12. Migration Notes (0.0.11 → 0.0.12)

1. **Quern Introduced**: `firstworks:quern` and the `firstworks:quern_grinding` recipe type are new in 0.0.12. During development, the planned Saddle Quern and Rotary Quern were consolidated into this single workstation before release, so no released-world migration from those IDs is required. Recipes use one source-agnostic `work` field (default `60`) shared by manual and external power.
2. **Flour/Dough Common Tags**: `firstworks:flour` is now tagged `#c:flours` + `#c:flours/wheat` and `firstworks:dough` is tagged `#c:doughs` + `#c:doughs/wheat`. Recipe-matching is tag-based, so foreign flour/dough items can join progression by editing tags only.
3. **Vanilla Food Overrides**: `minecraft:bread` and `minecraft:cookie` now require `#c:doughs/wheat`; `minecraft:cake` requires `#c:flours/wheat`. Wheat → flour → dough → bread/cookies is the new progression (see [Food Recipe Overrides & Interop IDs](#7-food-recipe-overrides--interop-ids)). Packs that want vanilla behavior should delete these three override files or remove the routes with KubeJS.
4. **QuernDriveable API is Rate-Based**: `setDriven(boolean)`/`isDriven()` remain for compatibility, but the primary contract is now `getDriveRate()`/`setDriveRate(int)`. `setDriven(true)` engages at `quernDefaultDrivenWorkPerTick` (default 1), matching previous 1 work/tick behavior. Addons driving with hard-coded `1` continue to work unchanged.
5. **New Config Options**: `quernManualWorkPerCrank` (default 5) and `quernDefaultDrivenWorkPerTick` (default 1) in `firstworks-server.toml`. At the defaults, a 60-work wheat batch takes 12 manual cranks or 60 driven ticks (3 seconds).
