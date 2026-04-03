# SpigotPlugins_Bunker
Spigot bunker plugin customized for a Minecraft server and developed by Remag501.

The bunker plugin allows players to visit a bunker within their world via SlimeWorldManager/Advanced Slime Paper. Bunkers are created ahead of time by the admin to avoid lag. When a user buys a bunker, they are assigned a bunker file instead of creating a new world on the spot. 

This plugin uses the /bunker command with arguments to work
- /bunker or /bunker home to visit your bunker
- /bunker buy to purchase a bunker and have it assigned to you
- /bunker visit [player] to visit a player's bunker
- /bunker upgrade <level> allows player to add upgrades to their bunker
- /bunker reload will reload config and bunker.yml (may be moved to admin in future)

For admins only, be careful using this command as it can create lag
- /bunkeradmin add [bunkerCount] to increase the total available bunkers on the server. More bunkers are more resources/lag.
- /bunkeradmin preview allows you to preview bunker from config
- /bunkeradmin migrate will change existing bunkers to existing settings *not implemented*
- /bunkeradmin upgrade <level> applies upgrade to the preview world

Dependencies are handled with Maven and are expected to be provided:
- WorldEdit
- SlimeWorldManager / Advanced Slime Paper

v 1.3 Patch Notes:
- Internals reworked heavily
- Levels can now be added to config
- Bunkeradmin is a seperate command from bunker admin
- Players can now use preview their bunker before creating them

Future plans and bugs:
- Fix bug with citizens not teleporting on other servers
- Visit argument does not use config or prevent visitors from opening chest
- Migrate argument is not implemented
- Add Kgenerators to bunker levels (Priority)
- Add Holograms to bunker levels