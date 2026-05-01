package me.remag501.bunker.managers;

import me.remag501.bunker.BunkerPlugin;
import me.remag501.bunker.core.BunkerInstance;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BunkerConfigManager {

    private final BunkerPlugin plugin;

    private Map<String, String> messages = new HashMap<>();
    private Map<String, Double> doubles = new HashMap<>();
    private Map<String, BunkerInstance> bunkerInstances = new HashMap<>();
    private Location spawnLocation = null;
    private String templateWorldName = "";

    public BunkerConfigManager(BunkerPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        bunkerInstances.clear();

        // Load messages
        messages.put("argCommandUsage", config.getString("argCommandUsage"));
        messages.put("visitCommandUsage", config.getString("visitCommandUsage"));
        messages.put("noBunker", config.getString("noBunker"));
        messages.put("outOfBunkers", config.getString("outOfBunkers"));
        messages.put("alreadyPurchased", config.getString("alreadyPurchased"));
        messages.put("bunkerPurchased", config.getString("bunkerPurchased"));
        messages.put("playerNotExist", config.getString("playerNotExist"));
        messages.put("homeMsg", config.getString("homeMsg"));
        messages.put("pendingVisitRequest", config.getString("pendingVisitRequest"));
        messages.put("requestMsg", config.getString("requestMsg"));
        messages.put("acceptRequest", config.getString("acceptRequest"));
        messages.put("declineRequest", config.getString("declineRequest"));
        messages.put("allowVisit", config.getString("allowVisit"));
        messages.put("declineVisit", config.getString("declineVisit"));

        // Global world template used for bunker world cloning
        templateWorldName = config.getString("templateWorldName");

        // Load doubles
        doubles.put("x", config.getDouble("x"));
        doubles.put("y", config.getDouble("y"));
        doubles.put("z", config.getDouble("z"));
        doubles.put("spawnX", config.getDouble("spawnX"));
        doubles.put("spawnY", config.getDouble("spawnY"));
        doubles.put("spawnZ", config.getDouble("spawnZ"));
        doubles.put("yaw", config.getDouble("yaw"));
        doubles.put("pitch", config.getDouble("pitch"));
        doubles.put("npcId", config.getDouble("npcId"));
        doubles.put("npcX", config.getDouble("npcCoords.x"));
        doubles.put("npcY", config.getDouble("npcCoords.y"));
        doubles.put("npcZ", config.getDouble("npcCoords.z"));
        doubles.put("npcYaw", config.getDouble("npcCoords.yaw"));
        doubles.put("npcPitch", config.getDouble("npcCoords.pitch"));


        // Load spawn location
        ConfigurationSection spawnSection = config.getConfigurationSection("spawn");
        if (spawnSection != null) {
            double x = spawnSection.getDouble("x");
            double y = spawnSection.getDouble("y");
            double z = spawnSection.getDouble("z");
            float yaw = (float) spawnSection.getDouble("yaw");
            float pitch = (float) spawnSection.getDouble("pitch");
            spawnLocation = new Location(null, x, y, z, yaw, pitch); // World must be set at runtime
            // Save to configManager if needed
        }

        // Load all bunker levels dynamically
        for (String bunkerKey : config.getKeys(false)) {
            if (bunkerKey.equals("spawn")) continue; // skip the global spawn

            ConfigurationSection section = config.getConfigurationSection(bunkerKey);
            if (section == null) continue;

            // Load schematics
            List<Map<?, ?>> schematicList = section.getMapList("schematics");

            List<BunkerInstance.SchematicWrapper> schematics = new ArrayList<>();
            for (Map<?, ?> map : schematicList) {
                String name = (String) map.get("name");
                Map<?, ?> coords = (Map<?, ?>) map.get("coords");

                if (name == null || coords == null) {
                    plugin.getLogger().warning("Skipping schematic entry due to missing name or coords in section: " + section.getName());
                    continue;
                }

                File file = new File(plugin.getDataFolder(), "schematics/" + name);
                if (!file.exists()) {
                    plugin.getLogger().warning("Schematic file not found: " + name);
                    continue;
                }

                Location loc = parseLocationMap(coords);
                schematics.add(new BunkerInstance.SchematicWrapper(file, loc));
            }

            // Load NPCs
            List<Map<?, ?>> npcList = section.getMapList("npcs");
            List<BunkerInstance.NPCInfo> npcs = npcList.stream().map(map -> {
                int id = (int) map.get("id");
                Location loc = parseLocationMap((Map<?, ?>) map.get("coords"));
                return new BunkerInstance.NPCInfo(id, loc);
            }).collect(Collectors.toList());

            // Load Generators
            List<Map<?, ?>> generatorList = section.getMapList("generators");
            List<BunkerInstance.GeneratorInfo> generators = generatorList.stream().map(map -> {
                String type = (String) map.get("type");
                Location loc = parseLocationMap((Map<?, ?>) map.get("coords"));
                return new BunkerInstance.GeneratorInfo(type, loc);
            }).collect(Collectors.toList());

            // Load Holograms
            List<Map<?, ?>> hologramList = section.getMapList("holograms");
            List<BunkerInstance.HologramInfo> holograms = hologramList.stream().map(map -> {
                String name = (String) map.get("name");
                String type = (String) map.get("type");
                Location loc = parseLocationMap((Map<?, ?>) map.get("coords"));
                return new BunkerInstance.HologramInfo(name, type, loc);
            }).collect(Collectors.toList());

            // Load Remove-Holograms
            List<String> removeHolograms = section.getStringList("remove-holograms");

            // Save to instance map
            bunkerInstances.put(bunkerKey, new BunkerInstance(bunkerKey, schematics, npcs, generators, holograms, removeHolograms));
        }

    }

    private Location parseLocationMap(Map<?, ?> map) {
        double x = ((Number) map.get("x")).doubleValue();
        double y = ((Number) map.get("y")).doubleValue();
        double z = ((Number) map.get("z")).doubleValue();
        float yaw = map.containsKey("yaw") ? ((Number) map.get("yaw")).floatValue() : 0f;
        float pitch = map.containsKey("pitch") ? ((Number) map.get("pitch")).floatValue() : 0f;
        return new Location(null, x, y, z, yaw, pitch); // you'll assign the world later
    }


    private Location getLocationFromSection(ConfigurationSection sec, org.bukkit.World world) {
        if (sec == null) return null;
        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw", 0f);
        float pitch = (float) sec.getDouble("pitch", 0f);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    public double getDouble(String key) {
        return doubles.getOrDefault(key, 0.0);
    }

    public BunkerInstance getBunkerInstance(String name) {
        return bunkerInstances.get(name);
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public String getTemplateWorldName() {
        return templateWorldName;
    }
}

