package com.carrental.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    @TempDir
    Path tempDir;   // JUnit даёт каждому тесту свою чистую папку

    @Test
    void dataSurvivesReopeningTheDatabase() throws Exception {
        Path file = tempDir.resolve("test.db");

        // "первый запуск приложения": пишем данные
        Database first = new Database(file);
        try (Connection c = first.connect(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE t (id INTEGER, name TEXT)");
            st.execute("INSERT INTO t VALUES (1, 'hello')");
        }

        // "рестарт": новый объект на том же файле
        Database second = new Database(file);
        try (Connection c = second.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM t WHERE id = 1")) {
            assertTrue(rs.next());
            assertEquals("hello", rs.getString("name"));
        }
    }

    @Test
    void resetRemovesEverything() throws Exception {
        Path file = tempDir.resolve("test.db");
        Database db = new Database(file);
        try (Connection c = db.connect(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE t (id INTEGER)");
        }
        db.reset();
        assertFalse(file.toFile().exists());
    }
}