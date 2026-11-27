package datastructure.models;

public class StatusHistory {
    private long eventId;
    private long powerBankId;
    private String oldStatus;
    private String newStatus;
    private String changeDate;

    public StatusHistory(long id, String oldStatus, String newStatus, String date) {
        this(0, id, oldStatus, newStatus, date);
    }

    public StatusHistory(long eventId, long powerBankId, String oldStatus, String newStatus, String date) {
        this.eventId = eventId;
        this.powerBankId = powerBankId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changeDate = date;
    }

    public long getPowerBankId() { return powerBankId; }
    public long getEventId() { return eventId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getChangeDate() { return changeDate; }

    @Override
    public String toString() {
        return "PowerBank " + powerBankId + ": " + oldStatus + " -> " + newStatus + " (" + changeDate + ")";
    }
}
