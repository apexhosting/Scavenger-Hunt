package dev.geri.scavenger.utils;

import dev.geri.scavenger.entities.Game;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class Utils {

    /**
     * @param material The item to get the name of
     * @return The formatted name of the item
     */
    public static String getItemName(Material material) {
        StringBuilder itemName = new StringBuilder();
        int i = 0;
        String[] nameRaw = material.name().toLowerCase().split("_");
        for (String itemNamePart : nameRaw) {
            itemName.append(StringUtils.capitalize(itemNamePart));
            i++;
            if (i < nameRaw.length) itemName.append(" ");
        }
        return itemName.toString();
    }

    /**
     * Convert a list of {@link Game.Item} into actual {@link ItemStack}-s
     *
     * @param items The list of items to convert
     * @return The converted list of {@link ItemStack}-s
     */
    public static ArrayList<ItemStack> getItemStacksFromItems(ArrayList<Game.Item> items, Material... material) {
        ArrayList<ItemStack> newItems = new ArrayList<>();
        for (Game.Item item : items) newItems.add(item.getItemStack(material.length > 0 && material[0] != null ? material[0] : null));
        return newItems;
    }
}