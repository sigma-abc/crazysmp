package dev.crazysmp.gui;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.home.HomeEntry;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import java.util.*;

/**
 * HomeGUI — updated with:
 *  1. Slot chooser button (slot 24): opens a 9-slot GUI where each slot
 *     represents an inventory slot index so the player can pick which
 *     hotbar/inventory slot a home is "pinned" to (stored in metadata).
 *  2. "Set Home Here" button (slot 25): closes the menu and asks the
 *     player to type a home name in chat, then auto-creates the home.
 */
public class HomeGUI {

    private static final int HOMES_PER_PAGE = 21; // rows 1-3 (slots 0-20)

    private final CrazySMPPlugin plugin;

    public HomeGUI(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Main GUI ─────────────────────────────────────────────────────────────

    public void open(Player player, int page) {
        String title = CrazySMPPlugin.c(
            plugin.getConfig().getString("homes.gui-title", "&6⌂ Your Homes")
        ) + (page > 0 ? " §8[§7" + (page + 1) + "§8]" : "");

        Inventory inv = Bukkit.createInventory(null, 27, title);

        List<HomeEntry> homeList = new ArrayList<>(
            plugin.getHomeManager().getHomes(player.getUniqueId()).values()
        );

        int start = page * HOMES_PER_PAGE;
        int end   = Math.min(start + HOMES_PER_PAGE, homeList.size());
        int totalHomes = homeList.size();
        int maxHomes   = plugin.getHomeManager().getMaxHomes(player);

        // Fill home slots 0-20
        Material fillerMat = Material.matchMaterial(
            plugin.getConfig().getString("homes.filler-material", "GRAY_STAINED_GLASS_PANE")
        );
        if (fillerMat == null) fillerMat = Material.GRAY_STAINED_GLASS_PANE;

        for (int slot = 0; slot < HOMES_PER_PAGE; slot++) {
            int idx = start + slot;
            if (idx < end) {
                inv.setItem(slot, buildHomeItem(homeList.get(idx)));
            } else {
                ItemStack filler = new ItemStack(fillerMat);
                ItemMeta fm = filler.getItemMeta();
                fm.setDisplayName(" ");
                filler.setItemMeta(fm);
                inv.setItem(slot, filler);
            }
        }

        // ── Navigation row (slots 21-26) ─────────────────────────────────────

        // Slot 21: previous page
        if (page > 0) {
            inv.setItem(21, buildNavItem(Material.ARROW, "&7← Previous Page", ""));
        } else {
            inv.setItem(21, buildFillerPane());
        }

        // Slot 22: info book
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(CrazySMPPlugin.c("&6Your Homes"));
        im.setLore(List.of(
            CrazySMPPlugin.c("&7Used: &e" + totalHomes
                + " &7/ &e" + (maxHomes == Integer.MAX_VALUE ? "∞" : maxHomes)),
            CrazySMPPlugin.c("&7Page: &e" + (page + 1)),
            "",
            CrazySMPPlugin.c("&eLeft-click &7a home to teleport"),
            CrazySMPPlugin.c("&eRight-click &7a home to change icon"),
            CrazySMPPlugin.c("&eShift+click &7a home to delete")
        ));
        info.setItemMeta(im);
        inv.setItem(22, info);

        // Slot 23: next page
        if (end < homeList.size()) {
            inv.setItem(23, buildNavItem(Material.ARROW, "&7Next Page →", ""));
        } else {
            inv.setItem(23, buildFillerPane());
        }

        // Slot 24: Slot Chooser — lets player choose which hotbar slot a home is
        ItemStack slotChooser = new ItemStack(Material.COMPARATOR);
        ItemMeta sci = slotChooser.getItemMeta();
        sci.setDisplayName(CrazySMPPlugin.c("&bChoose Home Slot"));
        sci.setLore(List.of(
            CrazySMPPlugin.c("&7Click to open the slot picker."),
            CrazySMPPlugin.c("&7Pin a home to a specific inventory slot"),
            CrazySMPPlugin.c("&7so you can quick-access it later.")
        ));
        slotChooser.setItemMeta(sci);
        inv.setItem(24, slotChooser);

        // Slot 25: Set Home Here (closes GUI + prompts chat input)
        ItemStack setHere = new ItemStack(Material.EMERALD);
        ItemMeta shi = setHere.getItemMeta();
        boolean canSetMore = totalHomes < maxHomes || maxHomes == Integer.MAX_VALUE;
        if (canSetMore) {
            shi.setDisplayName(CrazySMPPlugin.c("&a✦ Set Home Here"));
            shi.setLore(List.of(
                CrazySMPPlugin.c("&7Click to create a home at"),
                CrazySMPPlugin.c("&7your current location."),
                "",
                CrazySMPPlugin.c("&eThe menu will close and you"),
                CrazySMPPlugin.c("&ewill be asked to type a name.")
            ));
        } else {
            shi.setDisplayName(CrazySMPPlugin.c("&c✦ Home Limit Reached"));
            shi.setLore(List.of(
                CrazySMPPlugin.c("&7You have used all &e" + maxHomes),
                CrazySMPPlugin.c("&7home slot(s).")
            ));
        }
        setHere.setItemMeta(shi);
        inv.setItem(25, setHere);

        // Slot 26: Close
        inv.setItem(26, buildNavItem(Material.BARRIER, "&cClose", ""));

        player.openInventory(inv);
        player.setMetadata("home_gui_page",
            new org.bukkit.metadata.FixedMetadataValue(plugin, page));
        // Store can-set flag so GUIListener can read it
        player.setMetadata("home_can_set",
            new org.bukkit.metadata.FixedMetadataValue(plugin, canSetMore));
    }

    // ── Slot Chooser GUI ─────────────────────────────────────────────────────

    /**
     * Opens a 9-slot inventory.  Each slot represents hotbar slot 1-9.
     * Clicking a slot stores "home_slot_<N> -> homeName" in the player's
     * persistent data, letting other systems quick-teleport on item use.
     */
    public void openSlotChooser(Player player) {
        // First ask which home to pin
        Inventory inv = Bukkit.createInventory(null, 27,
            CrazySMPPlugin.c("&8⌂ Pin Home to Slot"));

        List<HomeEntry> homes = new ArrayList<>(
            plugin.getHomeManager().getHomes(player.getUniqueId()).values()
        );

        if (homes.isEmpty()) {
            ItemStack none = new ItemStack(Material.PAPER);
            ItemMeta nm = none.getItemMeta();
            nm.setDisplayName(CrazySMPPlugin.c("&cYou have no homes to pin."));
            none.setItemMeta(nm);
            inv.setItem(13, none);
        } else {
            for (int i = 0; i < Math.min(homes.size(), 21); i++) {
                HomeEntry h = homes.get(i);
                ItemStack it = new ItemStack(h.getIcon());
                ItemMeta m = it.getItemMeta();
                m.setDisplayName(CrazySMPPlugin.c("&6⌂ &e" + h.getName()));
                m.setLore(List.of(
                    CrazySMPPlugin.c("&7Click to pick which inventory"),
                    CrazySMPPlugin.c("&7slot this home is pinned to.")
                ));
                it.setItemMeta(m);
                inv.setItem(i, it);
            }
        }

        // Bottom row: slot number selectors (slots 18-26 → hotbar 1-9)
        for (int hs = 0; hs < 9; hs++) {
            ItemStack slotBtn = new ItemStack(Material.values()[
                Material.WHITE_STAINED_GLASS_PANE.ordinal() + Math.min(hs, 15)
            ]);
            ItemMeta sm = slotBtn.getItemMeta();
            sm.setDisplayName(CrazySMPPlugin.c("&bSlot &f" + (hs + 1)));
            sm.setLore(List.of(CrazySMPPlugin.c("&7Assign selected home to hotbar slot " + (hs + 1))));
            slotBtn.setItemMeta(sm);
            inv.setItem(18 + hs, slotBtn);
        }

        player.setMetadata("slot_chooser_open",
            new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        player.openInventory(inv);
    }

    // ── Icon picker GUI ──────────────────────────────────────────────────────

    public void openIconPicker(Player player, String homeName) {
        Inventory inv = Bukkit.createInventory(null, 27,
            CrazySMPPlugin.c("&8Pick an icon for &6" + homeName));
        Material[] icons = {
            Material.PLAYER_HEAD, Material.DIAMOND, Material.EMERALD,
            Material.GOLD_INGOT, Material.IRON_INGOT, Material.NETHERITE_INGOT,
            Material.BEACON, Material.RESPAWN_ANCHOR, Material.ENDER_CHEST,
            Material.CHEST, Material.RED_BED, Material.COMPASS,
            Material.MAP, Material.GRASS_BLOCK, Material.NETHERRACK,
            Material.END_STONE, Material.CAMPFIRE, Material.LANTERN,
            Material.TORCH, Material.GLOWSTONE, Material.AMETHYST_SHARD,
            Material.QUARTZ, Material.PRISMARINE_CRYSTALS, Material.BLAZE_ROD,
            Material.NETHER_STAR, Material.CHORUS_FRUIT, Material.APPLE
        };
        for (int i = 0; i < Math.min(icons.length, 27); i++) {
            ItemStack item = new ItemStack(icons[i]);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName(CrazySMPPlugin.c("&e" +
                icons[i].name().toLowerCase().replace("_", " ")));
            m.setLore(List.of(CrazySMPPlugin.c("&7Click to use as home icon")));
            item.setItemMeta(m);
            inv.setItem(i, item);
        }
        player.setMetadata("home_icon_pick",
            new org.bukkit.metadata.FixedMetadataValue(plugin, homeName));
        player.openInventory(inv);
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildHomeItem(HomeEntry home) {
        ItemStack item = new ItemStack(home.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CrazySMPPlugin.c("&6⌂ &e" + home.getName()));
        Location l = home.getLocation();
        meta.setLore(List.of(
            CrazySMPPlugin.c("&7World: &f" + l.getWorld().getName()),
            CrazySMPPlugin.c("&7X: &f" + Math.round(l.getX())
                + " &7Y: &f" + Math.round(l.getY())
                + " &7Z: &f" + Math.round(l.getZ())),
            "",
            CrazySMPPlugin.c("&eLeft-click &7to teleport"),
            CrazySMPPlugin.c("&eRight-click &7to change icon"),
            CrazySMPPlugin.c("&cShift+click &7to delete")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNavItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CrazySMPPlugin.c(name));
        if (!lore.isEmpty()) meta.setLore(List.of(CrazySMPPlugin.c(lore)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFillerPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(" ");
        item.setItemMeta(m);
        return item;
    }
}
