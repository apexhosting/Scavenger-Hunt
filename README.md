# Scavenger-Hunt

A VERY simple scavenger hunt plugin

---  

## ⚙️ Project Dependencies:

- Citizens: https://www.spigotmc.org/resources/citizens.13811/
- PlaceholderAPI (soft-dependent): https://www.spigotmc.org/resources/placeholderapi.6245/

---  

## ❔ Commands & Permissions

- `/items` (`scavenger.player.items`) — Open the GUI for all the required items
- `/game start` (`scavenger.admin.start`) — Start a game
- `/game cheat` (`scavenger.admin.cheat`) — Cheat all the required items in
- `/game stop`  (`scavenger.admin.stop`) — Stop the running game in the world
- `/game reload` (`scavenger.admin.reload`) — Stop all running games and reload all the config settings

---  

## Placeholders

The plugin supports PlaceholderAPI

- `%scavenger_return_count%` — Returns the amount of items the player has returned
- `%scavenger_return_count_formatted%` — Returns the `settings.placeholderapi-format` setting from the config parsed for
  PAPI placeholders as well as colours

## 📋 Default Configuration

```yml  
lang:
  error: "&cAn unknown exception occurred, please report this to staff!"
  permission: "&cYou do not have access to this command!"
  player-only: "&cYou must be a player to execute this command!"
  unknown-command: "&cThis is not a valid command, please use &4/%command% help &cfor help!"
  reload: "&aSuccessfully reloaded the plugin!"
  reload-fail: "&cThere was an error reloading the plugin! Please review the console for more information."
  plugin-disabled: "&cThe plugin is currently disabled, please resolve any errors in the console and use the &4/%command% reload &ccommand!"
  not-in-progress: "&cThe game has not started yet, please wait!"
  starter-item-remove-fail: "&cYou should keep your starter items in case you get stuck!"

  game-loading: "&c\n&c\n&2⚠ &lWARNING&f: &aThe game is now loading! Make sure to read the book in your first hotbar slot for more information!\n&c"
  game-starting: "&aThe game is starting in &2%remaining% &aseconds!"
  game-started: "&2&l✔ &a&lGAME STARTED! &f— &aBest of luck everyone!"

  no-pvp: "&cYou are not allowed to damage players during grace period!"
  pvp-enable: "&6⚠ &cPVP will be enabled in: &4%remaining% seconds!"
  pvp-enabled: "&c\n&c\n&6⚠ &lWARNING&f: &cPVP is now enabled!\n&c"

  game-stopped: "&2&l✔ &a&lGAME STOPPED!"

  game-in-progress: "&cThere is a game in progress already! Stop it using &4/%command% stop&c!"
  game-not-in-progress: "&cYou are not in a game currently! Start a new game using &4/%command% start&c!"
  game-loading-in-progress: "&cThe game is still loading, please wait a few seconds to use this command!"

  item-craft-notification: |
    &c
    &6🗡 &lHEADS UP! &8— &eYou just crafted a required item: &6%itemname%
    &eMake sure to return it to &6Doug &eat spawn!
    &c
  item-pickup-notification: |
    &c
    &6🗡 &lHEADS UP! &8— &eYou just picked up a required item: &6%itemname%
    &eMake sure to return it to &6Doug &eat spawn!
    &c

  no-item-provided: "&cYou must provide an item to return!"
  incorrect-item-provided: "&cThis item is not required for this round!"
  item-already-provided: "&cYou have already returned this item!"
  item-not-enough: "&cYou need &4%required% &cof this item, you currently only have &4%amount%&c! "
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

  # The format of the %scavenger_return_count_formatted% placeholder
  # All PlaceholderAPI placeholders will work here
  # Full colour support as well
  placeholderapi-format: "&3Amount returned: &3%scavenger_return_count%"

  # A list of custom sounds
  # The sounds have to be from here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html
  # The volume and pitch are optional, pitch must be between 0.1-2.0
  sounds:
    # Played for everyone when the start countdown is clicking
    start-countdown:
      sound: BLOCK_NOTE_BLOCK_CHIME
      volume: 1
      pitch: 1

    # Played for everyone when the game starts
    start:
      sound: ENTITY_ENDER_DRAGON_GROWL
      volume: 10
      pitch: 2

    # Played for everyone when the pvp countdown is clicking
    pvp-countdown:
      sound: BLOCK_NOTE_BLOCK_PLING

    # Played for everyone when PVP is enabled
    pvp:
      sound: ENTITY_ENDER_DRAGON_GROWL

    # Played for everyone when the game is over
    final-win:
      sound: UI_TOAST_CHALLENGE_COMPLETE

    # Played for the player in case they pick up an item they will need
    item-pickup-notification:
      sound: BLOCK_NOTE_BLOCK_BIT
      pitch: 1.1

    # Played for the player in case they craft an item they will need
    item-craft-notification:
      sound: BLOCK_NOTE_BLOCK_BIT
      pitch: 1.1

  # Execute a special set of commands through the console whenever the game stage is updated
  # Set it to {} to not execute any
  execute-commands:
    # When the game is 'loaded' and the start countdown is initiated
    onload: { }
    # When the countdown is finished
    onstart:
      - effect give @a invisibility 10 10 true
    # When the grace period is over
    onpvp: { }
    # When the game is won
    onfinish: { }

  # Given in seconds, set it to 0 to start instantly
  start-count-down: 15

  grace-period:
    # Given in seconds, set it to 0 to enable PVP on start, -1 to disable PVP entirely
    time: 180

    # Whether to disable certain damage types during grace period
    disable-fire: true
    disable-fall: true

  # The size the worldborder on start and during the game
  border-size:
    small: 70
    expanded: 1500

  # How many winners to wait for the game to end
  winners: 3

  # Whether to clear players' inventories when the game starts
  clear-inventory:
    on-start: true
    on-stop: true

  # The allowed worlds for the game
  worlds:
    - event_world
    - event_world_nether
    - event_world_the_end

  # The spawnpoint of the world where players respawn
  spawnpoint:
    world: event_world
    x: 153
    y: 68
    z: 158

  spawn-npcs-before-start: true

  # A list of NPCs to spawn in game
  return-npcs:
    doug:
      displayname: "&8[&cNPC&8] &rDoug"
      skin: "ApexHosting"
      world: event_world
      look-close: true
      x: 137.6
      y: 68
      z: 158.5

  # Settings for the NPCs GUIs
  return-gui:

    # The title of the GUI
    title: "&#206694Items"

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
        name: "&r%itemname%"

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
    on-pvp-kill: false

    # Whether to drop items when a user dies on their own
    on-natural-death: true

  pvp-bossbar:
    enabled: true

    # The title of the pvp bossbar
    title: "&bPVP will be enabled in: &3%remaining% &bseconds!"

    # A valid BarColor or AUTO to let the plugin handle colouring (https://hub.spigotmc.org/javadocs/spigot/org/bukkit/boss/BarColor.html)
    colour: AUTO

  # Whether to ensure starter items are not dropped on death
  keep-starter-items: true

  # A list of items given to everyone on start
  starter-items:

    compass:
      material: COMPASS
      name: "&6Golden Compass"
      lore:
        - "&c"
        - "&e&oUse this item to get back to spawn"
        - "&e&oin case you ever get lost!"
      point-to:
        world: event_world
        x: 137.6
        y: 68
        z: 158.5

    book:
      material: WRITTEN_BOOK
      name: "&3⛏ &bStarter Instructions!"
      author: "&cDoug"
      type: original
      pages:
        1: |
          Welcome to ApexHosting's &3&oScavenger Hunt &revent!
          &8&m                            &r

          The first &33 &rto complete the hunt get an &#f68e00Amazon Gift Card&r as well as free server hosting!

          &8&oContinue reading for rules & objectives:                           ⤵
        2: |
          &l1. &rGo out in the world and gather the necessary items. Bring these items to Doug, located at spawn.

          &l2. &rThe items needed can be checked with him or through the &3&o/items &rcommand!

          &l3. &rPVP will be enabled after a few minutes!    &8&oContinue reading: ⤵
        3: |
          If you die to another player, you will &nrespawn with all your items&r.

          If you happen to die to nature causes, your &nitems will be dropped &ron the ground, but you will still respawn!

          &3&oBest of luck and we hope have fun!

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