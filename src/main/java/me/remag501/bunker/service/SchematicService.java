package me.remag501.bunker.service;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.remag501.core.api.task.TaskService;
import me.remag501.bunker.BunkerPlugin;
import me.remag501.bunker.core.BunkerInstance;
import org.apache.commons.logging.Log;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class SchematicService {

    private final TaskService taskService;
    private final Logger logger;

    public SchematicService(TaskService taskService, Logger logger) {
        this.taskService = taskService;
        this.logger = logger;
    }

    public void addSchematic(BunkerInstance bunkerInstance, String worldName) {
        AtomicInteger attempts = new AtomicInteger();

        taskService.subscribe(BunkerPlugin.SYSTEM_ID, "schematic-" + worldName, 0, 20, false, (ticks) -> {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                // Move to main thread for WorldEdit operations
                taskService.delay(1, () -> {
                    for (BunkerInstance.SchematicWrapper wrapper : bunkerInstance.getSchematics()) {
                        paste(world, wrapper);
                    }
                    logger.info("Pasted all schematics for bunker world: " + worldName);
                });
                return true;
            } else if (attempts.incrementAndGet() >= 100) {
                logger.warning("World " + worldName + " failed to load. Paste aborted.");
                return true;
            }
            return false;
        });
    }

    private void paste(World world, BunkerInstance.SchematicWrapper wrapper) {
        Clipboard clipboard = loadSchematic(wrapper.file);
        if (clipboard == null) return;

        Location loc = wrapper.location.clone();
        loc.setWorld(world);

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            editSession.setFastMode(true);

            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()))
                    .build();

            Operations.complete(operation);
        } catch (WorldEditException e) {
            logger.severe("WorldEdit error while pasting: " + e.getMessage());
        }
    }

    // Helper
    private Clipboard loadSchematic(File file) {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) return null;

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}