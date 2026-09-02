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
- `catalyst`: optional catalyst/tool/mold ingredient.
- `catalyst_count`: optional catalyst count, default 1.
- `consume_catalyst`: whether the catalyst is consumed, default false.
- `result`: result item stack.
- `work`: manual strokes for manual stations or processing ticks for heated stations.

When multiple recipes at one station match the loaded input and catalyst, Firstworks selects the recipe with the largest satisfied `input_count`; recipe id is the deterministic tie-breaker. Packmakers should give intentionally selectable forms distinct batch sizes or catalysts rather than relying on recipe-manager iteration order.

The bundled Pottery Wheel uses the batch itself as an in-world selector: 1 Refractory Clay shapes a tuyère, 2 shapes a casting mold, and 3 shapes a crucible. Adding material resets manual progress, so the player can load the intended batch before beginning to shape it without a GUI.

The Pottery Wheel advances with empty-hand interactions. The Stone Anvil advances when struck with an item in `firstworks:hammers`. The Kiln consumes coal/charcoal and advances over time. The Crucible Furnace consumes coal/charcoal but only advances while receiving Bellows air; use a Bellows adjacent to the furnace to stoke it. Fuel is a reserve rather than recipe state, so adding more fuel to a running heated station preserves its current progress and does not consume another fuel item immediately.

## Resin tapping
Resin scars attach to horizontal faces of blocks in `firstworks:resin_trees`. Their four visible growth stages progress from a fresh cut to a resin-heavy ripe scar; only the ripe stage can be harvested. The scar remains attached to the tapped face and resets to its fresh visual state after harvesting.

## Default primitive-copper contract
The bundled default path is raw copper → cast billet (Crucible Furnace + fired mold + air) → annealed billet (Kiln) → worked billet (Stone Anvil + hammer). Firstworks copper knife, wire, fasteners, and workshop upgrades are downstream of the worked billet so packs can gate mature metallurgy later without replacing the primitive mechanics.
