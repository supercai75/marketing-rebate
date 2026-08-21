package com.rebate.servlet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rebate.dao.BaseDao;
import com.rebate.dao.ProjectDao;
import com.rebate.dao.StageMonthConfigDao;
import com.rebate.model.Project;
import com.rebate.model.StageMonthConfig;
import com.rebate.model.UserContext;
import com.rebate.service.StageMonthService;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目管理
 */
public class ProjectServlet extends BaseServlet {

    private final ProjectDao projectDao = new ProjectDao();
    private final StageMonthConfigDao stageConfigDao = new StageMonthConfigDao();

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
            case "getStageConfig": doGetStageConfig(req, resp, p); break;
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
            case "getStageConfig":
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

    /**
     * 加载项目的阶段-月份配置(非12个月周期时由用户定义)。
     * 返回 { isFullYear, monthCount, defaultRanges, configs }
     */
    private void doGetStageConfig(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        Project proj = projectDao.findById(id);
        Map<String, Object> r = new LinkedHashMap<>();
        if (proj == null) { ResponseUtil.fail(resp, "项目不存在"); return; }
        boolean isFull = StageMonthService.isFullYearPeriod(proj.getPeriodStartDate(), proj.getPeriodEndDate());
        r.put("isFullYear", isFull);
        r.put("monthCount", StageMonthService.monthCount(proj.getPeriodStartDate(), proj.getPeriodEndDate()));
        r.put("defaultRanges", StageMonthService.computeDefaultRanges(proj.getPeriodStartDate()));
        List<StageMonthConfig> configs = stageConfigDao.listByProject(id);
        r.put("configs", configs);
        ResponseUtil.ok(resp, r);
    }

    private void doAdd(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, UserContext u) {
        Long id = BaseDao.executeInTransaction(conn -> {
            Project po = parseProjectFields(p);
            po.setCreatedBy(u.getId());
            po.setStatus("NEW");
            // 分组懒创建（事务内）：优先用 projectGroupId（已有分组）；否则用 projectGroupName 走懒创建
            Long gId = WebUtil.getLong(p, "projectGroupId", 0);
            String gName = WebUtil.getSafeParam(p, "projectGroupName");
            if (gId != null && gId > 0) {
                po.setProjectGroupId(gId);
            } else if (gName != null && !gName.trim().isEmpty()) {
                po.setProjectGroupId(projectDao.ensureGroupWithConn(conn, gName, u.getId()));
            }
            Long newId = projectDao.insertWithConn(conn, po);
            // 阶段-月份配置(事务内一并保存): 非12个月必须带配置
            saveStageConfigsInTx(conn, newId, po.getPeriodStartDate(), po.getPeriodEndDate(), p, true);
            return newId;
        });
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
    }

    private void doUpdate(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        Project po = projectDao.findById(id);
        if (po == null) { ResponseUtil.fail(resp, "项目不存在"); return; }
        BaseDao.executeInTransaction(conn -> {
            Project upd = parseProjectFields(p);
            upd.setId(id);
            upd.setCreatedBy(po.getCreatedBy());
            // 分组懒创建(事务内)
            Long gId = WebUtil.getLong(p, "projectGroupId", 0);
            String gName = WebUtil.getSafeParam(p, "projectGroupName");
            if (gId != null && gId > 0) {
                upd.setProjectGroupId(gId);
            } else if (gName != null && !gName.trim().isEmpty()) {
                upd.setProjectGroupId(projectDao.ensureGroupWithConn(conn, gName, u.getId()));
            }
            projectDao.updateWithConn(conn, upd);
            // 阶段-月份配置(事务内一并保存): 非12个月必须带配置
            saveStageConfigsInTx(conn, id, upd.getPeriodStartDate(), upd.getPeriodEndDate(), p, true);
            return null;
        });
        ResponseUtil.ok(resp);
    }

    /**
     * 事务内同步阶段-月份配置。
     *  - 周期整12个月：删除已有配置(走默认3个月一阶段)，不要求前端传值。
     *  - 周期非12个月 + requireWhenNonFull=true：校验 stageConfigs 并保存，否则抛异常。
     *  - 周期非12个月 + requireWhenNonFull=false：未传 stageConfigs 时仅清空旧配置（交由后续编辑补充）。
     */
    private void saveStageConfigsInTx(Connection conn, Long projectId, Date periodStart, Date periodEnd,
                                      Map<String, Object> p, boolean requireWhenNonFull) throws Exception {
        // 先清空旧配置(无论周期类型)
        stageConfigDao.deleteByProjectWithConn(conn, projectId);
        boolean isFull = StageMonthService.isFullYearPeriod(periodStart, periodEnd);
        if (isFull) {
            // 整12个月: 走默认规则，无需保存
            return;
        }
        List<StageMonthConfig> configs = parseStageConfigs(p, projectId);
        if (configs.isEmpty()) {
            if (requireWhenNonFull) {
                if (periodStart == null || periodEnd == null) {
                    throw new RuntimeException("非12个月周期项目必须填写起始/终止日期并定义阶段月份区间");
                }
                throw new RuntimeException("项目周期非12个月，请定义每个阶段(S1-S4)对应的月份区间");
            }
            // BPM引入等场景: 不要求立即配置, 留空让用户后续编辑时补齐
            return;
        }
        if (periodStart == null || periodEnd == null) {
            throw new RuntimeException("非12个月周期项目必须填写起始/终止日期");
        }
        validateStageConfigs(configs, periodStart, periodEnd);
        for (StageMonthConfig c : configs) {
            stageConfigDao.insertWithConn(conn, c);
        }
    }

    /**
     * 解析前端传入的 stageConfigs (JSON字符串或数组对象)。
     */
    @SuppressWarnings("unchecked")
    private List<StageMonthConfig> parseStageConfigs(Map<String, Object> p, Long projectId) {
        Object raw = p.get("stageConfigs");
        if (raw == null) return new ArrayList<>();
        List<Map<String, Object>> list;
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            if (s.isEmpty()) return new ArrayList<>();
            list = new Gson().fromJson(s, new TypeToken<List<Map<String, Object>>>() {}.getType());
        } else if (raw instanceof List) {
            list = (List<Map<String, Object>>) raw;
        } else {
            return new ArrayList<>();
        }
        List<StageMonthConfig> result = new ArrayList<>();
        for (Map<String, Object> m : list) {
            StageMonthConfig c = new StageMonthConfig();
            c.setProjectId(projectId);
            c.setStageCode(asString(m.get("stageCode")));
            c.setStartYyyymm(asInt(m.get("startYyyymm")));
            c.setEndYyyymm(asInt(m.get("endYyyymm")));
            result.add(c);
        }
        return result;
    }

    private void validateStageConfigs(List<StageMonthConfig> configs, Date periodStart, Date periodEnd) {
        if (configs.size() != 4) {
            throw new RuntimeException("阶段-月份配置必须包含 S1-S4 共4个阶段");
        }
        java.util.Map<String, StageMonthConfig> byCode = new LinkedHashMap<>();
        for (StageMonthConfig c : configs) {
            String code = c.getStageCode();
            if (code == null || !java.util.Arrays.asList("S1","S2","S3","S4").contains(code)) {
                throw new RuntimeException("阶段编号无效: " + code);
            }
            if (c.getStartYyyymm() == null || c.getEndYyyymm() == null
                    || c.getStartYyyymm() < 100000 || c.getStartYyyymm() > 999912
                    || c.getEndYyyymm() < 100000 || c.getEndYyyymm() > 999912) {
                throw new RuntimeException("阶段 " + code + " 的月份区间格式不合法(需为YYYYMM)");
            }
            if (c.getEndYyyymm() < c.getStartYyyymm()) {
                throw new RuntimeException("阶段 " + code + " 的截止月份不能早于起始月份");
            }
            if (byCode.put(code, c) != null) {
                throw new RuntimeException("阶段编号重复: " + code);
            }
        }
        // 阶段顺序: S1.start <= S2.start <= S3.start <= S4.start
        int prevEnd = -1;
        for (String sc : new String[]{"S1","S2","S3","S4"}) {
            StageMonthConfig c = byCode.get(sc);
            if (c.getStartYyyymm() <= prevEnd) {
                throw new RuntimeException("阶段 " + sc + " 的起始月份必须晚于前一阶段的截止月份");
            }
            prevEnd = c.getEndYyyymm();
        }
        // 区间应覆盖整个项目周期
        int periodStartYm = yyyymm(periodStart);
        int periodEndYm = yyyymm(periodEnd);
        if (byCode.get("S1").getStartYyyymm() > periodStartYm) {
            throw new RuntimeException("阶段S1起始月份必须不晚于项目起始月份(" + periodStartYm + ")");
        }
        if (byCode.get("S4").getEndYyyymm() < periodEndYm) {
            throw new RuntimeException("阶段S4截止月份必须不早于项目终止月份(" + periodEndYm + ")");
        }
    }

    private static int yyyymm(Date d) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(d);
        return c.get(java.util.Calendar.YEAR) * 100 + (c.get(java.util.Calendar.MONTH) + 1);
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        String xStr = String.valueOf(o);
        try {
            if(xStr.indexOf(".")>=0) {
            	return (int)Double.parseDouble(xStr);
            }else {
            	return Integer.parseInt(xStr);
            }
        } catch (NumberFormatException e) {
        	return null;
        }
    }

    private void doDelete(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        projectDao.delete(id);
        ResponseUtil.ok(resp);
    }

    /**
     * 从 BPM 弹出窗口选择立项后，判定：已存在则更新，不存在则新增
     * 新增时创建人=当前用户，创建时间=当前时间，bpmProcessId=BPM流程实例ID
     *
     * 整个操作是事务性的：项目主表保存 + 阶段-月份配置清理 在同一 Connection 内。
     * BPM 引入场景不传 stageConfigs：
     *   - 整12个月项目 → 清理旧配置后走默认规则；
     *   - 非12个月项目 → 清理旧配置后留空，用户编辑项目时可补充阶段定义（此时才强制要求填写）。
     */
    private void doImportBpm(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, UserContext u) {
        String projectCode = WebUtil.getSafeParam(p, "projectCode");
        if (projectCode == null || projectCode.isEmpty()) {
            ResponseUtil.fail(resp, "项目编号不能为空");
            return;
        }
        String bpmProcessId = WebUtil.getSafeParam(p, "bpmProcessId");
        String psStr = WebUtil.getSafeParam(p, "periodStartDate");
        String peStr = WebUtil.getSafeParam(p, "periodEndDate");
        Date periodStart = (psStr != null && !psStr.isEmpty()) ? Date.valueOf(psStr) : null;
        Date periodEnd = (peStr != null && !peStr.isEmpty()) ? Date.valueOf(peStr) : null;

        // 先在事务外判断是否存在（只读），再在事务内执行 insert/update+同步阶段配置
        Project existing = projectDao.findByProjectCode(projectCode);
        Long id = BaseDao.executeInTransaction(conn -> {
            if (existing != null) {
                // 更新
                existing.setProjectName(WebUtil.getSafeParam(p, "projectName"));
                existing.setBrand(WebUtil.getSafeParam(p, "brand"));
                existing.setCoProduct(WebUtil.getSafeParam(p, "coProduct"));
                existing.setCoMode(WebUtil.getSafeParam(p, "coMode"));
                existing.setCoYear(WebUtil.getSafeParam(p, "coYear"));
                if (periodStart != null) existing.setPeriodStartDate(periodStart);
                if (periodEnd != null) existing.setPeriodEndDate(periodEnd);
                existing.setRegion(WebUtil.getSafeParam(p, "region"));
                existing.setTargetScale(toBd(p.get("targetScale")));
                existing.setExpectedRebate(toBd(p.get("expectedRebate")));
                existing.setExpectedCost(toBd(p.get("expectedCost")));
                existing.setDescription(WebUtil.getSafeParam(p, "description"));
                if (bpmProcessId != null && !bpmProcessId.isEmpty()) {
                    existing.setBpmProcessId(bpmProcessId);
                }
                projectDao.updateWithConn(conn, existing);
                // 阶段配置：BPM 引入不强制要求，留空让后续编辑补齐
                saveStageConfigsInTx(conn, existing.getId(),
                        existing.getPeriodStartDate(), existing.getPeriodEndDate(), p, false);
                return existing.getId();
            } else {
                // 新增：创建人=当前用户，创建时间=当前时间，引入后可自由编辑
                Project po = new Project();
                po.setProjectCode(projectCode);
                po.setProjectName(WebUtil.getSafeParam(p, "projectName"));
                po.setBrand(WebUtil.getSafeParam(p, "brand"));
                po.setCoProduct(WebUtil.getSafeParam(p, "coProduct"));
                po.setCoMode(WebUtil.getSafeParam(p, "coMode"));
                po.setCoYear(WebUtil.getSafeParam(p, "coYear"));
                if (periodStart != null) po.setPeriodStartDate(periodStart);
                if (periodEnd != null) po.setPeriodEndDate(periodEnd);
                po.setRegion(WebUtil.getSafeParam(p, "region"));
                po.setTargetScale(toBd(p.get("targetScale")));
                po.setExpectedRebate(toBd(p.get("expectedRebate")));
                po.setExpectedCost(toBd(p.get("expectedCost")));
                po.setDescription(WebUtil.getSafeParam(p, "description"));
                po.setBpmProcessId(bpmProcessId);
                po.setBpmProjectId(projectCode);
                po.setBpmSynced(1);
                po.setStatus("NEW");
                po.setCreatedBy(u.getId());
                Long newId = projectDao.insertWithConn(conn, po);
                // 阶段配置：BPM 引入不强制要求，留空让后续编辑补齐
                saveStageConfigsInTx(conn, newId,
                        po.getPeriodStartDate(), po.getPeriodEndDate(), p, false);
                return newId;
            }
        });
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
    }

    /**
     * 查询 BPM 近一年立项列表（供用户选择）
     * 支持按项目名称模糊筛选
     */
    private void doListBpmProjects(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        String projectName = WebUtil.getSafeParam(p, "projectName");
        List<Map<String, Object>> list = projectDao.listBpmProjects(projectName);
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

    private Project parseProjectFields(Map<String, Object> p) {
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
        return po;
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
