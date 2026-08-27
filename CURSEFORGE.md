# Firstworks

Firstworks brings primitive, hands-on processing into the world through functional wooden Barrels and hand-operated Looms. Load materials directly into each workstation and watch the work take shape without a machine screen or instant conversion.

Cordage, weaving, and leatherworking form the first complete production chains, built around reusable processing systems for grounded survival progression.

## The Barrel

- Holds materials and fluids with distinct **input** and **output** stores sharing 4000 mB capacity
- Processes recipes over time while open or sealed, as the recipe requires (consuming only needed fluid)
- Displays stored items and fluids directly in the world with separate input/output level rendering
- Opens and seals by hand without a GUI (shift + empty hand retrieves input items)
- Responds to rising redstone pulses for simple automation
- Supports sided item and fluid transfer through standard NeoForge capabilities (top input, bottom output, side combined)
- Optionally collects rainwater when enabled in config
- Locks automated item and fluid transfer while sealed
- Comes in every vanilla wood family
- Accepts custom recipes from datapacks and KubeJS

Fill a Barrel with buckets or bottles, add a material, and seal it to begin a valid process. Opening it early stops the current progress but leaves the contents in place.

## Fibre, cordage, and stronger tools

Gather Plant Fibre from grass and ferns, twist emergency Crude Cordage, and ret better fibres in a Barrel. Hold a durable Hand Spindle with fibre in your offhand to spin it into Twine, complete with a rotating three-dimensional spindle, working sounds, and particles. Twine can be woven into Cloth or combined into Rope. Gathering by hand has a modest chance to produce fibre, while using a sword guarantees it. Primitive Bone and Flint tools offer early options before metalworking. Wooden, stone, bone, and flint tools need a simple binding, while iron, gold, and diamond tools call for proper Rope.

Bind a vanilla torch, stick, and Crude Cordage or Rope into a **Standing Torch**—a rustic, 1.6-block tall floor-supported torch stand (light level 14) with flame and smoke particles. Flint, Plant Fibre, and either primitive binding also make a single-use **Fire Starter** that ignites or relights campfires before breaking after one use.

An early Clay Bucket bridges the gap before iron: shape it from clay, fire it on a campfire, then use it to collect and place water or Tannin Solution for Barrel work. Tannin Solution behaves like brown-tinted water when placed, while the clay vessel deliberately refuses lava and unrelated fluids.

## Hide tanning

Animals drop **Raw Hide** instead of ready-made leather, and crafting 4 Rabbit Hides yields Raw Hide instead of finished leather. Producing leather demonstrates the full primitive workflow:

1. Soak Raw Hide in water.
2. Scrape the softened hide with a sword.
3. Strip logs with an axe to gather Tree Bark.
4. Brew bark and water into Tannin Solution inside a sealed Barrel.
5. Tan the scraped hide in the solution.
6. Dry the treated hide in a furnace.

The result is still vanilla leather, but it comes from a small workshop and a process you can see rather than a single crafting recipe.

## Fleece, wool, and bedding

Sheep provide Raw Fleece in the color of their coat. Wash it in a sealed Barrel to make Clean Wool, recolor it with dyes when needed, and pack matching wool into blocks. A proper bed brings the fibre and wool crafts together, requiring Cloth, Clean Wool, and planks.

## Weaving by hand

Turn Twine or String into Cloth on a functional wooden Loom. Load thread directly onto the frame, then work the shuttle by hand as the woven material visibly grows. Finished Cloth is collected from the Loom itself, and unfinished thread can be removed without opening a menu.

- A Loom for every vanilla wood family
- Accurate frame-shaped collision and selection bounds
- Smooth shuttle movement and visible weaving progress
- Output-driven weave colors that also work with custom recipes
- Data-driven Barrel, spindle, and Loom recipes with typed KubeJS recipe/event support
- Item capabilities for loading thread and extracting finished work
- Empty-hand interaction compatible with deployer-style automation

## Primitive masonry

Load one clay ball into a single-cavity Wooden Brick Mold (featuring an integrated molding board base for reliable center interaction) and press twice with an empty hand. The material visibly spreads farther across the cavity after each press, ending as a fully packed clay-textured surface tinted from the recipe output. Collect the unfired brick and fire it over a campfire, then mix sand and water into Wet Mortar in a sealed Barrel to bind structural blocks. The Mold supports hopper loading, output extraction, empty-handed Create Deployers, and custom press counts through datapacks or KubeJS.

## Made for survival and modpacks

Firstworks can stand alone as a tactile early-game mechanic or serve as the primitive processing layer of a larger progression pack. Its leather, textile, and tool-binding progressions can each be adjusted to fit the experience you want.

Optional integrations make the system easier to read without changing its in-world character:

- **Jade** shows live Barrel status and Loom loading, weaving progress, and completed output.
- **JEI** adds dedicated Barrel Processing, Hand Spinning, and Loom Weaving recipe categories.

## Credits

Firstworks is inspired by [TerraFirmaCraft](https://www.curseforge.com/minecraft/mc-mods/terrafirmacraft) and its hands-on approach to believable survival progression.

Available for **Minecraft 1.21.1 on NeoForge**.
