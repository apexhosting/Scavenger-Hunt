package dev.geri.scavenger.entities;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.logging.Logger;

public class GameManager {

    private final Plugin plugin;
    private final Logger logger;

    public GameManager(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    private final HashMap<World, Game> pendingGames = new HashMap<>();

    public void removeGame(Game game) {
        pendingGames.remove(game.getWorld());
    }

    public HashMap<World, Game> getPendingGames() {
        return pendingGames;
    }

    public Game getPendingGame(World world) {
        return pendingGames.get(world);
    }

    public void addGame(World world, Game game) {
        this.pendingGames.put(world, game);
    }

    // Currently doing: add proper game management —
    public void cleanUp(Game game) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.removeGame(game);
            Bukkit.broadcastMessage("Game finished!"); // debug
        }, 200);
    }
}
