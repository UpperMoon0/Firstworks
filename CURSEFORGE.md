# Firstworks

Firstworks brings primitive, hands-on processing into the world through Barrels, hand tools, Looms, clay and masonry stations, resin tapping, Pottery Wheels, Stone Anvils, Bellows, Crucible Furnaces, Querns, and Charcoal Mounds. Load materials directly into each workstation and watch the work take shape without a machine screen or instant conversion.

Its early-survival chains cover fibre and cloth, leatherworking, resin and tool bindings, masonry, refractory ceramics, primitive copperworking, lime/plaster construction, grain milling, and charcoal production. The systems are data-driven and deliberately reusable by modpacks.

## Barrels and wet processing

- Separate item/fluid input and output stores sharing 4000 mB fluid capacity
- Open/sealed recipe states with visible in-world contents
- Rain collection, redstone lid control, and standard NeoForge item/fluid capabilities
- Every vanilla wood family plus pack-extensible custom wood support
- Datapack and KubeJS recipe support

Barrels anchor tanning, fibre retting, mortar mixing, tannin brewing, and other early wet-processing recipes.

## Fibre, weaving, hides, and stronger tools

Gather Plant Fibre, twist Crude Cordage, ret better fibres, and spin Twine with the Hand Spindle. Wooden Looms turn Twine or String into Cloth through visible manual shuttle work. Sheep provide color-aware Raw Fleece that can be washed into Clean Wool for textile progression.

Animals provide Raw Hide instead of ready-made leather. Soak, scrape, tan, and dry hides through a visible multi-step workflow. Heavy Leather extends that chain into tougher workshop components such as Bellows.

Primitive Flint and Bone Knives support fibre gathering and hide work. Resin and improved bindings extend the tool progression beyond disposable early cordage.

## Renewable resin tapping

Use a Resin Tap on supported trees to create a resin scar directly on the trunk. The scar visibly matures through multiple growth states and can be harvested repeatedly once ready.

Public resin-tree and tapping-tool tags let modpacks add third-party trees or tools without code. Resin feeds Hafting Compound and stronger primitive bindings.

## Pottery, refractory ceramics, and masonry

The Wooden Brick Mold shapes early masonry pieces by hand. The Pottery Wheel provides a broader data-driven workshop for shaping refractory components directly in-world.

Firstworks 0.0.14 adds:

- Grog and Refractory Clay
- Unfired and fired Refractory Bricks
- Crucibles and Tuyères
- Reusable Casting Molds
- A tactile Pottery Wheel with animated work and 1/2/3-item primary-input placement by top-plate hit radius
- Vanilla-furnace firing for ceramic components

Wet Mortar remains the primitive binder for structural brick construction.

## Primitive copperworking

Copper now has a complete optional primitive workshop route before mature foundry machinery:

1. Load raw copper into the **Crucible Furnace** with a reusable Casting Mold.
2. Supply tagged furnace fuel such as Coal or Charcoal.
3. Work adjacent **Bellows** to provide the airflow required for processing.
4. Cast a Copper Billet.
5. Anneal the billet in a vanilla furnace.
6. Work the annealed billet on a **Stone Anvil** with a hammer to produce a vanilla Copper Ingot.

Bellows strokes bank a finite airflow reserve, while Crucible Furnace fuel remains a separate reserve that can be topped up without resetting the active batch. Copper Ingots then feed Copper Fasteners and the Copper Knife.

Modpacks can add fuels through `#firstworks:crucible_furnace_fuels` and can disable Firstworks' vanilla-copper smelting bypass removal through `enablePrimitiveCopperProgression` while keeping all workshop mechanics available.

## Mortar, Quern, grain, and construction materials

The Mortar & Pestle handles small precision grinding jobs. The hand-operated Quern handles bulk milling: load a batch, turn the runner stone manually, and retrieve the finished product. Item insertion and extraction can be automated, but processing work remains intentionally manual.

Mill wheat into Flour (`#c:flours/wheat`), knead Wheat Dough (`#c:doughs/wheat`), and use it for early food progression. Quern recipes support explicit overlap priority for pack-defined ingredients.

The construction chain also includes Lime and Plaster, including reversible Plaster Blocks for early finished building surfaces.

## Earthen charcoal mounds

Build a connected log pile, encase it with suitable earthen blocks, leave an opening, ignite it, then seal the mound. Smoke, sound, and state changes communicate carbonization in-world. Finished mounds produce physical layered Charcoal Pile blocks rather than silently converting inventory items.

## Baskets and other survival pieces

- 9-slot Woven Baskets with hopper/automation support
- Standing Torches for rustic floor-supported lighting
- Fire Starters for primitive ignition
- Clay Buckets for water and tannin handling before iron

## Made for survival and modpacks

Firstworks can stand alone as a tactile early-game overhaul or serve as the primitive processing layer of a larger technology pack. Major progression systems have server-configurable switches, while recipes, public tags, workshop overlap priority, fuel acceptance, and KubeJS lifecycle events provide finer packmaker control.

Optional integrations make the systems readable without replacing their in-world character:

- **Jade** shows live status for Barrels, Looms, Brick Molds, Mortar & Pestle, Querns, Charcoal Mounds/Piles, Pottery Wheels, Stone Anvils, Crucible Furnaces, and Bellows airflow.
- **JEI** adds categories for Barrel Processing, Hand Spinning, Loom Weaving, Brick Molding, Mortar Grinding, Quern Grinding, Workshop Processing, and Charcoal Mound information. Workshop views include station catalysts, Bellows requirements, and pack-added Crucible fuels.
- **KubeJS** supports custom recipes, wood registration, and start/completion lifecycle events, including cancellable workshop starts.

For the full recipe schemas, public tags, configuration options, automation contract, and KubeJS examples, see `docs/PACKMAKERS.md` in the project repository.

## Credits

Firstworks is inspired by [TerraFirmaCraft](https://www.curseforge.com/minecraft/mc-mods/terrafirmacraft) and its hands-on approach to believable survival progression.

Available for **Minecraft 1.21.1 on NeoForge**.
