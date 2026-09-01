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
- `station`: `pottery_wheel`, `kiln`, `stone_anvil`, or `crucible_furnace`.
- `ingredient`: primary input ingredient.
- `input_count`: optional primary-input count, default 1.
- `catalyst`: optional catalyst/tool/mold ingredient.
- `catalyst_count`: optional catalyst count, default 1.
- `consume_catalyst`: whether the catalyst is consumed, default false.
- `result`: result item stack.
- `work`: manual strokes for manual stations or processing ticks for heated stations.

The Pottery Wheel advances with empty-hand interactions. The Stone Anvil advances when struck with an item in `firstworks:hammers`. The Kiln consumes coal/charcoal and advances over time. The Crucible Furnace consumes coal/charcoal but only advances while receiving Bellows air; use a Bellows adjacent to the furnace to stoke it.

## Default primitive-copper contract
The bundled default path is raw copper → cast billet (Crucible Furnace + fired mold + air) → annealed billet (Kiln) → worked billet (Stone Anvil + hammer). Firstworks copper tools, wire, fasteners, shears, buckets, and workshop upgrades are downstream of the worked billet so packs can gate mature metallurgy later without replacing the primitive mechanics.
