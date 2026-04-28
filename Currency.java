package dev.crazysmp.economy;

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
        this.id = id;
        this.displayName = displayName;
        this.symbol = symbol;
        this.symbolBefore = symbolBefore;
        this.decimalPlaces = decimalPlaces;
        this.startingBalance = startingBalance;
        this.vault = vault;
        this.color = color;
    }

    public String format(double amount) {
        String fmt = decimalPlaces == 0
                ? String.valueOf((long) amount)
                : String.format("%." + decimalPlaces + "f", amount);
        // Add thousands separator
        String[] parts = fmt.split("\\.");
        parts[0] = parts[0].replaceAll("(\\d)(?=(\\d{3})+$)", "$1,");
        fmt = parts.length > 1 ? parts[0] + "." + parts[1] : parts[0];
        return symbolBefore ? symbol + fmt : fmt + symbol;
    }

    public String getId()            { return id; }
    public String getDisplayName()   { return displayName; }
    public String getSymbol()        { return symbol; }
    public int getDecimalPlaces()    { return decimalPlaces; }
    public double getStartingBalance() { return startingBalance; }
    public boolean isVault()         { return vault; }
    public String getColor()         { return color; }
}