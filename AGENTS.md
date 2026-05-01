# AGENTS.md — Guidance for AI coding agents working on BGSBunker

This file captures the minimal, actionable knowledge an automated agent needs to be productive in this repo.

Key points (read before editing code):
- Architecture: `BunkerPlugin` wires services and managers in `onEnable()` — prefer constructor DI and singletons exposed there.
- Runtime vs Templates: `config.yml` (templates, e.g. `main`) describes schematics/NPCs/holograms; `bunkers.yml` stores runtime ownership (now keyed by UUID and stores a slime file name like `bunker_0.slime`). See `BunkerConfigManager` and `ConfigManager`.
- World lifecycle: worlds are slime files (ASP). `BunkerWorldLifecycleService` handles loading via `AdvancedSlimePaperAPI` and provides `executeWhenWorldLoaded/Ready` hooks; use these to bootstrap systems.
- Session-based systems: NPCs and holograms are created when a bunker world loads and must be torn down before unload. See `BunkerCreationManager.bootstrapRuntimeSystems(...)` and the new `teardownRuntimeSystems(...)` method.

Important files to reference:
- `BunkerPlugin.java` — registration of services, commands, and event subscriptions (EventService, TaskService).
- `managers/BunkerCreationManager.java` — orchestration of world creation, bootstrap and teardown.
- `service/BunkerWorldLifecycleService.java` — API wrappers for ASP load/execute/unload logic.
- `service/NPCService.java` and `service/HologramService.java` — session-based spawn/teardown logic (added tracking for runtime clones).
- `service/SchematicService.java` — schematic paste uses TaskService subscribe/retry pattern.
- `core/BunkerInstance.java` — data model for templates (schematics, npcs, holograms, generators).

Agent rules & idioms (concrete):
- Use BGSApi.* services (EventService, TaskService, CommandService) — do not create your own schedulers or listeners.
- For async world work, prefer TaskService.delay/subscribe and Paper's getChunkAtAsync then switch back to main thread via TaskService.delay(1, ...).
- When adding features that touch runtime worlds, update both bootstrap and teardown paths. Look for `bootstrapRuntimeSystems` and `teardownRuntimeSystems`.
- Keep generator rehydration disabled until generator API issues are diagnosed — avoid touching `GeneratorService#rehydrateGenerators` unless user asks.
- When adding commands, register them both to `plugin.yml` and via `BGSApi.commands().registerSubcommand(...)` so they are available under `/bgs`.

Examples (from repo):
- Event subscription: see `BunkerWorldLifecycleListener` and `BunkerPlugin` world-load hooks.
- Retry pattern: `SchematicService.addSchematic` and `NPCService.addNPC` use `taskService.subscribe(..., (attempt) -> {...})` and return true to stop.

If you change persistent formats (bunkers.yml) — note: current work migrated keys to UUID and world names to `*.slime`. This is a full migration; do not implement legacy fallbacks unless requested.

When in doubt: run `mvn clean package` and test on a local aspaper server with the required plugins (ASP, WorldEdit, Citizens, DecentHolograms). The repo's `pom.xml` still targets the ASP backend.

Quick grep targets for agents:
- `bootstrapRuntimeSystems` `teardownRuntimeSystems` (BunkerCreationManager)
- `executeWhenWorldLoaded` (BunkerWorldLifecycleService)
- `AdvancedSlimePaperAPI` (ASP interactions)

Keep entries concise and concrete. Update this file when new repo-wide conventions are introduced.
