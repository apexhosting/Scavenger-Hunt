package dev.apexhosting.scavenger.utils;

import dev.apexhosting.scavenger.Scavenger;
import dev.apexhosting.scavenger.entities.Game;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class EventListener implements Listener {

    private final Scavenger plugin;
    private final HashMap<UUID, ArrayList<ItemStack>> checkedItems;
    private Game game;

    public EventListener(Scavenger plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
        this.checkedItems = new HashMap<>();
    }

    public void updateGame(Game game) {
        this.game = game;
        this.checkedItems.clear();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player killed = e.getEntity();
        Player killer = e.getEntity().getKiller();

        if (!game.isInProgress()) return;
        if (!game.playerExists(killed)) return;

        if (killer != null) { // Killed
            if (!game.shouldDropItemsOnKill()) {
                e.setKeepInventory(true);
                e.setKeepLevel(true);
                e.setDroppedExp(0);
                e.getDrops().clear();
                return;
            }
        } else if (!game.shouldDropItemsOnDeath()) { // Died
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.setDroppedExp(0);
            e.getDrops().clear();
            return;
        }

        // Remove starter items
        if (game.shouldKeepStarterItems()) {
            e.getDrops().removeIf(itemStack -> itemStack.getItemMeta() != null && itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "starter-item"), PersistentDataType.INTEGER));
        }

    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();

        if (!game.isInProgress()) return;
        if (!game.playerExists(player)) return;
        if (!game.shouldKeepStarterItems()) return;

        ItemMeta itemMeta = e.getItemDrop().getItemStack().getItemMeta();
        if (itemMeta == null) return;

        if (itemMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, "starter-item"), PersistentDataType.INTEGER)) {
            player.sendMessage(plugin.getLang("starter-item-remove-fail"));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        if (!game.isInProgress()) return;
        if (!game.playerExists(player)) return;

        if (game.shouldKeepStarterItems()) {
            if (player.getInventory().isEmpty()) this.game.giveStarterItems(player);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!game.isInProgress()) return;
        if (!game.playerExists(player)) return;
        if (game.isPvpEnabled()) return;

        switch (e.getCause()) {
            case FIRE, FIRE_TICK -> {
                if (game.isGracePeriodFireDamageDisabled()) {
                    e.setDamage(0);
                    e.setCancelled(true);
                }
            }

            case FALL -> {
                e.setDamage(0);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler // Handle fishing rods
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getHitEntity() instanceof Player attacked)) return;
        if (!(e.getEntity() instanceof FishHook hook)) return;
        if (!(hook.getShooter() instanceof Player attacker)) return;

        if (!game.playerExists(attacked)) return;
        if (game.isPvpEnabled()) return;

        hook.remove();
        attacker.sendMessage(plugin.getLang("no-pvp"));
    }

    @EventHandler // Still handle fishing rods
    public void onPlayerFish(PlayerFishEvent e) {
        if (!(e.getCaught() instanceof Player attacked)) return;
        Player attacker = e.getPlayer();

        if (!game.playerExists(attacked)) return;
        if (game.isPvpEnabled()) return;

        e.getHook().remove();
        e.setCancelled(true);

        attacker.sendMessage(plugin.getLang("no-pvp"));
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player attacked)) return;

        Entity damager = e.getDamager();

        // Open GUI if it's an NPC
        if (game.getNPC(e.getEntity()) != null && damager instanceof Player player) {
            if (!game.isInProgress(true)) {
                player.sendMessage(plugin.getLang("not-in-progress"));
                return;
            }

            player.openInventory(game.getInventory(player, false));
            e.setCancelled(true);
            return;
        }

        if (!game.playerExists(attacked)) return;
        if (game.isPvpEnabled()) return;

        // Melee
        if (damager instanceof Player attacker) {
            attacker.sendMessage(plugin.getLang("no-pvp"));
            e.setCancelled(true);
            return;
        }

        // Exotic stuff
        if (damager instanceof EnderCrystal) e.setCancelled(true);
        if (damager instanceof AreaEffectCloud) e.setCancelled(true);
        if (damager instanceof TNTPrimed) e.setCancelled(true);

        // Potions
        if (damager instanceof ThrownPotion potion) {
            if (potion.getShooter() instanceof Player attacker) {
                attacker.sendMessage(plugin.getLang("no-pvp"));
                e.setCancelled(true);
                return;
            }
        }

        // Fireworks
        if (damager instanceof Firework firework) {
            if (firework.getShooter() instanceof Player attacker) {
                attacker.sendMessage(plugin.getLang("no-pvp"));
                e.setCancelled(true);
                return;
            }
        }

        // Arrows, tridents, etc
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player attacker) {
                attacker.sendMessage(plugin.getLang("no-pvp"));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler // Handle explosions and such
    public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
        if (!(e.getEntity() instanceof Player attacked)) return;
        if (e.getDamager() == null) return;

        if (!game.playerExists(attacked)) return;
        if (game.isPvpEnabled()) return;

        if (e.getDamager().getType() == Material.TNT || e.getDamager().getType() == Material.END_CRYSTAL) {
            e.setCancelled(true);
        }
    }


    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        if (!(e.getSource().getHolder() instanceof Player player)) return;

        if (!game.playerExists(player)) return;
        if (!game.isInProgress()) return;

        if (!game.getPlayerInventories().containsValue(e.getDestination())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;

        if (!game.playerExists(player)) return;
        if (!game.isInProgress(true)) return;

        if (!game.getPlayerInventories().containsValue(e.getInventory())) return;

        e.setCancelled(true);
    }


    @EventHandler
    public void onCraftItem(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!game.playerExists(player)) return;
        if (!game.isInProgress(true)) return;

        ItemStack item = e.getCurrentItem();
        if (item == null) return;

        int required = game.getRequirementForItem(item);
        if (checkedItems.get(player.getUniqueId()) == null) this.checkedItems.put(player.getUniqueId(), new ArrayList<>());
        if (required == item.getAmount() && !game.hasPlayerCompletedItem(player, item) && !checkedItems.get(player.getUniqueId()).contains(item)) {
            player.sendMessage(plugin.parsePlaceholders(plugin.getLang("item-craft-notification"), "%itemname%", Utils.getItemName(item.getType())));
            this.game.playSound(player, "item-craft-notification");

            // Update list
            ArrayList<ItemStack> updatedList = checkedItems.get(player.getUniqueId());
            updatedList.add(item);
            this.checkedItems.put(player.getUniqueId(), updatedList);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!game.playerExists(player)) return;
        if (!game.isInProgress(true)) return;

        ItemStack item = e.getItem().getItemStack();
        int required = game.getRequirementForItem(item);
        if (checkedItems.get(player.getUniqueId()) == null) this.checkedItems.put(player.getUniqueId(), new ArrayList<>());
        if (required == item.getAmount() && !game.hasPlayerCompletedItem(player, item) && !checkedItems.get(player.getUniqueId()).contains(item)) {
            player.sendMessage(plugin.parsePlaceholders(plugin.getLang("item-pickup-notification"), "%itemname%", Utils.getItemName(item.getType())));
            this.game.playSound(player, "item-pickup-notification");

            ArrayList<ItemStack> updatedList = checkedItems.get(player.getUniqueId());
            updatedList.add(item);
            this.checkedItems.put(player.getUniqueId(), updatedList);
        }

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        if (e.getItem() == null || e.getItem().getItemMeta() == null) return;
        if (!e.getItem().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "items-item"), PersistentDataType.INTEGER)) return;

        Player player = e.getPlayer();

        if (!player.isSneaking()) return;
        if (!game.isInProgress(true) || !game.playerExists(e.getPlayer())) {
            player.sendMessage(plugin.getLang("not-in-progress"));
            return;
        }

        player.openInventory(game.getInventory(player, false));
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;

        NPC npc = game.getNPC(e.getRightClicked());
        if (npc == null) return;

        Player player = e.getPlayer();

        if (!game.isInProgress(true)) {
            player.sendMessage(plugin.getLang("not-in-progress"));
            e.setCancelled(true);
            return;
        }

        if (!game.playerExists(player)) return;

        e.setCancelled(true);

        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (itemStack.getType() == Material.AIR || player.isSneaking()) {
            player.openInventory(game.getInventory(player, false));
            return;
        }

        if (game.hasPlayerCompletedItem(player, itemStack)) {
            player.sendMessage(plugin.getLang("item-already-provided"));
            return;
        }

        int requiredAmount = game.getRequirementForItem(itemStack);

        if (requiredAmount == 0) {
            player.sendMessage(plugin.getLang("incorrect-item-provided"));
            return;
        }

        if (itemStack.getAmount() < requiredAmount) {
            player.sendMessage(plugin.parsePlaceholders(plugin.getLang("item-not-enough"), "%required%", requiredAmount, "%amount%", itemStack.getAmount()));
            return;
        }

        if (itemStack.getAmount() == requiredAmount) {
            player.getInventory().remove(player.getInventory().getItemInMainHand());
        } else {
            itemStack.setAmount(itemStack.getAmount() - requiredAmount);
        }

        this.game.completeItem(player, itemStack);

        ArrayList<ItemStack> updatedMissingItems = game.getMissingItems(player);

        player.sendMessage(plugin.parsePlaceholders(plugin.getLang("item-accepted"), "%current%", game.getRequiredItems().size() - updatedMissingItems.size(), "%total%", game.getRequiredItems().size()));

        if (updatedMissingItems.size() == 0) {
            game.win(plugin, player);
            return;
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (ItemStack missingItem : updatedMissingItems) {
            if (missingItem.getAmount() > 1) sb.append(missingItem.getAmount()).append("x ");
            sb.append(Utils.getItemName(missingItem.getType()));
            i++;
            if (i < updatedMissingItems.size()) sb.append(", ");
        }

        player.sendMessage(plugin.parsePlaceholders(plugin.getLang("items-left"), "%items%", sb));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        if (!game.isInProgress()) return;
        if (!game.playerExists(player)) {
            player.setGameMode(GameMode.SPECTATOR);
            return;
        }

        this.game.updateScoreboard(player);
    }

}
