package datastructure.models;

/** One customer request in the project-wide FIFO waiting queue. */
public class WaitingRequest {
    private final long requestId;
    private final long customerId;
    private final long preferredStationId;
    private final String requestDate;
    private String status;
    private Long assignedPowerBankId;

    public WaitingRequest(long requestId, long customerId, long preferredStationId,
                          String requestDate, String status) {
        this.requestId = requestId;
        this.customerId = customerId;
        this.preferredStationId = preferredStationId;
        this.requestDate = requestDate;
        this.status = status == null ? "WAITING" : status;
    }

    public long getRequestId() { return requestId; }
    public long getCustomerId() { return customerId; }
    public long getPreferredStationId() { return preferredStationId; }
    public String getRequestDate() { return requestDate; }
    public String getStatus() { return status; }
    public Long getAssignedPowerBankId() { return assignedPowerBankId; }
    public void setStatus(String status) { this.status = status; }
    public void assign(long powerBankId) { this.assignedPowerBankId = powerBankId; this.status = "FULFILLED"; }

    @Override public String toString() {
        return "WaitingRequest{id=" + requestId + ", customer=" + customerId
                + ", preferredStation=" + preferredStationId + ", requestedAt=" + requestDate
                + ", status=" + status + (assignedPowerBankId == null ? "" : ", powerBank=" + assignedPowerBankId) + "}";
    }
}
