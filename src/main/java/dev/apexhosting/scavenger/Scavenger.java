package dev.apexhosting.scavenger;

import dev.apexhosting.scavenger.entities.Game;
import dev.apexhosting.scavenger.utils.EventListener;
import dev.apexhosting.scavenger.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Scavenger extends JavaPlugin implements Listener, TabCompleter {

    private final Logger logger = Bukkit.getLogger();

    private Game game;
    private FileConfiguration config;
    private EventListener eventListener;

    @Override
    public void onEnable() {

        // Register commands
        PluginCommand scavengerCommand = this.getCommand("scavenger");
        if (scavengerCommand != null) scavengerCommand.setExecutor(this);
        PluginCommand itemsCommand = this.getCommand("items");
        if (itemsCommand != null) itemsCommand.setExecutor(this);

        // Load config settings and data
        this.eventListener = new EventListener(this, game);
        if (!this.reload()) return;

        // Register events
        Bukkit.getPluginManager().registerEvents(eventListener, this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (game == null) return;
        this.game.getNpcRegistry().deregisterAll();
        if (game.isInProgress()) game.stop();
    }

    private boolean reload() {
        this.reloadConfig();
        this.saveDefaultConfig();
        this.config = this.getConfig();

        if (game != null) game.stop();

        // Load all the pre-configured games
        try {
            this.game = new Game(this, config);
            this.eventListener.updateGame(game);
        } catch (Exception exception) {
            logger.severe("There was an issue loading one or more of the configuration settings: " + exception.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }

        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        else {
            sender.sendMessage(getLang("permission"));
            return false;
        }
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, String[] args) {

        // Handle /items
        if (command.getName().equalsIgnoreCase("items")) {
            if (sender instanceof Player player) {
                if (!this.hasPermission(sender, "scavenger.player.items")) return true;

                if (!game.isInProgress(true) || !game.playerExists(player)) {
                    sender.sendMessage(parsePlaceholders(getLang("game-not-in-progress"), "%command%", label));
                    return true;
                }

                player.openInventory(game.getInventory(player, false));

                return true;
            } else sender.sendMessage(getLang("player-only"));
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            if (!hasPermission(sender, "scavenger.help")) return true;
            sender.sendMessage(parsePlaceholders(getLang("help-lines", true), "%command%", label));
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "cheat" -> {
                if (sender instanceof Player player) {

                    if (!hasPermission(sender, "scavenger.admin.cheat")) return true;

                    if (!game.isInProgress() || !game.playerExists(player)) {
                        sender.sendMessage(parsePlaceholders(getLang("game-not-in-progress"), "%command%", label));
                        return true;
                    }

                    for (Game.Item item : game.getRequiredItems()) {
                        player.getInventory().addItem(item.getItemStack());
                    }

                } else sender.sendMessage(getLang("player-only"));
            }

            case "stop" -> {
                if (sender instanceof Player player) {

                    if (!hasPermission(sender, "scavenger.admin.stop")) return true;

                    if (!game.isInProgress()) {
                        sender.sendMessage(parsePlaceholders(getLang("game-not-in-progress"), "%command%", label));
                        return true;
                    }

                    if (!game.playerExists(player)) {
                        sender.sendMessage(parsePlaceholders(getLang("game-not-in-progress"), "%command%", label));
                        return true;
                    }

                    this.game.stop();
                    sender.sendMessage(getLang("game-stopped"));

                } else sender.sendMessage(getLang("player-only"));
            }

            case "start" -> {
                if (!hasPermission(sender, "scavenger.admin.start")) return true;

                if (game.isInProgress()) {
                    sender.sendMessage(parsePlaceholders(getLang("game-in-progress"), "%command%", label));
                    return true;
                }

                ArrayList<Player> players = new ArrayList<>();
                this.game.getWorlds().forEach(world -> players.addAll(world.getPlayers().stream().filter(p -> !p.hasMetadata("NPC")).collect(Collectors.toList())));
                this.game.start(this, players);
            }

            case "reload" -> {
                if (!hasPermission(sender, "scavenger.admin.reload")) return true;
                if (reload()) sender.sendMessage(getLang("reload"));
                else sender.sendMessage(getLang("reload-fail"));
            }

            default -> {
                if (hasPermission(sender, "scavenger.admin.help")) sender.sendMessage(parsePlaceholders(getLang("unknown-command"), "%command%", label));
            }

        }

        return true;
    }

    private final HashMap<String, String> baseCommandArguments = new HashMap<>() {{
        this.put("help", "help");
        this.put("cheat", "cheat");
        this.put("start", "start");
        this.put("stop", "stop");
        this.put("reload", "reload");
    }};


    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, @Nonnull String[] args) {
        if (!(sender instanceof Player)) return null;

        ArrayList<String> result = new ArrayList<>();

        if (args.length == 1) {
            for (Map.Entry<String, String> argument : baseCommandArguments.entrySet()) {
                if (sender.hasPermission(argument.getKey())) {
                    if (argument.getValue().toLowerCase().startsWith(args[0].toLowerCase())) result.add(argument.getKey());
                }
            }
            return result;
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
