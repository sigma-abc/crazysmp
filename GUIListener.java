package dev.crazysmp.listener;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.gui.HomeGUI;
import dev.crazysmp.gui.RTPGUI;
import dev.crazysmp.home.HomeEntry;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

/**
 * GUIListener — updated to handle:
 *  - "Set Home Here" button: closes GUI and prompts chat input for name.
 *  - Slot chooser button & sub-GUI interaction.
 *  - Chat intercept for home-naming (AsyncPlayerChatEvent).
 */
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
    public RTPGUI  getRTPGUI()  { return rtpGUI;  }

    // ── Block ALL item taking from plugin GUIs ────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        boolean isHomeGUI   = title.contains("⌂");
        boolean isRTPGUI    = title.contains("✈");
        boolean isIconPick  = title.contains("Pick an icon");
        boolean isSlotChooser = title.contains("Pin Home to Slot");

        if (!isHomeGUI && !isRTPGUI && !isIconPick && !isSlotChooser) return;

        event.setCancelled(true);
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (isHomeGUI)        handleHomeGUIClick(player, slot, clicked, event.getClick());
        else if (isRTPGUI)    handleRTPGUIClick(player, slot, clicked);
        else if (isIconPick)  handleIconPickClick(player, clicked);
        else if (isSlotChooser) handleSlotChooserClick(player, slot, clicked);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("⌂") || title.contains("✈")
                || title.contains("Pick an icon") || title.contains("Pin Home to Slot")) {
            event.setCancelled(true);
        }
    }

    // ── Chat intercept for home naming ────────────────────────────────────────

    /**
     * When a player is in "awaiting home name" mode (set by clicking the
     * Set Home Here button), their next chat message is treated as the home name.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getAwaitingHomeNameInput().contains(player.getUniqueId())) return;

        event.setCancelled(true);
        plugin.getAwaitingHomeNameInput().remove(player.getUniqueId());

        String homeName = event.getMessage().trim();
        if (homeName.isEmpty() || homeName.equalsIgnoreCase("cancel")) {
            player.sendMessage(CrazySMPPlugin.c(
                "&8[&6CrazySMP&8] &cHome creation cancelled."
            ));
            return;
        }
        if (homeName.length() > 24) {
            player.sendMessage(CrazySMPPlugin.c(
                "&cHome name too long (max 24 chars)."
            ));
            return;
        }

        // Must run on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            int maxHomes = plugin.getHomeManager().getMaxHomes(player);
            int current  = plugin.getHomeManager().getHomes(player.getUniqueId()).size();
            if (current >= maxHomes) {
                player.sendMessage(CrazySMPPlugin.c(
                    "&cYou have reached your home limit (" + maxHomes + ")."
                ));
                return;
            }
            plugin.getHomeManager().setHome(
                player.getUniqueId(), homeName, player.getLocation(), Material.PLAYER_HEAD
            );
            player.sendMessage(CrazySMPPlugin.c(
                "&8[&6CrazySMP&8] &aHome &6" + homeName + " &aset at your location!"
            ));
        });
    }

    // ── Home GUI handler ──────────────────────────────────────────────────────

    private void handleHomeGUIClick(Player player, int slot, ItemStack clicked, ClickType click) {
        // Close
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Navigation arrows
        if (clicked.getType() == Material.ARROW) {
            int page = getPage(player);
            if (slot == 21) homeGUI.open(player, Math.max(0, page - 1));
            else if (slot == 23) homeGUI.open(player, page + 1);
            return;
        }

        // Slot 24: Slot Chooser
        if (slot == 24) {
            homeGUI.openSlotChooser(player);
            return;
        }

        // Slot 25: Set Home Here — close GUI and ask for name in chat
        if (slot == 25) {
            boolean canSet = player.hasMetadata("home_can_set")
                && player.getMetadata("home_can_set").get(0).asBoolean();
            if (!canSet) {
                player.sendMessage(CrazySMPPlugin.c(
                    "&cYou have reached your home limit."
                ));
                return;
            }
            player.closeInventory();
            plugin.getAwaitingHomeNameInput().add(player.getUniqueId());
            player.sendMessage(CrazySMPPlugin.c(
                "&8[&6CrazySMP&8] &eType a name for your new home in chat."
            ));
            player.sendMessage(CrazySMPPlugin.c(
                "&8[&6CrazySMP&8] &7(Type &ccancel &7to abort)"
            ));
            return;
        }

        // Home items (slots 0-20)
        if (slot >= 0 && slot < 21) {
            String displayName = clicked.getItemMeta() != null
                ? clicked.getItemMeta().getDisplayName() : "";
            if (!displayName.contains("⌂")) return;
            // Strip colour codes to get home name
            String homeName = org.bukkit.ChatColor.stripColor(displayName)
                .replace("⌂ ", "").trim();

            var home = plugin.getHomeManager().getHome(player.getUniqueId(), homeName);
            if (home.isEmpty()) return;

            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                // Delete
                plugin.getHomeManager().deleteHome(player.getUniqueId(), homeName);
                player.sendMessage(CrazySMPPlugin.c(
                    "&8[&6CrazySMP&8] &cDeleted home &6" + homeName + "&c."
                ));
                homeGUI.open(player, getPage(player));
            } else if (click == ClickType.RIGHT) {
                // Icon picker
                homeGUI.openIconPicker(player, homeName);
            } else {
                // Teleport
                teleportWithDelay(player, home.get());
                player.closeInventory();
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
        Location startLoc = player.getLocation().clone();
        player.sendMessage(plugin.msgRaw("home-teleport-wait")
            .replace("%seconds%", String.valueOf(delay))
            .replace("%name%", home.getName()));
        player.setMetadata("tp_pending",
            new org.bukkit.metadata.FixedMetadataValue(plugin, true));
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

    // ── Slot Chooser handler ──────────────────────────────────────────────────

    private void handleSlotChooserClick(Player player, int slot, ItemStack clicked) {
        // Top 21 slots = home selection; bottom 9 = slot assignment
        if (slot < 21) {
            // Player clicked a home — store it as "pending pin" and highlight slot row
            String dn = clicked.getItemMeta() != null
                ? clicked.getItemMeta().getDisplayName() : "";
            if (!dn.contains("⌂")) return;
            String homeName = org.bukkit.ChatColor.stripColor(dn).replace("⌂ ", "").trim();
            player.setMetadata("slot_chooser_home",
                new org.bukkit.metadata.FixedMetadataValue(plugin, homeName));
            player.sendMessage(CrazySMPPlugin.c(
                "&8[&6CrazySMP&8] &eSelected home &6" + homeName
                + "&e. Now click a slot in the bottom row to pin it."
            ));
        } else if (slot >= 18 && slot <= 26) {
            // Bottom row — slot assignment
            if (!player.hasMetadata("slot_chooser_home")) {
                player.sendMessage(CrazySMPPlugin.c(
                    "&cPlease click a home first, then pick a slot."
                ));
                return;
            }
            String homeName = player.getMetadata("slot_chooser_home").get(0).asString();
            int hotbarSlot  = slot - 18; // 0-8
            // Store in persistent data container
            var pdc = player.getPersistentDataContainer();
            var key = new org.bukkit.NamespacedKey(plugin, "home_slot_" + hotbarSlot);
            pdc.set(key, org.bukkit.persistence.PersistentDataType.STRING, homeName);

            player.removeMetadata("slot_chooser_home", plugin);
            player.removeMetadata("slot_chooser_open", plugin);
            player.sendMessage(CrazySMPPlugin.c(
                "&8[&6CrazySMP&8] &aPinned home &6" + homeName
                + " &ato hotbar slot &e" + (hotbarSlot + 1) + "&a."
            ));
            player.closeInventory();
        }
    }

    // ── Icon picker handler ───────────────────────────────────────────────────

    private void handleIconPickClick(Player player, ItemStack clicked) {
        String homeName = "";
        if (player.hasMetadata("home_icon_pick")) {
            homeName = player.getMetadata("home_icon_pick").get(0).asString();
        }
        if (homeName.isEmpty()) { player.closeInventory(); return; }

        plugin.getHomeManager().updateIcon(player.getUniqueId(), homeName, clicked.getType());
        player.sendMessage(CrazySMPPlugin.c(
            "&aIcon updated for home &6" + homeName + "&a!"
        ));
        player.removeMetadata("home_icon_pick", plugin);
        player.closeInventory();
        homeGUI.open(player, 0);
    }

    // ── RTP GUI handler ───────────────────────────────────────────────────────

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
            player.sendMessage(plugin.msgRaw("rtp-cooldown")
                .replace("%seconds%", String.valueOf(cd)));
            return;
        }
        String worldName = plugin.getConfig().getString(
            "rtp.options." + worldKey + ".world", "world");
        player.closeInventory();
        player.sendMessage(plugin.msgRaw("rtp-searching"));
        plugin.getRTPManager().teleport(player, worldName);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getPage(Player player) {
        List<MetadataValue> meta = player.getMetadata("home_gui_page");
        return meta.isEmpty() ? 0 : meta.get(0).asInt();
    }
}
