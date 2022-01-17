package dev.geri.scavenger.entities;

import dev.geri.scavenger.utils.HexUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class GameManager {

    private final Plugin plugin;
    private final Logger logger;

    public GameManager(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    private final HashMap<String, Game> pendingGames = new HashMap<>();
    private final HashMap<String, Game> loadedGames = new HashMap<>();

    /**
     * Load all the predefined games from the configuration
     *
     * @param config The configuration to load it from
     * @throws Exception In case any of the config settings are wrong
     */
    public void loadGames(FileConfiguration config) throws Exception {

        // Stop all running games, in case it was a /game reload
        for (Game pendingGame : pendingGames.values()) pendingGame.stop();
        this.pendingGames.clear();
        this.loadedGames.clear();

        String basePath = "games";

        // Get all games from the config
        for (String gameName : config.getConfigurationSection(basePath).getKeys(false)) {

            final String gamePath = basePath + "." + gameName + ".";

            // Get basic config settings
            int requiredPlayers = config.getInt(gamePath + "required-players");
            boolean hardcore = config.getBoolean(gamePath + "hardcore");
            boolean dropItemsOnKill = config.getBoolean(gamePath + "drop-items.on-pvp-kill");
            boolean dropItemsOnDeath = config.getBoolean(gamePath + "drop-items.on-natural-death");
            boolean scoreBoardEnabled = config.getBoolean(gamePath + "scoreboard.enabled");

            int scoreboardShowPlayers = config.getInt(gamePath + "scoreboard.show-players");
            int gracePeriod = config.getInt(gamePath + "grace-period");
            int borderSize = config.getInt(gamePath + "border-size");
            int requiredWinners = config.getInt(gamePath + "winners");

            String displayName = config.getString(gamePath + "displayname");
            String scoreboardTitle = config.getString(gamePath + "scoreboard.title");
            Location spawnPoint = new Location(Bukkit.getWorld(config.getString(gamePath + "spawnpoint.world")), config.getInt(gamePath + "spawnpoint.x"), config.getInt(gamePath + "spawnpoint.y"), config.getInt(gamePath + "spawnpoint.z"));
            Location worldBorderCenter = new Location(null, config.getInt(gamePath + "border-center.x"), 0, config.getInt(gamePath + "border-center.x"));

            ArrayList<World> allowedWorlds = new ArrayList<>() {{
                for (String worldName : config.getStringList(gamePath + "worlds")) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) this.add(world);
                    else throw new NullPointerException("There was an issue loading one of the worlds: " + worldName);
                }
            }};

            ArrayList<Game.ReturnNPC> npcs = new ArrayList<>() {{
                for (String npcName : config.getConfigurationSection(gamePath + "return-npcs").getKeys(false)) {

                    // Basic NPC settings
                    String npcPath = gamePath + "return-npcs." + npcName + ".";
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
                        npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, npcDisplayName);
                        if (lookClose) npc.getOrAddTrait(LookClose.class).lookClose(true);
                        npc.getOrAddTrait(SkinTrait.class).setSkinName(skin);
                    } catch (Exception exception) {
                        throw new IllegalArgumentException("There was an issue creating one of the NPCs: (" + npcName + "): " + exception.getMessage());
                    }

                    // https://github.com/CitizensDev/Citizens2/blob/master/main/src/main/java/net/citizensnpcs/trait/
                    // https://wiki.citizensnpcs.co/API#Making_an_NPC_Move

                    this.add(new Game.ReturnNPC(npcDisplayName, npc, new Location(world, x, y, z)));
                }
            }};

            // Load items
            ArrayList<ItemStack> requiredItems = this.getItemsFromConfig(config, gamePath + "required-items");
            ArrayList<ItemStack> starterItems = this.getItemsFromConfig(config, gamePath + "starter-items");

            Game game = new Game(requiredPlayers, hardcore, dropItemsOnKill, dropItemsOnDeath, scoreBoardEnabled, scoreboardShowPlayers, gracePeriod, borderSize, requiredWinners, gameName.replaceAll("\\s", "_").toLowerCase(), displayName, scoreboardTitle, spawnPoint, worldBorderCenter, npcs, allowedWorlds, requiredItems, starterItems);

            loadedGames.put(gameName, game);
        }
    }

    private ArrayList<ItemStack> getItemsFromConfig(FileConfiguration config, String basePath) {
        ArrayList<ItemStack> items = new ArrayList<>();

        for (String itemName : config.getConfigurationSection(basePath).getKeys(false)) {

            // Basic item settings
            String itemPath = basePath + "." + itemName + ".";
            Material material = null;
            String name = null;
            int amount = 1;
            boolean hideEnchantments = false;
            ArrayList<String> lore = new ArrayList<>();
            HashMap<Enchantment, Integer> enchantments = new HashMap<>();

            // Book-only
            String author = null;
            BookMeta.Generation generation = null;
            ArrayList<String> pages = new ArrayList<>();

            for (Map.Entry<String, Object> setting : config.getConfigurationSection(itemPath).getValues(false).entrySet()) {
                String value = setting.getValue().toString();
                switch (setting.getKey().toLowerCase()) {
                    case "material" -> material = Material.valueOf(value);
                    case "amount" -> amount = Integer.parseInt(value);
                    case "name" -> name = HexUtils.colorify(value);
                    case "hide-enchants" -> hideEnchantments = Boolean.parseBoolean(value);
                    case "lore" -> {
                        for (String loreLine : config.getStringList(itemPath + ".lore")) lore.add(HexUtils.colorify(loreLine));
                    }
                    case "enchants" -> {
                        for (Map.Entry<String, Object> enchant : config.getConfigurationSection(itemPath + ".enchants").getValues(false).entrySet()) enchantments.put(Enchantment.getByKey(NamespacedKey.minecraft(enchant.getKey())), Integer.parseInt(enchant.getValue().toString()));
                    }

                    case "author" -> author = HexUtils.colorify(value);

                    case "pages" -> {
                        for (Map.Entry<String, Object> entry : config.getConfigurationSection(itemPath + ".pages").getValues(false).entrySet()) {
                            String page = HexUtils.colorify(entry.getValue().toString());
                            if (page.length() > 256) throw new IllegalArgumentException("Books must have less than 256 characters per page, page number: #" + entry.getKey() + ", item: " + itemName);
                            pages.add(page);
                        }

                        if (pages.size() > 100) throw new IllegalArgumentException("Books must be less than 100 pages long!");
                    }

                    case "type" -> generation = BookMeta.Generation.valueOf(value.toUpperCase());
                }
            }

            // Double-check material
            if (material == null) throw new IllegalArgumentException(itemName + " has an invalid material!");

            // Apply settings
            ItemStack itemStack = new ItemStack(material);
            itemStack.setAmount(amount);

            if (material == Material.WRITTEN_BOOK) {
                BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
                bookMeta.setAuthor(author);
                bookMeta.setPages(pages);
                bookMeta.setGeneration(generation);
                bookMeta.setTitle(name);
                itemStack.setItemMeta(bookMeta);
            }

            ItemMeta meta = itemStack.getItemMeta();
            if (name != null && name.length() > 0) meta.setDisplayName(name);
            if (lore.size() > 0) meta.setLore(lore);
            if (hideEnchantments) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemStack.setItemMeta(meta);

            // Add enchants
            for (Map.Entry<Enchantment, Integer> enchantmentEntry : enchantments.entrySet()) itemStack.addUnsafeEnchantment(enchantmentEntry.getKey(), enchantmentEntry.getValue());

            // Add it to the list
            items.add(itemStack);
        }

        return items;
    }

    /**
     * Mark a loaded game as pending/in progress
     *
     * @param game The game to mark
     */
    public void addGame(Game game) {
        this.pendingGames.put(game.getId(), game);
    }

    /**
     * Mark a loaded game as no longer pending
     *
     * @param game The game to mark
     */
    public void removeGame(Game game) {
        pendingGames.remove(game.getId());
    }

    /**
     * Mark a game as completed and clean it up
     *
     * @param game The game to process
     */
    public void cleanUp(Game game) {
        game.stop();
        this.removeGame(game);
        Bukkit.broadcastMessage("Game finished!"); // debug
    }

    /**
     * @return All the loaded games (not necessarily in progress)
     */
    public HashMap<String, Game> getLoadedGames() {
        return this.loadedGames;
    }

    /**
     * @return Get all in progress/pending games
     */
    public HashMap<String, Game> getPendingGames() {
        return this.pendingGames;
    }

    /**
     * Get a pending game from a player
     *
     * @param player The player to check for
     * @return A pending game or null if the player isn't in a pending games
     */
    public Game getPendingGame(Player player) {
        for (Game pendingGame : pendingGames.values()) if (pendingGame.playerExists(player)) return pendingGame;
        return null;
    }

    /**
     * Get a pending game from a world
     *
     * @param world The world to check for
     * @return A pending game or null if the world has no pending games
     */
    public Game getPendingGame(World world) {
        for (Game pendingGame : pendingGames.values()) if (pendingGame.getWorlds().contains(world)) return pendingGame;
        return null;
    }

    /**
     * Loop through all the games and clean everything up
     */
    public void cleanUpEverything() {
        // Todo: Perhaps this should not be called onDisable to ensure people don't get tp-d
        for (Game pendingGame : pendingGames.values()) this.cleanUp(pendingGame);
    }
}
