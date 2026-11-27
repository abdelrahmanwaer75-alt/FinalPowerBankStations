package powerbank;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import powerbank.bst.PowerBankBST;
import powerbank.linkedlist.ActiveRentalList;
import powerbank.linkedlist.RentalHistoryList;
import powerbank.linkedlist.StatusHistoryList;
import powerbank.models.PowerBank;
import powerbank.models.RentalRecord;
import powerbank.models.Reservation;
import powerbank.models.Station;
import powerbank.models.StatusHistory;
import powerbank.models.WaitingRequest;
import powerbank.queue.GlobalWaitingQueue;
import powerbank.queue.ReservationQueue;
import powerbank.sorting.Sorting;
import powerbank.utils.InputHelper;

/**
 * Database-backed Data Structures console. It loads a fresh in-memory snapshot
 * from the project's MySQL database whenever the user chooses Reload.
 * Configure DB_URL, DB_USER and DB_PASSWORD as environment variables.
 */
public final class DatabaseMain {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/powerbank_stations?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Africa/Cairo";

    private DatabaseMain() { }

    public static void main(String[] args) {
        String url = env("DB_URL", DEFAULT_URL);
        String user = env("DB_USER", "root");
        String password = env("DB_PASSWORD", "");
        Scanner scanner = new Scanner(System.in);
        InputHelper input = new InputHelper(scanner);
        Snapshot snapshot;

        try {
            snapshot = loadSnapshot(url, user, password);
            System.out.println("Connected to MySQL. Loaded current project data.");
        } catch (SQLException exception) {
            System.err.println("Could not load the project database: " + exception.getMessage());
            System.err.println("Check MySQL, DB_URL/DB_USER/DB_PASSWORD, and run the status-history migration if it is an older database.");
            scanner.close();
            return;
        }

        int choice;
        do {
            System.out.println("\n========== POWERBANK DATA STRUCTURES / MYSQL ==========");
            System.out.println("1. PowerBank inventory (BST, ID order)");
            System.out.println("2. Stations sorted by availability");
            System.out.println("3. Active reservations");
            System.out.println("4. Global waiting queue");
            System.out.println("5. Active rentals");
            System.out.println("6. Completed rental history");
            System.out.println("7. PowerBank status history");
            System.out.println("8. Reload data from MySQL");
            System.out.println("9. Reservations sorted by request date");
            System.out.println("10. Completed rentals sorted by start date");
            System.out.println("11. Data structure counts");
            System.out.println("0. Exit");
            choice = input.readInt("Choose: ");
            switch (choice) {
                case 1 -> snapshot.powerBanks.displayInOrder();
                case 2 -> {
                    Station[] stations = snapshot.stations.clone();
                    Sorting.sortStationsByAvailability(stations);
                    print(stations);
                }
                case 3 -> snapshot.reservations.display();
                case 4 -> snapshot.waitingQueue.display();
                case 5 -> snapshot.activeRentals.display();
                case 6 -> snapshot.rentalHistory.display();
                case 7 -> snapshot.statusHistory.display();
                case 8 -> {
                    try {
                        snapshot = loadSnapshot(url, user, password);
                        System.out.println("Database snapshot refreshed.");
                    } catch (SQLException exception) {
                        System.err.println("Reload failed: " + exception.getMessage());
                    }
                }
                case 9 -> {
                    Reservation[] reservations = snapshot.reservations.toArray();
                    Sorting.sortReservationsByDate(reservations);
                    print(reservations);
                }
                case 10 -> {
                    RentalRecord[] rentals = snapshot.rentalHistory.toArray();
                    Sorting.sortRentalsByStartDate(rentals);
                    print(rentals);
                }
                case 11 -> snapshot.printCounts();
                case 0 -> System.out.println("Goodbye.");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 0);
        scanner.close();
    }

    private static Snapshot loadSnapshot(String url, String user, String password) throws SQLException {
        Snapshot data = new Snapshot();
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            loadStations(connection, data);
            loadPowerBanks(connection, data);
            loadReservations(connection, data);
            loadWaitingQueue(connection, data);
            loadRentals(connection, data);
            loadStatusHistory(connection, data);
        }
        return data;
    }

    private static void loadStations(Connection connection, Snapshot data) throws SQLException {
        String sql = "SELECT s.id, s.name, s.status, s.total_capacity, "
                + "COUNT(p.id) AS bank_count, "
                + "SUM(CASE WHEN p.status='AVAILABLE' THEN 1 ELSE 0 END) AS available_count, "
                + "COALESCE((SELECT pp.price_per_minute FROM pricing_plans pp WHERE pp.is_active=TRUE "
                + "ORDER BY pp.effective_from DESC, pp.id DESC LIMIT 1), 0) AS price_per_minute "
                + "FROM stations s LEFT JOIN powerbanks p ON p.station_id=s.id "
                + "GROUP BY s.id, s.name, s.status, s.total_capacity ORDER BY s.id";
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                int total = rows.getInt("bank_count");
                int available = rows.getInt("available_count");
                data.addStation(new Station(rows.getLong("id"), rows.getString("name"), total, available,
                        "ACTIVE".equals(rows.getString("status")), rows.getDouble("price_per_minute")));
            }
        }
    }

    private static void loadPowerBanks(Connection connection, Snapshot data) throws SQLException {
        String sql = "SELECT id, station_id, status FROM powerbanks ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                long stationId = rows.getLong("station_id");
                if (rows.wasNull()) stationId = 0;
                data.powerBanks.insert(new PowerBank(rows.getLong("id"), rows.getString("status"), stationId));
            }
        }
    }

    private static void loadReservations(Connection connection, Snapshot data) throws SQLException {
        String sql = "SELECT id, user_id, station_id, powerbank_id, created_at, status FROM reservations "
                + "WHERE status IN ('PENDING','CONFIRMED','ACTIVE') ORDER BY created_at, id";
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                long bankId = rows.getLong("powerbank_id");
                if (rows.wasNull()) bankId = 0;
                data.reservations.enqueue(new Reservation(rows.getLong("id"), rows.getLong("user_id"),
                        rows.getLong("station_id"), bankId, date(rows, "created_at"), rows.getString("status")));
            }
        }
    }

    private static void loadWaitingQueue(Connection connection, Snapshot data) throws SQLException {
        String sql = "SELECT id, user_id, station_id, created_at, status FROM waiting_list "
                + "WHERE status='WAITING' ORDER BY created_at, id";
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                data.waitingQueue.enqueue(new WaitingRequest(rows.getLong("id"), rows.getLong("user_id"),
                        rows.getLong("station_id"), date(rows, "created_at"), rows.getString("status")));
            }
        }
    }

    private static void loadRentals(Connection connection, Snapshot data) throws SQLException {
        String activeSql = "SELECT id,user_id,powerbank_id,pickup_station_id,start_time,end_time,"
                + "COALESCE(final_cost,0) AS cost "
                + "FROM rentals WHERE status='ACTIVE' ORDER BY start_time,id";
        try (PreparedStatement statement = connection.prepareStatement(activeSql); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                data.activeRentals.insert(rental(rows, 0));
            }
        }

        String historySql = "SELECT r.id,r.user_id,r.powerbank_id,r.pickup_station_id,ret.return_station_id,"
                + "r.start_time,r.end_time,COALESCE(ret.final_cost,r.final_cost,0) AS cost "
                + "FROM rentals r LEFT JOIN returns ret ON ret.rental_id=r.id "
                + "WHERE r.status='COMPLETED' ORDER BY r.end_time,r.id";
        try (PreparedStatement statement = connection.prepareStatement(historySql); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                long returnStation = rows.getLong("return_station_id");
                if (rows.wasNull()) returnStation = 0;
                data.rentalHistory.insert(rental(rows, returnStation));
            }
        }
    }

    private static RentalRecord rental(ResultSet rows, long returnStationId) throws SQLException {
        long startStation = rows.getLong("pickup_station_id");
        String endTime = date(rows, "end_time");
        return new RentalRecord(rows.getLong("id"), rows.getLong("user_id"), rows.getLong("powerbank_id"),
                startStation, returnStationId, date(rows, "start_time"), endTime, rows.getDouble("cost"));
    }

    private static void loadStatusHistory(Connection connection, Snapshot data) throws SQLException {
        String sql = "SELECT powerbank_id,old_status,new_status,changed_at FROM powerbank_status_history ORDER BY changed_at,id";
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) data.statusHistory.insert(new StatusHistory(rows.getLong("powerbank_id"),
                    rows.getString("old_status"), rows.getString("new_status"), date(rows, "changed_at")));
        }
    }

    private static String date(ResultSet rows, String column) throws SQLException {
        java.sql.Timestamp value = rows.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime().format(DATE_FORMAT);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void print(Station[] stations) {
        if (stations.length == 0) System.out.println("No stations found.");
        for (Station station : stations) System.out.println(station);
    }

    private static void print(Reservation[] reservations) {
        if (reservations.length == 0) System.out.println("No active reservations.");
        for (Reservation reservation : reservations) System.out.println(reservation);
    }

    private static void print(RentalRecord[] rentals) {
        if (rentals.length == 0) System.out.println("No completed rentals.");
        for (RentalRecord rental : rentals) System.out.println(rental);
    }

    private static final class Snapshot {
        private final PowerBankBST powerBanks = new PowerBankBST();
        private final ReservationQueue reservations = new ReservationQueue();
        private final GlobalWaitingQueue waitingQueue = new GlobalWaitingQueue();
        private final ActiveRentalList activeRentals = new ActiveRentalList();
        private final RentalHistoryList rentalHistory = new RentalHistoryList();
        private final StatusHistoryList statusHistory = new StatusHistoryList();
        private Station[] stations = new Station[0];

        private void addStation(Station station) {
            Station[] expanded = new Station[stations.length + 1];
            System.arraycopy(stations, 0, expanded, 0, stations.length);
            expanded[stations.length] = station;
            stations = expanded;
        }

        private void printCounts() {
            System.out.println("Stations: " + stations.length);
            System.out.println("PowerBanks: " + powerBanks.count());
            System.out.println("Active reservations: " + reservations.count());
            System.out.println("Waiting requests: " + waitingQueue.count());
            System.out.println("Active rentals: " + activeRentals.count());
            System.out.println("Completed rentals: " + rentalHistory.count());
            System.out.println("Status events: " + statusHistory.count());
        }
    }
}
