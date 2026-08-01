package com.nstut.firstworks.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface FirstworksKubeEvents {
    EventGroup GROUP = EventGroup.of("FirstworksEvents");

    EventHandler BARREL_PROCESS_STARTING = GROUP.server("barrelProcessStarting", () -> BarrelProcessKubeEvent.class)
            .hasResult();
    EventHandler BARREL_PROCESS_COMPLETED = GROUP.server("barrelProcessCompleted", () -> BarrelProcessKubeEvent.class);
}
