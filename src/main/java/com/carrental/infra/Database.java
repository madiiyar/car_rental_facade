package com.carrental.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Единственная точка, которая знает, ГДЕ лежит файл БД.
 * Не знает ничего о домене: только выдаёт соединения.
 */
public final class Database {

    private final Path file;

    public Database(Path file) {
        this.file = file;
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);   // создаём папку data/, если её нет
            }
        } catch (IOException e) {
            throw new PersistenceException("Cannot create data directory", e);
        }
    }

    /** Новое соединение на каждую операцию: никакого кэша в памяти. */
    public Connection connect() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        } catch (SQLException e) {
            throw new PersistenceException("Cannot open database " + file, e);
        }
    }

    /** Сброс к чистому состоянию: просто удаляем файл. */
    public void reset() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new PersistenceException("Cannot reset database " + file, e);
        }
    }
}