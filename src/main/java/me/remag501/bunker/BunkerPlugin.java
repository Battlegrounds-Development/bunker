package me.remag501.bunker;

import me.remag501.bunker.listeners.BunkerWorldBootstrapListener;
import me.remag501.core.api.BGSApi;
import me.remag501.core.api.command.CommandService;
import me.remag501.core.api.event.EventService;
import me.remag501.core.api.task.TaskService;
import me.remag501.bunker.commands.BunkerAdminCommand;
import me.remag501.bunker.commands.BunkerCommand;
import me.remag501.bunker.listeners.BunkerWorldLifecycleListener;
import me.remag501.bunker.listeners.GeneratorBreakListener;
import me.remag501.bunker.listeners.OpenContainer;
import me.remag501.bunker.managers.AdminManager;
import me.remag501.bunker.managers.BunkerConfigManager;
import me.remag501.bunker.managers.BunkerCreationManager;
import me.remag501.bunker.managers.ConfigManager;
import me.remag501.bunker.service.BunkerWorldLifecycleService;
import me.remag501.bunker.service.GeneratorService;
import me.remag501.bunker.service.HologramService;
import me.remag501.bunker.service.NpcService;
import me.remag501.bunker.service.SchematicService;
import me.remag501.bunker.service.WorldGuardService;
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
        NpcService npcService = new NpcService(taskService, getLogger());
        SchematicService schematicService = new SchematicService(taskService, getLogger());
        WorldGuardService worldGuardService = new WorldGuardService(getLogger());
        BunkerWorldLifecycleService worldLifecycleService = new BunkerWorldLifecycleService(this, taskService, bunkerConfigManager);

        // Create managers
        BunkerCreationManager bunkerCreationManager = new BunkerCreationManager(taskService, getLogger(), configManager,
                bunkerConfigManager, generatorService, hologramService, npcService, schematicService, worldGuardService);
        AdminManager adminManager = new AdminManager(this, bunkerCreationManager, hologramService, generatorService, worldLifecycleService);

        // Register listeners
        new OpenContainer(eventService);
        new GeneratorBreakListener(eventService);
        new BunkerWorldLifecycleListener(eventService, worldLifecycleService);
        new BunkerWorldBootstrapListener(eventService, worldLifecycleService, bunkerCreationManager);

        // Setup commands
        BunkerCommand command = new BunkerCommand(this, bunkerConfigManager, bunkerCreationManager, worldLifecycleService);
        getCommand("bunker").setExecutor(command);
        commandService.registerSubcommand("bunker", command);
        BunkerAdminCommand adminCommand = new BunkerAdminCommand(bunkerConfigManager, bunkerCreationManager, adminManager, worldLifecycleService);
        getCommand("bunkeradmin").setExecutor(adminCommand);
        getCommand("bunkeradmin").setTabCompleter(adminCommand);
        commandService.registerSubcommand("bunkeradmin", adminCommand);

        // Send startup message
        getLogger().info("Bunker has been enabled!");

    }

    @Override
    public void onDisable() {
        BGSApi.events().unregisterListener(SYSTEM_ID, BunkerWorldLifecycleListener.NAMESPACE);
    }

}
