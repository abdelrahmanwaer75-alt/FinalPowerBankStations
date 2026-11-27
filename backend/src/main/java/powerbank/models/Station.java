package powerbank.models;

public class Station {
    private long id;
    private String name;
    private int totalPowerBanks;
    private int availablePowerBanks;
    private boolean active;
    private double pricePerMinute;

    public Station(long id, String name, int total, int available) {
        this(id, name, total, available, true, 0.166667);
    }

    public Station(long id, String name, int total, int available, boolean active, double pricePerMinute) {
        this.id = id;
        this.name = name;
        this.totalPowerBanks = total;
        this.availablePowerBanks = available;
        this.active = active;
        this.pricePerMinute = pricePerMinute;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public int getTotalPowerBanks() { return totalPowerBanks; }
    public int getAvailablePowerBanks() { return availablePowerBanks; }
    public boolean isActive() { return active; }
    public double getPricePerMinute() { return pricePerMinute; }

    public void setName(String name) { this.name = name; }
    public void setAvailablePowerBanks(int n) { this.availablePowerBanks = n; }
    public void setTotalPowerBanks(int n) { this.totalPowerBanks = n; }
    public void setActive(boolean active) { this.active = active; }
    public void setPricePerMinute(double price) { this.pricePerMinute = price; }

    @Override
    public String toString() {
        return "Station{id=" + id + ", name=" + name + ", available="
                + availablePowerBanks + "/" + totalPowerBanks + ", active=" + active
                + ", price/minute=" + pricePerMinute + "}";
    }
}
