# Stone/Copper Workshop API

Firstworks 0.0.14 exposes the Stone-to-Copper workshop as reusable mechanics. Pack-specific stages and quest locks should remain outside Firstworks.

## Public tags
- `firstworks:resin_trees` (block): living trees that can receive renewable resin scars.
- `firstworks:resin_tapping_tools` (item): tools accepted for tapping in addition to primitive knives.
- `firstworks:hammers` (item): tools accepted by the Stone Anvil.
- `firstworks:refractory_materials` (item): common refractory inputs/outputs.
- `firstworks:primitive_copper` (item): Firstworks primitive-copper products.
- `firstworks:strong_bindings` (item): includes Rope and Hafting Compound by default.

## Workshop recipes
Recipe type: `firstworks:workshop_processing`.

Fields:
- `station`: `pottery_wheel`, `kiln`, `stone_anvil`, or `crucible_furnace`. Unknown station ids are rejected during recipe loading instead of producing unreachable recipes.
- `ingredient`: primary input ingredient.
- `input_count`: optional primary-input count, default 1.
- `catalyst`: optional catalyst/tool/mold ingredient. Field presence is authoritative: an explicitly declared catalyst that resolves to an empty ingredient/tag still remains required and therefore matches nothing. Omit the field entirely for catalyst-free recipes.
- `catalyst_count`: optional catalyst count, default 1.
- `consume_catalyst`: whether the catalyst is consumed, default false. Declaring `consume_catalyst: true` without a catalyst is invalid.
- `result`: result item stack.
- `work`: manual strokes for manual stations or processing ticks for heated stations.

For Java/API integrations, `WorkshopRecipe` uses `WorkshopRecipeInput`, which carries the station, primary input stack, and catalyst stack. Standard `RecipeManager#getRecipeFor` calls therefore enforce the complete workshop state instead of matching only the primary item. Firstworks' station block entity then applies its deterministic multi-match ordering on top of that contract.

When multiple recipes at one station match the loaded input and catalyst, Firstworks first selects the recipe with the largest satisfied `input_count`. At equal batch size, a matching catalyst-specific recipe wins over a catalyst-free fallback. Recipe id is the final deterministic tie-breaker. Packmakers can therefore use distinct batch sizes or catalysts as intentional in-world selectors without depending on recipe-manager iteration order.

The bundled Pottery Wheel still uses batch size as its data contract: 1 Refractory Clay shapes a tuyère, 2 shapes a casting mold, and 3 shapes a crucible. Players do not have to count repeated insertion clicks, however: while holding a valid wheel ingredient, **sneak-right-click the top plate**. The inner, middle, and outer rings load a 1, 2, or 3 item batch in one physical placement gesture, and the rendered workpiece immediately previews the selected recipe. Normal item insertion remains available for arbitrary pack recipes and batch sizes above three. Adding material resets manual progress, so the intended batch is selected before shaping begins without a GUI.

The Pottery Wheel advances with empty-hand interactions. The Stone Anvil advances when struck with an item in `firstworks:hammers`. The Kiln consumes coal/charcoal and advances over time. The Crucible Furnace consumes coal/charcoal but only advances while receiving Bellows air; use a Bellows adjacent to the furnace to stoke it. Each successful Bellows press adds 160 ticks to a finite 480-tick air reserve instead of replacing the previous pulse, so rhythmic presses can bank up to three strokes while neglecting the furnace still lets airflow decay to zero. Stronger reserves are also reflected by denser furnace flame/spark feedback. Fuel is a reserve rather than recipe state, so adding more fuel to a running heated station preserves its current progress and does not consume another fuel item immediately. Input or catalyst top-ups on a heated station also preserve progress and running state when the selected recipe id remains unchanged; if the insertion changes the selected recipe, processing resets before the new recipe begins.

### GUI-free insertion role resolution
Normal right-click insertion favors workshop recipe roles before fuel when an item can serve more than one role. Existing input batches are allowed to grow to a larger matching `input_count` before a same-item catalyst is selected; once the loaded batch satisfies a recipe that needs the held catalyst, the catalyst slot is preferred. On heated stations, **sneak-right-click coal or charcoal to force it into the fuel reserve**. Automation remains explicit: item-handler slot 0 is input, slot 1 is catalyst, slot 2 is fuel, and slot 3 is output.

## Resin tapping
Resin scars attach to horizontal faces of blocks in `firstworks:resin_trees`. A tappable trunk must belong to a connected `firstworks:resin_trees` structure that reaches a `#minecraft:leaves` canopy, so isolated/player-placed logs are not renewable resin sources by themselves. Their four visible growth stages progress from a fresh cut to a resin-heavy ripe scar; only the ripe stage can be harvested. The scar remains attached to the tapped face and resets to its fresh visual state after harvesting.

## Default primitive-copper contract
The bundled default path is raw copper → cast billet (Crucible Furnace + fired mold + air) → annealed billet (Kiln) → worked billet (Stone Anvil + hammer). Firstworks copper knife, wire, fasteners, and workshop upgrades are downstream of the worked billet so packs can gate mature metallurgy later without replacing the primitive mechanics. Copper Knife repair intentionally uses vanilla copper ingots rather than requiring another worked billet.
