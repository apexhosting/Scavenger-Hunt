package dev.geri.scavenger.entities;

import dev.geri.scavenger.Scavenger;
import dev.geri.scavenger.utils.HexUtils;
import dev.geri.scavenger.utils.Utils;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
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

    /**
     * Represents the stage of the game, <code>NONE</code> is for when it has only been loaded yet, the rest are self-explanatory.
     */
    public enum Stage {
        NONE,
        WAITING_FOR_PLAYERS,
        LOADING,
        GRACE_PERIOD,
        PVP,
        FINISHED,
        RESETTING
    }

    private final Scavenger plugin;
    private BossBar bar;

    // Settings
    private final int requiredPlayers;
    private final boolean hardcore;
    private final boolean dropItemsOnKill;
    private final boolean dropItemsOnDeath;
    private final boolean scoreBoardEnabled;
    private final boolean pvpBossBarEnabled;
    private final int scoreboardShowPlayers;
    private final int gracePeriod;
    private final int borderSize;
    private final int requiredWinners;
    private final String id;
    private final String displayName;
    private final String scoreboardTitle;
    private final String pvpBossBarTitle;
    private final String pvpBossBarColour;
    private final String scoreboardLineFormat;
    private final Location spawnPoint;
    private final Location worldBorderCenter;
    private final ArrayList<Game.ReturnNPC> npcs;
    private final ArrayList<World> allowedWorlds;
    private final ArrayList<ItemStack> requiredItems;
    private final ArrayList<ItemStack> starterItems;

    private Stage stage;
    private Scoreboard scoreboard;
    private boolean pvpEnabled;
    private final HashMap<UUID, ArrayList<ItemStack>> playerReturnedItems = new HashMap<>();

    private final ArrayList<BukkitTask> scheduleTasks = new ArrayList<>();

    /**
     * Create a new game from a template
     */
    public Game(Scavenger plugin, int requiredPlayers, boolean hardcore, boolean dropItemsOnKill, boolean dropItemsOnDeath, boolean scoreBoardEnabled, boolean pvpBossBarEnabled, int scoreboardShowPlayers, int gracePeriod, int borderSize, int requiredWinners, String id, String displayName, String scoreboardTitle, String pvpBossBarTitle, String pvpBossBarColour, String scoreboardLineFormat, Location spawnPoint, Location worldBorderCenter, ArrayList<ReturnNPC> npcs, ArrayList<World> allowedWorlds, ArrayList<ItemStack> requiredItems, ArrayList<ItemStack> starterItems) {
        this.plugin = plugin;
        this.requiredPlayers = requiredPlayers;
        this.hardcore = hardcore;
        this.dropItemsOnKill = dropItemsOnKill;
        this.dropItemsOnDeath = dropItemsOnDeath;
        this.scoreBoardEnabled = scoreBoardEnabled;
        this.pvpBossBarEnabled = pvpBossBarEnabled;
        this.scoreboardShowPlayers = scoreboardShowPlayers;
        this.gracePeriod = gracePeriod;
        this.borderSize = borderSize;
        this.requiredWinners = requiredWinners;
        this.id = id;
        this.displayName = displayName;
        this.scoreboardTitle = scoreboardTitle;
        this.pvpBossBarTitle = pvpBossBarTitle;
        this.pvpBossBarColour = pvpBossBarColour;
        this.scoreboardLineFormat = scoreboardLineFormat;
        this.spawnPoint = spawnPoint;
        this.worldBorderCenter = worldBorderCenter;
        this.npcs = npcs;
        this.allowedWorlds = allowedWorlds;
        this.requiredItems = requiredItems;
        this.starterItems = starterItems;

        this.stage = Stage.NONE;
        this.pvpEnabled = false;
    }

    /**
     * Create a new game from a template
     *
     * @param game A new Game object
     */
    public Game(Game game) {
        this.plugin = game.plugin;
        this.requiredPlayers = game.requiredPlayers;
        this.hardcore = game.hardcore;
        this.dropItemsOnKill = game.dropItemsOnKill;
        this.dropItemsOnDeath = game.dropItemsOnDeath;
        this.scoreBoardEnabled = game.scoreBoardEnabled;
        this.pvpBossBarEnabled = game.pvpBossBarEnabled;
        this.scoreboardShowPlayers = game.scoreboardShowPlayers;
        this.gracePeriod = game.gracePeriod;
        this.borderSize = game.borderSize;
        this.requiredWinners = game.requiredWinners;
        this.id = game.id;
        this.displayName = game.displayName;
        this.scoreboardTitle = game.scoreboardTitle;
        this.pvpBossBarTitle = game.pvpBossBarTitle;
        this.pvpBossBarColour = game.pvpBossBarColour;
        this.scoreboardLineFormat = game.scoreboardLineFormat;
        this.spawnPoint = game.spawnPoint;
        this.worldBorderCenter = game.worldBorderCenter;
        this.npcs = game.npcs;
        this.allowedWorlds = game.allowedWorlds;
        this.requiredItems = game.requiredItems;
        this.starterItems = game.starterItems;
        this.stage = Stage.NONE;
        this.pvpEnabled = false;
    }

    /**
     * Start the game, this will spawn all the NPCs, move all the players, set up the world border, etc.
     *
     * @param plugin  The main instance of the plugin for the scheduler
     * @param players A list of players who are playing
     */
    public void start(Scavenger plugin, ArrayList<Player> players) {
        this.stage = Stage.LOADING;

        // Spawn NPCs
        for (Game.ReturnNPC npc : npcs) npc.getNPC().spawn(npc.getLocation());

        // Give items to players
        for (Player player : players) {
            if (player == null) continue;
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 150, 1, false, false, false));
            for (ItemStack starterItem : starterItems) player.getInventory().addItem(starterItem);
            this.playerReturnedItems.put(player.getUniqueId(), new ArrayList<>());
            player.setInvulnerable(true);
            player.teleport(spawnPoint);
        }

        // Start countdown
        this.scheduleTasks.add(new BukkitRunnable() {
            private int counter = 6; // todo

            @Override
            public void run() {
                this.counter--;
                if (counter != 0) announceMessage(plugin.parsePlaceholders(plugin.getLang("game-starting"), "%remaining%", counter), Sound.BLOCK_NOTE_BLOCK_CHIME); // todo
                else {
                    this.cancel();
                    startPhase2(plugin, players);
                }
            }
        }.runTaskTimer(plugin, 0, 2));
    }

    private void startPhase2(Scavenger plugin, ArrayList<Player> players) {

        bar = Bukkit.createBossBar(HexUtils.colorify(plugin.parsePlaceholders(pvpBossBarTitle, "%remaining%", gracePeriod * 60)), (pvpBossBarColour.equalsIgnoreCase("auto") ? BarColor.GREEN : BarColor.valueOf(pvpBossBarColour)), BarStyle.SEGMENTED_20);
        for (Player player : players)
            if (player != null) {
                player.setInvulnerable(false);
                if (pvpBossBarEnabled) bar.addPlayer(player);
            }

        this.announceMessage(plugin.getLang("game-started"), Sound.ENTITY_ENDER_DRAGON_GROWL);

        // Set world borders
        this.setWorldBorder(borderSize);

        this.stage = Stage.GRACE_PERIOD;


        if (gracePeriod == 0) {
            this.enablePVP();
        } else if (gracePeriod != -1) {
            this.scheduleTasks.add(new BukkitRunnable() {
                private final int original = gracePeriod * 60;
                private int counter = original; // todo

                @Override
                public void run() {
                    this.counter--;
                    bar.setTitle(HexUtils.colorify(plugin.parsePlaceholders(pvpBossBarTitle, "%remaining%", gracePeriod * 60)));
                    bar.setProgress((float) counter / original);
                    if (counter == 0) {
                        this.cancel();
                        enablePVP();
                    } else if (counter == 60 || counter == original / 2 || counter == 30 || counter == 10 || counter <= 5) {
                        announceMessage(plugin.parsePlaceholders(plugin.getLang("pvp-enable"), "%remaining%", counter), Sound.BLOCK_NOTE_BLOCK_PLING); // todo
                    }

                    if (pvpBossBarColour.equalsIgnoreCase("auto+")) {
                        if (counter == 60) bar.setColor(BarColor.YELLOW);
                        if (counter == 30) bar.setColor(BarColor.RED);
                    }
                }
            }.runTaskTimer(plugin, 0, 2));
        }
    }

    /**
     * Enable the PVP for the game and notify the players
     */
    public void enablePVP() {
        this.pvpEnabled = true;
        this.announceMessage(plugin.getLang("pvp-enabled"), Sound.ENTITY_ENDER_DRAGON_GROWL);
        this.bar.removeAll();
    }

    /**
     * Show a message to all the game's players with a custom sound
     *
     * @param title    The main message
     * @param subtitle The secondary message
     * @param fadeIn   The amount of ticks to fade the message in
     * @param stay     The amount of ticks to show the message for
     * @param fadeOut  The amount of tickets to fade the message out
     * @param sounds   A list of sounds to play for all playery
     */
    public void announceMessage(String title, String subtitle, int fadeIn, int stay, int fadeOut, Sound... sounds) {

        // Todo: allow for custom pitch and volume

        if (title == null) title = " ";
        if (subtitle == null) subtitle = " ";

        for (World world : allowedWorlds) {
            for (Player player : world.getPlayers()) {
                player.sendTitle(HexUtils.colorify(title), HexUtils.colorify(subtitle), fadeIn, stay, fadeOut);
                if (sounds != null && sounds.length > 0) for (Sound sound : sounds) player.playSound(player.getLocation(), sound, 1, 1);
            }
        }

    }

    // todo docs
    public void announceMessage(String message, Sound... sounds) {
        for (World world : allowedWorlds) {
            for (Player player : world.getPlayers()) {
                player.sendMessage(HexUtils.colorify(message));
                if (sounds != null && sounds.length > 0) for (Sound sound : sounds) player.playSound(player.getLocation(), sound, 1, 1);
            }
        }
    }

    private void setWorldBorder(int size) {
        for (World world : allowedWorlds) {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(worldBorderCenter);
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
        if (scoreboard == null) this.updateScoreboard();
        player.setScoreboard(scoreboard);
    }

    /**
     * Update the scoreboard for all players
     */
    public void updateScoreboard() {

        // Todo: make scoreboard lines customizable

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

        // Cancel pending tasks
        for (BukkitTask task : scheduleTasks) task.cancel();

        // Hide scoreboard
        for (UUID playerUUID : playerReturnedItems.keySet()) {

            Player player = Bukkit.getPlayer(playerUUID);
            // todo? does it have to use the cached results instead of World#getPlayers??
            if (player == null) continue;
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            player.setInvulnerable(true);
            player.teleport(spawnPoint);
            player.setInvulnerable(false);
        }

        this.bar.removeAll();

        this.setWorldBorder(20); // todo: perhaps have the spawn point and the world border center be the same to be safe?

        // Remove NPCs
        for (Game.ReturnNPC returnNPC : npcs) {
            NPC npc = returnNPC.getNPC();

            if (npc == null || !npc.isSpawned()) continue;

            Location location = npc.getEntity().getLocation();
            location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location, 500, 0, 1.5, 0);
            location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 10, 2);

            npc.destroy();
        }

        // Clear stored invs
        playerInventories.clear();
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
            // Todo add customization for this
            inventory = Bukkit.createInventory(player, 27, HexUtils.colorify("&#40B350&lDoug &r&8— &#B94A4D&oMissing Required Items"));

            int i = 9;
            for (ItemStack item : requiredItems) {
                if (hasPlayerCompletedItem(player, item)) {

                    item = new ItemStack(item);

                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(HexUtils.colorify("&a&lCompleted! &8— &f" + item.getAmount() + "x " + Utils.getItemName(item.getType())));
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        item.setItemMeta(meta);
                    }

                    item.addUnsafeEnchantment(Enchantment.MENDING, 1);
                    item.setType(Material.LIME_DYE);
                }

                inventory.setItem(i, item);
                i++;
            }

            playerInventories.put(player.getUniqueId(), inventory);

        }

        return inventory;
    }

    // Todo docs
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
        for (ItemStack requiredItem : requiredItems) if (requiredItem.getType() == item.getType()) return requiredItem.getAmount();
        return 0;
    }

    /**
     * Mark the game as won by a specific user // Todo: rewrite so there can be several winners
     *
     * @param plugin
     * @param gameManager
     * @param player
     */
    public void win(JavaPlugin plugin, GameManager gameManager, Player player) {
        this.stage = Stage.FINISHED;

        this.announceMessage("&6&l" + player.getDisplayName() + " won!", null, 30, 100, 30);

        // Todo: Ensure the stop there only gets called if it's actually being stopped
        Bukkit.getScheduler().runTaskLater(plugin, () -> gameManager.cleanUp(this), 60);
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
        this.updateScoreboard();
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
     * @param npc The NPC to check
     * @return Whether it exists in the game
     */
    public boolean npcExists(NPC npc) {
        for (ReturnNPC returnNPC : npcs) if (npc == returnNPC.getNPC()) return true;
        return false;
    }

    /**
     * Check if a player has completed a specific item
     *
     * @param player The player to check for
     * @param item   The item to check
     * @return Whether the player has completed a specific item
     */
    public boolean hasPlayerCompletedItem(Player player, ItemStack item) {
        for (ItemStack storedItem : playerReturnedItems.get(player.getUniqueId())) if (storedItem.getType() == item.getType() && item.isSimilar(storedItem)) return true;
        return false;
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

    /**
     * @return A list of allowed worlds for the game
     */
    public ArrayList<World> getWorlds() {
        return allowedWorlds;
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
    public ArrayList<ItemStack> getRequiredItems() {
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
            this.addAll(requiredItems);
        }};

        for (ItemStack itemStack : getPlayerCompletedItems(player)) missingItems.remove(itemStack);

        return missingItems;
    }

    /**
     * @return Whether the game is currently playing
     */
    public boolean isInProgress() {
        return stage == Stage.GRACE_PERIOD || stage == Stage.PVP;
    }

    /**
     * @return The ID/name of the game template
     */
    public String getId() {
        return id;
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

    @Override
    public String toString() {
        return "requiredPlayers: " + requiredPlayers + "\n" +
                "hardcore: " + hardcore + "\n" +
                "dropItemsOnKill: " + dropItemsOnKill + "\n" +
                "dropItemsOnDeath: " + dropItemsOnDeath + "\n" +
                "scoreBoardEnabled: " + scoreBoardEnabled + "\n" +
                "scoreboardShowPlayers: " + scoreboardShowPlayers + "\n" +
                "gracePeriod: " + gracePeriod + "\n" +
                "borderSize: " + borderSize + "\n" +
                "requiredWinners: " + requiredWinners + "\n" +
                "id: " + id + "\n" +
                "displayName: " + displayName + "\n" +
                "scoreboardTitle: " + scoreboardTitle + "\n" +
                "spawnPoint: " + spawnPoint + "\n" +
                "worldBorderCenter: " + worldBorderCenter + "\n" +
                "npcs: " + npcs + "\n" +
                "allowedWorlds: " + allowedWorlds + "\n" +
                "requiredItems: " + requiredItems + "\n" +
                "playerReturnedItems: " + playerReturnedItems + "\n" +
                "stage: " + stage + "\n" +
                "scoreboard: " + scoreboard + "\n" +
                "pvpEnabled: " + pvpEnabled + "\n";
    }

}
