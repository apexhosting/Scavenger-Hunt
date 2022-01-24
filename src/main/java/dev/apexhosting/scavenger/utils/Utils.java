package dev.apexhosting.scavenger.utils;

import dev.apexhosting.scavenger.entities.Game;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    public static ArrayList<ItemStack> getItemStacksFromItems(ArrayList<Game.Item> items, Material material, int amount) {
        ArrayList<ItemStack> newItems = new ArrayList<>();
        for (Game.Item item : items) newItems.add(item.getItemStack(material, amount));
        return newItems;
    }

    public static ArrayList<ItemStack> getItemStacksFromItems(ArrayList<Game.Item> items) {
        return getItemStacksFromItems(items, null, 0);
    }

    /**
     * Get a list of items from the config
     *
     * @param config   The instance of the config
     * @param basePath The path of the items
     * @return A list of {@link Game.Item}-ss
     * @throws Exception In case any of the settings are invalid
     */
    public static ArrayList<Game.Item> getItemsFromConfig(FileConfiguration config, String basePath) throws Exception {
        ArrayList<Game.Item> items = new ArrayList<>();

        for (String itemName : config.getConfigurationSection(basePath).getKeys(false)) {

            // Basic item settings
            String itemPath = basePath + "." + itemName + ".";
            String material = "";
            String name = null;
            String amountRaw = "1";
            boolean hideEnchants = false;
            ArrayList<String> lore = new ArrayList<>();
            HashMap<Enchantment, Integer> enchantments = new HashMap<>();

            // Book-only
            String author = null;
            BookMeta.Generation generation = null;
            ArrayList<String> pages = new ArrayList<>();

            // Compass-only
            Location pointTo = null;

            for (Map.Entry<String, Object> setting : config.getConfigurationSection(itemPath).getValues(false).entrySet()) {
                String value = setting.getValue().toString();
                switch (setting.getKey().toLowerCase()) {
                    case "material" -> material = value;
                    case "amount" -> amountRaw = value;
                    case "name" -> name = HexUtils.colorify(value);
                    case "hide-enchants" -> hideEnchants = Boolean.parseBoolean(value);
                    case "lore" -> {
                        for (String loreLine : config.getStringList(itemPath + ".lore")) lore.add(HexUtils.colorify(loreLine));
                    }
                    case "enchants" -> {
                        for (Map.Entry<String, Object> enchant : config.getConfigurationSection(itemPath + ".enchants").getValues(false).entrySet()) enchantments.put(Enchantment.getByKey(NamespacedKey.minecraft(enchant.getKey())), Integer.parseInt(enchant.getValue().toString()));
                    }

                    case "point-to" -> {
                        pointTo = new Location(Bukkit.getWorld(config.getString(itemPath + ".point-to.world")), config.getDouble(itemPath + ".point-to.x"), config.getDouble(itemPath + ".point-to.y"), config.getDouble(itemPath + ".point-to.z"));
                    }

                    case "author" -> author = HexUtils.colorify(value);
                    case "pages" -> {
                        for (Map.Entry<String, Object> entry : config.getConfigurationSection(itemPath + ".pages").getValues(false).entrySet()) {
                            String page = entry.getValue().toString();
                            pages.add(HexUtils.colorify(page));
                        }

                        if (pages.size() > 50) throw new IllegalArgumentException("Books must be less than 100 pages long!");
                    }
                    case "type" -> generation = BookMeta.Generation.valueOf(value.toUpperCase());
                }
            }

            // Apply settings
            ItemStack itemStack = new ItemStack(material.equalsIgnoreCase("AUTO") ? Material.AIR : Material.valueOf(material));
            itemStack.setAmount(amountRaw.equalsIgnoreCase("AUTO") ? -1 : Integer.parseInt(amountRaw));

            if (material.equalsIgnoreCase(Material.COMPASS.name())) {
                CompassMeta compassMeta = (CompassMeta) itemStack.getItemMeta();
                if (compassMeta != null && pointTo != null) {
                    compassMeta.setLodestoneTracked(false);
                    compassMeta.setLodestone(pointTo);
                    itemStack.setItemMeta(compassMeta);
                }
            }

            if (material.equalsIgnoreCase(Material.WRITTEN_BOOK.name())) {
                BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
                if (bookMeta != null) {
                    bookMeta.setAuthor(author);
                    bookMeta.setPages(pages);
                    bookMeta.setGeneration(generation);
                    bookMeta.setTitle(name);
                    itemStack.setItemMeta(bookMeta);
                }
            }

            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                if (name != null && name.length() > 0) meta.setDisplayName(name);
                if (lore.size() > 0) meta.setLore(lore);
                if (hideEnchants) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // Add it to the list
            items.add(new Game.Item(material, name, lore, amountRaw, meta, enchantments, hideEnchants));
        }

        return items;
    }

}