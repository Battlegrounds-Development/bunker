package me.remag501.bunker.listeners;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.guis.VaultSelector;
import com.artillexstudios.axvaults.vaults.VaultManager;
import me.remag501.core.api.event.EventService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.EnumSet;
import java.util.Set;

public class OpenContainer {

    public OpenContainer(EventService eventService) {
        eventService.subscribe(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK)
                .filter(event -> event.getClickedBlock() != null)
                .filter(event -> event.getClickedBlock().getWorld().getName().startsWith("bunker_"))
                .handler(event -> {
                    Block clickedBlock = event.getClickedBlock();
                    Material type = clickedBlock.getType();

                    // 1. Define the sets (Ideally move these to static constants for performance)
                    Set<Material> vaultTriggerBlocks = EnumSet.of(
                            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.ENDER_CHEST
                    );
                    // (Add shulker boxes to the EnumSet above)

                    Set<Material> blockedMenus = EnumSet.of(
                            Material.SMITHING_TABLE, Material.BREWING_STAND, Material.ENCHANTING_TABLE,
                            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.GRINDSTONE, Material.FURNACE,
                            Material.HOPPER, Material.DROPPER
                    );

                    // 2. Logic for Vault Access
                    if (vaultTriggerBlocks.contains(type) || type.name().contains("SHULKER_BOX")) {
                        event.setCancelled(true);

                        // Fixed AxVaults 2.12.1+ implementation
                        VaultManager.getPlayer(event.getPlayer()).thenAccept(vaultPlayer -> {
                            new VaultSelector(event.getPlayer(), vaultPlayer).open();
                        });
                        return;
                    }

                    // 3. Logic for blocked interaction
                    if (blockedMenus.contains(type)) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage("§c§l(!) §cYou can't use that in a bunker world.");
                    }
                });
        }

}
