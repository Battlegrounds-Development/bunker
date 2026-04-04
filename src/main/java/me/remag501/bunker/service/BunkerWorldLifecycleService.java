package me.remag501.bunker.service;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.loaders.file.FileLoader;
import me.remag501.bunker.BunkerPlugin;
import me.remag501.bunker.managers.BunkerConfigManager;
import me.remag501.core.api.task.TaskService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BunkerWorldLifecycleService {

    public static final String LOAD_WAIT_NAMESPACE = "world-load-wait";

    private final Plugin plugin;
    private final TaskService taskService;
    private final BunkerConfigManager bunkerConfigManager;
    private final AdvancedSlimePaperAPI api;
    private final FileLoader loader;
    private final Set<String> runtimeBootstrappedWorlds = ConcurrentHashMap.newKeySet();

    public BunkerWorldLifecycleService(Plugin plugin, TaskService taskService, BunkerConfigManager bunkerConfigManager) {
        this.plugin = plugin;
        this.taskService = taskService;
        this.bunkerConfigManager = bunkerConfigManager;
        this.api = AdvancedSlimePaperAPI.instance();
        this.loader = new FileLoader(new File(plugin.getDataFolder(), "slime_worlds"));
    }

    public void teleportToWorldSpawn(Player player, String worldName) {
        executeWhenWorldLoaded(worldName,
                world -> player.teleport(world.getSpawnLocation()),
                () -> player.sendMessage("Failed to load bunker world: " + worldName));
    }

    public void executeWhenWorldLoaded(String worldName, Consumer<World> onLoaded, Runnable onFailure) {
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            onLoaded.accept(existing);
            return;
        }

        if (!loadSlimeWorld(worldName)) {
            onFailure.run();
            return;
        }

        String namespace = LOAD_WAIT_NAMESPACE + "-" + worldName + "-" + UUID.randomUUID();
        taskService.subscribe(BunkerPlugin.SYSTEM_ID, namespace, 1, 5, (attempt) -> {
            World loadedWorld = Bukkit.getWorld(worldName);
            if (loadedWorld != null) {
                onLoaded.accept(loadedWorld);
                return true;
            }

            if (attempt >= 40) {
                onFailure.run();
                return true;
            }
            return false;
        });
    }

    public void executeWhenWorldReady(String worldName, Consumer<World> bootstrap, Consumer<World> onReady, Runnable onFailure) {
        executeWhenWorldLoaded(worldName, world -> {
            String key = world.getName().toLowerCase();

            if (!runtimeBootstrappedWorlds.contains(key)) {
                try {
                    bootstrap.accept(world);
                    runtimeBootstrappedWorlds.add(key);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed runtime bootstrap for world '" + world.getName() + "': " + ex.getMessage());
                    onFailure.run();
                    return;
                }
            }

            onReady.accept(world);
        }, onFailure);
    }

    public void scheduleUnloadCheck(String worldName) {
        if (!shouldManageWorld(worldName)) {
            return;
        }

        taskService.delay(100, () -> tryUnloadIfEmpty(worldName));
    }

    private void tryUnloadIfEmpty(String worldName) {
        if (!shouldManageWorld(worldName)) {
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null || !world.getPlayers().isEmpty()) {
            return;
        }

        boolean unloaded = Bukkit.unloadWorld(world, true);
        if (unloaded) {
            runtimeBootstrappedWorlds.remove(worldName.toLowerCase());
            plugin.getLogger().info("Unloaded idle slime world: " + worldName);
        }
    }

    private boolean shouldManageWorld(String worldName) {
        if (worldName == null || !worldName.startsWith("bunker_")) {
            return false;
        }

        String templateWorldName = bunkerConfigManager.getTemplateWorldName();
        return templateWorldName == null || !templateWorldName.equalsIgnoreCase(worldName);
    }

    private boolean loadSlimeWorld(String worldName) {
        if (api.getLoadedWorld(worldName) != null || Bukkit.getWorld(worldName) != null) {
            return true;
        }

        try {
            var slimeWorld = api.readWorld(loader, worldName, false, new SlimePropertyMap());
            api.loadWorld(slimeWorld, true);
            return true;
        } catch (UnknownWorldException e) {
            plugin.getLogger().warning("Slime world does not exist: " + worldName);
        } catch (CorruptedWorldException | NewerFormatException | IOException | IllegalArgumentException e) {
            plugin.getLogger().warning("Failed to load slime world '" + worldName + "': " + e.getMessage());
        }

        return false;
    }
}
