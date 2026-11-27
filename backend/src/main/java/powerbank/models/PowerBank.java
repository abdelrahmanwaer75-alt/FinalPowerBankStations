package powerbank.models;

public class PowerBank {
    private long id;
    private long stationId;
    private String status;

    public PowerBank(long id, String status, long stationId) {
        this.id = id;
        this.status = status;
        this.stationId = stationId;
    }

    public long getId() { return id; }
    public long getStationId() { return stationId; }
    public String getStatus() { return status; }
    public void setId(long id) { this.id = id; }
    public void setStationId(long stationId) { this.stationId = stationId; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "PowerBank{id=" + id + ", status=" + status + ", stationId=" + stationId + "}";
    }
}
