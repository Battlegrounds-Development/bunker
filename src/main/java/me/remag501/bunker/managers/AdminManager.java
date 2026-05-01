package me.remag501.bunker.managers;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import me.remag501.bunker.core.BunkerInstance;
import me.remag501.bunker.service.BunkerWorldLifecycleService;
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
    private final BunkerWorldLifecycleService worldLifecycleService;

    public AdminManager(Plugin plugin, BunkerCreationManager bunkerCreationManager, HologramService hologramService, GeneratorService generatorService, BunkerWorldLifecycleService worldLifecycleService) {
        this.plugin = plugin;
        this.bunkerCreationManager = bunkerCreationManager;
        this.hologramService = hologramService;
        this.generatorService = generatorService;
        this.worldLifecycleService = worldLifecycleService;
    }

    public void previewBunker(Player player) {
        // Delete the existing preview world if it exists
        World previewWorld = Bukkit.getWorld("bunker_preview");
        BunkerInstance bunkerInstance = bunkerCreationManager.getConfigManger().getBunkerInstance("main");

        if (previewWorld != null) {
            removeNpcAndHologramFromWorld(bunkerInstance, previewWorld);
            deletePreviewWorld(previewWorld, player);
        }

        // Create the new preview world asynchronously then teleport player when done
        bunkerCreationManager.createBunkerWorld("bunker_preview");

        // Bootstrap preview world and teleport player when ready
        worldLifecycleService.executeWhenWorldReady("bunker_preview",
                world -> bunkerCreationManager.bootstrapRuntimeSystems(world, null),
                world -> {
                    generatorService.createGenerator(player, world, bunkerInstance);
                    player.teleport(world.getSpawnLocation());
                    player.sendMessage(ChatColor.GREEN + "Teleported to preview bunker.");
                },
                () -> player.sendMessage(ChatColor.RED + "Preview bunker world not found."));

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

    private void removeNpcAndHologramFromWorld(BunkerInstance bunkerInstance, World previewWorld) {

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
        hologramService.removeSessionHolograms(bunkerInstance, "bunker_preview");

        // No generator deletion?

    }

}
