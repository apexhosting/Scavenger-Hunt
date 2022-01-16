package dev.geri.scavenger.entities;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;

public class Game {

    public enum Stage {
        WAITING_FOR_PLAYERS,
        LOADING,
        GRACE_PERIOD,
        PVP,
        FINISHED,
        RESETTING
    }

    private final World world;
    private boolean pvpEnabled;
    private Stage stage;

    private final ArrayList<ItemStack> requiredItems;
    private final HashMap<Player, ArrayList<ItemStack>> playerReturnedItems = new HashMap<>();

    public Game(World world, boolean isHardcore, ArrayList<ItemStack> requiredItems, ArrayList<Player> players) {
        this.stage = Stage.LOADING;
        this.world = world;
        this.requiredItems = requiredItems;
        this.pvpEnabled = false;

        for (Player player : players) {
            this.playerReturnedItems.put(player, new ArrayList<>());
        }


        this.stage = Stage.GRACE_PERIOD;

        // Todo: schedule pvp start
    }

    public void setPVP(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }


    public int getHowManyIsRequiredForItem(ItemStack itemStack) {
        for (ItemStack requiredItem : requiredItems) if (requiredItem.getType() == itemStack.getType()) return requiredItem.getAmount();
        return 0;

    }

    public void win(GameManager gameManager, Player player) {
        this.stage = Stage.FINISHED;

        for (Player p : world.getPlayers()) p.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + player.getDisplayName() + " won!", null, 30, 100, 30);

        // Todo: wait like 30 seconds and then clean up
        gameManager.cleanUp(this);
    }

    public void completeItem(Player player, ItemStack itemStack) {
        ArrayList<ItemStack> modifiedItemList = playerReturnedItems.get(player);
        modifiedItemList.add(itemStack);
        this.playerReturnedItems.put(player, modifiedItemList);
    }

    public void killPlayer(Player player) {
        playerReturnedItems.remove(player);
        // Todo: set gamemode and stuff
    }

    public World getWorld() {
        return world;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    // Todo: add a proper way of getting all this data, perhaps a way to cache it better?
    public ArrayList<ItemStack> getRequiredItems() {
        return requiredItems;
    }

    public ArrayList<ItemStack> getPlayerReturnedItems(Player player) {
        return playerReturnedItems.get(player);
    }

    public ArrayList<ItemStack> getMissingItems(Player player) {

        ArrayList<ItemStack> missingItems = new ArrayList<>() {{
            this.addAll(requiredItems);
        }};

        for (ItemStack itemStack : getPlayerReturnedItems(player)) {
            missingItems.remove(itemStack);
        }

        return missingItems;
    }

    public boolean isInProgress() {
        return stage == Stage.GRACE_PERIOD || stage == Stage.PVP;
    }
}
