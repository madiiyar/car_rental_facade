package com.carrental.fleet;

import com.carrental.infra.Database;
import com.carrental.infra.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
        createSchema();
    }

    private void createSchema() {
        try (Connection c = db.connect(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS cars (
                        id TEXT PRIMARY KEY,
                        model TEXT NOT NULL,
                        category TEXT NOT NULL,
                        daily_rate INTEGER NOT NULL)""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id TEXT PRIMARY KEY,
                        car_id TEXT NOT NULL,
                        customer_id TEXT NOT NULL,
                        start_date TEXT NOT NULL,
                        end_date TEXT NOT NULL,
                        total_price INTEGER NOT NULL,
                        status TEXT NOT NULL)""");
        } catch (SQLException e) {
            throw new PersistenceException("Cannot create fleet schema", e);
        }
    }

    // ---------- cars ----------

    public void saveCar(Car car) {
        String sql = "INSERT OR REPLACE INTO cars (id, model, category, daily_rate) VALUES (?, ?, ?, ?)";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, car.id());
            ps.setString(2, car.model());
            ps.setString(3, car.category());
            ps.setLong(4, car.dailyRate());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot save car " + car.id(), e);
        }
    }

    public Optional<Car> findCar(String id) {
        String sql = "SELECT id, model, category, daily_rate FROM cars WHERE id = ?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapCar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read car " + id, e);
        }
    }

    public List<Car> findAllCars() {
        List<Car> cars = new ArrayList<>();
        try (Connection c = db.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, model, category, daily_rate FROM cars ORDER BY id")) {
            while (rs.next()) {
                cars.add(mapCar(rs));
            }
            return cars;
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read cars", e);
        }
    }

    // ---------- reservations ----------

    /** Вставка или обновление: одна и та же команда сохраняет и новую бронь, и изменённый статус. */
    public void save(Reservation r) {
        String sql = """
                INSERT OR REPLACE INTO reservations
                (id, car_id, customer_id, start_date, end_date, total_price, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getCarId());
            ps.setString(3, r.getCustomerId());
            ps.setString(4, r.getStartDate().toString());
            ps.setString(5, r.getEndDate().toString());
            ps.setLong(6, r.getTotalPrice());
            ps.setString(7, r.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot save reservation " + r.getId(), e);
        }
    }

    public Optional<Reservation> findById(String id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapReservation(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read reservation " + id, e);
        }
    }

    public List<Reservation> findAll() {
        List<Reservation> list = new ArrayList<>();
        try (Connection c = db.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM reservations ORDER BY start_date, id")) {
            while (rs.next()) {
                list.add(mapReservation(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read reservations", e);
        }
    }

    /** Есть ли активная (HELD/CONFIRMED) бронь, пересекающаяся с [start, end)? */
    public boolean existsActiveOverlap(String carId, LocalDate start, LocalDate end) {
        String sql = """
                SELECT COUNT(*) FROM reservations
                WHERE car_id = ? AND status IN ('HELD', 'CONFIRMED')
                  AND start_date < ? AND end_date > ?""";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, carId);
            ps.setString(2, end.toString());
            ps.setString(3, start.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot check availability of " + carId, e);
        }
    }

    // ---------- mapping ----------

    private Car mapCar(ResultSet rs) throws SQLException {
        return new Car(rs.getString("id"), rs.getString("model"),
                rs.getString("category"), rs.getLong("daily_rate"));
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return Reservation.restore(
                rs.getString("id"),
                rs.getString("car_id"),
                rs.getString("customer_id"),
                LocalDate.parse(rs.getString("start_date")),
                LocalDate.parse(rs.getString("end_date")),
                rs.getLong("total_price"),
                ReservationStatus.valueOf(rs.getString("status")));
    }
}