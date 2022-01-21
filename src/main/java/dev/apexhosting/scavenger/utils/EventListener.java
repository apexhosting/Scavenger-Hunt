package dev.apexhosting.scavenger.utils;

import dev.apexhosting.scavenger.Scavenger;
import dev.apexhosting.scavenger.entities.Game;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class EventListener implements Listener {

    private final Scavenger plugin;
    private Game game;

    public EventListener(Scavenger plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    public void updateGame(Game game) {
        this.game = game;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player killed = e.getEntity();
        Player killer = e.getEntity().getKiller();

        if (!game.isInProgress()) return;
        if (!game.playerExists(killed)) return;

        if (killer != null) {
            if (!game.shouldDropItemsOnKill()) {
                e.setKeepInventory(true);
                e.setKeepLevel(true);
                e.setDroppedExp(0);
                e.getDrops().clear();
            }
        } else if (!game.shouldDropItemsOnDeath()) {
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.setDroppedExp(0);
            e.getDrops().clear();
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
        if (!game.isInProgress()) return;

        if (!game.getPlayerInventories().containsValue(e.getInventory())) return;

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

        if (plugin.getDescription().getVersion().startsWith("1.0")) player.sendMessage(HexUtils.colorify("\n&4&lWARNING&f: &cThis server is currently using an alpha version of &4Scavenger&c. Here be dragons!\n&c &c"));
        if (!game.playerExists(player) || !game.isInProgress()) return;

        this.game.updateScoreboard(player);
    }

}
