package dev.geri.scavenger.entities;

import dev.geri.scavenger.utils.HexUtils;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    // Settings
    private final int requiredPlayers;
    private final boolean hardcore;
    private final boolean dropItemsOnKill;
    private final boolean dropItemsOnDeath;
    private final boolean scoreBoardEnabled;
    private final int scoreboardShowPlayers;
    private final int gracePeriod;
    private final int borderSize;
    private final int requiredWinners;
    private final String id;
    private final String displayName;
    private final String scoreboardTitle;
    private final Location spawnPoint;
    private final Location worldBorderCenter;
    private final ArrayList<Game.ReturnNPC> npcs;
    private final ArrayList<World> allowedWorlds;
    private final ArrayList<ItemStack> requiredItems;
    private final ArrayList<ItemStack> starterItems;

    private Stage stage;
    private Scoreboard scoreboard;
    private boolean pvpEnabled;
    private final HashMap<Player, ArrayList<ItemStack>> playerReturnedItems = new HashMap<>();

    /**
     * Create a new game from a template
     */
    public Game(int requiredPlayers, boolean hardcore, boolean dropItemsOnKill, boolean dropItemsOnDeath, boolean scoreBoardEnabled, int scoreboardShowPlayers, int gracePeriod, int borderSize, int requiredWinners, String id, String displayName, String scoreboardTitle, Location spawnPoint, Location worldBorderCenter, ArrayList<ReturnNPC> npcs, ArrayList<World> allowedWorlds, ArrayList<ItemStack> requiredItems, ArrayList<ItemStack> starterItems) {
        this.requiredPlayers = requiredPlayers;
        this.hardcore = hardcore;
        this.dropItemsOnKill = dropItemsOnKill;
        this.dropItemsOnDeath = dropItemsOnDeath;
        this.scoreBoardEnabled = scoreBoardEnabled;
        this.scoreboardShowPlayers = scoreboardShowPlayers;
        this.gracePeriod = gracePeriod;
        this.borderSize = borderSize;
        this.requiredWinners = requiredWinners;
        this.id = id;
        this.displayName = displayName;
        this.scoreboardTitle = scoreboardTitle;
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
     * Start the game, this will spawn all the NPCs, move all the players, set up the world border, etc.
     *
     * @param plugin  The main instance of the plugin for the scheduler
     * @param players A list of players who are playing
     */
    public void start(JavaPlugin plugin, ArrayList<Player> players) {
        this.stage = Stage.LOADING;

        // Spawn NPCs
        for (Game.ReturnNPC npc : npcs) npc.getNPC().spawn(npc.getLocation());

        // Set world borders
        for (World world : allowedWorlds) {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(worldBorderCenter);
            border.setSize(borderSize);
        }

        for (Player player : players) {
            for (ItemStack starterItem : starterItems) player.getInventory().addItem(starterItem);
            this.playerReturnedItems.put(player, new ArrayList<>());
        }

        this.stage = Stage.GRACE_PERIOD;

        if (gracePeriod == 0) this.enablePVP();
        else if (gracePeriod != -1) Bukkit.getScheduler().runTaskLater(plugin, this::enablePVP, gracePeriod * 1200L);
    }

    /**
     * Enable the PVP for the game and notify the players
     */
    public void enablePVP() {
        this.pvpEnabled = true;
        this.announceMessage(null, "&cPVP is now enabled!", 30, 60, 30, Sound.ENTITY_ENDER_DRAGON_GROWL); // Todo: make sound customizable
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

    private HashMap<Player, String> cachedScores;

    /**
     * Update the scoreboard for all players
     */
    public void updateScoreboard() {

        // Todo: make scoreboard lines customizable

        Objective objective;

        if (scoreboard == null) {
            this.cachedScores = new HashMap<>();
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

            objective = scoreboard.registerNewObjective("sc_scoreboard", "dummy", HexUtils.colorify(scoreboardTitle));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        } else objective = scoreboard.getObjective("sc_scoreboard");

        int i = 0;
        for (Map.Entry<Player, ArrayList<ItemStack>> entry : playerReturnedItems.entrySet()) {

            if (i > scoreboardShowPlayers) break;

            if (cachedScores.get(entry.getKey()) != null) scoreboard.resetScores(cachedScores.get(entry.getKey()));

            String line = entry.getKey().getName() + " — " + entry.getValue().size();
            Score score = objective.getScore(line);
            this.cachedScores.put(entry.getKey(), line);
            score.setScore(0);

            i++;
        }

        // Update scoreboard for players
        for (Player player : playerReturnedItems.keySet()) player.setScoreboard(scoreboard);
    }

    /**
     * Stop the game, teleport all the players back, reset world border, etc
     */
    public void stop() {

        // Hide scoreboard
        for (Player player : playerReturnedItems.keySet()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            player.teleport(spawnPoint);
        }

        // Remove NPCs
        for (Game.ReturnNPC returnNPC : npcs) {
            NPC npc = returnNPC.getNPC();

            if (npc == null || !npc.isSpawned()) continue;

            Location location = npc.getEntity().getLocation();
            location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location, 500, 0, 1.5, 0);
            location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 10, 2);

            npc.destroy();
        }
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

        Bukkit.getScheduler().runTaskLater(plugin, () -> gameManager.cleanUp(this), 60);
    }

    /**
     * Mark an item as completed for a player and update the scoreboard
     *
     * @param player The player to mark it for
     * @param item   The item to mark as completed
     */
    public void completeItem(Player player, ItemStack item) {

        ArrayList<ItemStack> modifiedItemList = playerReturnedItems.get(player);
        modifiedItemList.add(item);
        this.playerReturnedItems.put(player, modifiedItemList);

        this.updateScoreboard();
    }

    /**
     * Check if a player exists in the game
     *
     * @param player The player to check
     * @return Whether it exists in the game
     */
    public boolean playerExists(Player player) {
        return this.playerReturnedItems.get(player) != null;
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
        for (ItemStack storedItem : playerReturnedItems.get(player)) if (storedItem.getType() == item.getType() && item.isSimilar(storedItem)) return true;
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
        return playerReturnedItems.get(player);
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
