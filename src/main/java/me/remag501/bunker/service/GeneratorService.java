package me.remag501.bunker.service;

import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import me.remag501.core.api.task.TaskService;
import me.remag501.bunker.core.BunkerInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.logging.Logger;

public class GeneratorService {

    private final TaskService taskService;
    private final Logger logger;

    public GeneratorService(TaskService taskService, Logger logger) {
        this.taskService = taskService;
        this.logger = logger;
    }

    public void createGenerator(Player player, World world, BunkerInstance bunkerInstance) {
        // 1. Get the NextGens API/Instance
        NextGens nextGensPlugin = NextGens.getInstance();
        if (nextGensPlugin == null) {
            logger.warning("NextGens instance is null!");
            return;
        }

        // 2. Get the GeneratorManager from NextGens
        com.muhammaddaffa.nextgens.generators.managers.GeneratorManager manager = nextGensPlugin.getGeneratorManager();

        List<BunkerInstance.GeneratorInfo> generators = bunkerInstance.getGenerators();
        if (generators == null || generators.isEmpty()) {
            return;
        }

        for (BunkerInstance.GeneratorInfo info : generators) {
            String type = info.type;
            Location loc = info.location.clone(); // Clone to prevent modifying the original reference
            loc.setWorld(world);

            // 3. Find the generator template
            Generator generator = manager.getGenerator(type);
            if (generator == null) {
                logger.warning("Generator type '" + type + "' not found in NextGens!");
                continue;
            }

            // 4. Use Native Paper API for Async Chunk Loading (Replaces PaperLib)
            // This fixes the NoClassDefFoundError
            world.getChunkAtAsync(loc).thenAccept(chunk -> {
                Block block = loc.getBlock();

                try {
                    // 5. Register the generator
                    // Using the player as the owner so it counts towards their limits/stats
                    ActiveGenerator activeGenerator = manager.registerGenerator(player, generator, block);
                    double mainTimer = activeGenerator.getTimer();
                    activeGenerator.setTimer(20); // Make it spawn instantly

                    taskService.delay(20, () -> {
                        activeGenerator.setTimer(mainTimer);
                    });

                    activeGenerator.setTimer(20);
                    activeGenerator.setCorrupted(false);


                    logger.info("Registered " + type + " at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                } catch (Exception e) {
                    logger.severe("Failed to register generator: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }


}

