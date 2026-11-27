package datastructure.queue;

import datastructure.models.PowerBank;
import datastructure.models.WaitingRequest;

/** A single FIFO queue across all stations; allocation skips requests for other stations. */
public class GlobalWaitingQueue {
    private QueueNode front;
    private QueueNode rear;
    private int size;

    public void enqueue(WaitingRequest request) {
        if (request == null || !"WAITING".equals(request.getStatus())) return;
        QueueNode node = new QueueNode(request);
        if (rear == null) front = rear = node;
        else { rear.next = node; rear = node; }
        size++;
    }

    public WaitingRequest dequeue() {
        if (front == null) return null;
        QueueNode node = front;
        front = front.next;
        if (front == null) rear = null;
        size--;
        return (WaitingRequest) node.data;
    }

    /** Station-aware dequeue: reserves the supplied available unit for the oldest eligible request. */
    public WaitingRequest dequeue(PowerBank availableBank) {
        return processNext(availableBank);
    }

    public WaitingRequest peek() { return front == null ? null : (WaitingRequest) front.data; }
    public boolean isEmpty() { return front == null; }
    public int count() { return size; }

    public WaitingRequest search(long requestId) {
        for (QueueNode node = front; node != null; node = node.next) {
            WaitingRequest request = (WaitingRequest) node.data;
            if (request.getRequestId() == requestId) return request;
        }
        return null;
    }

    public WaitingRequest searchByCustomerId(long customerId) {
        for (QueueNode node = front; node != null; node = node.next) {
            WaitingRequest request = (WaitingRequest) node.data;
            if (request.getCustomerId() == customerId && "WAITING".equals(request.getStatus())) return request;
        }
        return null;
    }

    public boolean cancelRequest(long requestId) {
        QueueNode previous = null;
        QueueNode current = front;
        while (current != null) {
            WaitingRequest request = (WaitingRequest) current.data;
            if (request.getRequestId() == requestId) {
                unlink(previous, current);
                request.setStatus("CANCELLED");
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    /** Returns the oldest still-waiting customer whose preferred station matches this unit. */
    public WaitingRequest findNextEligible(PowerBank bank) {
        if (bank == null || !"AVAILABLE".equals(bank.getStatus())) return null;
        for (QueueNode node = front; node != null; node = node.next) {
            WaitingRequest request = (WaitingRequest) node.data;
            if ("WAITING".equals(request.getStatus()) && request.getPreferredStationId() == bank.getStationId()) return request;
        }
        return null;
    }

    /** Removes and assigns the oldest eligible customer. Other requests retain their FIFO order. */
    public WaitingRequest processNext(PowerBank bank) {
        if (bank == null || !"AVAILABLE".equals(bank.getStatus())) return null;
        QueueNode previous = null;
        QueueNode current = front;
        while (current != null) {
            WaitingRequest request = (WaitingRequest) current.data;
            if ("WAITING".equals(request.getStatus()) && request.getPreferredStationId() == bank.getStationId()) {
                unlink(previous, current);
                request.assign(bank.getId());
                bank.setStatus("RESERVED");
                return request;
            }
            previous = current;
            current = current.next;
        }
        return null;
    }

    public WaitingRequest[] toArray() {
        WaitingRequest[] result = new WaitingRequest[size];
        int index = 0;
        for (QueueNode node = front; node != null; node = node.next) result[index++] = (WaitingRequest) node.data;
        return result;
    }

    public void display() {
        if (isEmpty()) { System.out.println("No customers are waiting."); return; }
        int position = 1;
        for (QueueNode node = front; node != null; node = node.next)
            System.out.println(position++ + ". " + node.data);
    }

    private void unlink(QueueNode previous, QueueNode current) {
        if (previous == null) front = current.next;
        else previous.next = current.next;
        if (current == rear) rear = previous;
        size--;
    }
}
