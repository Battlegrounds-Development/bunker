package me.remag501.bunker.managers;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import me.remag501.bunker.core.BunkerInstance;
import me.remag501.bunker.service.GeneratorService;
import me.remag501.bunker.service.HologramService;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class AdminManager {

    private final Plugin plugin;
    private final BunkerCreationManager bunkerCreationManager;
    private final HologramService hologramService;
    private final GeneratorService generatorService;

    public AdminManager(Plugin plugin, BunkerCreationManager bunkerCreationManager, HologramService hologramService, GeneratorService generatorService) {
        this.plugin = plugin;
        this.bunkerCreationManager = bunkerCreationManager;
        this.hologramService = hologramService;
        this.generatorService = generatorService;
    }

    public void previewBunker(Player player) {
        // Delete the existing preview world if it exists
        World previewWorld = Bukkit.getWorld("bunker_preview");
        BunkerInstance bunkerInstance = bunkerCreationManager.getConfigManger().getBunkerInstance("main");

        if (previewWorld != null) {

            // Prepare to delete any npcs in the world
            NPCRegistry registry = CitizensAPI.getNPCRegistry();
            List<NPC> toRemove = new ArrayList<>();

            // First collect NPCs to delete
            for (NPC npc : registry) {
                if (npc.isSpawned() && npc.getEntity().getWorld().equals(previewWorld)) {
                    toRemove.add(npc);
                }
            }
            // Then despawn and destroy them
            for (NPC npc : toRemove) {
                npc.despawn();
                npc.destroy();
            }

            // Delete all holograms in a world
            for (BunkerInstance.HologramInfo hologramInfo : bunkerInstance.getHolograms()) {
                hologramService.removeHologram("bunker_preview_" + hologramInfo.name);
            }

            // No generator deletion?

            deletePreviewWorld(previewWorld, player);
        }

        // Create the new preview world asynchronously then teleport player when done
        new BukkitRunnable() {
            boolean complete = false;

            @Override
            public void run() {
                if (complete) {
                    World newWorld = Bukkit.getWorld("bunker_preview");
                    if (newWorld != null) {
                        // Add generators in player name
                        generatorService.createGenerator(player, newWorld, bunkerInstance);

                        // Teleport player
                        Location spawn = newWorld.getSpawnLocation();
                        player.teleport(spawn);
                        player.sendMessage(ChatColor.GREEN + "Teleported to preview bunker.");
                    } else {
                        player.sendMessage(ChatColor.RED + "Preview bunker world not found.");
                    }
                    cancel();
                    return;
                }

                // Create the new preview world
                bunkerCreationManager.createBunkerWorld("bunker_preview");
                complete = true;
            }
        }.runTaskTimer(plugin, 0L, 0L);
    }

    private void deletePreviewWorld(World previewWorld, Player player) {
        String worldName = previewWorld.getName();

        try {
            var slimeWorld = AdvancedSlimePaperAPI.instance().getLoadedWorld(worldName);
            if (slimeWorld != null) {
                Bukkit.unloadWorld(previewWorld, false);
                slimeWorld.getLoader().deleteWorld(worldName);
                player.sendMessage(ChatColor.GRAY + "Deleted old preview world...");
                return;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("ASP preview delete failed for " + worldName + ": " + t.getMessage());
        }


        player.sendMessage(ChatColor.GRAY + "Deleted old preview world...");
    }

}
