package me.remag501.bunker.service;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.remag501.bunker.core.BunkerInstance;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.logging.Logger;

public class HologramService {

    private final Logger logger;

    public HologramService(Logger logger) {
        this.logger = logger;
    }

    /**
     * Bootstrap holograms for a world session. Clones templates into world-specific names.
     */
    public void addHologram(BunkerInstance bunkerInstance, World world) {
        List<BunkerInstance.HologramInfo> holograms = bunkerInstance.getHolograms();
        if (holograms == null || holograms.isEmpty()) return;

        for (BunkerInstance.HologramInfo info : holograms) {
            String templateName = info.name;
            String type = info.type;
            Location targetLocation = info.location;
            targetLocation.setWorld(world); // assign world

            Hologram template = DHAPI.getHologram(type);
            if (template == null) {
                logger.warning("Template hologram '" + type + "' not found. Skipping.");
                continue;
            }

            String cloneName = world.getName() + "_" + templateName;
            Hologram existingClone = DHAPI.getHologram(cloneName);
            if (existingClone != null) {
                existingClone.delete();
            }

            Hologram clone = template.clone(cloneName, targetLocation, true);
            clone.enable();

            logger.info("Cloned hologram '" + templateName + "' as '" + cloneName + "' at " + targetLocation);
        }
    }

    /**
     * Remove all hologram clones created for this world's session.
     */
    public void removeSessionHolograms(BunkerInstance bunkerInstance, String worldName) {
        List<BunkerInstance.HologramInfo> holograms = bunkerInstance.getHolograms();
        if (holograms == null || holograms.isEmpty()) return;

        for (BunkerInstance.HologramInfo info : holograms) {
            String cloneName = worldName + "_" + info.name;
            try {
                removeHologram(cloneName);
            } catch (Exception ex) {
                logger.warning("Failed to remove hologram " + cloneName + ": " + ex.getMessage());
            }
        }
    }


    /**
     * Removes holograms that are marked for removal in the bunker instance (different from removing bootstrapped holograms on world unload)
     */
    public void removeRemovalHolograms(BunkerInstance bunkerInstance, String worldName) {
        List<String> holograms = bunkerInstance.getRemoveHolograms();
        for (String hologramName: holograms) {
            removeHologram(worldName + "_" + hologramName);
        }
    }

    private void removeHologram(String hologramName) {
        Hologram hologram = DHAPI.getHologram(hologramName);
        if (hologram == null) {
            logger.warning("Hologram " + hologramName + " not found, cannot delete.");
            return;
        }
        hologram.delete();
    }

}

