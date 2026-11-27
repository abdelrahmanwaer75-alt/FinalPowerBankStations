package datastructure.linkedlist;

import datastructure.models.StatusHistory;

public class StatusHistoryList {
    private ListNode head;
    private int size;

    public void insert(StatusHistory history) {
        if (history == null) return;
        ListNode node = new ListNode(history);
        if (head == null) { head = node; size++; return; }
        ListNode current = head;
        while (current.next != null) current = current.next;
        current.next = node;
        size++;
    }

    public StatusHistory search(long powerBankId) {
        ListNode current = head;
        while (current != null) {
            StatusHistory item = (StatusHistory) current.data;
            if (item.getPowerBankId() == powerBankId) return item;
            current = current.next;
        }
        return null;
    }

    /** Returns every status change for this device, in insertion/time order. */
    public StatusHistory[] searchByPowerBankId(long powerBankId) {
        int matches = 0;
        for (ListNode node = head; node != null; node = node.next)
            if (((StatusHistory) node.data).getPowerBankId() == powerBankId) matches++;
        StatusHistory[] result = new StatusHistory[matches];
        int index = 0;
        for (ListNode node = head; node != null; node = node.next) {
            StatusHistory item = (StatusHistory) node.data;
            if (item.getPowerBankId() == powerBankId) result[index++] = item;
        }
        return result;
    }

    public int count() { return size; }

    public StatusHistory[] toArray() {
        StatusHistory[] result = new StatusHistory[size];
        int index = 0;
        for (ListNode node = head; node != null; node = node.next)
            result[index++] = (StatusHistory) node.data;
        return result;
    }

    public void display() {
        if (head == null) { System.out.println("No status history found."); return; }
        ListNode current = head;
        while (current != null) {
            System.out.println(current.data);
            current = current.next;
        }
    }
}
