package dev.apexhosting.scavenger.utils;

import dev.apexhosting.scavenger.entities.Game;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    case "material" -> material = value;
                    case "amount" -> amountRaw = value;
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
                            String page = entry.getValue().toString();
                            //if (page.length() > 256) throw new IllegalArgumentException("Books must have less than 256 characters per page, page number: #" + entry.getKey() + ", item: " + itemName);
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

            if (material.equalsIgnoreCase(Material.WRITTEN_BOOK.name())) {

                ArrayList<BaseComponent[]> convertedPages = new ArrayList<>();

                // Todo: make this actually work — maybe not

                for (String page : pages) {
                    ComponentBuilder cb = new ComponentBuilder();
                    StringBuilder previousComponent = new StringBuilder();
                    for (String word : page.split(" ")) {

                        if (word.startsWith("<hover=\"")) { // If there are no spaces V
                            if (word.endsWith("</hover>")) cb.append(parseHoverComponentes(HexUtils.colorify(word, true))).append(" ").reset();
                            else previousComponent.append(word).append(" ");
                        } else {
                            if (previousComponent.length() == 0) cb.append(word).append(" "); // Regular text
                            else { // If tag is closed
                                if (word.endsWith("</hover>")) {
                                    cb.append(parseHoverComponentes(HexUtils.colorify(previousComponent.append(word).toString(), true))).append(" ").reset();
                                    previousComponent.setLength(0);
                                } else {
                                    previousComponent.append(word).append(" ");
                                }
                            }
                        }
                    }

                    convertedPages.add(cb.create());
                }

                BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
                if (bookMeta != null) {
                    bookMeta.setAuthor(author);
                    bookMeta.spigot().setPages(convertedPages);
                    bookMeta.setGeneration(generation);
                    bookMeta.setTitle(name);
                    itemStack.setItemMeta(bookMeta);
                }
            }

            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                if (name != null && name.length() > 0) meta.setDisplayName(name);
                if (lore.size() > 0) meta.setLore(lore);
                if (hideEnchantments) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // Add it to the list
            items.add(new Game.Item(material, name, lore, amountRaw, meta, enchantments));
        }

        return items;
    }

    private static BaseComponent[] parseHoverComponentes(String text) {
        Pattern pattern = Pattern.compile("=\"(\n)?.*\">");
        Matcher matcher = pattern.matcher(text);
        String hover = "";
        if (matcher.find()) hover = matcher.group().replaceAll("^=\"|\">$", "");

        text = text.replaceAll("^<hover=\".*\">|</hover>$", "");
        return new ComponentBuilder(HexUtils.colorify(text, true)).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover))).create();
    }

}