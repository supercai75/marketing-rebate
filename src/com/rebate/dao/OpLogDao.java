package com.rebate.dao;

import com.rebate.model.OpLog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志 DAO
 */
public class OpLogDao {

    private OpLog mapLog(ResultSet rs) throws SQLException {
        OpLog log = new OpLog();
        log.setId(rs.getLong("id"));
        log.setUserId(rs.getLong("user_id"));
        log.setLoginName(rs.getString("login_name"));
        log.setModule(rs.getString("module"));
        log.setAction(rs.getString("action"));
        log.setContent(rs.getString("content"));
        log.setIp(rs.getString("ip"));
        log.setOpTime(rs.getTimestamp("op_time"));
        return log;
    }

    public List<OpLog> listLogs(Long userId, String module, String action, String keyword, 
                                String startTime, String endTime, Integer page, Integer pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM sys_op_log WHERE 1=1 ");
        List<Object> params = new java.util.ArrayList<>();
        
        if (userId != null && userId > 0) {
            sql.append("AND user_id = ? ");
            params.add(userId);
        }
        if (module != null && !module.isEmpty()) {
            sql.append("AND module = ? ");
            params.add(module);
        }
        if (action != null && !action.isEmpty()) {
            sql.append("AND action = ? ");
            params.add(action);
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (login_name LIKE ? OR content LIKE ?) ");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (startTime != null && !startTime.isEmpty()) {
            sql.append("AND op_time >= ? ");
            params.add(startTime + " 00:00:00");
        }
        if (endTime != null && !endTime.isEmpty()) {
            sql.append("AND op_time <= ? ");
            params.add(endTime + " 23:59:59");
        }
        
        sql.append("ORDER BY op_time DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);
        
        return BaseDao.query(sql.toString(), this::mapLog, params.toArray());
    }

    public int countLogs(Long userId, String module, String action, String keyword,
                         String startTime, String endTime) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_op_log WHERE 1=1 ");
        List<Object> params = new java.util.ArrayList<>();
        
        if (userId != null && userId > 0) {
            sql.append("AND user_id = ? ");
            params.add(userId);
        }
        if (module != null && !module.isEmpty()) {
            sql.append("AND module = ? ");
            params.add(module);
        }
        if (action != null && !action.isEmpty()) {
            sql.append("AND action = ? ");
            params.add(action);
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (login_name LIKE ? OR content LIKE ?) ");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (startTime != null && !startTime.isEmpty()) {
            sql.append("AND op_time >= ? ");
            params.add(startTime + " 00:00:00");
        }
        if (endTime != null && !endTime.isEmpty()) {
            sql.append("AND op_time <= ? ");
            params.add(endTime + " 23:59:59");
        }
        
        return BaseDao.queryOne(sql.toString(), rs -> rs.getInt(1), params.toArray());
    }

    public void insertLog(OpLog log) {
        String sql = "INSERT INTO sys_op_log(user_id, login_name, module, action, content, ip, op_time) VALUES(?, ?, ?, ?, ?, ?, ?)";
        BaseDao.update(sql, log.getUserId(), log.getLoginName(), log.getModule(), 
                log.getAction(), log.getContent(), log.getIp(), log.getOpTime());
    }

    public List<Map<String, Object>> listModules() {
        return BaseDao.query("SELECT DISTINCT module FROM sys_op_log ORDER BY module", 
                rs -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("module", rs.getString("module"));
                    return m;
                });
    }

    public List<Map<String, Object>> listActions(String module) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT action FROM sys_op_log");
        if (module != null && !module.isEmpty()) {
            sql.append(" WHERE module = ?");
            return BaseDao.query(sql.toString(), 
                    rs -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("action", rs.getString("action"));
                        return m;
                    }, module);
        }
        return BaseDao.query(sql.toString(), 
                rs -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("action", rs.getString("action"));
                    return m;
                });
    }

    public int deleteOldLogs(int days) {
        String sql = "DELETE FROM sys_op_log WHERE op_time < CURRENT_TIMESTAMP - INTERVAL '1 day' * ?";
        return BaseDao.update(sql, days);
    }
}
