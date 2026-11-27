package datastructure.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import datastructure.bst.PowerBankBST;
import datastructure.linkedlist.RentalHistoryList;
import datastructure.linkedlist.ActiveRentalList;
import datastructure.linkedlist.StatusHistoryList;
import datastructure.models.PowerBank;
import datastructure.models.RentalRecord;
import datastructure.models.Reservation;
import datastructure.models.Station;
import datastructure.models.StatusHistory;
import datastructure.queue.ReservationQueue;
import datastructure.queue.GlobalWaitingQueue;
import datastructure.sorting.Sorting;

public class PowerBankService {
    private final PowerBankBST powerBanks = new PowerBankBST();
    private final ReservationQueue reservations = new ReservationQueue();
    private final GlobalWaitingQueue waitingRequests = new GlobalWaitingQueue();
    private final RentalHistoryList rentalHistory = new RentalHistoryList();
    private final ActiveRentalList activeRentals = new ActiveRentalList();
    private final StatusHistoryList statusHistory = new StatusHistoryList();

    private Station[] stations = new Station[10];
    private int stationCount;
    private int nextReservationId = 1;
    private int nextRentalId = 1;
    private int nextWaitingRequestId = 1;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PowerBankService() {
        this(true);
    }

    public PowerBankService(boolean seedDemoData) {
        if (!seedDemoData) return;
        addStation(new Station(1, "Main Gate", 0, 0, true, 0.166667));
        addStation(new Station(2, "Library", 0, 0, true, 0.200000));
        addStation(new Station(3, "Food Court", 0, 0, true, 0.133333));
        addPowerBank(101, 1);
        addPowerBank(102, 1);
        addPowerBank(103, 1);
        addPowerBank(201, 2);
        addPowerBank(202, 2);
        addPowerBank(301, 3);
        addPowerBank(302, 3);
    }

    public PowerBankBST getPowerBanks() { return powerBanks; }
    public ReservationQueue getReservations() { return reservations; }
    public GlobalWaitingQueue getWaitingRequests() { return waitingRequests; }
    public RentalHistoryList getRentalHistory() { return rentalHistory; }
    public ActiveRentalList getActiveRentals() { return activeRentals; }
    public StatusHistoryList getStatusHistory() { return statusHistory; }

    public boolean addStation(Station station) {
        if (station == null || station.getId() <= 0 || station.getName() == null
                || station.getName().trim().isEmpty() || station.getPricePerMinute() < 0
                || findStation(station.getId()) != null) return false;
        ensureStationCapacity();
        stations[stationCount++] = station;
        return true;
    }

    public Station findStation(long id) {
        for (int i = 0; i < stationCount; i++)
            if (stations[i].getId() == id) return stations[i];
        return null;
    }

    public boolean updateStation(long id, String name, double price) {
        Station station = findStation(id);
        if (station == null || name == null || name.trim().isEmpty() || price < 0) return false;
        station.setName(name);
        station.setPricePerMinute(price);
        return true;
    }

    public boolean deactivateStation(long id) {
        Station station = findStation(id);
        if (station == null) return false;
        station.setActive(false);
        return true;
    }

    public void displayStations() {
        if (stationCount == 0) { System.out.println("No stations found."); return; }
        for (int i = 0; i < stationCount; i++) System.out.println(stations[i]);
    }

    public Station[] getStationsSortedByAvailability() {
        Station[] result = new Station[stationCount];
        for (int i = 0; i < stationCount; i++) result[i] = stations[i];
        Sorting.sortStationsByAvailability(result);
        return result;
    }

    public boolean addPowerBank(long id, long stationId) {
        Station station = findStation(stationId);
        if (id <= 0 || station == null || !station.isActive() || powerBanks.search(id) != null) return false;
        powerBanks.insert(new PowerBank(id, "AVAILABLE", stationId));
        station.setTotalPowerBanks(station.getTotalPowerBanks() + 1);
        station.setAvailablePowerBanks(station.getAvailablePowerBanks() + 1);
        return true;
    }

    public PowerBank searchPowerBank(long id) {
        return powerBanks.search(id);
    }

    public boolean deletePowerBank(long id) {
        PowerBank bank = powerBanks.search(id);
        if (bank == null || "RENTED".equals(bank.getStatus()) || "RESERVED".equals(bank.getStatus())) return false;
        Station station = findStation(bank.getStationId());
        if (station != null) {
            station.setTotalPowerBanks(Math.max(0, station.getTotalPowerBanks() - 1));
            if ("AVAILABLE".equals(bank.getStatus()))
                station.setAvailablePowerBanks(Math.max(0, station.getAvailablePowerBanks() - 1));
        }
        return powerBanks.delete(id);
    }

    public boolean updatePowerBankStatus(long id, String newStatus) {
        PowerBank bank = powerBanks.search(id);
        if (bank == null || newStatus == null) return false;
        String status = newStatus.trim().toUpperCase();
        if (!status.equals("AVAILABLE") && !status.equals("MAINTENANCE")) return false;
        if (bank.getStatus().equals("RENTED") || bank.getStatus().equals("RESERVED")) return false;
        if (bank.getStatus().equals(status)) return true;
        Station station = findStation(bank.getStationId());
        if (station != null) {
            if (bank.getStatus().equals("AVAILABLE") && status.equals("MAINTENANCE"))
                station.setAvailablePowerBanks(Math.max(0, station.getAvailablePowerBanks() - 1));
            else if (bank.getStatus().equals("MAINTENANCE") && status.equals("AVAILABLE"))
                station.setAvailablePowerBanks(station.getAvailablePowerBanks() + 1);
        }
        changeStatus(bank, status);
        return true;
    }

    public boolean reservePowerBank(long customerId, long stationId) {
        Station station = findStation(stationId);
        if (customerId <= 0 || station == null || !station.isActive()) return false;
        PowerBank bank = findAvailableBank(stationId);
        if (bank != null && waitingRequests.findNextEligible(bank) != null) {
            waitingRequests.enqueue(new datastructure.models.WaitingRequest(nextWaitingRequestId++, customerId,
                    stationId, LocalDateTime.now().format(formatter), "WAITING"));
            processNextWaiter(bank);
            return false;
        }
        if (bank == null) {
            waitingRequests.enqueue(new datastructure.models.WaitingRequest(nextWaitingRequestId++, customerId,
                    stationId, LocalDateTime.now().format(formatter), "WAITING"));
            return false;
        }
        changeStatus(bank, "RESERVED");
        station.setAvailablePowerBanks(Math.max(0, station.getAvailablePowerBanks() - 1));
        Reservation reservation = new Reservation(nextReservationId++, customerId, stationId,
                bank.getId(), LocalDateTime.now().format(formatter), "PENDING");
        reservations.enqueue(reservation);
        return true;
    }

    public boolean startRental(long customerId, long powerBankId) {
        PowerBank bank = powerBanks.search(powerBankId);
        if (bank == null || customerId <= 0) return false;
        Reservation reservation = reservations.findByPowerBank(customerId, powerBankId);
        if ("RESERVED".equals(bank.getStatus())) {
            if (reservation == null) return false;
            reservation.setStatus("COMPLETED");
            reservations.removeById(reservation.getId());
        } else if (!"AVAILABLE".equals(bank.getStatus())) {
            return false;
        } else {
            Station station = findStation(bank.getStationId());
            if (station != null) station.setAvailablePowerBanks(Math.max(0, station.getAvailablePowerBanks() - 1));
        }
        changeStatus(bank, "RENTED");
        activeRentals.insert(new RentalRecord(nextRentalId++, customerId, powerBankId,
                bank.getStationId(), bank.getStationId(), LocalDateTime.now().format(formatter), null, 0));
        return true;
    }

    public boolean returnPowerBank(long customerId, long powerBankId, long returnStationId) {
        PowerBank bank = powerBanks.search(powerBankId);
        Station returnStation = findStation(returnStationId);
        RentalRecord active = activeRentals.searchByCustomerAndPowerBank(customerId, powerBankId);
        if (bank == null || returnStation == null || !returnStation.isActive()
                || !"RENTED".equals(bank.getStatus()) || active == null) return false;

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = LocalDateTime.parse(active.getStartTime(), formatter);
        long minutes = Math.max(1, Duration.between(start, end).toMinutes());
        Station startStation = findStation(active.getStartStationId());
        double rate = startStation == null ? returnStation.getPricePerMinute() : startStation.getPricePerMinute();
        double cost = minutes * rate;

        active.complete(returnStationId, end.format(formatter), cost);
        activeRentals.delete(active.getId());
        rentalHistory.insert(active);

        if (active.getStartStationId() != returnStationId && startStation != null)
            startStation.setTotalPowerBanks(Math.max(0, startStation.getTotalPowerBanks() - 1));
        if (active.getStartStationId() != returnStationId)
            returnStation.setTotalPowerBanks(returnStation.getTotalPowerBanks() + 1);

        bank.setStationId(returnStationId);
        changeStatus(bank, "AVAILABLE");
        returnStation.setAvailablePowerBanks(returnStation.getAvailablePowerBanks() + 1);
        processNextWaiter(bank);
        return true;
    }

    public boolean cancelReservation(long reservationId) {
        Reservation[] all = reservations.toArray();
        for (Reservation reservation : all) {
            if (reservation.getId() == reservationId && "PENDING".equals(reservation.getStatus())) {
                reservation.setStatus("CANCELLED");
                reservations.removeById(reservationId);
                PowerBank bank = powerBanks.search(reservation.getPowerBankId());
                if (bank != null && "RESERVED".equals(bank.getStatus())) {
                    changeStatus(bank, "AVAILABLE");
                    Station station = findStation(bank.getStationId());
                    if (station != null) {
                        station.setAvailablePowerBanks(station.getAvailablePowerBanks() + 1);
                        processNextWaiter(bank);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean cancelNextReservation() {
        Reservation reservation = reservations.peek();
        return reservation != null && cancelReservation(reservation.getId());
    }

    public void displayInventory() { powerBanks.displayInOrder(); }
    public void displayWaitingQueue() { waitingRequests.display(); }
    public void displayActiveRentals() { activeRentals.display(); }
    public void displayRentalHistory() { rentalHistory.display(); }
    public void displayStatusHistory() { statusHistory.display(); }
    public void displayReservations() { reservations.display(); }

    public PowerBank[] getPowerBanksSortedById() {
        PowerBank[] result = new PowerBank[powerBanks.count()];
        powerBanks.copyToArray(result, new int[]{0});
        Sorting.sortPowerBanksById(result);
        return result;
    }

    public Reservation[] getReservationsSortedByDate() {
        Reservation[] result = reservations.toArray();
        Sorting.sortReservationsByDate(result);
        return result;
    }

    public RentalRecord[] getRentalsSortedByDate() {
        RentalRecord[] result = rentalHistory.toArray();
        Sorting.sortRentalsByDate(result);
        return result;
    }

    public void displayReports() {
        int total = 0;
        int available = 0;
        int rented = 0;
        int reserved = 0;
        int maintenance = 0;
        for (PowerBank bank : getPowerBanksSortedById()) {
            total++;
            if ("AVAILABLE".equals(bank.getStatus())) available++;
            else if ("RENTED".equals(bank.getStatus())) rented++;
            else if ("RESERVED".equals(bank.getStatus())) reserved++;
            else if ("MAINTENANCE".equals(bank.getStatus())) maintenance++;
        }
        System.out.println("Stations: " + stationCount);
        System.out.println("Total PowerBanks: " + total);
        System.out.println("Available: " + available);
        System.out.println("Rented: " + rented);
        System.out.println("Reserved: " + reserved);
        System.out.println("Maintenance: " + maintenance);
        System.out.println("Pending reservations: " + reservations.count());
        System.out.println("Completed rentals: " + rentalHistory.count());
    }

    private PowerBank findAvailableBank(long stationId) { return powerBanks.findAvailable(stationId); }

    private void processNextWaiter(PowerBank bank) {
        var request = waitingRequests.processNext(bank);
        if (request == null) return;
        Station station = findStation(bank.getStationId());
        if (station != null) station.setAvailablePowerBanks(Math.max(0, station.getAvailablePowerBanks() - 1));
        statusHistory.insert(new StatusHistory(bank.getId(), "AVAILABLE", "RESERVED", LocalDateTime.now().format(formatter)));
        reservations.enqueue(new Reservation(nextReservationId++, request.getCustomerId(), request.getPreferredStationId(),
                bank.getId(), LocalDateTime.now().format(formatter), "PENDING"));
    }

    private void changeStatus(PowerBank bank, String newStatus) {
        String oldStatus = bank.getStatus();
        bank.setStatus(newStatus);
        statusHistory.insert(new StatusHistory(bank.getId(), oldStatus, newStatus,
                LocalDateTime.now().format(formatter)));
    }

    private void ensureStationCapacity() {
        if (stationCount < stations.length) return;
        Station[] bigger = new Station[stations.length * 2];
        for (int i = 0; i < stations.length; i++) bigger[i] = stations[i];
        stations = bigger;
    }

}
