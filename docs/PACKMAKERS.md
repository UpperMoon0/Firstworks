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
   - [Recipe Registration](#kubejs-recipe-registration)
   - [Event Handlers & Payload Fields](#kubejs-event-handlers--payload-fields)
6. [Automation Capabilities & Sided Behavior](#6-automation-capabilities--sided-behavior)
7. [In-World Charcoal Mound System](#7-in-world-charcoal-mound-system)
8. [Jade & JEI Integration](#8-jade--jei-integration)
9. [Migration Notes (0.0.10 → 0.0.11)](#9-migration-notes-0010--0011)

---

## 1. Configuration

Starting in **0.0.11**, Firstworks registers all gameplay options as a **`SERVER`** configuration. This ensures that gameplay-authoritative rules, progression toggles, durations, and yield multipliers are automatically synchronized from dedicated servers to connected clients (and reflected accurately in client guides such as JEI and Jade).

- **Modpack Default Path**: `defaultconfigs/firstworks-server.toml`
- **Per-World Path**: `saves/<world>/serverconfig/firstworks-server.toml`

### Configuration Options

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
| `charcoalCarbonizeDuration` | Integer | `6000` | `20 – 72000` | Ticks required for a sealed mound to carbonize (default: 5 minutes). |
| `charcoalMinLogs` | Integer | `4` | `4 – 64` | Minimum number of connected logs required for a valid charcoal mound. |
| `charcoalMaxLogs` | Integer | `64` | `4 – 256` | Maximum connected logs allowed in a single charcoal mound. |
| `charcoalSealWindow` | Integer | `1200` | `20 – 6000` | Ticks allowed to seal the opening after ignition before charge fails (default: 60s). |
| `charcoalNormalYield` | Double | `0.75` | `0.05 – 1.0` | Charcoal multiplier when mound finishes without breach (75% yield). |
| `charcoalBreachedYield` | Double | `0.25` | `0.0 – 1.0` | Charcoal multiplier if mound is breached during carbonization (25% yield). |
| `plantFibreHandChance` | Double | `0.30` | `0.0 – 1.0` | Chance to gather Plant Fibre with an empty hand or non-knife tool. |
| `rawOchreGatherChance` | Double | `0.20` | `0.0 – 1.0` | Chance to gather Raw Ochre without a primitive knife. |

---

## 2. Public Tags Reference

Firstworks exposes data-driven tags for extensible pack integration:

### Item Tags (`data/firstworks/tags/item/`)
- `#firstworks:primitive_knives`: Tools recognized for hide scraping, guaranteed fibre harvesting, and ochre extraction (e.g. `firstworks:flint_knife`, `firstworks:bone_knife`).
- `#firstworks:primitive_bindings`: Items accepted for primitive tool recipes (e.g. `firstworks:crude_cordage`, `firstworks:twine`, `firstworks:plant_fibre`).
- `#firstworks:strong_bindings`: High-tier binding items for metal and diamond tools (e.g. `firstworks:rope`).
- `#firstworks:charcoal_igniters`: Items capable of igniting charcoal mounds (e.g. `firstworks:fire_starter`, `minecraft:flint_and_steel`).
- `#firstworks:raw_hides`: Hide items removed when normalizing animal drops before granting `firstworks:raw_hide` (e.g. `firstworks:raw_hide`, `naturalist:hide`).
- `#firstworks:tree_bark`: Stripped bark items used for brewing tannin solution.
- `#firstworks:barrels`: All barrel item variants.
- `#firstworks:looms`: All loom item variants.

### Block Tags (`data/firstworks/tags/block/`)
- `#firstworks:charcoal_woods`: Blocks accepted as fuel logs in charcoal mounds (defaults to `#minecraft:logs`).
- `#firstworks:charcoal_sealants`: Blocks accepted as airtight sealant shells (e.g. dirt, grass, coarse dirt, mud, clay, packed mud, gravel, terracotta).
- `#firstworks:ochre_sources`: World blocks capable of yielding raw ochre when broken (defaults to `#minecraft:terracotta` and `minecraft:red_sand`).
- `#firstworks:plant_fibre_sources`: Single-tall vegetation providing plant fibre (e.g. short grass, fern).
- `#firstworks:double_plant_fibre_sources`: Double-tall vegetation providing double plant fibre (e.g. tall grass, large fern).
- `#firstworks:barrels`: All barrel block variants.
- `#firstworks:looms`: All loom block variants.

### Entity Type Tags (`data/firstworks/tags/entity_type/`)
- `#firstworks:drops_bones`: Vertebrate entities that drop 1–2 bones on death when `addAnimalBoneDrops` is enabled.
- `#firstworks:no_bone_drops`: Packmaker veto tag excluding specific entities from bone drops regardless of fallback rules.
- `#firstworks:leather_drops_as_raw_hide`: Entities whose vanilla leather drops are replaced with raw hide.
- `#firstworks:no_raw_hide_drops`: Packmaker veto tag excluding specific entities from hide replacement.

---

## 3. Animal Material Profiles

Fine-grained animal drop configurations can be defined under `data/<namespace>/firstworks/animal_materials/<name>.json`.

### Precedence Order
1. **Exclusion Tags (`#firstworks:no_*`)**: Highest priority veto.
2. **Animal Material Profiles (`animal_materials/*.json`)**: Per-entity configured min/max bone and hide counts.
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
Held in hand with Hand Spindle.
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
*Automatically generates matching `kubejs:redwood_barrel` and `kubejs:redwood_loom` blocks, items, blockstates, models, loot tables, recipes, and JEI/Jade integration.*

### KubeJS Recipe Registration
In a **server script** (`kubejs/server_scripts/recipes.js`):
```js
ServerEvents.recipes(event => {
  // Barrel Processing
  event.recipes.firstworks.barrel_processing(
    'firstworks:tannin_soaked_hide',
    'firstworks:scraped_hide',
    'firstworks:tannin_solution',
    250
  ).outputFluid('minecraft:water', 250).time(1200).id('custom:tan_hide')

  // Loom Weaving
  event.recipes.firstworks.loom_weaving(
    'firstworks:cloth',
    'firstworks:twine'
  ).inputCount(4).strokes(16).id('custom:weave_cloth')

  // Hand Spinning
  event.recipes.firstworks.spinning(
    'firstworks:twine',
    'firstworks:retted_fibre'
  ).inputCount(2).time(40).id('custom:spin_twine')

  // Brick Molding
  event.recipes.firstworks.brick_molding(
    'firstworks:unfired_clay_brick',
    'minecraft:clay_ball'
  ).inputCount(1).presses(2).id('custom:mold_brick')

  // Mortar Grinding
  event.recipes.firstworks.mortar_grinding(
    'firstworks:ground_ochre',
    'firstworks:raw_ochre'
  ).inputCount(1).time(48).id('custom:grind_ochre')
})
```

### KubeJS Event Handlers & Payload Fields

All process events support `event.cancel()` on `*Starting` handlers.

```js
// 1. Barrel Processing
FirstworksEvents.barrelProcessStarting(event => {
  // event.cancel()
  // Fields: event.level, event.pos, event.barrel, event.recipeId, event.recipe,
  //         event.input, event.inputFluid, event.result, event.outputFluid
})
FirstworksEvents.barrelProcessCompleted(event => {
  console.info(`Barrel completed ${event.recipeId} at ${event.pos}`)
})

// 2. Loom Weaving
FirstworksEvents.loomWeavingStarting(event => {
  // Fields: event.level, event.pos, event.loom, event.recipeId, event.recipe,
  //         event.input, event.result
})
FirstworksEvents.loomWeavingCompleted(event => {
  console.info(`Wove ${event.result} at ${event.pos}`)
})

// 3. Spindle Spinning
FirstworksEvents.spindleSpinningStarting(event => {
  // Fields: event.level, event.player, event.recipeId, event.recipe,
  //         event.input, event.result
})
FirstworksEvents.spindleSpinningCompleted(event => {
  console.info(`${event.player.name.string} spun ${event.result}`)
})

// 4. Brick Molding
FirstworksEvents.brickMoldingStarting(event => {
  // Fields: event.level, event.pos, event.mold, event.recipeId, event.recipe,
  //         event.input, event.result
})
FirstworksEvents.brickMoldingCompleted(event => {
  console.info(`Molded ${event.result} at ${event.pos}`)
})

// 5. Mortar Grinding
FirstworksEvents.mortarGrindingStarting(event => {
  // Fields: event.level, event.pos, event.mortar, event.recipeId, event.recipe,
  //         event.input, event.result
})
FirstworksEvents.mortarGrindingCompleted(event => {
  console.info(`Mortar ground ${event.result} at ${event.pos}`)
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
  - Top/sides insert moldable ingredients; bottom extracts unfired output.
  - Create Deployer in empty-hand "Use" mode performs presses.
- **Woven Basket (`firstworks:basket`)**:
  - 9-slot primitive storage container with full automation support.
- **Mortar & Pestle (`firstworks:mortar_and_pestle`)**:
  - Exposes standard NeoForge `IItemHandler` capability.

---

## 7. In-World Charcoal Mound System

- **Construction**: 4 to 64 connected logs (`#firstworks:charcoal_woods`), encased in an airtight shell (`#firstworks:charcoal_sealants`) leaving 1 exposed face open for ignition.
- **Candidate Detection**: The mod calculates shell coverage ($\ge 50\%$). Ordinary standing trees ($< 10\%$ coverage) are ignored and pass through to vanilla flint-and-steel behavior.
- **Ignition & Sealing**:
  - Right-click the exposed log opening with `#firstworks:charcoal_igniters`.
  - Diegetic feedback: flame & smoke burst from opening; no action-bar popup.
  - Place a sealant block into the opening within `charcoalSealWindow` (default: 60s).
  - Instant transition with gravel "whumpf" sound (protected by a 1-tick deferred check against claim cancellation races).
- **Carbonization**:
  - Carbonizes for `charcoalCarbonizeDuration` (default: 5 min).
  - Subtle, capped smoke leaks through exterior sealant blocks (1–3 sources/sec).
- **Materialization**:
  - Consumed logs are replaced bottom-up directly in the world with physical `firstworks:charcoal_pile` blocks.
  - Normal yield: 75% (`charcoalNormalYield`).
  - Breached early yield: 25% (`charcoalBreachedYield`).

---

## 8. Jade & JEI Integration

- **Jade Tooltips**:
  - **Barrel**: Recipe progress, time remaining, output preview, input/output fluid stores.
  - **Loom**: Loaded threads, stroke progress, output preview.
  - **Brick Mold**: Loaded ingredient, press progress bar.
  - **Mortar & Pestle**: Empty status, loaded materials, grinding progress bar with seconds remaining, output ready alert.
  - **Charcoal Mound**: Active log count, seal countdown progress bar, carbonization progress bar, remaining time, and expected yield (resolving through the visible sealant shell).
  - **Charcoal Pile**: Stored charcoal count.
- **JEI Categories**:
  - Barrel Processing, Hand Spinning, Loom Weaving, Brick Molding, Mortar Grinding, and dynamic Charcoal Mound Information guide.

---

## 9. Migration Notes (0.0.10 → 0.0.11)

1. **Config Path Changed**: Move customizations from `config/firstworks-common.toml` to `defaultconfigs/firstworks-server.toml` (for modpacks) or `saves/<world>/serverconfig/firstworks-server.toml` (for existing worlds).
2. **Charcoal Piles in World**: Charcoal mounds now materialize physical `firstworks:charcoal_pile` blocks in the world upon completion instead of dropping item entities on uncover.
3. **Legacy Mound Migration**: Existing worlds with completed `"READY"` mounds automatically deserialize as `Phase.LEGACY_READY` and safely convert to physical charcoal piles when chunks load.
