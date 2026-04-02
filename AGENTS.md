# AGENTS.md - Bunker Plugin Development Guide

## Project Overview
**BGSBunker** is a Spigot/Paper Minecraft plugin that manages pre-created bunker worlds for players via Multiverse Core. Players can purchase bunkers (assigned from pre-generated instances), customize with schematics/NPCs/generators, and visit each other's worlds. This is a performance-optimized approach that avoids real-time world generation lag.

## Architecture & Data Flow

### Core Components
1. **BunkerPlugin** (entry point) - Initializes all services and managers via dependency injection using BGS Core API
2. **BunkerConfigManager** - Loads `config.yml` into memory; parses bunker level definitions (schematics, NPCs, generators, holograms)
3. **BunkerCreationManager** - Orchestrates bunker assignment and world construction; reads from `bunkers.yml` (player→bunker mapping)
4. **Services** (GeneratorService, SchematicService, HologramService, NPCService, WorldGuardService) - Handle specific world-building tasks
5. **Commands** (BunkerCommand, BunkerAdminCommand) - Route player/admin actions to managers

### Data Models
- **BunkerInstance** - Data class containing lists of SchematicWrapper, NPCInfo, GeneratorInfo, HologramInfo (loaded from config YAML)
- **bunkers.yml** - Persistent storage: player name → bunker ID mapping, total/assigned bunker counts
- **config.yml** - Template definitions: named bunker levels (e.g., "main", "upgraded") with coordinates for all components

### Critical Design Pattern: Two-Config System
- `config.yml` = **Template**: Admin-defined bunker blueprints with component coordinates
- `bunkers.yml` = **Runtime State**: Which players own which bunker instances (ID-based, not template-based)
- When player purchases bunker: Multiverse creates world from pre-generated template, BunkerCreationManager calls services to place schematics/NPCs/generators, updates `bunkers.yml`

## Key Files & Patterns

### Plugin Initialization (BunkerPlugin.java)
- Uses **BGS Core API** for EventService, TaskService, CommandService (injected dependency container)
- Services created separately with minimal dependencies (Logger, TaskService)
- Managers receive all services in constructor
- Event listeners instantiated but not stored (implicit registration via EventService)

### BGS Core API Access Pattern
The plugin uses `BGSApi` as the single entry point to access all Core services. **NEVER** instantiate services directly—always use `BGSApi.service()`:

```java
import me.remag501.core.api.BGSApi;

// In BunkerPlugin.onEnable() or anywhere in the plugin:
EventService eventService = BGSApi.events();
TaskService taskService = BGSApi.tasks();
CommandService commandService = BGSApi.commands();
```

**Available Services via BGSApi:**
- `tasks()` → TaskService (scheduling, delays, repeating tasks)
- `events()` → EventService (event subscriptions with filters)
- `commands()` → CommandService (register subcommands to `/bgs`)
- `ability()` → AbilityService (cooldowns, ultimates, stacks)
- `namespaces()` → NamespaceService (NamespacedKey generation)
- `attribute()` → AttributeService (player attribute modifiers)
- `combat()` → CombatStatsService (weapon/target damage mods)

### TaskService Patterns (Core Scheduling)
TaskService is used throughout BunkerPlugin instead of `BukkitRunnable`. All async work routes through this service:

**Delayed execution** (used in SchematicService and GeneratorService):
```java
taskService.delay(20, () -> {
    // Runs after 20 ticks on main thread
});
```

**Repeating task with retry logic** (SchematicService pattern):
```java
taskService.subscribe(BunkerPlugin.SYSTEM_ID, "schematic-" + worldName, 0, 20, false, (ticks) -> {
    World world = Bukkit.getWorld(worldName);
    if (world != null) {
        // Task succeeded, return true to stop repeating
        return true;
    } else if (attempts.incrementAndGet() >= 100) {
        logger.warning("Failed after 100 attempts");
        return true; // Stop retrying
    }
    // Return false to keep trying next tick
    return false;
});
```

**Async chunk loading** (GeneratorService pattern - uses Paper API):
```java
world.getChunkAtAsync(location).thenAccept(chunk -> {
    Block block = location.getBlock();
    // Safe to access block properties asynchronously
});
```

### EventService Patterns (Event Subscriptions)
EventService provides fluent, filter-based event handling without manually creating Listeners:

```java
// In listeners like GeneratorBreakListener or OpenContainer:
eventService.subscribe(PlayerInteractEvent.class)
    .owner(BunkerPlugin.SYSTEM_ID)
    .namespace("generator-protection")
    .filter(event -> event.getPlayer().hasPermission("bunker.breakgen"))
    .handler(event -> {
        // Handle event
    });
```

**Key points:**
- `.owner(UUID)` - Associates event with a system (use BunkerPlugin.SYSTEM_ID)
- `.namespace(String)` - Groups related subscriptions for cleanup
- `.filter(Predicate)` - Chain multiple filters; all must pass
- `.handler(Consumer)` - Receives the filtered event

### CommandService Patterns (Subcommand Registration)
Commands are registered to the `/bgs` namespace via CommandService:

```java
// In BunkerPlugin.onEnable():
BunkerCommand command = new BunkerCommand(...);
commandService.registerSubcommand("bunker", command);

// This allows both:
// /bunker (via standard plugin.yml)
// /bgs bunker (via BGS subcommand system)
```

### Bunker Creation Workflow (BunkerCreationManager)
```
assignBunker(playerName)
  → Check player doesn't already own bunker
  → Increment assignedBunkers in bunkers.yml
  → Create Multiverse world from template
  → Call services in sequence:
     - SchematicService.addSchematic() (async task with retry loop)
     - GeneratorService.createGenerator() (uses NextGens API)
     - NPCService (Citizens integration)
     - HologramService (clones from templates)
     - WorldGuardService (setup regions)
```

### Async Patterns
- **TaskService** (BGS Core) abstracts all async work - never use `BukkitRunnable` directly
- **SchematicService** uses `taskService.subscribe()` with retry logic (polls for world load, 100 retry limit)
- **GeneratorService** uses `world.getChunkAtAsync()` (Paper API) for non-blocking chunk loads
- Schematics pasted on main thread (WorldEdit limitation) but chunk loads are async

### External Plugin Integration Points
- **Multiverse-Core**: CreateWorldOptions API; worlds managed by MultiverseWorld objects
- **WorldEdit**: SchematicService loads .schem files; uses `EditSession` + `ClipboardHolder`
- **NextGens**: GeneratorService.createGenerator() → getGeneratorManager().registerGenerator()
- **Citizens**: NPCService instantiates NPCs; IDs stored in config
- **DecentHolograms**: HologramService clones named templates to world-specific names
- **WorldGuard**: WorldGuardService registers regions (implementation not shown but integrated)

## Configuration Structure

### config.yml Pattern
```yaml
# Global spawn (optional)
spawn:
  x: 0; y: 0; z: 0; yaw: 0; pitch: 0

# Bunker level definitions (any name)
main:
  schematics:
    - name: "bunker.schem"
      coords: { x: 0, y: 0, z: 0 }
  npcs:
    - id: 123
      coords: { x: 10, y: 65, z: 10, yaw: 0, pitch: 0 }
  generators:
    - type: "stone"
      coords: { x: -10, y: 65, z: -10 }
  holograms:
    - name: "banner_1"
      type: "template_name"
      coords: { x: 0, y: 70, z: 0 }
  remove-holograms:
    - "old_hologram"
```
- Multiple bunker levels can exist; each become separate world presets
- Coordinates are **relative** to world origin; services assign worlds at runtime

### bunkers.yml Pattern
```yaml
totalBunkers: 10        # Admin-set: how many pre-generated worlds exist
assignedBunkers: 5      # Runtime: how many claimed by players
PLAYERNAMEUPPER:
  id: 0                 # Bunker instance ID (index into pre-generated worlds)
  upgrades: ["level2"]  # Applied upgrades tracking
```

## Common Development Tasks

### Adding a New Bunker Component Type
1. Create Service class in `service/` (e.g., `CustomComponentService`)
2. Add field to BunkerInstance for component list (e.g., `List<CustomInfo>`)
3. Update BunkerConfigManager.reload() to parse config section and populate list
4. Instantiate in BunkerPlugin.onEnable()
5. Call from BunkerCreationManager workflow

### BGS Core Integration Checklist (When Adding New Features)
- Use `BGSApi.tasks()` for any scheduling (never `BukkitRunnable`)
- Use `BGSApi.events()` to register listeners (no manual Listener classes needed)
- Use `BGSApi.commands()` to register subcommands to `/bgs bunker` or `/bgs bunkeradmin`
- Store `BunkerPlugin.SYSTEM_ID` as owner UUID in all tasks and events
- Call `taskService.stopTask(BunkerPlugin.SYSTEM_ID, namespace)` during cleanup
- Never cache services—always call `BGSApi.service()` when needed (they're already cached internally)

### Debugging World State
- Use `/bunkeradmin preview` to load template into preview world (test path before assigning to players)
- Use `/bunkeradmin upgrade <level>` to apply configuration to preview world
- Check `target/classes/` for compiled configs (used at runtime)
- Logs prefixed with "[Bunker]" from getLogger()

### Testing Async Behavior
- TaskService tasks can be synchronous (`delay()`) or repeating (`subscribe()`)
- Schema paste failures: check world load (retry loop logs if 100 attempts exceeded)
- Generator failures: verify NextGens plugin loaded and generator type exists

## Build & Deployment

### Maven Build
```powershell
mvn clean package
```
- Output: `target/BGSBunker-1.2.1.jar` (shaded with dependencies)
- Java 21 target; uses maven-shade-plugin (no dependency reduction)
- Compiles resource filtering (plugin.yml variables populated)

### Dependencies (All `provided` scope - must be on server)
- **Paper API** 1.21.8 (core Bukkit/Spigot)
- **Multiverse-Core** 5.5.2
- **FastAsyncWorldEdit** 2.12.3 (WorldEdit facade)
- **NextGens** 1.30 (generator management)
- **Citizens** 2.0.39 (NPC management)
- **DecentHolograms** 2.9.7
- **WorldGuard** 7.1.0
- **AxVaults** 2.12.1
- **BGS Core** 1.0-3.0 (internal, provides TaskService/EventService/CommandService)

## Known Limitations & Future Work
- Visit command `/bunker visit [player]` not fully implemented (request queuing incomplete)
- Citizens NPCs sometimes don't teleport on multi-server setups
- No actual payment/economy hook (bunker assignment is manual for now)
- Migrate command (`/bunkeradmin migrate`) not implemented

## Module Structure
- **commands/**: BunkerCommand (player), BunkerAdminCommand (admin)
- **managers/**: ConfigManager (generic YAML), BunkerConfigManager (template), BunkerCreationManager (orchestrator), AdminManager (admin commands)
- **service/**: Task-specific services (schema paste, NPC spawn, generator setup, etc.)
- **listeners/**: GeneratorBreakListener (prevent player destruction), OpenContainer (permission checks)
- **core/**: BunkerInstance data model with inner static classes
