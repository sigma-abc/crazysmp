package dev.crazysmp.listener;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AttributeSwapListener implements Listener {

    private final CrazySMPPlugin plugin;
    private final Set<UUID> pendingRefresh = new HashSet<>();

    public AttributeSwapListener(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        scheduleAttributeRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryType.SlotType slotType = event.getSlotType();
        if (slotType == InventoryType.SlotType.ARMOR) {
            scheduleAttributeRefresh(player);
            return;
        }
        if (event.isShiftClick() && event.getCurrentItem() != null) {
            String name = event.getCurrentItem().getType().name();
            if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                    || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                    || name.equals("CARVED_PUMPKIN") || name.equals("SKULL")
                    || name.endsWith("_SKULL") || name.endsWith("_HEAD")) {
                scheduleAttributeRefresh(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        scheduleAttributeRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        scheduleAttributeRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() == EntityPotionEffectEvent.Cause.EXPIRATION
                || event.getCause() == EntityPotionEffectEvent.Cause.PLUGIN) {
            scheduleAttributeRefresh(player);
        }
    }

    private void scheduleAttributeRefresh(Player player) {
        UUID uid = player.getUniqueId();
        if (!pendingRefresh.add(uid)) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                pendingRefresh.remove(uid);
                if (!player.isOnline()) return;
                refreshAttributes(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    private void refreshAttributes(Player player) {
        // ── Paper 1.21.5+ uses short attribute names (no GENERIC_ prefix) ──
        // ── Paper ≤ 1.21.4  uses GENERIC_MAX_HEALTH, GENERIC_ATTACK_DAMAGE, etc. ──
        // This file targets Paper 1.21.5+. If you need ≤ 1.21.4, replace
        // Attribute.MAX_HEALTH → Attribute.GENERIC_MAX_HEALTH, etc.

        tickle(player, Attribute.MAX_HEALTH);
        tickle(player, Attribute.ATTACK_DAMAGE);
        tickle(player, Attribute.ATTACK_SPEED);
        forceTickle(player, Attribute.ARMOR);
        forceTickle(player, Attribute.ARMOR_TOUGHNESS);
        forceTickle(player, Attribute.KNOCKBACK_RESISTANCE);
        forceTickle(player, Attribute.MOVEMENT_SPEED);
    }

    private void tickle(Player player, Attribute attr) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        double base = inst.getBaseValue();
        inst.setBaseValue(base + 0.000001);
        inst.setBaseValue(base);
    }

    private void forceTickle(LivingEntity entity, Attribute attr) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst == null) return;
        double base = inst.getBaseValue();
        inst.setBaseValue(base + 0.000001);
        inst.setBaseValue(base);
    }
}