package datastructure.linkedlist;

import datastructure.models.RentalRecord;

public class RentalHistoryList {
    private ListNode head;
    private int size;

    public void insert(RentalRecord record) {
        if (record == null) return;
        ListNode node = new ListNode(record);
        if (head == null) { head = node; size++; return; }
        ListNode current = head;
        while (current.next != null) current = current.next;
        current.next = node;
        size++;
    }

    public RentalRecord search(long id) {
        ListNode current = head;
        while (current != null) {
            RentalRecord record = (RentalRecord) current.data;
            if (record.getId() == id) return record;
            current = current.next;
        }
        return null;
    }

    public int count() { return size; }

    public RentalRecord[] toArray() {
        RentalRecord[] result = new RentalRecord[count()];
        ListNode current = head;
        int i = 0;
        while (current != null) {
            result[i++] = (RentalRecord) current.data;
            current = current.next;
        }
        return result;
    }

    public void display() {
        if (head == null) { System.out.println("No rental records found."); return; }
        ListNode current = head;
        while (current != null) {
            System.out.println(current.data);
            current = current.next;
        }
    }
}
