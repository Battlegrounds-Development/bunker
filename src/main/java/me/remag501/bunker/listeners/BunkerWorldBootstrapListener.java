package me.remag501.bunker.listeners;

import me.remag501.bunker.managers.BunkerCreationManager;
import me.remag501.bunker.service.BunkerWorldLifecycleService;
import me.remag501.core.api.event.EventService;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class BunkerWorldBootstrapListener {

    public BunkerWorldBootstrapListener(EventService eventService, BunkerWorldLifecycleService lifecycleService, BunkerCreationManager creationManager) {
        // Bootstrap bunker worlds on load with deduplication
//        eventService.subscribe(WorldLoadEvent.class)
//                .filter(event -> event.getWorld().getName().startsWith("bunker_") && !event.getWorld().getName().equals("bunker_preview"))
//                .handler(event -> {
//                    var world = event.getWorld();
//                    lifecycleService.ensureBootstrapped(world,
//                            w -> creationManager.bootstrapRuntimeSystems(w),
//                            () -> {});
//                });

        // Teardown bunker worlds on unload
        eventService.subscribe(WorldUnloadEvent.class)
                .filter(event -> event.getWorld().getName().startsWith("bunker_"))
                .handler(event -> creationManager.teardownRuntimeSystems(event.getWorld()));
    }
}