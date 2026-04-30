package me.remag501.bunker.managers;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.loaders.file.FileLoader;
import me.remag501.core.api.task.TaskService;
import me.remag501.bunker.BunkerPlugin;
import me.remag501.bunker.core.BunkerInstance;
import me.remag501.bunker.service.*;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class BunkerCreationManager {

    private final TaskService taskService;
    private final Logger logger;
    private final ConfigManager bunkerConfig;
    private final BunkerConfigManager bunkerConfigManager;
    private final GeneratorService generatorService;
    private final HologramService hologramService;
    private final NpcService npcService;
    private final SchematicService schematicService;
    private final WorldGuardService worldGuardService;
    private final AdvancedSlimePaperAPI api;

    private final Set<UUID> runningTasks = new HashSet<>();

    public BunkerCreationManager(TaskService taskService, Logger logger, ConfigManager bunkerConfig, BunkerConfigManager bunkerConfigManager, GeneratorService generatorService,
                                 HologramService hologramService, NpcService npcService, SchematicService schematicService, WorldGuardService worldGuardService) {
        this.taskService = taskService;
        this.logger = logger;
        this.bunkerConfig = bunkerConfig;
        this.bunkerConfigManager = bunkerConfigManager;
        this.generatorService = generatorService;
        this.hologramService = hologramService;
        this.npcService = npcService;
        this.schematicService = schematicService;
        this.worldGuardService = worldGuardService;
        this.api = AdvancedSlimePaperAPI.instance();
    }

    // ---------------- Bunker Assignment & Config Access ----------------

    public boolean hasBunker(UUID playerId) {
        return bunkerConfig.getConfig().contains(getPlayerBasePath(playerId) + ".world");
    }

    public void reloadBunkerConfig() {
        bunkerConfig.reload(); // reloads from disk
    }

    public int getAssignedBunkers() {
        return bunkerConfig.getConfig().getInt("assignedBunkers");
    }

    public int getTotalBunkers() {
        return bunkerConfig.getConfig().getInt("totalBunkers");
    }

    public void setTotalBunkers(int total) {
        bunkerConfig.getConfig().set("totalBunkers", total);
        bunkerConfig.save();
    }

    public BunkerConfigManager getConfigManger() {
        return bunkerConfigManager;
    }

    public boolean upgradeBunker(Player player, String bunkerLevel) {
        // Update bunker config to show upgrades
        UUID playerId = player.getUniqueId();
        String upgradesPath = getPlayerBasePath(playerId) + ".upgrades";
        List<String> upgrades = bunkerConfig.getConfig().getStringList(upgradesPath);
        if (!upgrades.contains(bunkerLevel)) {
            upgrades.add(bunkerLevel);
            bunkerConfig.getConfig().set(upgradesPath, upgrades);
            bunkerConfig.save();
        } else {
            return false;
        }

        // Get world and upgrade bunker
        String worldName = getWorldName(playerId);
        if (worldName == null || worldName.isBlank()) {
            return false;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }

        return upgradeBunkerWorld(world, bunkerLevel, player);
    }

    public boolean assignBunker(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if own bunker or if enough exists
        if (hasBunker(playerId)) {
            return false;
        }

        int assigned = getAssignedBunkers();
        int total = getTotalBunkers();
        if (assigned >= total) {
            return false;
        }

        String worldName = "bunker_" + assigned;

        // Update the config
        bunkerConfig.getConfig().set("assignedBunkers", assigned + 1);
        bunkerConfig.getConfig().set(getPlayerBasePath(playerId) + ".world", worldName + ".slime");
        bunkerConfig.save();

        // Add generators to bunker (needs to belong to player)
        BunkerInstance bunkerInstance = bunkerConfigManager.getBunkerInstance("main");
        World world = Bukkit.getWorld(worldName);
        generatorService.createGenerator(player, world, bunkerInstance);

        return true;
    }

    public String getWorldName(UUID playerId) {
        String storedWorldName = bunkerConfig.getConfig().getString(getPlayerBasePath(playerId) + ".world");
        return normalizeStoredWorldName(storedWorldName);
    }

    public List<String> getKnownBunkerWorldNames() {
        List<String> worldNames = new java.util.ArrayList<>();

        for (String key : bunkerConfig.getConfig().getKeys(false)) {
            if (key.equalsIgnoreCase("totalBunkers") || key.equalsIgnoreCase("assignedBunkers")) {
                continue;
            }

            String worldName = bunkerConfig.getConfig().getString(key + ".world");
            if (worldName == null || worldName.isBlank()) {
                continue;
            }

            String normalized = normalizeStoredWorldName(worldName);
            if (normalized != null && !normalized.isBlank() && !worldNames.contains(normalized)) {
                worldNames.add(normalized);
            }
        }

        return worldNames;
    }

    public UUID getOwnerByWorldName(String worldName) {
        String normalizedTarget = normalizeStoredWorldName(worldName);
        if (normalizedTarget == null || normalizedTarget.isBlank()) {
            return null;
        }

        for (String key : bunkerConfig.getConfig().getKeys(false)) {
            if (key.equalsIgnoreCase("totalBunkers") || key.equalsIgnoreCase("assignedBunkers")) {
                continue;
            }

            String storedWorld = bunkerConfig.getConfig().getString(key + ".world");
            String normalizedStoredWorld = normalizeStoredWorldName(storedWorld);
            if (normalizedStoredWorld == null) {
                continue;
            }

            if (normalizedStoredWorld.equalsIgnoreCase(normalizedTarget)) {
                try {
                    return UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed keys; runtime config should be UUID-only now.
                }
            }
        }

        return null;
    }

    public void bootstrapRuntimeSystems(World world) {
        UUID ownerId = getOwnerByWorldName(world.getName());
        if (ownerId == null) {
            logger.warning("Could not resolve bunker owner for runtime bootstrap in world: " + world.getName());
            return;
        }

        bootstrapRuntimeSystems(world, ownerId);
    }

    public void bootstrapRuntimeSystems(World world, UUID ownerId) {
        applyWorldSettings(world);

        Set<String> levels = getAppliedLevels(ownerId);
        if (levels.isEmpty()) {
            levels.add("main");
        }

        for (String level : levels) {
            BunkerInstance instance = bunkerConfigManager.getBunkerInstance(level);
            if (instance == null) {
                logger.warning("Skipping runtime bootstrap level '" + level + "' for " + world.getName() + " (missing in config). ");
                continue;
            }

            worldGuardService.setupBunkerFlags(world);
            npcService.addNpc(world.getName(), instance);
            hologramService.addHologram(instance, world);
                // generatorService.rehydrateGenerators(world, ownerId, instance);
                // NOTE: generator rehydration is disabled until generator issues are diagnosed
        }
    }


    /**
     * Tear down session-based systems (NPCs, Holograms) when a bunker world is unloading.
     */
    public void teardownRuntimeSystems(World world) {
        UUID ownerId = getOwnerByWorldName(world.getName());
        if (ownerId == null) {
            logger.warning("Could not resolve bunker owner for teardown in world: " + world.getName());
            return;
        }

        Set<String> levels = getAppliedLevels(ownerId);
        if (levels.isEmpty()) {
            levels.add("main");
        }

        for (String level : levels) {

            BunkerInstance instance = bunkerConfigManager.getBunkerInstance(level);
            if (instance == null) continue;

            logger.info("reaching teardown for level " + level + " in world " + world.getName());

            // Remove NPC clones and hologram clones created during the session
            npcService.removeSessionNpcs(world.getName());
            hologramService.removeSessionHolograms(instance, world.getName());
        }

        logger.info("Teardown of runtime systems completed for " + world.getName());
    }
    private Set<String> getAppliedLevels(UUID ownerId) {
        Set<String> levels = new LinkedHashSet<>();
        levels.add("main");

        String upgradesPath = getPlayerBasePath(ownerId) + ".upgrades";
        List<String> upgrades = bunkerConfig.getConfig().getStringList(upgradesPath);
        levels.addAll(upgrades);
        return levels;
    }

    private String getPlayerBasePath(UUID playerId) {
        return playerId.toString();
    }

    private String normalizeStoredWorldName(String storedWorldName) {
        if (storedWorldName == null || storedWorldName.isBlank()) {
            return null;
        }
        return storedWorldName.endsWith(".slime")
                ? storedWorldName.substring(0, storedWorldName.length() - 6)
                : storedWorldName;
    }

    // ---------------- Bunker World Creation ----------------

    public boolean addBunkers(int count, CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (runningTasks.contains(playerId)) {
            player.sendMessage(ChatColor.RED + "A bunker creation task is already running.");
            return true;
        }

        runningTasks.add(playerId);
        int oldTotal = getTotalBunkers();
        setTotalBunkers(oldTotal + count);

        // Using your TaskService
        // UUID: playerId (so it's owned by them)
        // Category: "bunker-batch" (keeps it specific)
        // Delay: 0, Interval: 60 (3 seconds)
        taskService.subscribe(playerId, "bunker-batch", 0, 60, (iteration) -> {
            // 'iteration' is the 'elapsed' count from your TaskManager

            // 1. Check if we are finished
            if (iteration >= count) {
                sender.sendMessage(ChatColor.GREEN + "Successfully created " + count + " bunkers.");
                bunkerConfig.reload();
                runningTasks.remove(playerId);
                return true; // Terminate the task
            }

            // 2. Determine the specific world number
            String worldName = "bunker_" + (oldTotal + iteration);

            // 3. Trigger creation
            player.sendMessage(ChatColor.GRAY + "Generating " + worldName + "... (" + (iteration + 1) + "/" + count + ")");
            createBunkerWorld(worldName);

            return false; // Continue to next interval
        });

        return true;
    }

    public void createBunkerWorld(String worldName) {
        logger.info("Initializing creation for: " + worldName);
        BunkerInstance bunkerInstance = bunkerConfigManager.getBunkerInstance("main");
        String templateWorldName = bunkerConfigManager.getTemplateWorldName();
        logger.info("Using template world: " + templateWorldName);

        if (templateWorldName == null || templateWorldName.isBlank()) {
            logger.severe("Config key 'templateWorldName' is missing or empty. Cannot clone bunker world: " + worldName);
            return;
        }

        if (!tryCreateAspWorld(worldName, templateWorldName)) {
            logger.severe("ASP bunker world creation failed for '" + worldName + "'. No legacy backend fallback remains.");
            return;
        }

        // 2. Wait for the world to be loaded using TaskService
        // Owner: null (System task), Category: unique per world
        // Delay: 5 ticks, Interval: 10 ticks (0.5s)
        taskService.subscribe(BunkerPlugin.SYSTEM_ID, "world-init-" + worldName, 5, 10, (iterations) -> {
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                setupWorldContent(world, bunkerInstance);
//                Bukkit.unloadWorld(world, true); // Unload using bukkit because ASP listens to events and will handle cleanup
                return true; // Stop the task
            }

            // Optional: Timeout after 30 seconds (60 iterations at 10 ticks each)
            if (iterations >= 60) {
                logger.severe("Timed out waiting for world: " + worldName);
                return true;
            }

            logger.warning("World " + worldName + " not yet visible. Retrying...");
            return false; // Keep checking
        });
    }

    private boolean tryCreateAspWorld(String worldName, String templateWorldName) {
        try {
            var templateWorld = api.getLoadedWorld(templateWorldName);

            if (templateWorld == null) {
                logger.warning("ASP template world '" + templateWorldName + "' is not loaded.");
                return false;
            }

            var loader = new FileLoader(new File(getDataFolder(), "slime_worlds"));
            var clonedWorld = templateWorld.clone(worldName, loader);

            var instance = api.loadWorld(clonedWorld, true);
            if (instance == null) {
                logger.warning("ASP failed to load cloned world: " + worldName);
                return false;
            }

            taskService.delay(1, () -> {
                // IO operations must be done asynchronously to avoid blocking the main thread
                try {
                    api.saveWorld(clonedWorld);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return true;
        } catch (Throwable t) {
            logger.warning("ASP bunker world creation failed for '" + worldName + "': " + t.getMessage());
            return false;
        }
    }

    private File getDataFolder() {
//        return new File("bunker_worlds");
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BGSBunker");
        return plugin.getDataFolder();
    }

    public void setupWorldContent(World world, BunkerInstance bunkerInstance) {
        String worldName = world.getName();

        Location spawn = world.getSpawnLocation();
        int chunkX = spawn.getBlockX() >> 4;
        int chunkZ = spawn.getBlockZ() >> 4;

        // Force the chunk to load and STAY loaded during setup
        world.setChunkForceLoaded(chunkX, chunkZ, true);

        // Use the standard getChunkAtAsync with a small delay
        world.getChunkAtAsync(spawn).thenAccept(chunk -> taskService.delay(1, () -> {
            // 1. Apply settings first
            applyWorldSettings(world);

            schematicService.addSchematic(bunkerInstance, worldName);

            // 2. WorldGuard Phase
            worldGuardService.setupBunkerFlags(world);

            // 3. Citizens/Hologram Phase (no point now since these are added on world load)
//            npcService.addNPC(worldName, bunkerInstance);
//            hologramService.addHologram(bunkerInstance, world);

            // 4. Cleanup: Unforce the chunk so we don't leak memory with 100 worlds
            world.setChunkForceLoaded(chunkX, chunkZ, false);

            logger.info("Successfully initialized all systems for " + worldName);
        }));
    }

    private void applyWorldSettings(World world) {
        Location newSpawn = bunkerConfigManager.getSpawnLocation();
        world.setSpawnLocation(newSpawn);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    }

    public boolean upgradeBunkerWorld(World world, String bunkerLevel, Player player) {
        // Get the BunkerInstance for the world
        BunkerInstance bunkerInstance = bunkerConfigManager.getBunkerInstance(bunkerLevel);
        if (bunkerInstance == null) {
            logger.warning("No bunker instance found for world: " + world.getName());
            return false;
        }

        // Add schematic
        schematicService.addSchematic(bunkerInstance, world.getName());

        // Add NPC
        npcService.addNpc(world.getName(), bunkerInstance);

        // Add generator
        generatorService.createGenerator(player, world, bunkerInstance);

        // Add hologram to world
        hologramService.addHologram(bunkerInstance, world);

        // Remove holograms from world
        hologramService.removeRemovalHolograms(bunkerInstance, world.getName());

        logger.info("Bunker in world " + world.getName() + " upgraded to level " + bunkerLevel + ".");
        return true;
    }
}
