package com.nstut.firstworks.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface FirstworksKubeEvents {
    EventGroup GROUP = EventGroup.of("FirstworksEvents");

    EventHandler BARREL_PROCESS_STARTING = GROUP.server("barrelProcessStarting", () -> BarrelProcessKubeEvent.class)
            .hasResult();
    EventHandler BARREL_PROCESS_COMPLETED = GROUP.server("barrelProcessCompleted", () -> BarrelProcessKubeEvent.class);
    EventHandler LOOM_WEAVING_STARTING = GROUP.server("loomWeavingStarting", () -> LoomWeavingKubeEvent.class)
            .hasResult();
    EventHandler LOOM_WEAVING_COMPLETED = GROUP.server("loomWeavingCompleted", () -> LoomWeavingKubeEvent.class);
    EventHandler SPINDLE_SPINNING_STARTING = GROUP.server("spindleSpinningStarting", () -> SpindleSpinningKubeEvent.class)
            .hasResult();
    EventHandler SPINDLE_SPINNING_COMPLETED = GROUP.server("spindleSpinningCompleted", () -> SpindleSpinningKubeEvent.class);
    EventHandler BRICK_MOLDING_STARTING = GROUP.server("brickMoldingStarting", () -> BrickMoldingKubeEvent.class)
            .hasResult();
    EventHandler BRICK_MOLDING_COMPLETED = GROUP.server("brickMoldingCompleted", () -> BrickMoldingKubeEvent.class);
}
