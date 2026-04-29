package dev.crazysmp.economy;

/**
 * Currency — updated to include compact number formatting.
 *
 * formatCompact() converts:
 *   999       → $999.00   (unchanged below 1k)
 *   1000      → $1.0k
 *   4357      → $4.3k
 *   1200000   → $1.2M
 *   etc.
 */
public class Currency {

    private final String id;
    private final String displayName;
    private final String symbol;
    private final boolean symbolBefore;
    private final int decimalPlaces;
    private final double startingBalance;
    private final boolean vault;
    private final String color;

    public Currency(String id, String displayName, String symbol, boolean symbolBefore,
                    int decimalPlaces, double startingBalance, boolean vault, String color) {
        this.id             = id;
        this.displayName    = displayName;
        this.symbol         = symbol;
        this.symbolBefore   = symbolBefore;
        this.decimalPlaces  = decimalPlaces;
        this.startingBalance = startingBalance;
        this.vault          = vault;
        this.color          = color;
    }

    /** Full precision format (used for /balance, pay confirmations, etc.) */
    public String format(double amount) {
        String fmt = decimalPlaces == 0
            ? String.valueOf((long) amount)
            : String.format("%." + decimalPlaces + "f", amount);
        String[] parts = fmt.split("\\.");
        parts[0] = parts[0].replaceAll("(\\d)(?=(\\d{3})+$)", "$1,");
        fmt = parts.length > 1 ? parts[0] + "." + parts[1] : parts[0];
        return symbolBefore ? symbol + fmt : fmt + symbol;
    }

    /**
     * Compact format for /baltop and balance displays where space is limited.
     * Numbers >= 1,000 are shown as e.g. 4.3k, 1.2M, 3.4B.
     * Numbers < 1,000 fall back to normal format().
     */
    public String formatCompact(double amount) {
        String compactNum;
        if (amount >= 1_000_000_000) {
            compactNum = round1dp(amount / 1_000_000_000.0) + "B";
        } else if (amount >= 1_000_000) {
            compactNum = round1dp(amount / 1_000_000.0) + "M";
        } else if (amount >= 1_000) {
            compactNum = round1dp(amount / 1_000.0) + "k";
        } else {
            // Below 1k — use normal formatting
            return format(amount);
        }
        return symbolBefore ? symbol + compactNum : compactNum + symbol;
    }

    /** Round to 1 decimal place, stripping unnecessary .0 */
    private String round1dp(double value) {
        String s = String.format("%.1f", value);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()              { return id; }
    public String getDisplayName()     { return displayName; }
    public String getSymbol()          { return symbol; }
    public boolean isSymbolBefore()    { return symbolBefore; }
    public int getDecimalPlaces()      { return decimalPlaces; }
    public double getStartingBalance() { return startingBalance; }
    public boolean isVault()           { return vault; }
    public String getColor()           { return color; }
}
