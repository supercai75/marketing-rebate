package com.rebate.servlet;

import com.rebate.dao.ProjectDao;
import com.rebate.model.Project;
import com.rebate.model.UserContext;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;

/**
 * 项目管理
 */
public class ProjectServlet extends BaseServlet {

    private final ProjectDao projectDao = new ProjectDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "page";
        
        if (!checkPerm(u, op)) {
            ResponseUtil.forbidden(resp);
            return;
        }
        
        switch (op) {
            case "page": doPage(req, resp, p); break;
            case "list": doList(req, resp); break;
            case "listYears": doListYears(req, resp); break;
            case "listUndertakingDepts": doListUndertakingDepts(req, resp); break;
            case "listByYear": doListByYear(req, resp, p); break;
            case "listFilters": doListFilters(req, resp, p); break;
            case "groups": doListGroups(req, resp); break;
            case "get": doGet(req, resp, p); break;
            case "add": doAdd(req, resp, p, u); break;
            case "update": doUpdate(req, resp, p, u); break;
            case "delete": doDelete(req, resp, p); break;
            case "importFromBpm": doImportBpm(req, resp, p, u); break;
            case "checkPrevYear": doCheckPrevYear(req, resp, p); break;
            case "listBpmProjects": doListBpmProjects(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }
    
    private boolean checkPerm(UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "page":
            case "list":
            case "listYears":
            case "listUndertakingDepts":
            case "listByYear":
            case "listFilters":
            case "groups":
            case "get":
            case "checkPrevYear":
            case "listBpmProjects":
                return u.hasPerm("project:view");
            case "add":
            case "update":
            case "importFromBpm":
                return u.hasPerm("project:edit");
            case "delete":
                return u.hasPerm("project:edit");
            default:
                return false;
        }
    }

    private void doPage(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        int page = Math.max(1, WebUtil.getInt(p, "page", 1));
        int size = Math.max(1, Math.min(100, WebUtil.getInt(p, "size", 20)));
        Long gId = WebUtil.getLong(p, "projectGroupId", 0);
        Long gid = (gId == null || gId == 0) ? null : gId;
        List<Project> rows = projectDao.page(WebUtil.getSafeParam(p, "keyword"),
                WebUtil.getSafeParam(p, "status"), WebUtil.getSafeParam(p, "coYear"), gid, page, size);
        long total = projectDao.count(WebUtil.getSafeParam(p, "keyword"), WebUtil.getSafeParam(p, "status"),
                WebUtil.getSafeParam(p, "coYear"), gid);
        ResponseUtil.ok(resp, WebUtil.pageResult(page, size, total, rows));
    }

    private void doList(HttpServletRequest req, HttpServletResponse resp) {
        ResponseUtil.ok(resp, projectDao.listAll());
    }

    private void doListFilters(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        Long gId = WebUtil.getLong(p, "projectGroupId", 0);
        Long gid = (gId == null || gId == 0) ? null : gId;
        ResponseUtil.ok(resp, projectDao.listByFilters(
                WebUtil.getSafeParam(p, "coYear"), gid,
                WebUtil.getSafeParam(p, "keyword"), WebUtil.getSafeParam(p, "status"),
                WebUtil.getSafeParam(p, "undertakingDept")));
    }

    private void doListGroups(HttpServletRequest req, HttpServletResponse resp) {
        ResponseUtil.ok(resp, projectDao.listGroups());
    }
    
    private void doListYears(HttpServletRequest req, HttpServletResponse resp) {
        ResponseUtil.ok(resp, projectDao.listAllYears());
    }

    private void doListUndertakingDepts(HttpServletRequest req, HttpServletResponse resp) {
        ResponseUtil.ok(resp, projectDao.listAllUndertakingDepts());
    }
    
    private void doListByYear(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        String coYear = WebUtil.getSafeParam(p, "coYear");
        ResponseUtil.ok(resp, projectDao.listByYear(coYear));
    }

    private void doGet(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        ResponseUtil.ok(resp, projectDao.findById(id));
    }

    private void doAdd(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, UserContext u) {
        Project po = parseProject(p, u.getId());
        po.setCreatedBy(u.getId());
        po.setStatus("NEW");
        Long id = projectDao.insert(po);
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
    }

    private void doUpdate(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        Project po = projectDao.findById(id);
        if (po == null) { ResponseUtil.fail(resp, "项目不存在"); return; }
        Project upd = parseProject(p, u.getId());
        upd.setId(id);
        upd.setCreatedBy(po.getCreatedBy());
        projectDao.update(upd);
        ResponseUtil.ok(resp);
    }

    private void doDelete(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        projectDao.delete(id);
        ResponseUtil.ok(resp);
    }

    /**
     * 从 BPM 弹出窗口选择立项后，判定：已存在则更新，不存在则新增
     */
    private void doImportBpm(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, UserContext u) {
        String projectCode = WebUtil.getSafeParam(p, "projectCode");
        if (projectCode == null || projectCode.isEmpty()) {
            ResponseUtil.fail(resp, "项目编号不能为空");
            return;
        }
        // 检查是否已存在
        Project existing = projectDao.findByProjectCode(projectCode);
        if (existing != null) {
            // 更新
            existing.setProjectName(WebUtil.getSafeParam(p, "projectName"));
            existing.setBrand(WebUtil.getSafeParam(p, "brand"));
            existing.setCoProduct(WebUtil.getSafeParam(p, "coProduct"));
            existing.setCoMode(WebUtil.getSafeParam(p, "coMode"));
            existing.setCoYear(WebUtil.getSafeParam(p, "coYear"));
            String ps = WebUtil.getSafeParam(p, "periodStartDate");
            String pe = WebUtil.getSafeParam(p, "periodEndDate");
            if (ps != null && !ps.isEmpty()) existing.setPeriodStartDate(Date.valueOf(ps));
            if (pe != null && !pe.isEmpty()) existing.setPeriodEndDate(Date.valueOf(pe));
            existing.setRegion(WebUtil.getSafeParam(p, "region"));
            existing.setTargetScale(toBd(p.get("targetScale")));
            existing.setExpectedRebate(toBd(p.get("expectedRebate")));
            existing.setExpectedCost(toBd(p.get("expectedCost")));
            existing.setDescription(WebUtil.getSafeParam(p, "description"));
            projectDao.update(existing);
            ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", existing.getId()));
        } else {
            // 新增
            Project po = new Project();
            po.setProjectCode(projectCode);
            po.setProjectName(WebUtil.getSafeParam(p, "projectName"));
            po.setBrand(WebUtil.getSafeParam(p, "brand"));
            po.setCoProduct(WebUtil.getSafeParam(p, "coProduct"));
            po.setCoMode(WebUtil.getSafeParam(p, "coMode"));
            po.setCoYear(WebUtil.getSafeParam(p, "coYear"));
            String ps = WebUtil.getSafeParam(p, "periodStartDate");
            String pe = WebUtil.getSafeParam(p, "periodEndDate");
            if (ps != null && !ps.isEmpty()) po.setPeriodStartDate(Date.valueOf(ps));
            if (pe != null && !pe.isEmpty()) po.setPeriodEndDate(Date.valueOf(pe));
            po.setRegion(WebUtil.getSafeParam(p, "region"));
            po.setTargetScale(toBd(p.get("targetScale")));
            po.setExpectedRebate(toBd(p.get("expectedRebate")));
            po.setExpectedCost(toBd(p.get("expectedCost")));
            po.setDescription(WebUtil.getSafeParam(p, "description"));
            po.setBpmProjectId(projectCode);
            po.setBpmSynced(1);
            po.setStatus("NEW");
            po.setCreatedBy(u.getId());
            Long id = projectDao.insert(po);
            ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
        }
    }

    /**
     * 查询 BPM 近一年立项列表（供用户选择）
     */
    private void doListBpmProjects(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        List<Map<String, Object>> list = projectDao.listBpmProjects();
        // 标记本地已存在的项目编号
        List<String> codes = new java.util.ArrayList<>();
        for (Map<String, Object> row : list) {
            Object code = row.get("projectCode");
            if (code != null && !code.toString().trim().isEmpty()) {
                codes.add(code.toString());
            }
        }
        java.util.Set<String> existingCodes = new java.util.HashSet<>();
        for (String code : codes) {
            if (projectDao.findByProjectCode(code) != null) {
                existingCodes.add(code);
            }
        }
        for (Map<String, Object> row : list) {
            Object code = row.get("projectCode");
            row.put("isExisting", code != null && existingCodes.contains(code.toString()));
        }
        ResponseUtil.ok(resp, list);
    }

    /**
     * 检查上一年同名项目是否存在
     */
    private void doCheckPrevYear(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        String projectName = WebUtil.getSafeParam(p, "projectName");
        String coYear = WebUtil.getSafeParam(p, "coYear");
        List<Project> projects = projectDao.findByNameAndYear(projectName, coYear);
        ResponseUtil.ok(resp, projects);
    }

    private Project parseProject(Map<String, Object> p, Long userId) {
        Project po = new Project();
        po.setProjectCode(WebUtil.getSafeParam(p, "projectCode"));
        po.setProjectName(WebUtil.getSafeParam(p, "projectName"));
        po.setBrand(WebUtil.getSafeParam(p, "brand"));
        po.setCoProduct(WebUtil.getSafeParam(p, "coProduct"));
        po.setCoMode(WebUtil.getSafeParam(p, "coMode"));
        po.setCoYear(WebUtil.getSafeParam(p, "coYear"));
        String ps = WebUtil.getSafeParam(p, "periodStartDate");
        String pe = WebUtil.getSafeParam(p, "periodEndDate");
        if (ps != null && !ps.isEmpty()) po.setPeriodStartDate(Date.valueOf(ps));
        if (pe != null && !pe.isEmpty()) po.setPeriodEndDate(Date.valueOf(pe));
        po.setRegion(WebUtil.getSafeParam(p, "region"));
        po.setTargetScale(toBd(p.get("targetScale")));
        po.setExpectedRebate(toBd(p.get("expectedRebate")));
        po.setExpectedCost(toBd(p.get("expectedCost")));
        po.setDescription(WebUtil.getSafeParam(p, "description"));
        po.setOwnerUserId(WebUtil.getLong(p, "ownerUserId", 0) == 0 ? null : WebUtil.getLong(p, "ownerUserId", 0));
        po.setStatus(WebUtil.getSafeParam(p, "status"));
        po.setUndertakingDept(WebUtil.getSafeParam(p, "undertakingDept"));

        // 分组：优先用 projectGroupId（已有分组）；如果前端传的是 projectGroupName（文本或新建），走懒创建
        Long gId = WebUtil.getLong(p, "projectGroupId", 0);
        String gName = WebUtil.getSafeParam(p, "projectGroupName");
        if (gId != null && gId > 0) {
            po.setProjectGroupId(gId);
        } else if (gName != null && !gName.trim().isEmpty()) {
            po.setProjectGroupId(projectDao.ensureGroup(gName, userId));
        }
        return po;
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
