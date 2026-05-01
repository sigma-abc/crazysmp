package dev.crazysmp.economy;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyManager {

    private final CrazySMPPlugin plugin;
    private final Map<String, Currency> currencies = new LinkedHashMap<>();
    private Currency vaultCurrency;

    // In-memory balance store: uuid -> (currencyId -> balance)
    private final Map<UUID, Map<String, Double>> balances = new ConcurrentHashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public CurrencyManager(CrazySMPPlugin plugin) {
        this.plugin = plugin;
        loadCurrencies();
        loadData();
    }

    // ── Currency loading ──────────────────────────────────────

    private void loadCurrencies() {
        currencies.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("currencies");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            String path = "currencies." + id;
            String name  = plugin.getConfig().getString(path + ".display-name", id);
            String sym   = plugin.getConfig().getString(path + ".symbol", "$");
            String pos   = plugin.getConfig().getString(path + ".symbol-position", "before");
            int dec      = plugin.getConfig().getInt(path + ".decimal-places", 2);
            double start = plugin.getConfig().getDouble(path + ".starting-balance", 0.0);
            boolean vlt  = plugin.getConfig().getBoolean(path + ".vault", false);
            String color = plugin.getConfig().getString(path + ".color", "&a");

            Currency c = new Currency(id, name, sym, pos.equalsIgnoreCase("before"), dec, start, vlt, color);
            currencies.put(id, c);
            if (vlt && vaultCurrency == null) vaultCurrency = c;
        }
        if (currencies.isEmpty()) {
            // Safe fallback
            Currency def = new Currency("dollars", "Dollars", "$", true, 2, 500.0, true, "&a");
            currencies.put("dollars", def);
            vaultCurrency = def;
        }
        plugin.getLogger().info("Loaded " + currencies.size() + " currencies. Vault: "
                + (vaultCurrency != null ? vaultCurrency.getId() : "none"));
    }

    // ── Persistence ───────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "balances.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (Exception e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        for (String uuidStr : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Double> map = new HashMap<>();
                ConfigurationSection sec = dataConfig.getConfigurationSection(uuidStr);
                if (sec != null) {
                    for (String currId : sec.getKeys(false)) {
                        map.put(currId, sec.getDouble(currId, 0.0));
                    }
                }
                balances.put(uuid, map);
            } catch (Exception ignored) {}
        }
        plugin.getLogger().info("Loaded balances for " + balances.size() + " players.");
    }

    public void saveAll() {
        for (Map.Entry<UUID, Map<String, Double>> entry : balances.entrySet()) {
            String uuid = entry.getKey().toString();
            for (Map.Entry<String, Double> bal : entry.getValue().entrySet()) {
                dataConfig.set(uuid + "." + bal.getKey(), bal.getValue());
            }
        }
        try { dataConfig.save(dataFile); } catch (Exception e) { e.printStackTrace(); }
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveAll);
    }

    // ── Balance operations ────────────────────────────────────

    private Map<String, Double> getOrCreate(UUID uuid) {
        return balances.computeIfAbsent(uuid, k -> {
            Map<String, Double> map = new HashMap<>();
            for (Currency c : currencies.values()) map.put(c.getId(), c.getStartingBalance());
            return map;
        });
    }

    public double getBalance(UUID uuid, String currencyId) {
        Map<String, Double> map = getOrCreate(uuid);
        if (!map.containsKey(currencyId)) {
            Currency c = currencies.get(currencyId);
            double start = c != null ? c.getStartingBalance() : 0.0;
            map.put(currencyId, start);
            return start;
        }
        return map.get(currencyId);
    }

    public double getBalance(UUID uuid) {
        return getBalance(uuid, vaultCurrency != null ? vaultCurrency.getId() : "dollars");
    }

    public boolean hasBalance(UUID uuid, String currencyId, double amount) {
        return getBalance(uuid, currencyId) >= amount;
    }

    public boolean withdraw(UUID uuid, String currencyId, double amount) {
        double bal = getBalance(uuid, currencyId);
        if (bal < amount) return false;
        getOrCreate(uuid).put(currencyId, bal - amount);
        saveAsync();
        return true;
    }

    public boolean deposit(UUID uuid, String currencyId, double amount) {
        double bal = getBalance(uuid, currencyId);
        getOrCreate(uuid).put(currencyId, bal + amount);
        saveAsync();
        return true;
    }

    public void setBalance(UUID uuid, String currencyId, double amount) {
        getOrCreate(uuid).put(currencyId, Math.max(0, amount));
        saveAsync();
    }

    public void resetBalance(UUID uuid, String currencyId) {
        Currency c = currencies.get(currencyId);
        setBalance(uuid, currencyId, c != null ? c.getStartingBalance() : 0.0);
    }

    /** Import a balance directly (used by migration) */
    public void importBalance(UUID uuid, String currencyId, double amount) {
        getOrCreate(uuid).put(currencyId, amount);
    }

    public void flushSave() { saveAll(); }

    // ── Currency lookups ──────────────────────────────────────

    public Currency getCurrency(String id) { return currencies.get(id.toLowerCase()); }
    public Currency getVaultCurrency()     { return vaultCurrency; }
    public Collection<Currency> getCurrencies() { return currencies.values(); }

    public Currency resolveCurrency(String input) {
        if (input == null) return vaultCurrency;
        Currency exact = currencies.get(input.toLowerCase());
        if (exact != null) return exact;
        // Fuzzy match by name or symbol
        for (Currency c : currencies.values()) {
            if (c.getDisplayName().equalsIgnoreCase(input) || c.getSymbol().equals(input))
                return c;
        }
        return null;
    }

    // ── Baltop ───────────────────────────────────────────────
    // ── Reload ───────────────────────────────────────────────

    public void reload() {
        saveAll();            // persist any unsaved balances first
        balances.clear();     // wipe in-memory balances
        vaultCurrency = null; // reset vault currency reference
        loadCurrencies();     // re-read currencies from config.yml
        loadData();           // re-read balances from balances.yml
    }


    public List<Map.Entry<UUID, Double>> getTopBalances(String currencyId, int limit) {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Double>> entry : balances.entrySet()) {
            double bal = entry.getValue().getOrDefault(currencyId, 0.0);
            list.add(Map.entry(entry.getKey(), bal));
        }
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }
}