# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

### [1.1.1](https://github.com/Battlegrounds-Development/bunker/compare/v1.1.0...v1.1.1) (2026-03-25)

## 1.1.0 (2026-02-20)


### Features

* added /bgs bunker and /bgs bunkeradmin with commandService ([0f8c5da](https://github.com/Battlegrounds-Development/bunker/commit/0f8c5da70eb2e52b8a209941705d5413b89b427c))
* Added permissions to bunker ([c3ea9c8](https://github.com/Battlegrounds-Development/bunker/commit/c3ea9c869989b15e1aee53fef5b27edbfcf292f1))


### Bug Fixes

* added DI for all remaining objects within main class. (This build compiles) ([fc12a07](https://github.com/Battlegrounds-Development/bunker/commit/fc12a07db8c83220fa4d8617e5bf1b23f1b88f06))
* added DI for BunkerAdminCommand ([c877610](https://github.com/Battlegrounds-Development/bunker/commit/c87761033c19fa742a7c3c5c68f646ce96cf7e9f))
* added global uuid to prevent null pointer when registering tasks ([5a12c19](https://github.com/Battlegrounds-Development/bunker/commit/5a12c1917952a70702f1968d64668c00f299b384))
* BunkerConfigManager stores Schematic Wrapper correctly ([9e84f10](https://github.com/Battlegrounds-Development/bunker/commit/9e84f102099447c5813b606c292ae5e711768fed))
* BunkerCreationManager is ground truth in main file now ([af6c51d](https://github.com/Battlegrounds-Development/bunker/commit/af6c51dbf9be32f6d5a9002aea883021b8ff4db3))
* nextgens and axvaults are updated to work with plugin ([6eaba1e](https://github.com/Battlegrounds-Development/bunker/commit/6eaba1e49d682e3c12740636cbfca1a99a5a5ebe))
* world generation was not working api. Resolved by setting delay to a minimum of 1, and forcing chunk load ([91d935a](https://github.com/Battlegrounds-Development/bunker/commit/91d935afbc01dccf0b5f9d59c10a5bc78e63ebe3))
