package dev.geri.scavenger;

import dev.geri.scavenger.entities.Game;
import dev.geri.scavenger.entities.GameManager;
import dev.geri.scavenger.utils.Database;
import dev.geri.scavenger.utils.EventListener;
import dev.geri.scavenger.utils.HexUtils;
import dev.geri.scavenger.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Scavenger extends JavaPlugin implements Listener, TabCompleter {

    private final Logger logger = Bukkit.getLogger();

    private FileConfiguration config;
    private GameManager gameManager;
    private Database database;

    @Override
    public void onEnable() {

        // Register commands
        PluginCommand command = this.getCommand("scavenger");
        if (command != null) command.setExecutor(this);

        // Load db and game manager utils
        this.database = new Database(this, logger);
        this.gameManager = new GameManager(this, logger);

        // Load config settings
        boolean loadSuccess = this.reload();

        if (!loadSuccess) return;

        // Register events
        Bukkit.getPluginManager().registerEvents(new EventListener(this, gameManager), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        // Debug: Hacky way to see what's being called
        /*        final Listener listener = new Listener() {
            @EventHandler
            public void onEvent(Event e) {
                if (e instanceof PlayerMoveEvent) return;
                if (e instanceof ChunkLoadEvent) return;
                Bukkit.broadcastMessage(e.getEventName());
            }
        };

        RegisteredListener registeredListener = new RegisteredListener(listener, (listener1, event) -> {
            try {
                listener1.getClass().getDeclaredMethod("onEvent", Event.class).invoke(listener1, event);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }, EventPriority.NORMAL, this, false);
        for (HandlerList handler : HandlerList.getHandlerLists()) handler.register(registeredListener);*/

    }

    @Override
    public void onDisable() {
        this.gameManager.cleanUpEverything();
    }

    private boolean reload() {
        this.reloadConfig();
        this.saveDefaultConfig();
        this.config = this.getConfig();

        // Load all the pre-configured games
        try {
            this.gameManager.loadGames(config);
        } catch (Exception exception) {
            logger.severe("There was an issue loading one or more of the configuration settings: " + exception.getMessage());
            //Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }

        return true;
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(parsePlaceholders(getLang("help-lines", true), "%command%", label));
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "cheat" -> {
                if (sender instanceof Player player) {
                    Game game = gameManager.getPendingGame(player);

                    if (game == null) {
                        sender.sendMessage(getLang("game-not-in-progress"));
                        return true;
                    }

                    for (ItemStack itemStack : Utils.getItemStacksFromItems(game.getRequiredItems())) player.getInventory().addItem(itemStack);

                } else sender.sendMessage(getLang("player-only"));
            }

            case "stop" -> {
                if (sender instanceof Player player) {
                    Game game = gameManager.getPendingGame(player);

                    if (game == null) {
                        sender.sendMessage(getLang("game-not-in-progress"));
                        return true;
                    }

                    this.gameManager.cleanUp(game);
                    sender.sendMessage(getLang("game-stopped"));

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

                if (world == null) {
                    sender.sendMessage(getLang("no-world-provided"));
                    return true;
                }

                if (gameManager.getPendingGame(world) != null) {
                    sender.sendMessage(getLang("game-in-progress"));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + " You must provide a name to start, such as: my_game"); // debug
                    return true;
                }

                Game game = new Game(gameManager.getLoadedGames().get(args[1]));
                game.start(this, new ArrayList<>(world.getPlayers().stream().filter(player -> !player.hasMetadata("NPC")).collect(Collectors.toList())));
                gameManager.addGame(game);
            }

            case "reload" -> {
                if (reload()) sender.sendMessage(getLang("reload"));
                else sender.sendMessage(getLang("reload-fail"));
            }

            default -> sender.sendMessage(getLang("unknown-command"));

        }


        return true;
    }

    private final ArrayList<String> baseCommandArguments = new ArrayList<>() {{
        this.add("help");
        this.add("cheat");
        this.add("start");
        this.add("stop");
        this.add("reload");
    }};


    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, @Nonnull String[] args) {
        if (!(sender instanceof Player)) return null;

        ArrayList<String> result = new ArrayList<>();

        if (args.length == 1) {
            for (String argument : baseCommandArguments) if (argument.toLowerCase().startsWith(args[0].toLowerCase())) result.add(argument);
            return result;
        } else {

            if (args.length < 1) return null;

            switch (args[0].toLowerCase()) {
                case "start" -> {
                    for (String argument : gameManager.getLoadedGames().keySet()) if (argument.toLowerCase().startsWith(args[1].toLowerCase())) result.add(argument);
                    return result;
                }
            }

        }

        return null;
    }

    /**
     * Get a specific language setting from the config and parse its colours
     *
     * @param key    The key in the config, without 'lang.'
     * @param isList Whether it's a string list
     * @return The finished string or the error message if it's not found
     */
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

    /**
     * Parse a list of placeholders to a text
     *
     * @param s            The text
     * @param placeholders A list of placeholders and their value, such as <code>%placeholder1%, value1, %placeholder2%, value2</code>
     * @return The parsed text
     */
    public String parsePlaceholders(String s, Object... placeholders) {
        for (int i = 0; i < placeholders.length; i = i + 2) {
            s = s.replaceAll(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
        }
        return s;
    }

}
