package com.carrental.facade;

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

public class RentalChainRepository {

    private final Database db;

    public RentalChainRepository(Database db) {
        this.db = db;
        createSchema();
    }

    private void createSchema() {
        try (Connection c = db.connect(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS rental_chains (
                        id TEXT PRIMARY KEY,
                        customer_id TEXT NOT NULL,
                        car_id TEXT NOT NULL,
                        reservation_id TEXT NOT NULL,
                        deposit_id TEXT,
                        status TEXT NOT NULL)""");
        } catch (SQLException e) {
            throw new PersistenceException("Cannot create chain schema", e);
        }
    }

    public void save(RentalChain chain) {
        String sql = """
                INSERT OR REPLACE INTO rental_chains
                (id, customer_id, car_id, reservation_id, deposit_id, status)
                VALUES (?, ?, ?, ?, ?, ?)""";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, chain.getId());
            ps.setString(2, chain.getCustomerId());
            ps.setString(3, chain.getCarId());
            ps.setString(4, chain.getReservationId());
            ps.setString(5, chain.getDepositId());   // может быть null: в БД будет NULL
            ps.setString(6, chain.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot save chain " + chain.getId(), e);
        }
    }

    public Optional<RentalChain> findById(String id) {
        String sql = "SELECT * FROM rental_chains WHERE id = ?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read chain " + id, e);
        }
    }

    public List<RentalChain> findAll() {
        List<RentalChain> list = new ArrayList<>();
        try (Connection c = db.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM rental_chains ORDER BY id")) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read chains", e);
        }
    }

    public List<RentalChain> findByStatus(RentalChainStatus status) {
        List<RentalChain> list = new ArrayList<>();
        String sql = "SELECT * FROM rental_chains WHERE status = ? ORDER BY id";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read chains by status " + status, e);
        }
    }

    private RentalChain map(ResultSet rs) throws SQLException {
        return RentalChain.restore(
                rs.getString("id"),
                rs.getString("customer_id"),
                rs.getString("car_id"),
                rs.getString("reservation_id"),
                rs.getString("deposit_id"),   // для NULL вернётся null
                RentalChainStatus.valueOf(rs.getString("status")));
    }
}