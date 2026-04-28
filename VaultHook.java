package dev.crazysmp.economy;

import dev.crazysmp.CrazySMPPlugin;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VaultHook extends AbstractEconomy {

    private final CrazySMPPlugin plugin;
    private Currency currency;

    public VaultHook(CrazySMPPlugin plugin) {
        this.plugin = plugin;
        this.currency = plugin.getCurrencyManager().getVaultCurrency();
    }

    public void register() {
        plugin.getServer().getServicesManager().register(
                net.milkbowl.vault.economy.Economy.class,
                this,
                plugin,
                ServicePriority.High
        );
    }

    // ── Meta ──────────────────────────────────────────────────

    @Override public boolean isEnabled()         { return plugin.isEnabled(); }
    @Override public String getName()            { return "CrazySMPEssentials"; }
    @Override public boolean hasBankSupport()    { return false; }
    @Override public int fractionalDigits()      { return currency != null ? currency.getDecimalPlaces() : 2; }
    @Override public String currencyNamePlural() { return currency != null ? currency.getDisplayName() : "Dollars"; }
    @Override public String currencyNameSingular(){ return currency != null ? currency.getDisplayName() : "Dollar"; }

    @Override
    public String format(double amount) {
        return currency != null ? currency.format(amount) : String.format("$%.2f", amount);
    }

    @Override public List<String> getBanks() { return new ArrayList<>(); }

    // ── Balance ───────────────────────────────────────────────

    @Override
    public double getBalance(String name) {
        OfflinePlayer p = plugin.getServer().getOfflinePlayer(name);
        return getBalance(p);
    }
    @Override
    public double getBalance(String name, String world) { return getBalance(name); }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player == null || currency == null) return 0;
        return plugin.getCurrencyManager().getBalance(player.getUniqueId(), currency.getId());
    }
    @Override
    public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }

    // ── Has ───────────────────────────────────────────────────

    @Override
    public boolean has(String name, double amount) {
        return getBalance(name) >= amount;
    }
    @Override
    public boolean has(String name, String world, double amount) { return has(name, amount); }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }
    @Override
    public boolean has(OfflinePlayer player, String world, double amount) { return has(player, amount); }

    // ── Account ───────────────────────────────────────────────

    @Override public boolean hasAccount(String name)                      { return true; }
    @Override public boolean hasAccount(String name, String world)        { return true; }
    @Override public boolean hasAccount(OfflinePlayer player)             { return true; }
    @Override public boolean hasAccount(OfflinePlayer player, String world){ return true; }

    @Override
    public boolean createPlayerAccount(String name) {
        OfflinePlayer p = plugin.getServer().getOfflinePlayer(name);
        return createPlayerAccount(p);
    }
    @Override public boolean createPlayerAccount(String name, String world) { return createPlayerAccount(name); }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (player == null || currency == null) return false;
        plugin.getCurrencyManager().getBalance(player.getUniqueId(), currency.getId());
        return true;
    }
    @Override public boolean createPlayerAccount(OfflinePlayer player, String world) { return createPlayerAccount(player); }

    // ── Withdraw ──────────────────────────────────────────────

    @Override
    public EconomyResponse withdrawPlayer(String name, double amount) {
        return withdrawPlayer(plugin.getServer().getOfflinePlayer(name), amount);
    }
    @Override public EconomyResponse withdrawPlayer(String name, String world, double amount) { return withdrawPlayer(name, amount); }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null || currency == null)
            return fail("Invalid player or currency");
        if (amount < 0)
            return fail("Cannot withdraw negative amount");
        UUID uuid = player.getUniqueId();
        if (!plugin.getCurrencyManager().hasBalance(uuid, currency.getId(), amount))
            return fail("Insufficient funds");
        plugin.getCurrencyManager().withdraw(uuid, currency.getId(), amount);
        return success(amount, getBalance(player));
    }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount) { return withdrawPlayer(player, amount); }

    // ── Deposit ───────────────────────────────────────────────

    @Override
    public EconomyResponse depositPlayer(String name, double amount) {
        return depositPlayer(plugin.getServer().getOfflinePlayer(name), amount);
    }
    @Override public EconomyResponse depositPlayer(String name, String world, double amount) { return depositPlayer(name, amount); }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null || currency == null)
            return fail("Invalid player or currency");
        if (amount < 0)
            return fail("Cannot deposit negative amount");
        plugin.getCurrencyManager().deposit(player.getUniqueId(), currency.getId(), amount);
        return success(amount, getBalance(player));
    }
    @Override public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount) { return depositPlayer(player, amount); }

    // ── Banks (not supported) ─────────────────────────────────

    @Override public EconomyResponse isBankMember(String n, String p) { return notImpl(); }
    @Override public EconomyResponse isBankOwner(String n, String p)  { return notImpl(); }
    @Override public EconomyResponse bankBalance(String n)             { return notImpl(); }
    @Override public EconomyResponse bankHas(String n, double a)       { return notImpl(); }
    @Override public EconomyResponse bankWithdraw(String n, double a)  { return notImpl(); }
    @Override public EconomyResponse bankDeposit(String n, double a)   { return notImpl(); }
    @Override public EconomyResponse createBank(String n, String p)    { return notImpl(); }
    @Override public EconomyResponse deleteBank(String n)              { return notImpl(); }

    // ── Helpers ───────────────────────────────────────────────

    private EconomyResponse success(double amount, double balance) {
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null);
    }
    private EconomyResponse fail(String msg) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, msg);
    }
    private EconomyResponse notImpl() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
}