package com.rebate.util;

import com.rebate.dao.OpLogDao;
import com.rebate.model.OpLog;
import com.rebate.model.UserContext;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;

/**
 * 操作日志记录工具
 */
public class OpLogger {

    private static final OpLogDao dao = new OpLogDao();

    /**
     * 记录操作日志
     */
    public static void log(HttpServletRequest req, UserContext u, String module, String action, String content) {
        try {
            OpLog log = new OpLog();
            log.setUserId(u != null ? u.getId() : null);
            log.setLoginName(u != null ? u.getLoginName() : null);
            log.setModule(module);
            log.setAction(action);
            log.setContent(content);
            log.setIp(WebUtil.getClientIp(req));
            log.setOpTime(new Timestamp(System.currentTimeMillis()));
            dao.insertLog(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            e.printStackTrace();
        }
    }

    /**
     * 记录操作日志（简化版本）
     */
    public static void log(HttpServletRequest req, UserContext u, String module, String action) {
        log(req, u, module, action, null);
    }
}
