# DoomThemAll
DoomThemAll is a Minecraft (Bukkit) Mini-Game: Eliminate the other team using a Shotgun and Grenades!  
**Herzlich Willkommen, liebe "[Schlag den YouTuber](http://www.youtube.com/watch?v=-twLkPWfVEcT)"-Zuschauer!**

## How To Play
After joining a match you see a countdown above your inventory and you are allowed to investigate the map.  
At the beginning you join the red or the blue team and get a blaze-rod (Shotgun) and an egg (Grenade). Both can be used with an right-click. While you can use the shotgun almost every time, you have to kill somebody from the other team to get a new grenade.
After one team reached 25 points the game is over.

## Setup
Follow these steps to setup and configure "DoomThemAll" on your server:

1. Download the plugin: Grab the current release from [here](https://github.com/MarvinMenzerath/DoomThemAll/releases) or a development-version from [here](http://menzerath.eu:8080/job/DoomThemAll/) and put it in your plugins-folder.
2. Download Multiverse: Grab the current release from [here](http://dev.bukkit.org/bukkit-plugins/multiverse-core/) and put it in your plugins-folder.
3. Upload the maps: Upload the maps you want to play on and name them `dta-wX`, where "X" has to be a unique number. This will be the id of the arena. Example: `dta-w1`
This can be changed in the config-file (see `gameWorldPrefix`).
4. Now start your server and import the worlds. Example: `/mvimport dta-w1 normal`  After you did this for every arena, edit the Multiverse-world-config and change the following parameters (if you want to):
  * `allowweather: false`
  * `difficulty: PEACEFUL`
  * `animals:
      spawn: false`
  * `monsters:
      spawn: false`
  * `hunger: false`
5. Reload the Multiverse-Config: `/mvreload`
6. Set a DTA-Spawn: `/dta setlobby` You will spawn there if you type `/dta lobby`.
7. Configure the first arena:
  1. Teleport there: `/mvtp dta-w1`
  2. Now set every spawn (exactly 6!): Go to the place you want the players to spawn and type in `/dta setspawn [MAP] [SPAWN]`. Example: `/dta setspawn 1 1`
  3. Enable the arena: `/dta enable 1`.
  4. Place a sign to join the arena (every new line represents a line of the sign):
    1. `DoomThemAll`
    2. `__EMPTY__`
    3. `dta join [ARENA-ID]`
    4. `[MAP-NAME]`

## Commands

### User
* Teleport to the lobby: `/dta lobby`
* Join an arena: `/dta join [ARENA]`
* Leave an arena: `/dta leave`

### VIP / Premium / YouTuber / ...
You have to use permissions to allow access to these commands
* Start the current arena (10 sec countdown): `/dta start`

### Admin / OP
* Set lobby-spawn: `/dta setLobby`
* Set arena-spawn: `/dta setSpawn [ARENA] [SPAWN]`
* Enable / Disable arena: `/dta enable/disable [ARENA]`
* Set maximum score: `/dta setMaxScore [SCORE]`
* Set maximum players per arena: `/dta setMaxPlayers [PLAYERS]`
* Enable / Disable fun for OPs: `/dta funForOPs [TRUE / FALSE]`
* Reload config: `/dta reload`

## Permissions
If you use a permissions-plugin like PermissionsEx, you'll probably want to restrict access to certain features of DoomThemAll:
* `doomthemall.premium`: Player is able to join full games and use its Shotgun faster than the other players
* more will follow soon!

## Compatibility
Compiled and Tested with (Craft)Bukkit **1.7.9** (Beta-Build).

## FAQ
**Q**: I can not kill somebody from my team!  
**A**: This is the intended behaviour. Nobody wants assholes!

**Q**: I don't get any grenades!  
**A**: You have to kill somebody to get a new grenade.

## License
Copyright (C) 2013-2014 [Marvin Menzerath](http://menzerath.eu)

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [GNU General Public License](https://github.com/MarvinMenzerath/DoomThemAll/blob/master/LICENSE) for more details.
