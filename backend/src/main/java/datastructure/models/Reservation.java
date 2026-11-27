package datastructure.models;

public class Reservation {
    private long id;
    private long customerId;
    private long stationId;
    private long powerBankId;
    private String reservationDate;
    private String status;

    public Reservation(long id, long customerId, long stationId, String date, String status) {
        this(id, customerId, stationId, -1, date, status);
    }

    public Reservation(long id, long customerId, long stationId, long powerBankId, String date, String status) {
        this.id = id;
        this.customerId = customerId;
        this.stationId = stationId;
        this.powerBankId = powerBankId;
        this.reservationDate = date;
        this.status = status;
    }

    public long getId() { return id; }
    public long getCustomerId() { return customerId; }
    public long getStationId() { return stationId; }
    public long getPowerBankId() { return powerBankId; }
    public String getReservationDate() { return reservationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", customer=" + customerId + ", station=" + stationId
                + ", powerBank=" + powerBankId + ", date=" + reservationDate + ", status=" + status + "}";
    }
}
