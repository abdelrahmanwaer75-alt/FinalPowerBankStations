package powerbank.models;

public class StatusHistory {
    private long powerBankId;
    private String oldStatus;
    private String newStatus;
    private String changeDate;

    public StatusHistory(long id, String oldStatus, String newStatus, String date) {
        this.powerBankId = id;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changeDate = date;
    }

    public long getPowerBankId() { return powerBankId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getChangeDate() { return changeDate; }

    @Override
    public String toString() {
        return "PowerBank " + powerBankId + ": " + oldStatus + " -> " + newStatus + " (" + changeDate + ")";
    }
}
