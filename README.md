# Scavenger-Hunt
A Simple Scavenger Hunt Plugin

---  

## ⚙️ Project Dependencies:

- Citizens: https://www.spigotmc.org/resources/citizens.13811/
- PlaceholderAPI (soft-dependent): https://www.spigotmc.org/resources/placeholderapi.6245/

---  

## ❔ Commands & Permissions
_(work in progress)_
- `game cheat` - Cheat all the required items in
- `game start` - Force start a game without the required players
- `game stop` - Force stop a running game in the world

---  

## 📋 Default Configuration
_(work in progress)_
```yml  
lang:  
error: "&cAn unknown exception occurred, please report this to staff!"  
player-only: "&cYou must be a player to execute this command!"  
reload: "&aSuccessfully reloaded the plugin!"  
no-pvp: "&cYou are not allowed to damage players during grace period!"  
  
game-started: "&aGame starting..."  
  
no-world-provided: "&cYou must provide a world when starting a game from the console!"  
game-in-progress: "&cThis world already has a game in progress!"  
game-not-in-progress: "&cYou are not in a game currently!"  
  
no-item-provided: "&cYou must provide an item!"  
incorrect-item-provided: "&cThis item is not required for this round!"  
item-already-provided: "&cYou have already added this item!"  
item-not-enough: "&cYou need &4%required% &cof this item, you currently only have &%required%&c! "  
item-accepted: "&aCongrats, you now have &2%current% &aout of &2%total%&a items!"  
items-left: "&7Items left: &3%items%"  
  
help-lines:  
- "Todo"  
  
games:  
my_game:  
displayname: "&d&lMy Amazing Game"  
  
 # The amount of players to wait for, set to -1 to only forcestart it required-players: -1  
 # Whether players are put into spectator when they die hardcore: false  
 drop-items: on-pvp-kill: true on-natural-death: true  
 scoreboard: enabled: true title: "&aReturned Items" show-players: 10  
 # Where dead players respawn spawnpoint: world: world x: 12 y: 65 z: 12  
 return-npcs: notmydoug: displayname: "Not Doug, smh" skin: "Notch" world: world look-close: false x: 10 y: 65 z: 10  
 doug: displayname: "&8[&cNPC&8] &rDoug" skin: "ApexHosting" world: world look-close: true x: 0 y: 65 z: 0  
 # Given in minutes, set it to 0 to enable PVP on start, -1 to disable PVP grace-period: 0  
 # The size of the worldborder border-size: 500 border-center: x: 0 z: 0  
 # How many completers to wait for winners: 3  
 # The allowed worlds for the game worlds: - world - world_nether - world_the_end  
 # The required items for someone to win required-items: dirts: material: DIRT amount: 64  
 fancy_anvil: material: ANVIL name: "Rename an anvil to this!" amount: 2  
```  
  
---