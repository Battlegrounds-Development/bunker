package me.remag501.bunker.managers;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import me.remag501.core.api.task.TaskService;
import me.remag501.bunker.BunkerPlugin;
import me.remag501.bunker.core.BunkerInstance;
import me.remag501.bunker.service.*;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;

import java.util.HashSet;
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
    private final NPCService npcService;
    private final SchematicService schematicService;
    private final WorldGuardService worldGuardService;

    private final Set<UUID> runningTasks = new HashSet<>();

    public BunkerCreationManager(TaskService taskService, Logger logger, ConfigManager bunkerConfig, BunkerConfigManager bunkerConfigManager, GeneratorService generatorService,
                                 HologramService hologramService, NPCService npcService, SchematicService schematicService, WorldGuardService worldGuardService) {
        this.taskService = taskService;
        this.logger = logger;
        this.bunkerConfig = bunkerConfig;
        this.bunkerConfigManager = bunkerConfigManager;
        this.generatorService = generatorService;
        this.hologramService = hologramService;
        this.npcService = npcService;
        this.schematicService = schematicService;
        this.worldGuardService = worldGuardService;
    }

    // ---------------- Bunker Assignment & Config Access ----------------

    public boolean hasBunker(String playerName) {
        return bunkerConfig.getConfig().contains(playerName.toUpperCase() + ".id");
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
        String playerName = player.getName();
        List<String> upgrades = bunkerConfig.getConfig().getStringList(playerName.toUpperCase() + ".upgrades");
        Bukkit.getLogger().info(upgrades.toString());
        if (!upgrades.contains(bunkerLevel)) {
            upgrades.add(bunkerLevel);
            bunkerConfig.getConfig().set(playerName.toUpperCase() + ".upgrades", upgrades);
            bunkerConfig.save();
        } else
            return false;
        // Get world and upgrade bunker
        String worldName = getWorldName(playerName);
        World world = Bukkit.getWorld(worldName);
        return upgradeBunkerWorld(world, bunkerLevel, player);
    }

    public boolean assignBunker(String playerName) {
        // Check if own bunker or if enough exists
        if (hasBunker(playerName)) return false;
        // Enough should exist
        int assigned = getAssignedBunkers();
        int total = getTotalBunkers();
//        Bukkit.getPlayer(playerName).sendMessage("reached " + assigned + " " + total);
        if (assigned >= total) return false;

        // Update the config
        bunkerConfig.getConfig().set("assignedBunkers", assigned + 1);
        bunkerConfig.getConfig().set(playerName.toUpperCase() + ".id", assigned);
        bunkerConfig.save();

        // Add generators to bunker (needs to belong to player)
        BunkerInstance bunkerInstance = bunkerConfigManager.getBunkerInstance("main");
        World world = Bukkit.getWorld(getWorldName(playerName));
        generatorService.createGenerator(Bukkit.getPlayer(playerName), world, bunkerInstance);

        return true;
    }

    public String getWorldName(String playerName) {
        return "bunker_" + bunkerConfig.getConfig().getString(playerName.toUpperCase()+".id");
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

        boolean createdWithAsp = tryCreateAspWorld(worldName, templateWorldName);
        if (!createdWithAsp && !createLegacyWorld(worldName)) {
            return;
        }
        final boolean applyBaseSchematics = !createdWithAsp;

        if (!createdWithAsp) {
            logger.info("Falling back to legacy Multiverse/VoidGen creation for " + worldName);
        }

        // 2. Wait for the world to be loaded using TaskService
        // Owner: null (System task), Category: unique per world
        // Delay: 5 ticks, Interval: 10 ticks (0.5s)
        taskService.subscribe(BunkerPlugin.SYSTEM_ID, "world-init-" + worldName, 5, 10, (iterations) -> {
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                setupWorldContent(world, bunkerInstance, applyBaseSchematics);
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
            AdvancedSlimePaperAPI api = AdvancedSlimePaperAPI.instance();
            var templateWorld = api.getLoadedWorld(templateWorldName);
            if (templateWorld == null) {
                logger.warning("ASP template world '" + templateWorldName + "' is not loaded.");
                return false;
            }

            var clonedWorld = templateWorld.getSerializableCopy().clone(worldName);
            api.loadWorld(clonedWorld, false);
            return true;
        } catch (Throwable t) {
            logger.warning("ASP bunker world creation failed for '" + worldName + "': " + t.getMessage());
            return false;
        }
    }

    private boolean createLegacyWorld(String worldName) {
        MultiverseCoreApi mvApi = MultiverseCoreApi.get();
        WorldManager worldManager = mvApi.getWorldManager();

        if (worldManager.getWorld(worldName).isEmpty()) {
            CreateWorldOptions options = CreateWorldOptions.worldName(worldName)
                    .environment(World.Environment.NORMAL)
                    .worldType(WorldType.FLAT)
                    .generator("VoidGen")
                    .generateStructures(false);

            var result = worldManager.createWorld(options);
            if (result.isFailure()) {
                logger.severe("Multiverse failed to create " + worldName + ": " + result.getFailureReason());
                return false;
            }
        }
        return true;
    }

    public void setupWorldContent(World world, BunkerInstance bunkerInstance, boolean applyBaseSchematics) {
        String worldName = world.getName();
        MultiverseCoreApi mvApi = MultiverseCoreApi.get();

        WorldManager worldManager = mvApi.getWorldManager();
        MultiverseWorld mvWorld = worldManager.getWorld(world).getOrNull();

        // We target the spawn chunk. When this completes, the world is ticked and WorldGuard
        // will have recognized the new world instance.

        Location spawn = world.getSpawnLocation();
        int chunkX = spawn.getBlockX() >> 4;
        int chunkZ = spawn.getBlockZ() >> 4;

        // Force the chunk to load and STAY loaded during setup
        world.setChunkForceLoaded(chunkX, chunkZ, true);

        // Use the standard getChunkAtAsync with a small delay
        world.getChunkAtAsync(spawn).thenAccept(chunk -> {
            taskService.delay(1, () -> { // Give it 1 tick to stabilize

                // 1. Apply settings first
                applyWorldSettings(world, mvWorld);

                if (applyBaseSchematics) {
                    schematicService.addSchematic(bunkerInstance, worldName);
                }

                // 2. WorldGuard Phase
                worldGuardService.setupBunkerFlags(world);

                // 3. Citizens/Hologram Phase
                npcService.addNPC(worldName, bunkerInstance);
                hologramService.addHologram(bunkerInstance, world);

                // 4. Cleanup: Unforce the chunk so we don't leak memory with 100 worlds
                world.setChunkForceLoaded(chunkX, chunkZ, false);

                logger.info("Successfully initialized all systems for " + worldName);
            });
        });
    }

    private void applyWorldSettings(World world, MultiverseWorld mvWorld) {
        Location newSpawn = bunkerConfigManager.getSpawnLocation();
        if (mvWorld != null) {
            mvWorld.setAdjustSpawn(false);
            mvWorld.setSpawnLocation(newSpawn);
            mvWorld.setDifficulty(Difficulty.PEACEFUL);
        }
        world.setSpawnLocation(newSpawn);
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
        npcService.addNPC(world.getName(), bunkerInstance);

        // Add generator
        generatorService.createGenerator(player, world, bunkerInstance);

        // Add hologram to world
        hologramService.addHologram(bunkerInstance, world);

        // Remove holograms from world
        hologramService.removeHolograms(bunkerInstance, world.getName());

        logger.info("Bunker in world " + world.getName() + " upgraded to level " + bunkerLevel + ".");
        return true;
    }
}
