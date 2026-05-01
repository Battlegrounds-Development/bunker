package me.remag501.bunker.listeners;

import me.remag501.bunker.BunkerPlugin;
import me.remag501.bunker.service.BunkerWorldLifecycleService;
import me.remag501.core.api.event.EventService;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BunkerWorldLifecycleListener {

    public static final String NAMESPACE = "bunker-world-lifecycle";

    public BunkerWorldLifecycleListener(EventService eventService, BunkerWorldLifecycleService lifecycleService) {
        eventService.subscribe(PlayerChangedWorldEvent.class)
//                .owner(BunkerPlugin.SYSTEM_ID)
//                .namespace(NAMESPACE)
                .handler(event -> lifecycleService.scheduleUnloadCheck(event.getFrom().getName()));

        eventService.subscribe(PlayerQuitEvent.class)
//                .owner(BunkerPlugin.SYSTEM_ID)
//                .namespace(NAMESPACE)
                .handler(event -> lifecycleService.scheduleUnloadCheck(event.getPlayer().getWorld().getName()));

        eventService.subscribe(PlayerKickEvent.class)
//                .owner(BunkerPlugin.SYSTEM_ID)
//                .namespace(NAMESPACE)
                .handler(event -> lifecycleService.scheduleUnloadCheck(event.getPlayer().getWorld().getName()));
    }
}

