package com.rebate.dao;

import com.rebate.util.DBUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 基础 DAO：封装常见 JDBC 操作
 * <p>不引入 Spring/MyBatis，保持 Servlet 原生风格。</p>
 */
public class BaseDao {

    /**
     * 查询列表
     */
    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = new ArrayList<>();
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            c = DBUtil.getConnection();
            ps = c.prepareStatement(sql);
            bindParams(ps, params);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapper.map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("query failed: " + sql, e);
        } finally {
            DBUtil.close(c, ps, rs);
        }
        return list;
    }

    public static <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> l = query(sql, mapper, params);
        return l.isEmpty() ? null : l.get(0);
    }

    public static long count(String sql, Object... params) {
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            c = DBUtil.getConnection();
            ps = c.prepareStatement(sql);
            bindParams(ps, params);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("count failed: " + sql, e);
        } finally {
            DBUtil.close(c, ps, rs);
        }
    }

    public static int update(String sql, Object... params) {
        Connection c = null;
        PreparedStatement ps = null;
        try {
            c = DBUtil.getConnection();
            ps = c.prepareStatement(sql);
            bindParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update failed: " + sql, e);
        } finally {
            DBUtil.close(c, ps, null);
        }
    }

    /**
     * 在事务中执行一段 SQL
     */
    public static int updateInTx(String sql, Object... params) {
        Connection c = null;
        PreparedStatement ps = null;
        try {
            c = DBUtil.getConnection();
            c.setAutoCommit(false);
            ps = c.prepareStatement(sql);
            bindParams(ps, params);
            int r = ps.executeUpdate();
            c.commit();
            return r;
        } catch (SQLException e) {
            if (c != null) try { c.rollback(); } catch (Exception ignore) {}
            throw new RuntimeException("updateInTx failed: " + sql, e);
        } finally {
            DBUtil.close(c, ps, null);
        }
    }

    /**
     * 使用外部Connection执行更新（用于事务）
     */
    public static int updateWithConn(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            bindParams(ps, params);
            return ps.executeUpdate();
        } finally {
            if (ps != null) try { ps.close(); } catch (Exception ignore) {}
        }
    }

    /**
     * 使用外部Connection执行插入并返回ID（用于事务）
     */
    public static Long insertReturnIdWithConn(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            bindParams(ps, params);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
            return null;
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignore) {}
            if (ps != null) try { ps.close(); } catch (Exception ignore) {}
        }
    }

    public static Long insertReturnId(String sql, Object... params) {
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            c = DBUtil.getConnection();
            ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            bindParams(ps, params);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("insert failed: " + sql, e);
        } finally {
            DBUtil.close(c, ps, rs);
        }
    }

    public static void bindParams(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int idx = i + 1;
            if (p == null) {
                ps.setObject(idx, null);
            } else if (p instanceof String) {
                ps.setString(idx, (String) p);
            } else if (p instanceof Integer) {
                ps.setInt(idx, (Integer) p);
            } else if (p instanceof Long) {
                ps.setLong(idx, (Long) p);
            } else if (p instanceof Double) {
                ps.setDouble(idx, (Double) p);
            } else if (p instanceof Float) {
                ps.setFloat(idx, (Float) p);
            } else if (p instanceof BigDecimal) {
                ps.setBigDecimal(idx, (BigDecimal) p);
            } else if (p instanceof java.sql.Date) {
                ps.setDate(idx, (java.sql.Date) p);
            } else if (p instanceof java.sql.Timestamp) {
                ps.setTimestamp(idx, (java.sql.Timestamp) p);
            } else if (p instanceof java.util.Date) {
                ps.setTimestamp(idx, new Timestamp(((java.util.Date) p).getTime()));
            } else if (p instanceof Boolean) {
                ps.setBoolean(idx, (Boolean) p);
            } else {
                ps.setObject(idx, p);
            }
        }
    }

    public static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(o.toString());
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
