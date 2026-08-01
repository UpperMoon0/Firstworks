# Firstworks

Firstworks is a standalone NeoForge 1.21.1 mod about primitive, in-world processing. Its centerpiece is a wooden Barrel: load it with items and fluids, seal the lid, and let time do the work. There is no machine screen and no instant crafting step—the ingredients, fluid, and finished product remain visible in the world.

The included hide-tanning chain is the first complete use of this system, not its limit. Barrel processes are data-driven and can be expanded by modpacks through datapacks or KubeJS.

Firstworks has no dependency on Create or the Inventors modpack.

## Primitive barrel processing

- Craft a Barrel from matching planks and slabs. Every vanilla wood family has a variant.
- Add ingredients and fluid directly to the open Barrel.
- Close or open the lid with an empty hand. Valid sealed recipes progress over time.
- See the stored item and fluid without opening a menu.
- Collect completed output before toggling the lid again.
- Toggle the lid with a rising redstone pulse for simple early automation.

Opening a working Barrel cancels its current progress without consuming or ejecting the contents. Standard NeoForge item and fluid capabilities are also exposed for modded automation: items enter from above, finished items leave below, and side access can inspect both item slots.

## Built-in leatherworking

Firstworks uses the Barrel to turn leather into a physical early-game production chain:

1. Animals that normally drop leather drop Raw Hide instead.
2. Soak Raw Hides in water.
3. Scrape each Soaked Hide with any sword, consuming one durability.
4. Strip logs with an axe to collect two Tree Bark.
5. Seal Tree Bark in water to brew Tannin Solution.
6. Tan Scraped Hide in the solution for one Minecraft day.
7. Dry the Tannin-Soaked Hide on a campfire to produce vanilla leather.

Animal leather replacement is controlled by `config/firstworks-common.toml`. Packs that provide their own early-game progression can disable it while continuing to use the Barrel system.

## Datapacks and KubeJS

Barrel processes use the `firstworks:barrel_processing` recipe type. A recipe can consume an ingredient and fluid, produce an item and/or replacement fluid, define its duration, and require the Barrel to be sealed or open.

KubeJS is optional. When installed, Firstworks registers the typed `event.recipes.firstworks.barrel_processing` helper and events for the start and completion of a Barrel process.

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
})

FirstworksEvents.barrelProcessStarting(event => {
  // event.cancel() prevents this process until the contents or lid changes.
})

FirstworksEvents.barrelProcessCompleted(event => {
  console.info(`Completed ${event.recipeId} at ${event.pos}`)
})
```

## Optional integrations

- **Jade** shows the Barrel's live state, remaining time, output, and progress bar in-world.
- **JEI** provides a Barrel Processing recipe category with item and fluid inputs, outputs, duration, and all wood variants as catalysts.

## Building

Run `./gradlew build` (`gradlew.bat build` on Windows). The distributable jar is created under `build/libs`.
