# Firstworks

Firstworks is a standalone NeoForge 1.21.1 mod about grounded early-game crafts and slow, physical processing. It has no dependency on Create or the Inventors modpack.

## Current gameplay

- Animals that normally drop leather instead drop raw hide.
- Craft any Firstworks Barrel from matching planks and slabs, then fill it with one water bucket or four water bottles (1,000 mB).
- Add up to four raw hides, close the lid, and wait five minutes for soaked hides.
- Craft soaked hide with any sword to scrape it; each hide consumes one sword durability.
- Strip logs with an axe to collect two tree bark per block, then brew tannin by adding four bark to 1,000 mB water and sealing the barrel for one Minecraft day.
- Add scraped hides to tannin solution and seal the barrel for one Minecraft day.
- Dry tannin-soaked hide on a campfire to finish vanilla leather.

Empty-hand interaction opens or seals the barrel; completed output is collected before the lid toggles. The barrel has no menu. It exposes standard NeoForge item and fluid capabilities for optional automation: items enter from above, finished items leave below, and side access can see both item slots.

A rising redstone pulse also toggles the lid exactly once. Removing power only rearms the barrel for the next pulse. Manual or redstone-driven unsealing cancels current recipe progress without consuming or ejecting the contents.

Barrel processes are datapack recipes using the `firstworks:barrel_processing` type. Recipes may consume an ingredient and fluid, produce an item and/or replacement fluid, set their duration, and require either a sealed or open barrel.

## KubeJS

KubeJS is optional. When installed, Firstworks registers the typed `event.recipes.firstworks.barrel_processing` recipe helper and two server events.

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
  // event.cancel() prevents this process until the barrel contents or lid changes.
})

FirstworksEvents.barrelProcessCompleted(event => {
  console.info(`Completed ${event.recipeId} at ${event.pos}`)
})
```

Animal leather replacement is controlled by `config/firstworks-common.toml`. Packs that own their loot progression can disable it and provide raw hides through KubeJS instead.

## Jade and JEI

Both integrations are optional. Jade shows a Barrel's live status, remaining time, and progress bar in-world. JEI provides a Barrel Processing category with item and fluid inputs, item or fluid outputs, processing duration, and every wood variant as a catalyst.

## Building

Run `./gradlew build` (`gradlew.bat build` on Windows). The normal distributable jar is created under `build/libs`.
