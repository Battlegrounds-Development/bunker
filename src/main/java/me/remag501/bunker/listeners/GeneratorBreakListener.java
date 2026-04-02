package me.remag501.bunker.listeners;

import com.muhammaddaffa.nextgens.api.events.generators.GeneratorBreakEvent;
import me.remag501.core.api.event.EventService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GeneratorBreakListener {

    public GeneratorBreakListener(EventService eventService) {
        // Register this in your Bunker plugin's initialization logic
        eventService.subscribe(GeneratorBreakEvent.class)
                // Filter: Only cancel if they LACK the permission
                .filter(event -> !event.getPlayer().hasPermission("bunker.breakgen"))
                .handler(event -> {
                    // Since the filter passed, we know they don't have permission
                    event.setCancelled(true);

                    // Optional: If you want to notify them, but keep it quiet by default as in your snippet
                    // event.getPlayer().sendMessage("§c§l(!) §cYou can't break your own generator!");
                });
    }

}
