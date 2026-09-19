# Firstworks

Firstworks is a standalone NeoForge 1.21.1 mod about primitive, in-world processing. Barrels, hand-operated textile tools, clay and masonry stations, resin tapping, Pottery Wheels, Stone Anvils, Bellows, Crucible Furnaces, Querns, and Charcoal Mounds turn early-game materials into tangible workshop projects without machine menus or instant conversions.

The mod has no hard dependency on Create or other tech mods. Its recipes, tags, progression switches, automation surfaces, Jade tooltips, JEI categories, and KubeJS lifecycle hooks are designed to be reusable by modpacks and datapacks.

---

## Modpack & Datapack Developers

Looking to configure recipes, progression gates, custom wood types, KubeJS events, animal drop profiles, public material tags, or server settings?

👉 **[Read the Full Packmaker & Datapack Developer Guide](docs/PACKMAKERS.md)**

---

## Core Features & Mechanics

### 🪵 Primitive Barrel Processing
- **In-World Crafting**: Craft Barrels from matching planks and slabs across every vanilla wood family.
- **Dual Fluid Stores**: Separate input and output fluid stores share a 4000 mB capacity, preventing recipe contamination.
- **Rain Collection**: Open barrels gradually collect rainwater during precipitation (configurable).
- **Automation Ready**: Top face for input items/fluid, bottom face for output items/fluid, side faces for bi-directional transfer. Sealing the lid locks transfer.
- **Redstone Control**: Rising redstone pulses toggle the lid open and closed.

### 🧵 Textile Crafting & Hand Weaving
- **Hand Spindle**: Hold in main hand with Retted Fibre in off-hand to hand-spin Twine.
- **Looms**: Load Twine or String, use the side post where the shuttle rests to throw it, and use the lower crossbar to change the A/B shed. Basic Cloth needs four alternating passes; the beater packs each pass automatically.
- **Automation**: Loom item insertion/extraction remains supported. Mechanical operation must target the correct side post and shed control; a fixed generic click does not weave automatically.
- **Sheep & Fleece**: Sheep drop color-aware Raw Fleece. Wash fleece in water-filled Barrels to make Clean Wool for textile beds.

### 🌲 Resin, Bindings & Primitive Tools
- **Renewable Resin Tapping**: Use a Resin Tap on supported trees to create harvestable resin scars instead of relying on one-off crafting sources.
- **Pack-Friendly Trees & Tools**: Public resin-tree and tapping-tool tags allow packs to add compatible woods and tools without code.
- **Hafting Compound**: Resin feeds stronger primitive bindings and the early tool progression.
- **Primitive Knives**: Flint, Bone, and Copper Knives support fibre harvesting, hide processing, and early crafting.
- **Cordage & Bindings**: Hand-twist Plant Fibre into Crude Cordage; progressively stronger bindings gate tool construction.

### 🧱 Clay, Refractory & Masonry Work
- **Wooden Brick Mold**: Place clay into the mold, press it by hand, and retrieve unfired bricks.
- **Pottery Wheel**: Shape data-driven workshop recipes directly in-world, including refractory crucibles, tuyères, and casting molds. The top plate supports 1/2/3-item primary-input batch placement by hit radius.
- **Vanilla Furnace Firing**: Fire ceramics and refractory parts in standard furnaces rather than a separate machine GUI.
- **Refractory Chain**: Produce grog, refractory clay, refractory bricks, fired crucibles, tuyères, and reusable casting molds for primitive metallurgy.
- **Wet Mortar**: Mix sand and water in sealed Barrels to create wet mortar for structural brick construction.

### 🔨 Primitive Copper Workshop
- **Crucible Furnace**: Cast raw copper into billets using a reusable casting mold and fuel from the public `#firstworks:crucible_furnace_fuels` tag.
- **Bellows Airflow**: Crucible processing requires active adjacent Bellows air. Repeated strokes can bank a finite airflow reserve.
- **Annealing**: Cast billets are annealed through vanilla furnace recipes.
- **Stone Anvil**: Manually work annealed billets with a hammer into vanilla Copper Ingots.
- **Downstream Copper**: Copper Ingots feed Copper Fasteners and the Copper Knife.
- **Pack Control**: `enablePrimitiveCopperProgression` can restore vanilla/datapack copper smelting while leaving the Firstworks workshop mechanics available.

### 🪨 Quern Workstation
- **Bulk Milling**: Load grain, minerals, pigments, or pack-defined ingredients into the central feed eye.
- **Manual Cranking**: Right-click with an empty hand to turn the crank; work per crank is configurable.
- **Hand Operated**: Item insertion and output extraction can be automated, but processing itself remains manual.
- **Recipe Priority**: Data-driven Quern recipes support explicit priority for intentional overlap handling.
- **Flour & Dough**: Mill wheat into Flour (`#c:flours/wheat`), knead it into Wheat Dough (`#c:doughs/wheat`), then bake or craft it into early foods.

### 🥣 Mortar & Pestle
- Place on any surface, add materials such as Raw Ochre, Charcoal, or Bone, pound the center to crush them, then hold use on the inner bowl/rim to grind with an animated in-world pestle.

### 🔥 Earthen Charcoal Mounds
- **Physical Construction**: Stack connected logs and encase them with suitable earthen blocks, leaving one opening.
- **Diegetic Feedback**: Ignite the opening, then seal the mound to begin carbonization with smoke, sound, and visual state changes.
- **Physical Charcoal Piles**: Finished mounds replace logs bottom-up with layered Charcoal Pile blocks storing charcoal directly in-world.

### 🧱 Lime & Plaster Construction
- Smelt calcite into Lime, combine it into Plaster, and craft reversible Plaster Blocks for an early construction-material loop.

### 📦 Woven Baskets
- 9-slot primitive storage crafted from woven fibres, with hopper and automation support.

### 🥩 Data-Driven Animal Materials & Hide Tanning
- Vertebrate animals drop bones and raw hide from customizable animal material profiles.
- Soak Raw Hides, scrape them with primitive knives, brew Tannin Solution from tree bark, tan scraped hides, and dry them into Leather or Heavy Leather progression materials.

### 🕯️ Other Early Survival Pieces
- **Standing Torches**: Floor-supported rustic torch stands.
- **Clay Buckets**: Fire-hardened vessels capable of carrying water and tannin solution.

---

## Workshop API

Pottery Wheels, Stone Anvils, and Crucible Furnaces share the data-driven `firstworks:workshop_processing` recipe type. Recipes can declare primary input, optional catalyst, result, work requirement, catalyst consumption, and explicit overlap `priority`.

Workshop stations expose separate input, catalyst, fuel, and output roles for automation. Same-recipe input/catalyst top-ups preserve active Crucible Furnace progress, and reserve-fuel top-ups do not restart the batch.

For full JSON schemas, supported ranges, public tags, progression toggles, and KubeJS examples, see **[docs/PACKMAKERS.md](docs/PACKMAKERS.md)**.

---

## Integrations

- **Jade**: Live in-world status for Barrels, Looms, Brick Molds, Mortar & Pestle, Querns, Charcoal Mounds/Piles, Pottery Wheels, Stone Anvils, Crucible Furnaces, and Bellows airflow.
- **JEI**: Dedicated categories for Barrel Processing, Hand Spinning, Loom Weaving, Brick Molding, Mortar Grinding, Quern Grinding, Workshop Processing, and dynamic Charcoal Mound information. Workshop views include station catalysts, reusable/consumed catalyst state, Bellows requirements, and tagged Crucible fuels.
- **KubeJS**: Custom wood registration, custom recipes, and process start/completion lifecycle hooks, with cancellable start events including `workshopProcessingStarting` and completion events including `workshopProcessingCompleted`.
- **Patchouli (optional)**: Adds the craftable **Firstworks Field Guide** with in-game instructions for the reworked workstations. Firstworks remains fully loadable without Patchouli.
- **Automation**: Stable item/fluid handlers are exposed for pack machinery while manual-only mechanics remain explicitly manual.

---

## Configuration

Firstworks uses **`SERVER`** configuration so gameplay options synchronize to multiplayer clients:
- Modpack defaults: `defaultconfigs/firstworks-server.toml`
- Per-world settings: `saves/<world>/serverconfig/firstworks-server.toml`

Major progression systems can be individually enabled or disabled, including textile, masonry, grain, tool-binding, and primitive-copper progression. See the [Packmaker Guide](docs/PACKMAKERS.md) for the complete configuration table and defaults.

---

## Building from Source

Run `./gradlew build` (or `gradlew.bat build` on Windows). The compiled JAR is output to `build/libs/`.

CI additionally runs the NeoForge GameTest server to exercise progression and workshop behavior.

---

## Credits

Inspired by [TerraFirmaCraft](https://www.curseforge.com/minecraft/mc-mods/terrafirmacraft) and its tactile approach to early-game survival progression.


### 0.0.15 manual workstation controls

- **Stone Anvil:** load a workpiece, reheat it by sneak-using a hammer beside a lit campfire or hot crucible, then follow the recipe's center/edge/horn actions. A cooled piece keeps its progress and can be reheated. Custom smashing recipes retain their simple hammer behavior.
- **Mortar and Pestle:** pound the center to crush coarse material, then hold use on the inner bowl/rim to grind. Releasing pauses work; there is no autonomous finish timer.
- **Loom:** throw from the shuttle's current side and change the A/B shed at the lower crossbar. Recipes may define repeating patterns.
- **Crucible Furnace:** glow and particles follow actual fuel-backed processing with airflow, independently of retained progress.

See [packmaker documentation](docs/PACKMAKERS.md#0015-workstation-interactions) for optional recipe metadata and compatibility behavior.
