package dev.apexhosting.scavenger.entities;

import dev.apexhosting.scavenger.Scavenger;
import dev.apexhosting.scavenger.utils.HexUtils;
import dev.apexhosting.scavenger.utils.Utils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.Gravity;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class Game {

    // Settings
    private final boolean dropItemsOnKill;
    private final boolean dropItemsOnDeath;
    private final boolean scoreBoardEnabled;
    private final boolean pvpBossBarEnabled;
    private final boolean clearInventoryOnStart;
    private final boolean clearInventoryOnStop;
    private final boolean gracePeriodDisableFireDamage;
    private final boolean gracePeriodDisableFallDamage;
    private final int scoreboardShowPlayers;
    private final int gracePeriod;
    private final int smallBorderSize;
    private final int expandedBorderSize;
    private final int requiredWinners;
    private final int startCountDown;
    private final String scoreboardTitle;
    private final String pvpBossBarTitle;
    private final String pvpBossBarColour;
    private final String scoreboardLineFormat;
    private final Location spawnPoint;
    private final ArrayList<ReturnNPC> NPCs;
    private final ArrayList<World> allowedWorlds;
    private final ArrayList<Game.Item> requiredItems;
    private final ArrayList<ItemStack> starterItems;
    private final Game.ReturnGui returnGui;
    private final HashMap<String, ArrayList<String>> specialCommands;
    private final HashMap<String, CustomSound> sounds;

    // Internal
    private BossBar bossbar;
    private Stage stage;
    private Scoreboard scoreboard;
    private NPCRegistry npcRegistry;
    private boolean pvpEnabled;
    private final Scavenger plugin;
    private final HashMap<UUID, ArrayList<ItemStack>> playerReturnedItems = new HashMap<>();
    private final ArrayList<BukkitTask> scheduleTasks = new ArrayList<>();
    private final ArrayList<UUID> winners = new ArrayList<>();

    /**
     * Create a new game from a template
     */
    public Game(Scavenger plugin, FileConfiguration config) throws Exception {

        // Internal
        this.plugin = plugin;
        this.pvpEnabled = false;

        String basepath = "settings.";

        // Get basic config settings
        this.dropItemsOnKill = config.getBoolean(basepath + "drop-items.on-pvp-kill");
        this.dropItemsOnDeath = config.getBoolean(basepath + "drop-items.on-natural-death");
        this.scoreBoardEnabled = config.getBoolean(basepath + "scoreboard.enabled");
        this.pvpBossBarEnabled = config.getBoolean(basepath + "pvp-bossbar.enabled");
        this.clearInventoryOnStart = config.getBoolean(basepath + "clear-inventory.on-start");
        this.clearInventoryOnStop = config.getBoolean(basepath + "clear-inventory.on-stop");
        this.gracePeriodDisableFireDamage = config.getBoolean(basepath + "grace-period.disable-fire");
        this.gracePeriodDisableFallDamage = config.getBoolean(basepath + "grace-period.disable-fall");

        this.scoreboardShowPlayers = config.getInt(basepath + "scoreboard.show-players");
        this.gracePeriod = config.getInt(basepath + "grace-period.time");
        this.smallBorderSize = config.getInt(basepath + "border-size.small");
        this.expandedBorderSize = config.getInt(basepath + "border-size.expanded");
        this.requiredWinners = config.getInt(basepath + "winners");
        this.startCountDown = config.getInt(basepath + "start-count-down", 10);

        this.pvpBossBarTitle = config.getString(basepath + "pvp-bossbar.title");
        this.pvpBossBarColour = config.getString(basepath + "pvp-bossbar.colour");
        this.scoreboardTitle = config.getString(basepath + "scoreboard.title");
        this.scoreboardLineFormat = config.getString(basepath + "scoreboard.line-format");
        this.spawnPoint = new Location(Bukkit.getWorld(config.getString(basepath + "spawnpoint.world", "")), config.getInt(basepath + "spawnpoint.x"), config.getInt(basepath + "spawnpoint.y"), config.getInt(basepath + "spawnpoint.z"));

        // Load special event commands
        this.specialCommands = new HashMap<>();
        for (String key : config.getConfigurationSection(basepath + "execute-commands").getKeys(false)) {
            this.specialCommands.put(key, new ArrayList<>(config.getStringList(basepath + "execute-commands." + key)));
        }

        // Load sounds
        this.sounds = new HashMap<>();
        ConfigurationSection soundSection = config.getConfigurationSection(basepath + "sounds");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                Sound sound = Sound.valueOf(config.getString(basepath + "sounds." + key + ".sound"));
                float volume = (float) config.getDouble(basepath + "sounds." + key + ".volume", 1.0f);
                float pitch = (float) config.getDouble(basepath + "sounds." + key + ".pitch", 1.0f);
                this.sounds.put(key, new CustomSound(sound, volume, pitch));
            }
        }

        // Load GUI
        ArrayList<Game.Item> items = Utils.getItemsFromConfig(config, basepath + "return-gui.items");
        this.returnGui = new Game.ReturnGui(config.getString(basepath + "return-gui.title"), config.getInt(basepath + "return-gui.rows"), items.get(0), items.get(1));

        // Load allowed worlds
        this.allowedWorlds = new ArrayList<>() {{
            for (String worldName : config.getStringList(basepath + "worlds")) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) this.add(world);
                else throw new NullPointerException("There was an issue loading one of the worlds: " + worldName);
            }
        }};

        // Load return NPCs
        this.NPCs = new ArrayList<>();
        if (npcRegistry != null) npcRegistry.deregisterAll();
        this.npcRegistry = CitizensAPI.createNamedNPCRegistry(plugin.getName(), new MemoryNPCDataStore());
        for (String npcName : config.getConfigurationSection(basepath + "return-npcs").getKeys(false)) {

            // Basic NPC settings
            String npcPath = basepath + "return-npcs." + npcName + ".";
            boolean lookClose = config.getBoolean(npcPath + "look-close");
            String npcDisplayName = config.getString(npcPath + "displayname");
            String skin = config.getString(npcPath + "skin");
            World world = Bukkit.getWorld(config.getString(npcPath + "world", ""));
            int x = config.getInt(npcPath + "x");
            int y = config.getInt(npcPath + "y");
            int z = config.getInt(npcPath + "z");

            if (world == null) throw new NullPointerException("There was an issue loading one of the worlds for an NPC!");

            NPC npc;
            try {
                npc = npcRegistry.createNPC(EntityType.PLAYER, npcDisplayName);
                if (lookClose) npc.getOrAddTrait(LookClose.class).lookClose(true);
                npc.getOrAddTrait(Gravity.class).gravitate(false);
                npc.getOrAddTrait(SkinTrait.class).setSkinName(skin);
            } catch (Exception exception) {
                throw new IllegalArgumentException("There was an issue creating one of the NPCs: (" + npcName + "): " + exception.getMessage());
            }

            this.NPCs.add(new Game.ReturnNPC(npcDisplayName, npc, new Location(world, x, y, z)));
        }

        // Load items
        this.setStage(Stage.NONE);
        this.requiredItems = Utils.getItemsFromConfig(config, basepath + "required-items");
        this.starterItems = Utils.getItemStacksFromItems(Utils.getItemsFromConfig(config, basepath + "starter-items"));
    }


    /**
     * Start the game, this will spawn all the NPCs, move all the players, set up the world border, etc.
     *
     * @param plugin  The main instance of the plugin for the scheduler
     * @param players A list of players who are playing
     */
    public void start(Scavenger plugin, ArrayList<Player> players) {

        // Announce the loading
        this.announceMessage(plugin.getLang("game-loading"), sounds.get("load"));

        // Give items to players
        for (Player player : players) {
            if (player == null) continue;
            if (clearInventoryOnStart) player.getInventory().clear();
            this.giveStarterItems(player);
            this.playerReturnedItems.put(player.getUniqueId(), new ArrayList<>());
            player.setInvulnerable(true);
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(spawnPoint);
        }

        this.setStage(Stage.LOADING);

        // Spawn NPCs
        for (ReturnNPC npc : NPCs) npc.getNPC().spawn(npc.getLocation());

        // Start countdown
        this.scheduleTasks.add(new BukkitRunnable() {
            private int counter = startCountDown;

            @Override
            public void run() {
                this.counter--;
                if (counter <= 0) {
                    this.cancel();
                    startPhase2(plugin, players);
                } else if (counter == 60 || counter == gracePeriod / 2 || counter == 30 || counter == 10 || counter <= 5) {
                    announceMessage(plugin.parsePlaceholders(plugin.getLang("game-starting"), "%remaining%", counter), sounds.get("start-countdown"));
                }
            }
        }.runTaskTimer(plugin, 0, 20));
    }

    private void startPhase2(Scavenger plugin, ArrayList<Player> players) {

        this.bossbar = Bukkit.createBossBar(HexUtils.colorify(plugin.parsePlaceholders(pvpBossBarTitle, "%remaining%", gracePeriod * 60)), (pvpBossBarColour.equalsIgnoreCase("auto") ? BarColor.GREEN : BarColor.valueOf(pvpBossBarColour)), BarStyle.SEGMENTED_20);
        for (Player player : players) {
            if (player != null) { // Todo: ensure you can't abuse this
                player.setInvulnerable(false);
                if (pvpBossBarEnabled) bossbar.addPlayer(player);
            }
        }

        // Announce the start
        this.announceMessage(plugin.getLang("game-started"), sounds.get("start"));

        // Set world borders
        this.setWorldBorder(expandedBorderSize);

        this.setStage(Stage.GRACE_PERIOD);

        if (gracePeriod == 0) {
            this.enablePVP();
        } else if (gracePeriod != -1) {
            this.scheduleTasks.add(new BukkitRunnable() {
                private int counter = gracePeriod;

                @Override
                public void run() {
                    this.counter--;
                    bossbar.setTitle(HexUtils.colorify(plugin.parsePlaceholders(pvpBossBarTitle, "%remaining%", counter)));
                    bossbar.setProgress((float) counter / gracePeriod);
                    if (counter == 0) {
                        this.cancel();
                        enablePVP();
                    } else if (counter == 60 || counter == gracePeriod / 2 || counter == 30 || counter == 10 || counter <= 5) {
                        announceMessage(plugin.parsePlaceholders(plugin.getLang("pvp-enable"), "%remaining%", counter), sounds.get("pvp-countdown"));
                    }

                    if (pvpBossBarColour.equalsIgnoreCase("AUTO")) {
                        if (counter == 60) bossbar.setColor(BarColor.YELLOW);
                        if (counter == 30) bossbar.setColor(BarColor.RED);
                    }
                }
            }.runTaskTimer(plugin, 0, 20));
        }
    }

    /**
     * Enable the PVP for the game and notify the players
     */
    public void enablePVP() {
        this.setStage(Stage.PVP);
        this.pvpEnabled = true;
        this.announceMessage(plugin.getLang("pvp-enabled"), sounds.get("pvp"));
        this.bossbar.removeAll();
    }

    /**
     * Send a message to all the worlds with an optional custom sound
     *
     * @param message The message to send
     * @param sounds  An optional list of sounds
     */
    public void announceMessage(String message, CustomSound... sounds) {
        for (World world : allowedWorlds) {
            for (Player player : world.getPlayers()) {
                player.sendMessage(HexUtils.colorify(message));
                if (sounds != null && sounds.length > 0) for (CustomSound gameSound : sounds) {
                    if (gameSound != null) {
                        player.playSound(player.getLocation(), gameSound.getSound(), gameSound.getVolume(), gameSound.getPitch());
                    }
                }
            }
        }
    }

    private void setWorldBorder(int size) {
        for (World world : allowedWorlds) {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(spawnPoint);
            border.setSize(size, size / 20);
        }
    }

    private HashMap<UUID, String> cachedScores;

    /**
     * Show the scoreboard for a specific player
     *
     * @param player The player to show it to
     */
    public void updateScoreboard(Player player) {
        if (!scoreBoardEnabled) return;
        if (scoreboard == null) this.updateScoreboard();
        player.setScoreboard(scoreboard);
    }

    /**
     * Update the scoreboard for all players
     */
    public void updateScoreboard() {

        if (!scoreBoardEnabled) return;

        // Create new scoreboard if there isn't one
        Objective objective;
        if (scoreboard == null) {
            this.cachedScores = new HashMap<>();
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

            objective = scoreboard.registerNewObjective("sc_scoreboard", "dummy", HexUtils.colorify(scoreboardTitle));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        } else objective = scoreboard.getObjective("sc_scoreboard");

        List<Map.Entry<UUID, ArrayList<ItemStack>>> entries = new ArrayList<>(playerReturnedItems.entrySet());
        LinkedHashMap<UUID, Integer> sortedLeaderBoard = new LinkedHashMap<>(entries.size());

        entries.sort((e1, e2) -> {
            Integer v1 = e1.getValue().size();
            Integer v2 = e2.getValue().size();
            if (v1.equals(v2)) return 0;
            else if (v1 > v2) return -1;
            else return 1;
        });

        for (Map.Entry<UUID, ArrayList<ItemStack>> entry : entries) sortedLeaderBoard.put(entry.getKey(), entry.getValue().size());

        // (Re-)fill scoreboard with values
        int i = 1;
        for (Map.Entry<UUID, Integer> entry : sortedLeaderBoard.entrySet()) {

            Player player = Bukkit.getPlayer(entry.getKey());

            if (player == null) continue;
            if (i > scoreboardShowPlayers) break;
            if (cachedScores.get(entry.getKey()) != null) scoreboard.resetScores(cachedScores.get(entry.getKey()));

            if (entry.getValue() == 0) continue;

            String line = HexUtils.colorify(plugin.parsePlaceholders(scoreboardLineFormat, "%place%", i, "%playername%", player.getDisplayName(), "%returncount%", entry.getValue()));
            Score score = objective.getScore(line);
            score.setScore(0);
            this.cachedScores.put(entry.getKey(), line);

            i++;
        }

        // Update scoreboard for players
        for (UUID playerUUID : playerReturnedItems.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;
            player.setScoreboard(scoreboard);
        }
    }

    /**
     * Stop the game, teleport all the players back, reset world border, etc
     */
    public void stop() {
        this.setStage(Stage.RESETTING);

        // Cancel pending tasks
        for (BukkitTask task : scheduleTasks) task.cancel();

        // Despawn NPCs
        for (Game.ReturnNPC returnNPC : NPCs) {
            NPC npc = returnNPC.getNPC();
            if (npc == null || !npc.isSpawned()) continue;
            npc.despawn();
        }

        // Hide scoreboard
        for (UUID playerUUID : playerReturnedItems.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            player.setInvulnerable(true);
            if (clearInventoryOnStop) player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(spawnPoint);
            player.setInvulnerable(false);
        }

        if (bossbar != null) this.bossbar.removeAll();
        this.winners.clear();

        this.setWorldBorder(smallBorderSize);

        // Clear stored invs
        this.playerInventories.clear();
        this.setStage(Stage.NONE);
        this.pvpEnabled = false;
    }

    private final HashMap<UUID, Inventory> playerInventories = new HashMap<>();

    /**
     * Get the list of items for a player
     *
     * @param player The player to get it for
     * @return The compiled inventory
     */
    public Inventory getInventory(Player player, boolean update) {
        Inventory inventory = playerInventories.get(player.getUniqueId());

        if (inventory == null || update) {
            inventory = Bukkit.createInventory(player, returnGui.getRowCount() * 9, HexUtils.colorify(returnGui.getTitle()));

            int i = 9;
            for (Game.Item requiredItem : requiredItems) {
                Game.Item formattingItem = this.hasPlayerCompletedItem(player, requiredItem.getItemStack()) ? returnGui.getCompletedItem() : returnGui.getRegularItem();
                Game.Item formattedItem = new Game.Item(formattingItem);
                formattedItem.setDisplayName(HexUtils.colorify(plugin.parsePlaceholders(formattingItem.getDisplayName(), "%itemname%", HexUtils.colorify(requiredItem.getDisplayName() != null ? requiredItem.getDisplayName() : Utils.getItemName(requiredItem.getMaterial())), "%amount%", requiredItem.getAmount())));
                if (formattingItem.getLore() == null || formattedItem.getLore().size() == 0) formattedItem.setLore(requiredItem.getLore());
                if (formattingItem.getEnchantments() == null || formattedItem.getEnchantments().size() == 0) formattedItem.setEnchantments(requiredItem.getEnchantments());
                if (formattingItem.shouldHideEnchants() || formattedItem.shouldHideEnchants()) formattedItem.hideEnchants(true);
                inventory.setItem(i, formattedItem.getItemStack(requiredItem.getMaterial(), requiredItem.getAmount()));
                i++;
            }

            playerInventories.put(player.getUniqueId(), inventory);

        }

        return inventory;
    }

    /**
     * @return A {@link HashMap} of players' UUIDs and their updated menu inventories
     */
    public HashMap<UUID, Inventory> getPlayerInventories() {
        return playerInventories;
    }

    /**
     * Get how many of an item is needed
     *
     * @param item The item to check
     * @return How many of an item is needed or 0 if it is not required
     */
    public int getRequirementForItem(ItemStack item) {
        for (ItemStack requiredItem : Utils.getItemStacksFromItems(requiredItems)) if (requiredItem.isSimilar(item)) return requiredItem.getAmount();
        return 0;
    }

    public ArrayList<ReturnNPC> getReturnNPCs() {
        return NPCs;
    }

    /**
     * Mark add a winner to the game and end it if the requirement is met
     *
     * @param plugin The main instance of the plugin
     * @param player The player to add
     */
    public void win(Scavenger plugin, Player player) {

        this.winners.add(player.getUniqueId());

        if (winners.size() < requiredWinners) {
            this.announceMessage(plugin.parsePlaceholders(plugin.getLang("chat-announce-new-winner"), "%place%", winners.size(), "%playername%", player.getDisplayName(), "%maxwinners%", requiredWinners));
            return;
        }

        this.setStage(Stage.FINISHED);

        String message = plugin.getLang("chat-announce-all-winners");
        for (int i = 1; i < 100; i++) {
            message = message.replaceAll("%top" + i + "%", winners.size() > i - 1 ? (Bukkit.getPlayer(winners.get(i - 1)) != null ? Bukkit.getPlayer(winners.get(i - 1)).getDisplayName() : "Unknown player") : " - ");
        }

        this.announceMessage(message, sounds.get("final-win"));

        Bukkit.getScheduler().runTaskLater(plugin, this::stop, 60);
    }

    private void setStage(Stage stage) {
        this.stage = stage;

        String name;
        switch (stage) {
            case LOADING -> name = "onloading";
            case GRACE_PERIOD -> name = "onstart";
            case PVP -> name = "onpvp";
            case FINISHED -> name = "onfinish";
            default -> {
                return;
            }
        }

        ArrayList<String> commands = this.specialCommands.get(name);
        if (commands != null) {
            for (String command : commands) {
                Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
            }
        }
    }

    /**
     * Mark an item as completed for a player and update the scoreboard
     *
     * @param player The player to mark it for
     * @param item   The item to mark as completed
     */
    public void completeItem(Player player, ItemStack item) {

        ArrayList<ItemStack> modifiedItemList = playerReturnedItems.get(player.getUniqueId());
        modifiedItemList.add(item);
        this.playerReturnedItems.put(player.getUniqueId(), modifiedItemList);

        this.getInventory(player, true);
        if (scoreBoardEnabled) this.updateScoreboard();
    }

    /**
     * Check if a player exists in the game
     *
     * @param player The player to check
     * @return Whether it exists in the game
     */
    public boolean playerExists(Player player) {
        return this.playerReturnedItems.get(player.getUniqueId()) != null;
    }

    /**
     * Check if an NPC exists in the game
     *
     * @param entity The entity to check
     * @return The {@link NPC} or null in case it does not exist in the game
     */
    public NPC getNPC(Entity entity) {
        return npcRegistry.getNPC(entity);
    }

    /**
     * Check if a player has completed a specific item
     *
     * @param player The player to check for
     * @param item   The item to check
     * @return Whether the player has completed a specific item
     */
    public boolean hasPlayerCompletedItem(Player player, ItemStack item) {
        for (ItemStack storedItem : playerReturnedItems.get(player.getUniqueId())) {
            if (storedItem.getType() == item.getType() && item.isSimilar(storedItem)) return true;
        }
        return false;
    }

    /**
     * @return A list of allowed worlds for the game
     */
    public ArrayList<World> getWorlds() {
        return allowedWorlds;
    }

    public NPCRegistry getNpcRegistry() {
        return npcRegistry;
    }

    /**
     * @return Whether PVP is enabled in the world
     */
    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    /**
     * @return All the required items to complete the game
     */
    public ArrayList<Item> getRequiredItems() {
        return requiredItems;
    }

    /**
     * Get all the completed items for a player
     *
     * @param player The player to check for
     * @return The list of completed items, possibly empty or null if the player is not part of the game
     */
    public ArrayList<ItemStack> getPlayerCompletedItems(Player player) {
        return playerReturnedItems.get(player.getUniqueId());
    }

    /**
     * Get all the missing items for a player
     *
     * @param player The player to check for
     * @return A list of missing items, possibly empty
     */
    public ArrayList<ItemStack> getMissingItems(Player player) {

        ArrayList<ItemStack> missingItems = new ArrayList<>() {{
            this.addAll(Utils.getItemStacksFromItems(requiredItems));
        }};

        ArrayList<ItemStack> completedItems = new ArrayList<>(getPlayerCompletedItems(player));

        for (ItemStack completedItem : completedItems) {
            missingItems.removeIf(completedItem::isSimilar);
        }

        return missingItems;
    }

    /**
     * @return Whether the game is currently playing
     */
    public boolean isInProgress(boolean... excludeBeforeStarted) {
        if (excludeBeforeStarted != null && excludeBeforeStarted.length > 0 && excludeBeforeStarted[0]) return stage != Stage.NONE && stage != Stage.LOADING;
        return stage != Stage.NONE;
    }

    /**
     * @return Whether items should be dropped when a user is killed by another player
     */
    public boolean shouldDropItemsOnKill() {
        return dropItemsOnKill;
    }

    /**
     * @return Whether items should be dropped when a user is killed by natural causes
     */
    public boolean shouldDropItemsOnDeath() {
        return dropItemsOnDeath;
    }

    /**
     * @return Whether fire damage should be taken by players during grace period
     */
    public boolean isGracePeriodFireDamageDisabled() {
        return gracePeriodDisableFireDamage;
    }

    /**
     * @return Whether fall damage should be taken by players during grace period
     */
    public boolean isGracePeriodFallDamageDisabled() {
        return gracePeriodDisableFallDamage;
    }

    /**
     * @return The customized return GUI instance
     */
    public ReturnGui getReturnGui() {
        return returnGui;
    }

    /**
     * Represents a custom sound
     */
    public static class CustomSound {
        private final Sound sound;
        private float volume;
        private float pitch;

        public CustomSound(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        /**
         * @return The actual type of sound
         */
        public Sound getSound() {
            return sound;
        }

        /**
         * @return The volume of the sound
         */
        public float getVolume() {
            return volume;
        }

        /**
         * @return The pitch of the sound
         */
        public float getPitch() {
            return pitch;
        }

        /**
         * Set the volume of the sound to a new value
         *
         * @param volume The new value
         * @return The builder class
         */
        public CustomSound setVolume(float volume) {
            this.volume = volume;
            return this;
        }

        /**
         * Set the pitch of the sound to a new value
         *
         * @param pitch The new value
         * @return The builder class
         */
        public CustomSound setPitch(float pitch) {
            this.pitch = pitch;
            return this;
        }
    }

    /**
     * Represents a custom item
     */
    public static class Item {
        private final String materialRaw;
        private final String amountRaw;
        private int amount;
        private boolean hideEnchants;
        private String displayName;
        private Material material;
        private ArrayList<String> lore = new ArrayList<>();
        private HashMap<Enchantment, Integer> enchantments = new HashMap<>();
        private ItemMeta itemMeta;
        private ItemStack itemStack;

        /**
         * Create a new custom item
         *
         * @param materialRaw    The raw material enum or AUTO if it's parsed on the spot
         * @param displayName    The name of the item or null for none
         * @param lore           The list of lore lines or null for none
         * @param amountRaw      The amount of items this holds or AUTO if it's parsed on the spot
         * @param additionalMeta Any additional {@link ItemMeta} settings
         */
        public Item(String materialRaw, String displayName, ArrayList<String> lore, String amountRaw, ItemMeta additionalMeta, HashMap<Enchantment, Integer> enchantments, boolean hideEnchants) {
            if (!materialRaw.equalsIgnoreCase("AUTO")) this.material = Material.valueOf(materialRaw);
            if (!amountRaw.equalsIgnoreCase("AUTO")) this.amount = Integer.parseInt(amountRaw);
            this.materialRaw = materialRaw;
            this.amountRaw = amountRaw;
            if (displayName != null) this.displayName = displayName;
            if (lore != null) this.lore = lore;
            this.itemMeta = additionalMeta;
            this.enchantments = enchantments;
            this.hideEnchants = hideEnchants;
        }

        /**
         * Copy constructor
         */
        public Item(Item item) {
            this.material = item.material;
            this.materialRaw = item.materialRaw;
            this.lore = item.lore;
            this.displayName = item.displayName;
            this.amount = item.amount;
            this.amountRaw = item.amountRaw;
            this.enchantments = item.enchantments;
            this.itemMeta = item.itemMeta;
            this.itemStack = item.itemStack;
            this.hideEnchants = item.hideEnchants;
        }

        /**
         * Get the generated item and provide an amount and a material to parse in case the amount or material is set to AUTO in the config
         *
         * @param material The material to parse it with
         * @param amount   The amount the parse it with
         * @return The updated {@link ItemStack}
         */
        public ItemStack getItemStack(Material material, int amount) {
            if (materialRaw.equalsIgnoreCase("AUTO") || amountRaw.equalsIgnoreCase("AUTO")) {
                if (material != null) this.material = material;
                if (amount != 0) this.amount = amount;
                return this.getItemStack(amount != 0 || material != null);
            }

            return this.getItemStack();
        }

        /**
         * Get the generated item
         *
         * @param update Whether to update all the settings and rebuild the item
         * @return The updated {@link ItemStack}
         */
        public ItemStack getItemStack(boolean... update) {
            if (update.length > 0 && update[0] || itemStack == null) {
                this.updateProperties();
            }
            return itemStack;
        }

        /**
         * Update all the properties of the item
         *
         * @return The update item
         */
        public Game.Item updateProperties() {

            this.itemStack = new ItemStack(material == null && isMaterialAuto() ? Material.AIR : material);
            this.itemStack.setAmount(amount);

            if (itemMeta == null) this.itemMeta = itemStack.getItemMeta();

            if (amount == 0 && !isAmountAuto()) {
                try {
                    amount = Integer.parseInt(amountRaw);
                } catch (NumberFormatException exception) {
                    amount = 0;
                }
            }

            if (itemMeta != null) {
                this.itemMeta.setDisplayName(displayName);
                this.itemMeta.setLore(lore);
                if (hideEnchants) itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                this.itemStack.setItemMeta(itemMeta);
            }

            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                this.itemStack.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }

            return this;
        }

        public Material getMaterial() {
            return material;
        }

        public String getMaterialRaw() {
            return materialRaw;
        }

        /**
         * @return Whether the item's material should be parsed on spot
         */
        public boolean isMaterialAuto() {
            return materialRaw.equalsIgnoreCase("AUTO");
        }

        public boolean isAmountAuto() {
            return amountRaw.equalsIgnoreCase("AUTO");
        }

        public ArrayList<String> getLore() {
            return lore;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getAmount() {
            return amount;
        }

        public HashMap<Enchantment, Integer> getEnchantments() {
            return enchantments;
        }

        public ItemMeta getItemMeta() {
            return itemMeta;
        }

        public boolean shouldHideEnchants() {
            return hideEnchants;
        }

        public Item setMaterial(Material material) {
            this.material = material;
            return this;
        }

        public Item setLore(ArrayList<String> lore) {
            this.lore = lore;
            return this;
        }

        public Item setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Item setAmount(int amount) {
            this.amount = amount;
            return this;
        }

        public Item setEnchantments(HashMap<Enchantment, Integer> enchantments) {
            this.enchantments = enchantments;
            return this;
        }

        public Item setItemMeta(ItemMeta itemMeta) {
            this.itemMeta = itemMeta;
            return this;
        }

        public Item setItemStack(ItemStack itemStack) {
            this.itemStack = itemStack;
            return this;
        }

        public Item hideEnchants(boolean shouldHideEnchants) {
            this.hideEnchants = shouldHideEnchants;
            return this;
        }

        @Override
        public String toString() {
            return "material: " + material + " \n" +
                    "materialRaw: " + materialRaw + " \n" +
                    "lore: " + lore + " \n" +
                    "displayName: " + displayName + " \n" +
                    "amount: " + amount + " \n" +
                    "amountRaw: " + amountRaw + " \n" +
                    "enchantments: " + enchantments + " \n" +
                    "itemMeta: " + itemMeta + " \n" +
                    "itemStack: " + itemStack + " \n";
        }
    }

    /**
     * Represents a stored NPC
     */
    public record ReturnNPC(String displayName, NPC npc, Location location) {

        public String getDisplayname() {
            return displayName;
        }

        public NPC getNPC() {
            return npc;
        }

        public Location getLocation() {
            return location;
        }
    }


    public static class ReturnGui extends Gui {

        private final Item regularItem;
        private final Item completedItem;

        public ReturnGui(String title, int rowCount, Item regularItem, Item completedItem) {
            super(title, rowCount);
            this.regularItem = regularItem;
            this.completedItem = completedItem;
        }

        /**
         * Create a new {@link ReturnGui} instance from an existing one
         *
         * @param returnGui The instance to copy
         */
        public ReturnGui(ReturnGui returnGui) {
            super(returnGui.getTitle(), returnGui.rowCount);
            this.regularItem = returnGui.getRegularItem();
            this.completedItem = returnGui.getCompletedItem();
        }

        public Item getRegularItem() {
            return regularItem;
        }

        public Item getCompletedItem() {
            return completedItem;
        }
    }

    /**
     * Represents the stage of the game, <code>NONE</code> is for when it has only been loaded yet, the rest are self-explanatory.
     */
    public enum Stage {
        NONE,
        LOADING,
        GRACE_PERIOD,
        PVP,
        FINISHED,
        RESETTING
    }


}

