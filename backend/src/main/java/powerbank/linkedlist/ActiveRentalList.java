package powerbank.linkedlist;

import powerbank.models.RentalRecord;

/** Linked list of rentals currently in progress. */
public class ActiveRentalList {
    private ListNode head;
    private int size;

    public void insert(RentalRecord rental) {
        if (rental == null) return;
        ListNode node = new ListNode(rental);
        if (head == null) head = node;
        else {
            ListNode current = head;
            while (current.next != null) current = current.next;
            current.next = node;
        }
        size++;
    }

    public RentalRecord search(long rentalId) {
        for (ListNode node = head; node != null; node = node.next) {
            RentalRecord rental = (RentalRecord) node.data;
            if (rental.getId() == rentalId) return rental;
        }
        return null;
    }

    public RentalRecord searchByCustomerAndPowerBank(long customerId, long powerBankId) {
        for (ListNode node = head; node != null; node = node.next) {
            RentalRecord rental = (RentalRecord) node.data;
            if (rental.getCustomerId() == customerId && rental.getPowerBankId() == powerBankId) return rental;
        }
        return null;
    }

    public boolean delete(long rentalId) {
        ListNode previous = null;
        ListNode current = head;
        while (current != null) {
            if (((RentalRecord) current.data).getId() == rentalId) {
                if (previous == null) head = current.next;
                else previous.next = current.next;
                size--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    public int count() { return size; }

    public RentalRecord[] toArray() {
        RentalRecord[] result = new RentalRecord[size];
        int index = 0;
        for (ListNode node = head; node != null; node = node.next) result[index++] = (RentalRecord) node.data;
        return result;
    }

    public void display() {
        if (head == null) { System.out.println("No active rentals."); return; }
        for (ListNode node = head; node != null; node = node.next) System.out.println(node.data);
    }
}
