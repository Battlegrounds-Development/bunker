package me.remag501.bunker.service;

import me.remag501.core.api.task.TaskService;
import me.remag501.bunker.BunkerPlugin;
import me.remag501.bunker.core.BunkerInstance;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.List;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;

public class NPCService {

    private final TaskService taskService;
    private final Logger logger;
    // Track spawned clones per-world so we can teardown on unload
    private final Map<String, List<NPC>> spawnedNpcs = new ConcurrentHashMap<>();

    public NPCService(TaskService taskService, Logger logger) {
        this.taskService = taskService;
        this.logger = logger;
    }

    public void addNPC(String worldName, BunkerInstance bunkerInstance) {
        int[] attempts = {0};
        int maxAttempts = 100;

        taskService.subscribe(BunkerPlugin.SYSTEM_ID, "npc-spawn-" + worldName, 0, 20, false, (ticks) -> {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                // We are SYNC here because taskService.subscribe is sync
                List<BunkerInstance.NPCInfo> npcs = bunkerInstance.getNpcs();
                if (npcs == null || npcs.isEmpty()) return true;

                for (BunkerInstance.NPCInfo info : npcs) {
                    Location loc = info.location;
                    if (loc == null) continue;
                    loc.setWorld(world);

                    // 1. Request the chunk ASYNC first
                    world.getChunkAtAsync(loc).thenRun(() -> {
                        // 2. Jump back to SYNC to modify the world/NPCs
                        taskService.delay(1, () -> {

                            // 3. Handle the Citizens NPC (clone + track for teardown)
                            NPC original = CitizensAPI.getNPCRegistry().getById(info.id);
                            if (original != null) {
                                NPC clone = original.clone();
//                                clone.setName(worldName + "_" + original.getName());
                                clone.spawn(loc);
                                clone.teleport(loc, null);
                                spawnedNpcs.computeIfAbsent(worldName, k -> new CopyOnWriteArrayList<>()).add(clone);
                                logger.info("NPC and Barrier successfully spawned at " + worldName + " (id=" + clone.getId() + ")");
                            }
                        });
                    });
                }
                return true; // Stop the subscription timer
            }

            if (++attempts[0] >= maxAttempts) {
                logger.warning("NPC spawn timed out for " + worldName);
                return true;
            }
            return false; // Keep checking
        });
    }

    /**
     * Remove any NPC clones that were spawned for this world during runtime bootstrap.
     */
    public void removeNPCs(String worldName) {
        logger.info("Removing NPCs for world: " + worldName);
        List<NPC> list = spawnedNpcs.remove(worldName);
        if (list == null || list.isEmpty()) return;

        for (NPC npc : list) {
            try {
                if (npc.isSpawned()) npc.despawn();
            } catch (Exception ignored) {}
            try {
                CitizensAPI.getNPCRegistry().deregister(npc);
            } catch (Exception ignored) {}
        }
        logger.info("Tore down " + list.size() + " NPCs for world " + worldName);
    }



}

