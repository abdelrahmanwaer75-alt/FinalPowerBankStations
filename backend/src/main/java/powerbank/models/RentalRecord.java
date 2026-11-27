package powerbank.models;

public class RentalRecord {
    private long id;
    private long customerId;
    private long powerBankId;
    private long startStationId;
    private long returnStationId;
    private String startTime;
    private String endTime;
    private double cost;

    public RentalRecord(long id, long customerId, long powerBankId, long startStationId,
                        long returnStationId, String start, String end, double cost) {
        this.id = id;
        this.customerId = customerId;
        this.powerBankId = powerBankId;
        this.startStationId = startStationId;
        this.returnStationId = returnStationId;
        this.startTime = start;
        this.endTime = end;
        this.cost = cost;
    }

    public long getId() { return id; }
    public long getCustomerId() { return customerId; }
    public long getPowerBankId() { return powerBankId; }
    public long getStartStationId() { return startStationId; }
    public long getReturnStationId() { return returnStationId; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public double getCost() { return cost; }
    public void complete(long returnedTo, String end, double finalCost) {
        this.returnStationId = returnedTo;
        this.endTime = end;
        this.cost = finalCost;
    }

    @Override
    public String toString() {
        return "Rental{id=" + id + ", customer=" + customerId + ", powerBank=" + powerBankId
                + ", from=" + startStationId + ", returnedTo=" + returnStationId
                + ", start=" + startTime + ", end=" + endTime + ", cost=" + cost + "}";
    }
}
