package me.remag501.bunker.commands;

import me.remag501.bunker.managers.AdminManager;
import me.remag501.bunker.managers.BunkerCreationManager;
import me.remag501.bunker.managers.BunkerConfigManager;
import me.remag501.bunker.service.BunkerWorldLifecycleService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BunkerAdminCommand implements CommandExecutor, TabCompleter {

    private final BunkerConfigManager bunkerConfigManager;
    private final BunkerCreationManager bunkerCreationManager;
    private final AdminManager adminManager;
    private final BunkerWorldLifecycleService worldLifecycleService;

    public BunkerAdminCommand(BunkerConfigManager configManger, BunkerCreationManager bunkerCreationManager, AdminManager adminManager,
                              BunkerWorldLifecycleService worldLifecycleService) {
        this.bunkerConfigManager = configManger;
        this.bunkerCreationManager = bunkerCreationManager;
        this.adminManager = adminManager;
        this.worldLifecycleService = worldLifecycleService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
            sender.sendMessage(ChatColor.RED + "Usage: /bunkeradmin <add|preview|migrate|upgrade|tp>");
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

            case "tp":
            case "teleport":
                return handleForceTeleport(sender, args);

            case "reload":
                bunkerConfigManager.reload();
                bunkerCreationManager.reloadBunkerConfig();
                sender.sendMessage("Bunker config reloaded.");
                sender.sendMessage(bunkerCreationManager.getTotalBunkers() + " " + bunkerCreationManager.getAssignedBunkers());
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use add, preview, migrate, upgrade, or tp.");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterPrefix(args[0], List.of("add", "preview", "migrate", "upgrade", "playerupgrade", "tp", "teleport", "reload"));
        }

        if (args.length == 2 && isTeleportSubcommand(args[0])) {
            return filterPrefix(args[1], List.of("player", "world"));
        }

        if (args.length == 3 && isTeleportSubcommand(args[0])) {
            if (args[1].equalsIgnoreCase("player")) {
                List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                return filterPrefix(args[2], players);
            }
            if (args[1].equalsIgnoreCase("world")) {
                List<String> worlds = new ArrayList<>(bunkerCreationManager.getKnownBunkerWorldNames());
                Collections.sort(worlds);
                return filterPrefix(args[2], worlds);
            }
        }

        return Collections.emptyList();
    }

    private boolean handleForceTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(ChatColor.RED + "Only players can force teleport into bunkers.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /bunkeradmin tp <player|world> <name>");
            return true;
        }

        String mode = args[1].toLowerCase(Locale.ROOT);
        String target = args[2];

        return switch (mode) {
            case "player" -> forceTeleportToPlayerBunker(admin, target);
            case "world" -> forceTeleportToWorld(admin, target);
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: /bunkeradmin tp <player|world> <name>");
                yield true;
            }
        };
    }

    private boolean forceTeleportToPlayerBunker(Player admin, String playerName) {
        var offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer == null) {
            senderMessage(admin, ChatColor.RED + "Targeted player is offline or unknown.");
            return true;
        }

        if (!bunkerCreationManager.hasBunker(offlinePlayer.getUniqueId())) {
            senderMessage(admin, ChatColor.RED + "Targeted player does not have a bunker!");
            return true;
        }

        String worldName = bunkerCreationManager.getWorldName(offlinePlayer.getUniqueId());
        return forceTeleportToWorld(admin, worldName);
    }

    private boolean forceTeleportToWorld(Player admin, String worldName) {
        String normalizedWorldName = normalizeWorldName(worldName);
        if (normalizedWorldName == null || normalizedWorldName.isBlank()) {
            senderMessage(admin, ChatColor.RED + "Invalid world name.");
            return true;
        }

        worldLifecycleService.executeWhenWorldLoaded(normalizedWorldName,
                world -> {
                    admin.teleport(world.getSpawnLocation());
                    admin.sendMessage(ChatColor.GREEN + "Teleported to bunker world: " + normalizedWorldName);
                },
                () -> admin.sendMessage(ChatColor.RED + "Could not load bunker world: " + normalizedWorldName));
        return true;
    }

    private boolean isTeleportSubcommand(String value) {
        return value != null && (value.equalsIgnoreCase("tp") || value.equalsIgnoreCase("teleport"));
    }

    private String normalizeWorldName(String worldName) {
        if (worldName == null) return null;
        String trimmed = worldName.trim();
        if (trimmed.endsWith(".slime")) {
            return trimmed.substring(0, trimmed.length() - 6);
        }
        return trimmed;
    }

    private List<String> filterPrefix(String input, List<String> options) {
        if (input == null || input.isBlank()) {
            return options;
        }

        String lower = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private void senderMessage(Player player, String message) {
        player.sendMessage(message);
    }

    public BunkerCreationManager getBunkerCreationManager() {
        return bunkerCreationManager;
    }
}
