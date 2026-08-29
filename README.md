# Firstworks

Firstworks is a standalone NeoForge 1.21.1 mod about primitive, in-world processing. Its wooden Barrels, hand-operated Looms, Brick Molds, Mortars, and Charcoal Mounds turn early-game materials into tangible workshop projects without machine menus or instant conversions.

Firstworks has no hard dependencies on Create or other tech mods, while offering clean integration for automation, Jade tooltips, JEI recipe views, and KubeJS customization.

---

## Modpack & Datapack Developers

Looking to configure recipes, custom wood types, KubeJS events, animal drop profiles, or server settings?
👉 **[Read the Full Packmaker & Datapack Developer Guide](docs/PACKMAKERS.md)**

---

## Core Features & Mechanics

### 🪵 Primitive Barrel Processing
- **In-World Crafting**: Craft Barrels from matching planks and slabs across every vanilla wood family.
- **Dual Fluid Stores**: Separate input and output fluid stores share a 4000 mB capacity, preventing recipe contamination.
- **Rain Collection**: Open barrels gradually collect rainwater during precipitation (can be disabled or tuned in config).
- **Automation Ready**: Top face for input items/fluid, bottom face for output items/fluid, side faces for bi-directional transfer. Sealing the lid locks transfer.
- **Redstone Control**: Rising redstone pulses toggle the lid open and closed.

### 🧵 Textile Crafting & Hand Weaving
- **Hand Spindle**: Hold in main hand with Retted Fibre in off-hand to hand-spin Twine.
- **Looms**: Load 4 Twine or String into a wooden Loom and work the shuttle with 16 empty-hand strokes to weave Cloth.
- **Automation**: Empty-handed Create Deployers can operate the Loom shuttle.
- **Sheep & Fleece**: Sheep drop color-aware Raw Fleece. Wash fleece in water-filled Barrels to make Clean Wool for textile beds.

### 🧱 Primitive Masonry & Clay Molding
- **Wooden Brick Mold**: Place clay into the mold, press twice with an empty hand, and retrieve unfired bricks.
- **Campfire Firing**: Fire unfired bricks and unfired clay buckets over campfires.
- **Wet Mortar**: Mix sand and water in sealed barrels to create wet mortar for binding structural brick blocks.

### 🔥 Earthen Charcoal Mounds
- **Physical Construction**: Stack connected logs (configurable via `charcoalMinLogs`–`charcoalMaxLogs`; defaults to 4–64) and encase them with dirt, grass, mud, or clay, leaving one opening.
- **Diegetic Feedback**: Light the opening with a Fire Starter or flint and steel to see smoke and flame bursts. Seal the opening to hear a muffled dirt-thud "whumpf" and transition into carbonization.
- **Physical Charcoal Piles**: Finished mounds replace logs bottom-up with layered `Charcoal Pile` blocks (`firstworks:charcoal_pile`) storing 1–4 charcoal per block.

### 🥣 Mortar & Pestle
- Place on any surface, add grindable materials (Raw Ochre, Charcoal, Bone), and right-click to grind. Features an animated in-world pestle.

### 🔪 Primitive Knives, Cordage & Tools
- **Flint & Bone Knives**: Used for guaranteed plant fibre harvesting from grass, hide scraping, and early crafting.
- **Cordage & Bindings**: Hand-twist Plant Fibre into Crude Cordage. Wooden and stone tools require primitive bindings; metal and diamond tools require Rope.
- **Standing Torches**: Floor-supported rustic torch stands (light level 14).
- **Clay Buckets**: Fire-hardened vessels capable of carrying water and tannin solution.

### 📦 Woven Baskets
- 9-slot primitive storage container crafted from woven fibres, with full automation and hopper support.

### 🥩 Data-Driven Animal Materials & Hide Tanning
- Vertebrate animals drop bones and raw hide based on customizable data-driven animal material profiles.
- Soak Raw Hides in water, scrape them with a primitive knife, brew Tannin Solution from tree bark, tan scraped hides, and dry them into finished leather.

---

## Integrations

- **Jade**: Live in-world tooltips for Barrels, Looms, Brick Molds, Mortar & Pestle, Charcoal Mounds (with progress bars and remaining time visible through the outer shell), and Charcoal Piles.
- **JEI**: Dedicated recipe categories for Barrel Processing, Hand Spinning, Loom Weaving, Brick Molding, Mortar Grinding, and dynamic Charcoal Mound Information.
- **KubeJS**: Custom wood type registration (`Firstworks.registerWoodType`), recipe support via `event.custom({...})`, and event handlers for process start/completion.

---

## Configuration

Firstworks uses **`SERVER`** configuration so gameplay options synchronize to multiplayer clients:
- Modpack defaults: `defaultconfigs/firstworks-server.toml`
- Per-world settings: `saves/<world>/serverconfig/firstworks-server.toml`

See the [Packmaker Guide](docs/PACKMAKERS.md) for full configuration tables and defaults.

---

## Building from Source

Run `./gradlew build` (or `gradlew.bat build` on Windows). The compiled JAR is output to `build/libs/`.

---

## Credits

Inspired by [TerraFirmaCraft](https://www.curseforge.com/minecraft/mc-mods/terrafirmacraft) and its tactile approach to early-game survival progression.
