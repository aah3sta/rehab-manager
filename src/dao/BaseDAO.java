package dao;

import database.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared JDBC plumbing for the module 4-8 DAOs: every query uses a
 * PreparedStatement and try-with-resources, so subclasses only write SQL
 * and a map() method.
 */
public abstract class BaseDAO<T> {

    /** Turns the current ResultSet row into a model object. */
    protected abstract T map(ResultSet rs) throws SQLException;

    protected List<T> queryList(String sql, Object... params) throws SQLException {
        List<T> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    protected T queryOne(String sql, Object... params) throws SQLException {
        List<T> list = queryList(sql, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /** INSERT that returns the new AUTO_INCREMENT id, or -1. */
    protected long insert(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(stmt, params);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    /** UPDATE / DELETE; returns true if a row changed. */
    protected boolean execute(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, params);
            return stmt.executeUpdate() > 0;
        }
    }

    /** For COUNT / SUM queries that return one number. */
    protected long queryLong(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private static void bind(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof LocalDate) stmt.setDate(i + 1, Date.valueOf((LocalDate) p));
            else if (p instanceof LocalDateTime) stmt.setTimestamp(i + 1, Timestamp.valueOf((LocalDateTime) p));
            else if (p == null) stmt.setNull(i + 1, Types.VARCHAR);
            else stmt.setObject(i + 1, p);
        }
    }

    /**
     * Shared "filter by patient / staff / day" query for the GUI screens.
     * Table and column names come from the subclass (fixed strings), never from user input.
     */
    protected List<T> filter(String table, String dateColumn, Long patientId, Long staffId, LocalDate day)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + table + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (patientId != null) { sql.append(" AND PATIENT_ID = ?"); params.add(patientId); }
        if (staffId != null)   { sql.append(" AND STAFF_ID = ?");   params.add(staffId); }
        if (day != null) {
            sql.append(" AND ").append(dateColumn).append(" >= ? AND ").append(dateColumn).append(" < ?");
            params.add(day.atStartOfDay());
            params.add(day.plusDays(1).atStartOfDay());
        }
        sql.append(" ORDER BY ").append(dateColumn).append(" DESC");
        return queryList(sql.toString(), params.toArray());
    }

    // ---- small helpers for map() ----

    protected static LocalDateTime toDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    protected static LocalDate toDate(Date d) {
        return d == null ? null : d.toLocalDate();
    }

    /** [start of day, start of next day) – keeps date indexes usable. */
    protected static Object[] dayRange(LocalDate day) {
        return new Object[]{day.atStartOfDay(), day.plusDays(1).atStartOfDay()};
    }
}
