package com.carrental.handover;

import com.carrental.infra.Database;
import com.carrental.infra.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HandoverRepository {

    private final Database db;

    public HandoverRepository(Database db) {
        this.db = db;
        createSchema();
    }

    private void createSchema() {
        try (Connection c = db.connect(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS vehicle_condition (
                        car_id TEXT PRIMARY KEY,
                        damage_score INTEGER NOT NULL,
                        gps_online INTEGER NOT NULL)""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS handovers (
                        id TEXT PRIMARY KEY,
                        reservation_id TEXT NOT NULL,
                        car_id TEXT NOT NULL,
                        status TEXT NOT NULL)""");
        } catch (SQLException e) {
            throw new PersistenceException("Cannot create handover schema", e);
        }
    }

    // ---------- vehicle condition ----------

    /** Задать оценку повреждения; значение GPS (если строки ещё нет) по умолчанию онлайн. */
    public void setDamageScore(String carId, int score) {
        String sql = """
                INSERT INTO vehicle_condition (car_id, damage_score, gps_online) VALUES (?, ?, 1)
                ON CONFLICT(car_id) DO UPDATE SET damage_score = excluded.damage_score""";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, carId);
            ps.setInt(2, score);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot save damage score for " + carId, e);
        }
    }

    /** Включить/выключить GPS-устройство машины; оценка повреждения (если строки ещё нет) равна 0. */
    public void setGpsOnline(String carId, boolean online) {
        String sql = """
                INSERT INTO vehicle_condition (car_id, damage_score, gps_online) VALUES (?, 0, ?)
                ON CONFLICT(car_id) DO UPDATE SET gps_online = excluded.gps_online""";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, carId);
            ps.setInt(2, online ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot save GPS state for " + carId, e);
        }
    }

    /** Оценка повреждения; если данных нет, считаем 0 (машина целая). */
    public int getDamageScore(String carId) {
        String sql = "SELECT damage_score FROM vehicle_condition WHERE car_id = ?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, carId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("damage_score") : 0;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read damage score for " + carId, e);
        }
    }

    /** Онлайн ли GPS-устройство; если данных нет, считаем что онлайн. */
    public boolean isGpsOnline(String carId) {
        String sql = "SELECT gps_online FROM vehicle_condition WHERE car_id = ?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, carId);
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next() || rs.getInt("gps_online") == 1;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read GPS state for " + carId, e);
        }
    }

    // ---------- handovers ----------

    public void save(Handover h) {
        String sql = """
                INSERT OR REPLACE INTO handovers (id, reservation_id, car_id, status)
                VALUES (?, ?, ?, ?)""";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, h.getId());
            ps.setString(2, h.getReservationId());
            ps.setString(3, h.getCarId());
            ps.setString(4, h.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot save handover " + h.getId(), e);
        }
    }

    public Optional<Handover> findById(String id) {
        String sql = "SELECT * FROM handovers WHERE id = ?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read handover " + id, e);
        }
    }

    public List<Handover> findAll() {
        List<Handover> list = new ArrayList<>();
        try (Connection c = db.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM handovers ORDER BY id")) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read handovers", e);
        }
    }

    /** Есть ли у машины активная выдача (то есть работает ли трекинг)? */
    public boolean existsActiveHandover(String carId) {
        String sql = "SELECT COUNT(*) FROM handovers WHERE car_id = ? AND status = 'ACTIVE'";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, carId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot check active handover of " + carId, e);
        }
    }

    private Handover map(ResultSet rs) throws SQLException {
        return Handover.restore(
                rs.getString("id"),
                rs.getString("reservation_id"),
                rs.getString("car_id"),
                HandoverStatus.valueOf(rs.getString("status")));
    }
}