package dev.geri.scavenger.utils;

import dev.geri.scavenger.Scavenger;
import dev.geri.scavenger.entities.Game;
import dev.geri.scavenger.entities.GameManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class EventListener implements Listener {

    private final Scavenger plugin;
    private final GameManager gameManager;

    public EventListener(Scavenger plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player killed = e.getEntity();
        Player killer = e.getEntity().getKiller();

        Game game = gameManager.getPendingGame(killed);
        if (game == null) return;

        if (killer != null && !game.shouldDropItemsOnKill() || !game.shouldDropItemsOnDeath()) {
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.setDroppedExp(0);
            e.getDrops().clear();
        }
    }

    @EventHandler // Handle fishing rods
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getHitEntity() instanceof Player attacked)) return;
        if (!(e.getEntity() instanceof FishHook hook)) return;
        if (!(hook.getShooter() instanceof Player attacker)) return;

        Game game = gameManager.getPendingGame(attacked);
        if (game == null) return;
        if (game.isPvpEnabled()) return;

        hook.remove();
        attacker.sendMessage(plugin.getLang("no-pvp"));
    }

    @EventHandler // Still handle fishing rods
    public void onPlayerFish(PlayerFishEvent e) {
        if (!(e.getCaught() instanceof Player attacked)) return;

        Game game = gameManager.getPendingGame(attacked);
        Player attacker = e.getPlayer();
        if (game == null) return;
        if (game.isPvpEnabled()) return;

        e.getHook().remove();
        e.setCancelled(true);

        attacker.sendMessage(plugin.getLang("no-pvp"));
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player attacked)) return;

        Game game = gameManager.getPendingGame(attacked);
        if (game == null) return;
        if (game.isPvpEnabled()) return;

        Entity damager = e.getDamager();

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

        Game game = gameManager.getPendingGame(attacked);
        if (game == null) return;
        if (game.isPvpEnabled()) return;

        if (e.getDamager().getType() == Material.TNT || e.getDamager().getType() == Material.END_CRYSTAL) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        if (!e.getRightClicked().hasMetadata("NPC")) return;

        Player player = e.getPlayer();
        Game game = gameManager.getPendingGame(player);
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(e.getRightClicked());
        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (game == null || !game.npcExists(npc)) return;

        if (!game.isInProgress()) {
            player.sendMessage(plugin.getLang("game-not-in-progress"));
            return;
        }

        if (itemStack.getType() == Material.AIR) {
            player.sendMessage(plugin.getLang("no-item-provided"));
            return;
        }

        if (game.hasPlayerCompletedItem(player, itemStack)) {
            player.sendMessage(plugin.getLang("item-already-provided"));
            return;
        }

        if (!game.getRequiredItems().contains(itemStack)) {

            int required = game.getRequirementForItem(itemStack);

            if (required == 0) {
                player.sendMessage(plugin.getLang("incorrect-item-provided"));
                return;
            }

            if (itemStack.getAmount() < required) {
                player.sendMessage(plugin.parsePlaceholders(plugin.getLang("item-not-enough"), "%required%", required, "%amount%", itemStack.getAmount()));
                return;
            }

            itemStack = new ItemStack(itemStack);
            itemStack.setAmount(required);
        }

        if (player.getInventory().removeItem(itemStack).size() > 0) {
            player.sendMessage(plugin.getLang("error"));
            return;
        }

        game.completeItem(player, itemStack);

        ArrayList<ItemStack> updatedMissingItems = game.getMissingItems(player);

        player.sendMessage(plugin.parsePlaceholders(plugin.getLang("item-accepted"), "%current%", game.getRequiredItems().size() - updatedMissingItems.size(), "%total%", game.getRequiredItems().size()));

        if (updatedMissingItems.size() == 0) {
            game.win(plugin, gameManager, player);
            return;
        }

        StringBuilder sb = new StringBuilder(); // Todo actually implement thi-s as customizable and not ugly
        for (ItemStack missingItem : updatedMissingItems) sb.append(missingItem.getType().name()).append("(").append(missingItem.getAmount()).append("x), ");

        player.sendMessage(plugin.parsePlaceholders(plugin.getLang("items-left"), "%items%", sb));
    }

}
