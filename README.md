# Scavenger-Hunt
A VERY simple scavenger hunt plugin

---  

## ⚙️ Project Dependencies:

- Citizens: https://www.spigotmc.org/resources/citizens.13811/
- PlaceholderAPI (soft-dependent): https://www.spigotmc.org/resources/placeholderapi.6245/

---  

## ❔ Commands & Permissions
- `game start` (`scavenger.admin.start`) - Start a game 
- `game cheat` (`scavenger.admin.cheat`) - Cheat all the required items in
- `game stop`  (`scavenger.admin.stop`) - Stop the running game in the world
- `game reload` (`scavenger.admin.reload`) - Stop all running games and reload all the config settings

---  

## 📋 Default Configuration
_(work in progress)_
```yml  
lang:
  error: "&cAn unknown exception occurred, please report this to staff!"
  permission: "&cYou do not have access to this command!"
  player-only: "&cYou must be a player to execute this command!"
  unknown-command: "&cThis is not a valid command, please use &4/%command% help &cfor help!"
  reload: "&aSuccessfully reloaded the plugin!"
  reload-fail: "&cThere was an error reloading the plugin! Please review the console for more information."

  game-starting: "&aThe game is starting in &2%remaining% &aseconds!"
  game-started: "&2&l✔ &a&lGAME STARTED! &f— &aBest of luck everyone!"

  no-pvp: "&cYou are not allowed to damage players during grace period!"
  pvp-enable: "&6⚠ &cPVP will be enabled in: &4%remaining% seconds!"
  pvp-enabled: "&c\n&c\n&6⚠ &lWARNING&f: &cPVP is now enabled!\n&c"

  game-stopped: "&2&l✔ &a&lGAME STOPPED!"

  game-in-progress: "&cThere is a game in progress already! Stop it using &4/%command% stop&c!"
  game-not-in-progress: "&cYou are not in a game currently! Start a new game using &4/%command% start&c!"

  no-item-provided: "&cYou must provide an item to return!"
  incorrect-item-provided: "&cThis item is not required for this round!"
  item-already-provided: "&cYou have already returned this item!"
  item-not-enough: "&cYou need &4%required% &cof this item, you currently only have &4%required%&c! "
  item-accepted: "&a&lCONGRATS! &8— &aYou now have &2%current% &aout of &2%total%&a items!"
  items-left: "&7Items left: &3%items%"

  # Available placeholders: %place%, %playername%, %maxwinners%
  chat-announce-new-winner: "&c\n&a&l#%place% WINNER! &8— &6%playername% &ehas become the &6#%place% &ewinner! The game will end once there are &6%maxwinners% &ewinners!\n&c"

  # Available placeholders: %top1%-%top100%
  chat-announce-all-winners: |
    &c
    &c
    &c
    &c
    &8&m                                                                               &r
    &6&lGAME OVER! &8— &eCongratulations everyone!
    &c
    &#e1b031&lWinner #1&f: &7%top1%
    &#d3d3d3&lWinner #2&f: &7%top2%
    &#b39b7d&lWinner #3&f: &7%top3%
    &8&m                                                                               &r

  # Available placeholders: %command%
  help-lines:
    - "&c"
    - "&8&m                                                                               &r"
    - "<g:#13425E:#0A6EAB>&lAPEX HOSTING&f: &r&#40B350Scavenger Hunt"
    - "&cPlease do note that the plugin is currently work in progress."
    - "&c"
    - " &r• &#B94A4D/%command% help &7—&f Here you are!"
    - " &r• &#B94A4D/%command% start <template> &7—&f Start a new game"
    - " &r• &#B94A4D/%command% cheat &7—&f Cheat in all the items for testing"
    - " &r• &#B94A4D/%command% stop &7—&f Force stop the stop game"
    - " &r• &#B94A4D/%command% reload &7—&f Stop the game, reload all configuration values"
    - "&8&m                                                                               &r"

settings:

  # Given in seconds, set it to 0 to enable PVP on start, -1 to disable PVP entirely
  grace-period: 10

  # The size the worldborder on start and during the game
  border-size:
    small: 50
    expanded: 500

  # How many winners to wait for the game to end
  winners: 2

  # The allowed worlds for the game
  worlds:
    - world
    - world_nether
    - world_the_end

  # The spawnpoint of the world where players respawn
  spawnpoint:
    world: world
    x: 12
    y: 65
    z: 12

  # A list of NPCs to spawn in game
  return-npcs:
    doug:
      displayname: "&8[&cNPC&8] &rDoug"
      skin: "ApexHosting"
      world: world
      look-close: true
      x: 0
      y: 65
      z: 0

  # Settings for the NPCs GUIs
  return-gui:

    # The title of the GUI
    title: "&cSome title"

    # The amount of rows in the GUI, min 1, max 6
    rows: 3

    # See below for a list of all settings for items
    items:

      # Settings for non-completed items
      regular:
        # A special material or AUTO to parse it for the item
        material: AUTO
        # A special amount of items or AUTO to parse it for the item
        amount: AUTO
        # The name of the item, available placeholders: %itemname%, %amount%
        name: "%itemname% test"

      # Settings for completed items, all settings the same as above
      completed:
        material: LIME_DYE
        amount: AUTO
        name: "&a&lCOMPLETED! &8— &f%amount%x %itemname%"
        hide-enchants: true
        enchants:
          mending: 1

  scoreboard:
    enabled: true

    # The title of the scoreboard
    title: "&7-=[&6&lLeaderboard&7]=-"

    # The lines of the scoreboard, available placeholders: %place%, %playername%, %returncount%
    line-format: "&7#%place%) &#fed256%playername%&f &7(&f%returncount% &7items)"

    # How many player to show at most 15 max
    show-players: 10

  drop-items:
    # Whether to drop items when a user is killed
    on-pvp-kill: true

    # Whether to drop items when a user dies on their own
    on-natural-death: true

  pvp-bossbar:
    enabled: true

    # The title of the pvp bossbar
    title: "&bPVP will be enabled in: &3%remaining% &bseconds!"

    # A valid BarColor or AUTO to let the plugin handle colouring (https://hub.spigotmc.org/javadocs/spigot/org/bukkit/boss/BarColor.html)
    colour: AUTO

  # A list of items given to everyone on start
  starter-items:

    # This is to show all the settings of the items
    test-item:
      material: BARRIER
      amount: 3
      name: "&6&lThis is an item's name"
      lore:
        - "This is lore line #1"
        - "This is lore line #2"
        - "... and so on ..."
      enchants:
        mending: 1
        unbreaking: 3
      hide-enchants: false

    book:
      material: WRITTEN_BOOK
      name: "&4&lStarter Instructions"
      hide-enchants: true
      enchantments:
        mending: 1
      author: "&cDoug"
      type: original
      pages:
        1: |
          Welcome to ApexHosting's Scavenger Hunt event!

          The first 3 to complete the hunt get an amazon gift card as well as free hosting!

        2: |
          Page 2 yes

  # A list of required items to win the game
  # You can use all item settings mentioned above but
  # the player won't be able to get mending on a music
  # disk for example.
  required-items:
    enchanting_table:
      material: ENCHANTING_TABLE
    anvil:
      material: ANVIL
    golden_apple:
      material: GOLDEN_APPLE
    cornflower:
      material: CORNFLOWER
    tnt:
      material: TNT
    lectern:
      material: LECTERN
    mushroom_stew:
      material: MUSHROOM_STEW
    egg:
      material: EGG
    bread:
      material: BREAD  
```  
  
---