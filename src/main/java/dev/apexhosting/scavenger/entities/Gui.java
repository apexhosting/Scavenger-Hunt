package dev.apexhosting.scavenger.entities;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

public class Gui {

    protected String title;
    protected int rowCount;
    protected Inventory inventory;

    /**
     * Create a new GUI instance
     *
     * @param title    The title of the menu
     * @param rowCount The amount of rows it has min 1, max 6
     */
    public Gui(String title, int rowCount) {
        this.title = title;
        this.rowCount = rowCount;
        this.inventory = updateInventory();
    }

    public Inventory getInventory(boolean... update) {
        if (update.length > 0 & update[0]) this.updateInventory();
        return inventory;
    }

    protected Inventory updateInventory() {
        this.inventory = Bukkit.createInventory(null, rowCount * 9, title);
        return inventory;
    }

    public String getTitle() {
        return title;
    }

    public int getRowCount() {
        return rowCount;
    }

    /**
     * Set the title of the gui. Warning, this will create a new {@link Inventory} object
     *
     * @param title The new title to set it to
     * @return The updated GUI
     */
    public Gui setTitle(String title) {
        this.updateInventory();
        this.title = title;
        return this;
    }


    /**
     * Set the amount of rows for the gui. Warning, this will create a new {@link Inventory} object
     *
     * @param rowCount The amount of rows to set it to
     * @return The updated GUI
     */
    public Gui setRowCount(int rowCount) {
        this.updateInventory();
        this.rowCount = rowCount;
        return this;
    }


}
