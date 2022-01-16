package dev.geri.scavenger;

import dev.geri.scavenger.entities.Game;
import dev.geri.scavenger.entities.GameManager;
import dev.geri.scavenger.utils.Database;
import dev.geri.scavenger.utils.HexUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class Scavenger extends JavaPlugin implements Listener, TabCompleter {

    private final Logger logger = Bukkit.getLogger();

    private ArrayList<Block> returnBlockCache;
    private FileConfiguration config;
    private Database database;
    private GameManager gameManager;

    private HashMap<String, ArrayList<ItemStack>> requiredItems = new HashMap<>();

    @Override
    public void onEnable() {

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);

        // Register commands
        PluginCommand command = this.getCommand("scavenger");
        if (command != null) command.setExecutor(this);

        // Load db and game manager utils
        this.database = new Database(this, logger);
        this.gameManager = new GameManager(this, logger);

        // Load config settings
        this.reload();
    }

    private void reload() {
        this.reloadConfig();
        this.saveDefaultConfig();
        this.config = this.getConfig();
        this.requiredItems = new HashMap<>();

        // Get all the predefined required items — Todo: rewrite to not be ugly awful inefficient
        for (String templateName : config.getConfigurationSection("templates").getKeys(false)) {

            ArrayList<ItemStack> itemStacks = new ArrayList<>();

            for (String itemName : config.getConfigurationSection("templates." + templateName).getKeys(false)) {

                Material material = null;
                String name = null;
                int amount = 1;
                HashMap<Enchantment, Integer> enchantments = new HashMap<>();

                for (Map.Entry<String, Object> setting : config.getConfigurationSection("templates." + templateName + "." + itemName).getValues(false).entrySet()) {

                    String value = setting.getValue().toString();

                    switch (setting.getKey().toLowerCase()) {

                        case "material" -> material = Material.valueOf(value);
                        case "amount" -> amount = Integer.parseInt(value);
                        case "name" -> name = value;
                        case "enchants" -> {
                            for (Map.Entry<String, Object> enchant : config.getConfigurationSection("templates." + templateName + "." + itemName + ".enchants").getValues(false).entrySet()) {
                                enchantments.put(Enchantment.getByKey(NamespacedKey.minecraft(enchant.getKey())), Integer.parseInt(enchant.getValue().toString()));
                            }
                        }


                    }

                }

                if (material == null) {
                    logger.severe("Unable to load a setting: " + itemName);
                    return;
                }

                ItemStack itemStack = new ItemStack(material);
                itemStack.setAmount(amount);

                ItemMeta meta = itemStack.getItemMeta();
                meta.setDisplayName(name);
                itemStack.setItemMeta(meta);

                for (Map.Entry<Enchantment, Integer> enchantmentEntry : enchantments.entrySet()) itemStack.addEnchantment(enchantmentEntry.getKey(), enchantmentEntry.getValue());


                itemStacks.add(itemStack);

            }

            requiredItems.put(templateName, itemStacks);
        }

        // Get the saved blocks
        try {
            this.returnBlockCache = database.getReturnBlocks();
        } catch (SQLException | ClassNotFoundException exception) {
            logger.warning("There was an error loading the return blocks: " + exception.getMessage());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();

        if (block == null || !returnBlockCache.contains(block)) return;

        e.setCancelled(true); // Todo: check for gamemode, etc

        Game game = gameManager.getPendingGame(player.getWorld());

        if (game == null || !game.isInProgress()) {
            player.sendMessage(getLang("game-not-in-progress"));
            return;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (itemStack.getType() == Material.AIR) {
            player.sendMessage(getLang("no-item-provided"));
            return;
        }

        ArrayList<ItemStack> missingItems = game.getMissingItems(player);

        boolean isMissing = false;

        for (ItemStack missingItem : missingItems) {
            if (missingItem.getType() == itemStack.getType()) {
                isMissing = true;
                break;
            }
        }

        if (!isMissing) { // Todo: Ensure missing != got it already
            player.sendMessage(getLang("incorrect-item-provided"));
            return;
        }

        if (game.getRequiredItems().contains(itemStack)) player.getInventory().remove(itemStack);
        else {
            int required = game.getHowManyIsRequiredForItem(itemStack);

            if (required == 0) {
                player.sendMessage(getLang("incorrect-item-provided"));
                return;
            }

            if (itemStack.getAmount() < required) {
                player.sendMessage(getLang("item-not-enough")
                        .replaceAll("%required%", String.valueOf(required))
                        .replaceAll("%amount%", String.valueOf(itemStack.getAmount()))
                );
                return;
            }

            itemStack = new ItemStack(itemStack.getType(), required);

            player.getInventory().removeItem(itemStack);
        }

        game.completeItem(player, itemStack);

        ArrayList<ItemStack> updatedMissingItems = game.getMissingItems(player);

        player.sendMessage(getLang("item-accepted")
                .replaceAll("%current%", String.valueOf(game.getRequiredItems().size() - updatedMissingItems.size()))
                .replaceAll("%total%", String.valueOf(game.getRequiredItems().size()))
        );

        if (updatedMissingItems.size() == 0) {
            game.win(gameManager, player);
            return;
        }

        StringBuilder sb = new StringBuilder(); // Todo actually implement this as customizable and not ugly
        for (ItemStack missingItem : updatedMissingItems) sb.append(missingItem.getType().name()).append("(").append(missingItem.getAmount()).append("x), ");

        player.sendMessage(getLang("items-left").replaceAll("%items%", sb.toString()));

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(getLang("help-lines", true));
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "cheat" -> {
                if (sender instanceof Player player) {

                    if (args.length == 1) {
                        player.sendMessage(ChatColor.RED + " You must provide a name to cheat, such as: test_items");  // debug
                        return true;
                    }

                    for (ItemStack itemStack : requiredItems.get(args[1])) player.getInventory().addItem(itemStack);

                } else sender.sendMessage(getLang("player-only"));
            }

            case "start" -> { // Todo: add a way to schedule this instead

                World world;

                if (sender instanceof Player player) {
                    world = player.getWorld();
                } else {
                    if (args.length < 2 || Bukkit.getWorld(args[1]) == null) {
                        sender.sendMessage(getLang("no-world-provided"));
                        return true;
                    } else {
                        world = Bukkit.getWorld(args[1]);
                    }
                }


                if (gameManager.getPendingGame(world) != null) {
                    sender.sendMessage(getLang("game-in-progress"));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + " You must provide a name to start, such as: test_items"); // debug
                    return true;
                }

                Game game = new Game(world, false, requiredItems.get(args[1]), new ArrayList<>(world.getPlayers()));
                gameManager.addGame(world, game);

                sender.sendMessage(getLang("game-started"));
            }

            case "reload" -> {
                this.reload();
                sender.sendMessage(getLang("reload"));
            }

            case "addblock" -> {
                if (sender instanceof Player player) {
                    Block block = player.getTargetBlock(null, 5);

                    if (block.getType() == Material.AIR) {
                        player.sendMessage(getLang("invalid-block"));
                        return true;

                    } else {

                        if (returnBlockCache.contains(block)) {
                            player.sendMessage(getLang("block-already-added"));
                            return true;
                        }

                        try {
                            database.addReturnBlock(block);
                        } catch (SQLException | ClassNotFoundException exception) {
                            player.sendMessage(getLang("error"));
                            logger.severe("There was an error adding a return block to the database: " + exception.getMessage());
                            return true;
                        }

                        player.sendMessage(getLang("block-added"));
                        this.returnBlockCache.add(block);
                    }

                } else sender.sendMessage(getLang("player-only"));
            }

            case "removeblock" -> {
                if (sender instanceof Player player) {
                    Block block = player.getTargetBlock(null, 5);

                    if (block.getType() == Material.AIR) {
                        player.sendMessage(getLang("invalid-block"));
                        return true;

                    } else {

                        if (returnBlockCache.contains(block)) {
                            player.sendMessage(getLang("block-not-added"));
                            return true;
                        }

                        try {
                            database.removeReturnBlock(block);
                        } catch (SQLException | ClassNotFoundException exception) {
                            player.sendMessage(getLang("error"));
                            logger.severe("There was an error removing a return spot to the database: " + exception.getMessage());
                            return true;
                        }

                        player.sendMessage(getLang("block-removed"));
                        this.returnBlockCache.add(block);
                    }

                } else sender.sendMessage(getLang("player-only"));
            }
        }

        return true;
    }

    private final ArrayList<String> arguments = new ArrayList<>() {{
        this.add("cheat");
        this.add("start");
        this.add("reload");
        this.add("addblock");
        this.add("removeblock");
    }};

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> result = new ArrayList<>();

        if (args.length == 1) {
            for (String argument : arguments) if (argument.toLowerCase().startsWith(args[0].toLowerCase())) result.add(argument);
            return result;
        }

        return null;
    }

    public String getLang(String key, boolean... isList) {

        String rawLang;

        if (isList != null && isList.length > 0) {
            List<String> rawList = config.getStringList("lang." + key);

            if (rawList.size() == 0) rawLang = "&cError loading message...";
            else {
                StringBuilder sb = new StringBuilder();
                for (String rawLine : rawList) sb.append(rawLine).append("\n");
                rawLang = sb.toString();
            }
        } else {
            rawLang = config.getString("lang." + key, "&cError loading message...");
        }

        return HexUtils.colorify(rawLang);
    }

}
