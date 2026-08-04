package com.rebate.filter;

import com.rebate.dao.OpLogDao;
import com.rebate.model.OpLog;
import com.rebate.model.UserContext;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;

/**
 * 操作日志过滤器
 * <p>自动记录所有业务操作到 sys_op_log 表</p>
 */
public class OpLogFilter implements Filter {

    private static final OpLogDao dao = new OpLogDao();

    // 不需要记录的操作（查询类）
    private static final String[] EXCLUDED_OPS = {
            "page", "list", "get", "current", "login", "logout", "departments", 
            "companies", "getPermissions", "listYears", "listByYear", "listRecords",
            "listWithUpstream", "listMonthSummary", "listByProject", "listExpense",
            "listLabor", "listReceivable", "listPayable", "listReceived", "listPaid",
            "load", "loadCurrent", "loadInvalid", "loadSource", "listCanSplit"
    };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        String method = req.getMethod();

        // 跳过文件下载、健康检查等API
        if (uri.contains("/file/download") || uri.contains("/api/health")) {
            chain.doFilter(request, response);
            return;
        }

        // 只记录 POST/PUT/DELETE 请求（修改类操作）
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查是否是需要排除的操作
        String op = req.getParameter("op");
        if (op != null) {
            for (String excluded : EXCLUDED_OPS) {
                if (excluded.equalsIgnoreCase(op)) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        // 获取登录用户
        UserContext u = null;
        try {
            u = TokenUtil.getLoginUser(req, UserContext.class);
        } catch (Exception ignore) {
        }

        // 解析模块名
        String module = parseModule(uri);

        // 解析操作内容
        String content = parseContent(req, op);

        // 执行请求
        try {
            chain.doFilter(request, response);
        } finally {
            // 请求完成后记录日志
            try {
                String action = parseAction(op);
                if (action != null && !action.isEmpty() && module != null && !"系统".equals(module)) {
                    OpLog log = new OpLog();
                    log.setUserId(u != null ? u.getId() : null);
                    log.setLoginName(u != null ? u.getLoginName() : null);
                    log.setModule(module);
                    log.setAction(action);
                    log.setContent(content);
                    log.setIp(WebUtil.getClientIp(req));
                    log.setOpTime(new Timestamp(System.currentTimeMillis()));
                    dao.insertLog(log);
                }
            } catch (Exception e) {
                // 日志记录失败不影响主业务
                e.printStackTrace();
            }
        }
    }

    private String parseModule(String uri) {
        if (uri.contains("/api/auth")) return "认证管理";
        if (uri.contains("/api/user")) return "用户管理";
        if (uri.contains("/api/role")) return "角色管理";
        if (uri.contains("/api/project")) return "项目管理";
        if (uri.contains("/api/upstream-agreement")) return "上游协议管理";
        if (uri.contains("/api/downstream-agreement")) return "下游协议管理";
        if (uri.contains("/api/upstream-flow")) return "上游流向管理";
        if (uri.contains("/api/downstream-flow")) return "下游流向管理";
        if (uri.contains("/api/cost")) return "费用投入管理";
        if (uri.contains("/api/receivable-payable")) return "应收应付管理";
        if (uri.contains("/api/received-paid")) return "实收实付管理";
        if (uri.contains("/api/project-staff")) return "项目人员管理";
        if (uri.contains("/api/oplog")) return "操作日志管理";
        if (uri.contains("/api/overview")) return "概览管理";
        return "系统";
    }

    private String parseAction(String op) {
        if (op == null || op.isEmpty()) {
            return "操作";
        }
        // 根据 op 参数判断操作
        switch (op.toLowerCase()) {
            case "add":
            case "create":
            case "insert":
            case "import":
            case "importfrombpm":
            case "addSettle":
            case "addRebateRule":
                return "新增";
            case "update":
            case "edit":
            case "modify":
            case "save":
            case "saveWithSettles":
            case "saveWithRules":
                return "修改";
            case "delete":
            case "remove":
            case "deleteAll":
                return "删除";
            case "setfinal":
                return "设为终版";
            case "cancelfinal":
                return "取消终版";
            case "updatepassword":
                return "修改密码";
            case "updatepermissions":
                return "设置权限";
            case "export":
            case "exportRecords":
            case "exportMonthSummary":
            case "exportCurrent":
            case "exportInvalid":
            case "exportExpense":
            case "exportLabor":
            case "exportReceivable":
            case "exportPayable":
            case "exportReceived":
            case "exportPaid":
                return "导出";
            case "allocate":
            case "allocatePerson":
            case "allocateExpense":
                return "分摊";
            case "split":
            case "confirmSplit":
                return "分解";
            case "refresh":
            case "sync":
                return "同步";
            default:
                return op;
        }
    }

    private String parseContent(HttpServletRequest req, String op) {
        StringBuilder sb = new StringBuilder();
        // 添加关键参数到日志内容中
        String id = req.getParameter("id");
        if (id != null && !id.isEmpty()) {
            sb.append("ID=").append(id).append("; ");
        }
        String projectId = req.getParameter("projectId");
        if (projectId != null && !projectId.isEmpty()) {
            sb.append("项目ID=").append(projectId).append("; ");
        }
        String agreementId = req.getParameter("agreementId");
        if (agreementId != null && !agreementId.isEmpty()) {
            sb.append("协议ID=").append(agreementId).append("; ");
        }
        String month = req.getParameter("month");
        if (month != null && !month.isEmpty()) {
            sb.append("月份=").append(month).append("; ");
        }
        String year = req.getParameter("year");
        if (year != null && !year.isEmpty()) {
            sb.append("年度=").append(year).append("; ");
        }
        String batchId = req.getParameter("batchId");
        if (batchId != null && !batchId.isEmpty()) {
            sb.append("批次ID=").append(batchId).append("; ");
        }
        return sb.toString();
    }

    @Override
    public void destroy() {
    }
}
