package dev.geri.scavenger.utils;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;

public class Utils {

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

}
