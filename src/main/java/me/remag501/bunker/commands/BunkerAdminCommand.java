package me.remag501.bunker.commands;

import me.remag501.bunker.managers.AdminManager;
import me.remag501.bunker.managers.BunkerCreationManager;
import me.remag501.bunker.managers.BunkerConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BunkerAdminCommand implements CommandExecutor {

    private final BunkerConfigManager bunkerConfigManager;
    private final BunkerCreationManager bunkerCreationManager;
    private final AdminManager adminManager;

    public BunkerAdminCommand(BunkerConfigManager configManger, BunkerCreationManager bunkerCreationManager, AdminManager adminManager) {
        this.bunkerConfigManager = configManger;
        this.bunkerCreationManager = bunkerCreationManager;
        this.adminManager = adminManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
//        if (!(sender instanceof Player)) {
//            sender.sendMessage("Only players can run this command.");
//            return true;
//        }
//
//        Player player = (Player) sender;
//        String playerName = player.getName();

        if (!sender.hasPermission("bunker.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /bunkeradmin <add|preview|migrate|upgrade>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bunkeradmin add <amount>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    if (amount <= 0) {
                        sender.sendMessage(ChatColor.RED + "Amount must be a positive number.");
                        return true;
                    }
                    bunkerCreationManager.addBunkers(amount, sender);
                    sender.sendMessage(ChatColor.GREEN + "Starting creation for " + amount + " bunkers.");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number.");
                }
                return true;

            case "preview":
                // Load or teleport the admin to a preview world, you can modify this logic
                if (sender instanceof Player player) {
                    adminManager.previewBunker(player);
                }
//                World previewWorld = Bukkit.getWorld("bunker_preview");
//                if (previewWorld == null) {
//                    player.sendMessage(ChatColor.RED + "Preview world not found.");
//                    return true;
//                }
//                player.teleport(previewWorld.getSpawnLocation());
//                player.sendMessage(ChatColor.GREEN + "Teleported to preview world.");
                return true;

            case "migrate":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bunkeradmin migrate <amount>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    if (amount <= 0) {
                        sender.sendMessage(ChatColor.RED + "Amount must be positive.");
                        return true;
                    }
                    sender.sendMessage(ChatColor.GRAY + "Migration has not been added yet");
//                    // Implement your migration logic here
//                    boolean success = bunkerCreationManager.migrateOldFormat(amount);
//                    if (success) {
//                        player.sendMessage(ChatColor.GREEN + "Migrated " + amount + " entries.");
//                    } else {
//                        player.sendMessage(ChatColor.RED + "Migration failed or incomplete.");
//                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number.");
                }
                return true;

            case "upgrade":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bunkeradmin upgrade <level>");
                    return true;
                }

                String level = args[1];
//                String worldName = bunkerCreationManager.getWorldName(playerName);
//                String currentWorld = player.getWorld().getName();
                World previewWorld = Bukkit.getWorld("bunker_preview");
                if (previewWorld == null) {
                    sender.sendMessage(ChatColor.RED + "Preview world does not exist.");
                    return true;
                }

//                if (!currentWorld.equals("bunker_preview")) {
//                    player.sendMessage(ChatColor.RED + "You must be in preview bunker to upgrade.");
//                    return true;
//                }

                if (sender instanceof Player player) {
                    if (bunkerCreationManager.upgradeBunkerWorld(previewWorld, level, player)) {
                        player.sendMessage(ChatColor.GREEN + "Bunker upgraded with: " + level);
                    } else {
                        player.sendMessage(ChatColor.RED + "Upgrade does not exist.");
                    }
                }


                return true;

            case "playerupgrade":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bunker playerupgrade <player> <level>");
                    return true;
                }

                String targetPlayer = args[1];
                Player player = Bukkit.getPlayer(targetPlayer);
                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "Targeted player is offline!");
                    return true;
                }

                if (!bunkerCreationManager.hasBunker(player.getUniqueId())) {
                    sender.sendMessage(ChatColor.RED + "Targeted player does not have a bunker!");
                    return true;
                }

                String playerLevel = args[2];

                if (bunkerCreationManager.upgradeBunker(player, playerLevel))
                    sender.sendMessage(ChatColor.GREEN + "Granted the upgrade " + playerLevel + "!");
                else
                    sender.sendMessage(ChatColor.RED + "The upgrade " + playerLevel + " is already owned or does not exist!");
                return true;

            case "reload":
                bunkerConfigManager.reload();
                bunkerCreationManager.reloadBunkerConfig();
                sender.sendMessage("Bunker config reloaded.");
                sender.sendMessage(bunkerCreationManager.getTotalBunkers() + " " + bunkerCreationManager.getAssignedBunkers());
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use add, preview, migrate, or upgrade.");
                return true;
        }
    }

    public BunkerCreationManager getBunkerCreationManager() {
        return bunkerCreationManager;
    }
}
