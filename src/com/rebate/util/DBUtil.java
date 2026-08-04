package com.rebate.util;

import com.rebate.config.AppConfig;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 数据库工具类
 * <p>修复版：正确实现连接归还功能</p>
 */
public class DBUtil {

    private static final Logger log = Logger.getLogger(DBUtil.class.getName());

    private static DataSource dataSource;

    static {
        try {
            Class.forName(AppConfig.get("jdbc.driver", "org.postgresql.Driver"));
        } catch (ClassNotFoundException e) {
            log.warning("PostgreSQL driver not found, please put postgresql-42.x.jar in WEB-INF/lib");
        }
        dataSource = new SimpleDataSource(
                AppConfig.get("jdbc.url"),
                AppConfig.get("jdbc.username"),
                AppConfig.get("jdbc.password"),
                AppConfig.getInt("db.pool.initialSize", 5),
                AppConfig.getInt("db.pool.maxActive", 20)
        );
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (Exception ignore) {}
        if (ps != null) try { ps.close(); } catch (Exception ignore) {}
        if (conn != null) try { conn.close(); } catch (Exception ignore) {}
    }

    public static void close(Connection conn) {
        if (conn != null) try { conn.close(); } catch (Exception ignore) {}
    }

    /**
     * 简单连接池（修复版）
     */
    static class SimpleDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String pwd;
        private final java.util.concurrent.BlockingQueue<PooledConnection> pool;
        private final int maxActive;
        private final int maxIdle;
        private volatile int created = 0;
        private final Object createLock = new Object();

        SimpleDataSource(String url, String user, String pwd, int initSize, int maxActive) {
            this.url = url;
            this.user = user;
            this.pwd = pwd;
            this.maxActive = maxActive;
            this.maxIdle = Math.max(initSize, Math.min(10, maxActive));
            this.pool = new java.util.concurrent.LinkedBlockingQueue<>(maxActive);
            for (int i = 0; i < initSize; i++) {
                try {
                    Connection c = createConn();
                    if (c != null) {
                        pool.offer(new PooledConnection(c, this));
                        created++;
                    }
                } catch (Exception ignore) {}
            }
        }

        private Connection createConn() throws SQLException {
            Connection c = DriverManager.getConnection(url, user, pwd);
            c.setAutoCommit(true);
            return c;
        }

        void returnToPool(PooledConnection pc) {
            if (pc == null || pc.isReallyClosed()) return;
            try {
                if (pool.size() < maxIdle) {
                    if (!pc.delegate.isClosed() && pc.delegate.isValid(1)) {
                        pc.delegate.setAutoCommit(true);
                        pool.offer(pc);
                        return;
                    }
                }
                // 超过空闲连接数或连接无效，直接关闭
                synchronized (createLock) {
                    created--;
                }
                pc.reallyClose();
            } catch (Exception e) {
                pc.reallyClose();
            }
        }

        @Override
        public Connection getConnection() throws SQLException {
            PooledConnection pc = pool.poll();
            if (pc != null) {
                if (!pc.delegate.isClosed() && pc.delegate.isValid(1)) {
                    return pc;
                } else {
                    // 连接无效，关闭并减少计数
                    pc.reallyClose();
                    synchronized (createLock) {
                        created--;
                    }
                }
            }
            
            // 尝试创建新连接
            synchronized (createLock) {
                if (created < maxActive) {
                    Connection c = createConn();
                    created++;
                    return new PooledConnection(c, this);
                }
            }
            
            // 等待连接
            try {
                pc = pool.poll(30, java.util.concurrent.TimeUnit.SECONDS);
                if (pc != null && !pc.delegate.isClosed() && pc.delegate.isValid(1)) {
                    return pc;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            throw new SQLException("No available connection from pool");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return Logger.getLogger("db"); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { 
            if (iface.isInstance(this)) return (T) this;
            throw new SQLException("Not a wrapper for " + iface);
        }
        @Override public boolean isWrapperFor(Class<?> iface) { return iface.isInstance(this); }
    }

    static class PooledConnection implements Connection {
        private final Connection delegate;
        private final SimpleDataSource pool;
        private volatile boolean closed = false;

        PooledConnection(Connection delegate, SimpleDataSource pool) {
            this.delegate = delegate;
            this.pool = pool;
        }

        boolean isReallyClosed() { return closed; }

        void reallyClose() {
            if (!closed) {
                closed = true;
                try { delegate.close(); } catch (Exception ignore) {}
            }
        }

        @Override public void close() throws SQLException {
            if (!closed) {
                pool.returnToPool(this);
            }
        }

        @Override public java.sql.Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException { return delegate.prepareStatement(sql); }
        @Override public java.sql.CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { delegate.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
        @Override public void commit() throws SQLException { delegate.commit(); }
        @Override public void rollback() throws SQLException { delegate.rollback(); }
        @Override public boolean isClosed() throws SQLException { return closed; }
        @Override public java.sql.DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { delegate.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
        @Override public void setCatalog(String catalog) throws SQLException { delegate.setCatalog(catalog); }
        @Override public String getCatalog() throws SQLException { return delegate.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { delegate.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
        @Override public java.sql.SQLWarning getWarnings() throws SQLException { return delegate.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { delegate.clearWarnings(); }
        @Override public java.sql.Statement createStatement(int rsType, int rsConcurrency) throws SQLException { return delegate.createStatement(rsType, rsConcurrency); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int rsType, int rsConcurrency) throws SQLException { return delegate.prepareStatement(sql, rsType, rsConcurrency); }
        @Override public java.sql.CallableStatement prepareCall(String sql, int rsType, int rsConcurrency) throws SQLException { return delegate.prepareCall(sql, rsType, rsConcurrency); }
        @Override public java.util.Map<String, Class<?>> getTypeMap() throws SQLException { return delegate.getTypeMap(); }
        @Override public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException { delegate.setTypeMap(map); }
        @Override public void setHoldability(int holdability) throws SQLException { delegate.setHoldability(holdability); }
        @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
        @Override public java.sql.Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override public java.sql.Savepoint setSavepoint(String name) throws SQLException { return delegate.setSavepoint(name); }
        @Override public void rollback(java.sql.Savepoint savepoint) throws SQLException { delegate.rollback(savepoint); }
        @Override public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException { delegate.releaseSavepoint(savepoint); }
        @Override public java.sql.Statement createStatement(int rsType, int rsConcurrency, int rsHoldability) throws SQLException { return delegate.createStatement(rsType, rsConcurrency, rsHoldability); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int rsType, int rsConcurrency, int rsHoldability) throws SQLException { return delegate.prepareStatement(sql, rsType, rsConcurrency, rsHoldability); }
        @Override public java.sql.CallableStatement prepareCall(String sql, int rsType, int rsConcurrency, int rsHoldability) throws SQLException { return delegate.prepareCall(sql, rsType, rsConcurrency, rsHoldability); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int autoGenKeys) throws SQLException { return delegate.prepareStatement(sql, autoGenKeys); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int[] colIndexes) throws SQLException { return delegate.prepareStatement(sql, colIndexes); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, String[] colNames) throws SQLException { return delegate.prepareStatement(sql, colNames); }
        @Override public java.sql.Clob createClob() throws SQLException { return delegate.createClob(); }
        @Override public java.sql.Blob createBlob() throws SQLException { return delegate.createBlob(); }
        @Override public java.sql.NClob createNClob() throws SQLException { return delegate.createNClob(); }
        @Override public java.sql.SQLXML createSQLXML() throws SQLException { return delegate.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return delegate.isValid(timeout); }
        @Override public String getClientInfo(String name) throws SQLException { 
            try { return delegate.getClientInfo(name); } 
            catch (SQLFeatureNotSupportedException e) { return null; }
        }
        @Override public java.util.Properties getClientInfo() throws SQLException { 
            try { return delegate.getClientInfo(); } 
            catch (SQLFeatureNotSupportedException e) { return new java.util.Properties(); }
        }
        @Override public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException { return delegate.createArrayOf(typeName, elements); }
        @Override public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException { return delegate.createStruct(typeName, attributes); }
        @Override public void setSchema(String schema) throws SQLException { delegate.setSchema(schema); }
        @Override public String getSchema() throws SQLException { return delegate.getSchema(); }
        @Override public void abort(java.util.concurrent.Executor executor) throws SQLException { delegate.abort(executor); }
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int millis) throws SQLException { delegate.setNetworkTimeout(executor, millis); }
        @Override public int getNetworkTimeout() throws SQLException { return delegate.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { 
            if (iface.isInstance(delegate)) return (T) delegate;
            throw new SQLException("Not a wrapper for " + iface);
        }
        @Override public boolean isWrapperFor(Class<?> iface) { return iface.isInstance(delegate); }
        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            delegate.setClientInfo(name, value);
        }
        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            delegate.setClientInfo(properties);
        }
    }
}
