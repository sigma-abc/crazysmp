package dev.crazysmp.listener;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.gui.HomeGUI;
import dev.crazysmp.gui.RTPGUI;
import dev.crazysmp.home.HomeEntry;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

import java.util.*;

public class GUIListener implements Listener {

    private final CrazySMPPlugin plugin;
    private final HomeGUI homeGUI;
    private final RTPGUI rtpGUI;

    public GUIListener(CrazySMPPlugin plugin) {
        this.plugin = plugin;
        this.homeGUI = new HomeGUI(plugin);
        this.rtpGUI  = new RTPGUI(plugin);
    }

    public HomeGUI getHomeGUI() { return homeGUI; }
    public RTPGUI getRTPGUI()   { return rtpGUI; }

    // ── Block ALL item taking from plugin GUIs ────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        boolean isHomeGUI = title.contains("⌂");
        boolean isRTPGUI  = title.contains("✈");
        boolean isIconPick = title.contains("Pick an icon");

        if (!isHomeGUI && !isRTPGUI && !isIconPick) return;

        // Always cancel to prevent stealing
        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (isHomeGUI) handleHomeGUIClick(player, slot, clicked, event.getClick(), title);
        else if (isRTPGUI) handleRTPGUIClick(player, slot, clicked);
        else if (isIconPick) handleIconPickClick(player, clicked);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("⌂") || title.contains("✈") || title.contains("Pick an icon")) {
            event.setCancelled(true);
        }
    }

    // ── Home GUI handler ──────────────────────────────────────

    private void handleHomeGUIClick(Player player, int slot, ItemStack clicked, ClickType click, String title) {
        // Close button
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }

        // Navigation
        if (clicked.getType() == Material.ARROW) {
            int page = getPage(player);
            if (slot == 21) homeGUI.open(player, Math.max(0, page - 1));
            else if (slot == 23) homeGUI.open(player, page + 1);
            return;
        }

        // Home item (slots 0-20)
        if (slot >= 0 && slot <= 20) {
            String displayName = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : "";
            // Strip colour codes to get home name
            String homeName = displayName.replaceAll("§[0-9a-fk-or]", "").replace("⌂ ", "").trim();
            if (homeName.isEmpty()) return;

            if (click.isShiftClick()) {
                // Delete home
                boolean deleted = plugin.getHomeManager().deleteHome(player.getUniqueId(), homeName);
                if (deleted) {
                    player.sendMessage(plugin.msgRaw("home-deleted").replace("%name%", homeName));
                    homeGUI.open(player, getPage(player));
                }
            } else if (click == ClickType.RIGHT) {
                // Change icon
                player.closeInventory();
                homeGUI.openIconPicker(player, homeName);
            } else {
                // Teleport
                player.closeInventory();
                Optional<HomeEntry> home = plugin.getHomeManager().getHome(player.getUniqueId(), homeName);
                home.ifPresent(h -> teleportWithDelay(player, h));
            }
        }
    }

    private void teleportWithDelay(Player player, HomeEntry home) {
        int delay = plugin.getConfig().getInt("homes.teleport-delay", 3);
        boolean cancelOnMove = plugin.getConfig().getBoolean("homes.cancel-on-move", true);

        if (delay <= 0) {
            player.teleport(home.getLocation());
            player.sendMessage(plugin.msgRaw("home-teleported").replace("%name%", home.getName()));
            return;
        }

        player.sendMessage(plugin.msgRaw("home-teleporting")
                .replace("%name%", home.getName()).replace("%seconds%", String.valueOf(delay)));

        Location startLoc = player.getLocation().clone();
        player.setMetadata("tp_pending", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (cancelOnMove && player.getLocation().distanceSquared(startLoc) > 0.5) {
                player.sendMessage(plugin.msgRaw("home-cancelled"));
                player.removeMetadata("tp_pending", plugin);
                return;
            }
            player.teleport(home.getLocation());
            player.sendMessage(plugin.msgRaw("home-teleported").replace("%name%", home.getName()));
            player.removeMetadata("tp_pending", plugin);
        }, delay * 20L);
    }

    // ── Icon picker handler ───────────────────────────────────

    private void handleIconPickClick(Player player, ItemStack clicked) {
        String homeName = "";
        if (player.hasMetadata("home_icon_pick")) {
            homeName = player.getMetadata("home_icon_pick").get(0).asString();
        }
        if (homeName.isEmpty()) { player.closeInventory(); return; }

        plugin.getHomeManager().updateIcon(player.getUniqueId(), homeName, clicked.getType());
        player.sendMessage(CrazySMPPlugin.c("&aIcon updated for home &6" + homeName + "&a!"));
        player.removeMetadata("home_icon_pick", plugin);
        player.closeInventory();
        homeGUI.open(player, 0);
    }

    // ── RTP GUI handler ───────────────────────────────────────

    private void handleRTPGUIClick(Player player, int slot, ItemStack clicked) {
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        if (clicked.getType() == Material.COMPASS) return;

        String worldKey = switch (slot) {
            case 10 -> "overworld";
            case 13 -> "nether";
            case 16 -> "end";
            default -> null;
        };
        if (worldKey == null) return;

        long cd = plugin.getRTPManager().getCooldownRemaining(player.getUniqueId());
        if (cd > 0) {
            player.sendMessage(plugin.msgRaw("rtp-cooldown").replace("%seconds%", String.valueOf(cd)));
            return;
        }

        String worldName = plugin.getConfig().getString("rtp.options." + worldKey + ".world", "world");
        player.closeInventory();
        player.sendMessage(plugin.msgRaw("rtp-searching"));
        plugin.getRTPManager().teleport(player, worldName);
    }

    // ── Helpers ───────────────────────────────────────────────

    private int getPage(Player player) {
        if (player.hasMetadata("home_gui_page")) {
            List<MetadataValue> meta = player.getMetadata("home_gui_page");
            if (!meta.isEmpty()) return meta.get(0).asInt();
        }
        return 0;
    }
}