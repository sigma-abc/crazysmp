package dev.crazysmp.command;

import org.bukkit.entity.Player;

// Dummy event for home open
public class HomesOpenEvent extends org.bukkit.event.Event {
    private static final org.bukkit.event.HandlerList HANDLERS = new org.bukkit.event.HandlerList();
    private final Player player;

    public HomesOpenEvent(Player p) {
        this.player = p;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public org.bukkit.event.HandlerList getHandlers() {
        return HANDLERS;
    }

    public static org.bukkit.event.HandlerList getHandlerList() {
        return HANDLERS;
    }
}
