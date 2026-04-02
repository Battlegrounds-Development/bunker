package me.remag501.bunker;

import com.sk89q.worldguard.WorldGuard;
import me.remag501.core.api.BGSApi;
import me.remag501.core.api.command.CommandService;
import me.remag501.core.api.event.EventService;
import me.remag501.core.api.task.TaskService;
import me.remag501.bunker.commands.BunkerAdminCommand;
import me.remag501.bunker.commands.BunkerCommand;
//import me.remag501.bunker.commands.BunkerCommandOld;
import me.remag501.bunker.listeners.GeneratorBreakListener;
import me.remag501.bunker.listeners.OpenContainer;
import me.remag501.bunker.managers.AdminManager;
import me.remag501.bunker.managers.BunkerCreationManager;
import me.remag501.bunker.managers.BunkerConfigManager;
import me.remag501.bunker.managers.ConfigManager;
import me.remag501.bunker.service.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class BunkerPlugin extends JavaPlugin {

    public static final UUID SYSTEM_ID = UUID.nameUUIDFromBytes("BUNKER_TASK".getBytes());

    @Override
    public void onEnable() {

        // Plugin startup logic
        saveDefaultConfig();
        ConfigManager configManager = new ConfigManager(this, "bunkers.yml");
        BunkerConfigManager bunkerConfigManager = new BunkerConfigManager(this);

        // Get services from BGS Core API
        EventService eventService = BGSApi.events();
        TaskService taskService = BGSApi.tasks();
        CommandService commandService = BGSApi.commands();

        // Create services
        HologramService hologramService = new HologramService(getLogger());
        GeneratorService generatorService = new GeneratorService(taskService, getLogger());
        NPCService npcService = new NPCService(taskService, getLogger());
        SchematicService schematicService = new SchematicService(taskService, getLogger());
        WorldGuardService worldGuardService = new WorldGuardService(getLogger());

        // Create managers
        BunkerCreationManager bunkerCreationManager = new BunkerCreationManager(taskService, getLogger(), configManager,
                bunkerConfigManager, generatorService, hologramService, npcService, schematicService, worldGuardService);
        AdminManager adminManager = new AdminManager(this, bunkerCreationManager, hologramService, generatorService);

        // Register listeners
        new OpenContainer(eventService);
        new GeneratorBreakListener(eventService);

        // Setup commands
        BunkerCommand command = new BunkerCommand(this, bunkerConfigManager, bunkerCreationManager);
        getCommand("bunker").setExecutor(command);
        commandService.registerSubcommand("bunker", command);
        BunkerAdminCommand adminCommand = new BunkerAdminCommand(bunkerConfigManager, bunkerCreationManager, adminManager);
        getCommand("bunkeradmin").setExecutor(adminCommand);
        commandService.registerSubcommand("bunkeradmin", command);

        // Send startup message
        getLogger().info("Bunker has been enabled!");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
