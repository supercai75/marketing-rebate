package com.rebate.servlet;

import com.rebate.dao.OpLogDao;
import com.rebate.model.OpLog;
import com.rebate.util.ExcelUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志 Servlet
 */
public class OpLogServlet extends BaseServlet {

    private final OpLogDao dao = new OpLogDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, com.rebate.model.UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "list";
        
        if (!checkPerm(u, op)) {
            ResponseUtil.forbidden(resp);
            return;
        }
        
        switch (op) {
            case "list": doList(req, resp, p); break;
            case "listModules": doListModules(req, resp); break;
            case "listActions": doListActions(req, resp, p); break;
            case "export": doExport(req, resp, p); break;
            case "cleanup": doCleanup(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }

    private boolean checkPerm(com.rebate.model.UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "list":
            case "listModules":
            case "listActions":
            case "export":
                return u.hasPerm("log:view");
            case "cleanup":
                return u.hasPerm("log:cleanup");
            default:
                return false;
        }
    }

    private void doList(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        Long userId = WebUtil.getLong(p, "userId", 0L);
        if (userId == 0) userId = null;
        String module = WebUtil.getSafeParam(p, "module");
        String action = WebUtil.getSafeParam(p, "action");
        String keyword = WebUtil.getSafeParam(p, "keyword");
        String startTime = WebUtil.getSafeParam(p, "startTime");
        String endTime = WebUtil.getSafeParam(p, "endTime");
        Integer page = WebUtil.getInt(p, "page", 1);
        Integer pageSize = WebUtil.getInt(p, "pageSize", 20);
        
        List<OpLog> logs = dao.listLogs(userId, module, action, keyword, startTime, endTime, page, pageSize);
        int total = dao.countLogs(userId, module, action, keyword, startTime, endTime);
        int totalPages = (total + pageSize - 1) / pageSize;
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", logs);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        ResponseUtil.ok(resp, result);
    }

    private void doListModules(HttpServletRequest req, HttpServletResponse resp) {
        ResponseUtil.ok(resp, dao.listModules());
    }

    private void doListActions(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        String module = WebUtil.getSafeParam(p, "module");
        ResponseUtil.ok(resp, dao.listActions(module));
    }

    private void doExport(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        Long userId = WebUtil.getLong(p, "userId", 0L);
        if (userId == 0) userId = null;
        String module = WebUtil.getSafeParam(p, "module");
        String action = WebUtil.getSafeParam(p, "action");
        String keyword = WebUtil.getSafeParam(p, "keyword");
        String startTime = WebUtil.getSafeParam(p, "startTime");
        String endTime = WebUtil.getSafeParam(p, "endTime");

        // 导出全部不分页，最多导出一万条
        List<OpLog> logs = dao.listLogs(userId, module, action, keyword, startTime, endTime, 1, 10000);

        List<String> headers = new ArrayList<>();
        headers.add("操作时间");
        headers.add("操作人");
        headers.add("模块");
        headers.add("操作");
        headers.add("内容");
        headers.add("IP地址");

        List<List<String>> rows = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (OpLog log : logs) {
            List<String> row = new ArrayList<>();
            row.add(log.getOpTime() != null ? sdf.format(log.getOpTime()) : "");
            row.add(log.getLoginName() != null ? log.getLoginName() : "");
            row.add(log.getModule() != null ? log.getModule() : "");
            row.add(log.getAction() != null ? log.getAction() : "");
            row.add(log.getContent() != null ? log.getContent() : "");
            row.add(log.getIp() != null ? log.getIp() : "");
            rows.add(row);
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd");
        String fileName = "操作日志_" + fileNameFormat.format(new java.util.Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }

    private void doCleanup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        int deletedCount = dao.deleteOldLogs(180); // 清理180天（半年）以前的日志
        ResponseUtil.ok(resp, deletedCount);
    }
}
