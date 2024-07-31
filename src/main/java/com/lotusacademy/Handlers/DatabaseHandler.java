package com.lotusacademy.Handlers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseHandler {
    private static final String DB_URL = "jdbc:sqlite:punishments.db";
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS punishments (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT NOT NULL," +
                        "user_id TEXT NOT NULL," +
                        "punishment_id TEXT NOT NULL," +
                        "reason TEXT NOT NULL);";
                try (PreparedStatement pstmt = conn.prepareStatement(createTableSQL)) {
                    pstmt.execute();
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void addPunishment(String username, String userId, String punishmentId, String reason) {
        String sql = "INSERT INTO punishments(username, user_id, punishment_id, reason) VALUES(?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, userId);
            pstmt.setString(3, punishmentId);
            pstmt.setString(4, reason);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static ResultSet getPunishmentsByUserId(String userId) {
        String sql = "SELECT * FROM punishments WHERE user_id = ?";

        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static ResultSet getPunishmentsByUsername(String username) {
        String sql = "SELECT * FROM punishments WHERE username = ?";

        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static ResultSet getPunishmentById(String punishmentId) {
        String sql = "SELECT * FROM punishments WHERE punishment_id = ?";

        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, punishmentId);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static void removePunishment(String punishmentId) {
        String sql = "DELETE FROM punishments WHERE punishment_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, punishmentId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
