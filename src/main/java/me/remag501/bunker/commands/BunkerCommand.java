package me.remag501.bunker.commands;

import me.remag501.bunker.BunkerPlugin;
import me.remag501.bunker.managers.BunkerCreationManager;
import me.remag501.bunker.managers.BunkerConfigManager;
import me.remag501.bunker.service.BunkerWorldLifecycleService;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BunkerCommand implements CommandExecutor {
    private final BunkerPlugin plugin;
    private final BunkerConfigManager bunkerConfigManager;
    private final BunkerCreationManager bunkerCreationManager;
    private final BunkerWorldLifecycleService worldLifecycleService;

    public BunkerCommand(BunkerPlugin plugin, BunkerConfigManager bunkerConfigManager, BunkerCreationManager bunkerCreationManager,
                         BunkerWorldLifecycleService worldLifecycleService) {
        this.plugin = plugin;
        this.bunkerConfigManager = bunkerConfigManager;
        this.bunkerCreationManager = bunkerCreationManager;
        this.worldLifecycleService = worldLifecycleService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("bunker.use"))
            return true;

        if (args.length == 0 || args[0].equalsIgnoreCase("home")) {
            // Teleport to own bunker after ensuring the slime world is loaded.
            if (!bunkerCreationManager.hasBunker(player.getUniqueId())) {
//                player.sendMessage(bunkerConfigManager.getMessage("noBunker"));
                bunkerCreationManager.assignBunker(player);
//                return true;
            }

            String worldName = bunkerCreationManager.getWorldName(player.getUniqueId());
            if (worldName == null || worldName.isBlank()) {
                player.sendMessage("Bunker world not found!");
                return true;
            }

            worldLifecycleService.executeWhenWorldReady(worldName,
                    world -> bunkerCreationManager.bootstrapRuntimeSystems(world, player.getUniqueId()),
                    world -> {
                        player.teleport(world.getSpawnLocation());
                        player.sendMessage(bunkerConfigManager.getMessage("homeMsg"));
                    },
                    () -> player.sendMessage("Bunker world not found!"));
            return true;
        }

        switch (args[0].toLowerCase()) {
//            case "buy":
//                if (bunkerCreationManager.hasBunker(player.getUniqueId())) {
//                    player.sendMessage(bunkerConfigManager.getMessage("alreadyOwnBunker"));
//                    return true;
//                }
//                if (bunkerCreationManager.assignBunker(player)) {
//                    player.sendMessage(bunkerConfigManager.getMessage("bunkerPurchased"));
//                } else {
//                    player.sendMessage(bunkerConfigManager.getMessage("outOfBunkers"));
//                }
//                return true;

//            case "visit":
//                player.sendMessage("This command is temporarily removed");
//                return true;
//
//            case "accept":
//                if (!visitRequestManager.hasPendingRequest(player.getUniqueId())) {
//                    player.sendMessage("You have no pending visit requests.");
//                    return true;
//                }
//                Player requester = visitRequestManager.getPendingRequest(player.getUniqueId());
//                if (requester != null) {
//                    // Teleport visitor to bunker owner
//                    String ownerWorldName = bunkerCreationManager.getWorldName(playerName);
//                    World ownerWorld = plugin.getServer().getWorld(ownerWorldName);
//                    if (ownerWorld != null) {
//                        Location spawn = ownerWorld.getSpawnLocation();
//                        requester.teleport(spawn);
//                        requester.sendMessage("You have been teleported to " + playerName + "'s bunker.");
//                        player.sendMessage("You accepted the visit request.");
//                    } else {
//                        player.sendMessage("Bunker world not found.");
//                    }
//                } else {
//                    player.sendMessage("Requester is not online.");
//                }
//                return true;
//
//            case "decline":
//                if (!visitRequestManager.hasPendingRequest(player.getUniqueId())) {
//                    player.sendMessage("You have no pending visit requests.");
//                    return true;
//                }
//                visitRequestManager.removeRequest(player.getUniqueId());
//                player.sendMessage("You declined the visit request.");
//                return true;

            default:
                player.sendMessage(bunkerConfigManager.getMessage("argCommandUsage"));
                return true;
        }
    }
}
