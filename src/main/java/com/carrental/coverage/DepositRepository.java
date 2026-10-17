package com.carrental.coverage;

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

public class DepositRepository {

    private final Database db;

    public DepositRepository(Database db) {
        this.db = db;
        createSchema();
    }

    private void createSchema() {
        try (Connection c = db.connect(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS cards (
                        customer_id TEXT PRIMARY KEY,
                        credit_limit INTEGER NOT NULL)""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS deposits (
                        id TEXT PRIMARY KEY,
                        reservation_id TEXT NOT NULL,
                        customer_id TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        status TEXT NOT NULL)""");
        } catch (SQLException e) {
            throw new PersistenceException("Cannot create coverage schema", e);
        }
    }

    // ---------- card limits ----------

    public void saveCardLimit(String customerId, long limit) {
        String sql = "INSERT OR REPLACE INTO cards (customer_id, credit_limit) VALUES (?, ?)";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.setLong(2, limit);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot save card limit for " + customerId, e);
        }
    }

    /** Лимит карты клиента; если карты нет, считаем лимит равным 0. */
    public long getCardLimit(String customerId) {
        String sql = "SELECT credit_limit FROM cards WHERE customer_id = ?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("credit_limit") : 0;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read card limit for " + customerId, e);
        }
    }

    /** Сумма активных холдов клиента (AUTHORIZED и CAPTURED). */
    public long sumActiveAmount(String customerId) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) FROM deposits
                WHERE customer_id = ? AND status IN ('AUTHORIZED', 'CAPTURED')""";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot sum deposits for " + customerId, e);
        }
    }

    // ---------- deposits ----------

    public void save(Deposit d) {
        String sql = """
                INSERT OR REPLACE INTO deposits (id, reservation_id, customer_id, amount, status)
                VALUES (?, ?, ?, ?, ?)""";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getReservationId());
            ps.setString(3, d.getCustomerId());
            ps.setLong(4, d.getAmount());
            ps.setString(5, d.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot save deposit " + d.getId(), e);
        }
    }

    public Optional<Deposit> findById(String id) {
        String sql = "SELECT * FROM deposits WHERE id = ?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read deposit " + id, e);
        }
    }

    public List<Deposit> findAll() {
        List<Deposit> list = new ArrayList<>();
        try (Connection c = db.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM deposits ORDER BY id")) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Cannot read deposits", e);
        }
    }

    private Deposit map(ResultSet rs) throws SQLException {
        return Deposit.restore(
                rs.getString("id"),
                rs.getString("reservation_id"),
                rs.getString("customer_id"),
                rs.getLong("amount"),
                DepositStatus.valueOf(rs.getString("status")));
    }
}