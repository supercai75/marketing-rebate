package com.rebate.servlet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rebate.dao.AgreementSubDao;
import com.rebate.dao.BaseDao;
import com.rebate.dao.ProjectDao;
import com.rebate.dao.RebateRuleDao;
import com.rebate.dao.UpstreamAgreementDao;
import com.rebate.model.AssessGroup;
import com.rebate.model.AttachFile;
import com.rebate.model.Project;
import com.rebate.model.RebateRule;
import com.rebate.model.TeamTarget;
import com.rebate.model.UpstreamAgreement;
import com.rebate.util.FileUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.*;

/**
 * 上游协议管理
 */
@MultipartConfig
public class UpstreamAgreementServlet extends BaseServlet {

    private final UpstreamAgreementDao dao = new UpstreamAgreementDao();
    private final AgreementSubDao subDao = new AgreementSubDao();
    private final RebateRuleDao ruleDao = new RebateRuleDao();
    private final ProjectDao projectDao = new ProjectDao();

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
            case "listAll": ResponseUtil.ok(resp, dao.listAllCurrent()); break;
            case "get": doGet(req, resp, p); break;
            case "add": doAdd(req, resp, p, u); break;
            case "update": doUpdate(req, resp, p); break;
            case "delete": doDelete(req, resp, p); break;
            case "importFromBpm": doImportBpm(req, resp, p, u); break;
            case "listBpmAgreements": doListBpmAgreements(req, resp, p); break;
            case "uploadAttach": doUploadAttach(req, resp, u); break;
            case "deleteAttach": doDeleteAttach(req, resp, p); break;
            case "listAttachs": doListAttachs(req, resp, p); break;
            case "listRebateRules": doListRebateRules(req, resp, p); break;
            case "listAssessGroups": doListAssessGroups(req, resp, p); break;
            case "getAssessGroup": doGetAssessGroup(req, resp, p); break;
            case "addAssessGroup": doAddAssessGroup(req, resp, p, u); break;
            case "updateAssessGroup": doUpdateAssessGroup(req, resp, p); break;
            case "deleteAssessGroup": doDeleteAssessGroup(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }

    private void doList(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        boolean currentOnly = !"0".equals(WebUtil.getSafeParam(p, "showHistory"));
        int page = WebUtil.getInt(p, "page", 1);
        int pageSize = WebUtil.getInt(p, "pageSize", 20);
        
        List<UpstreamAgreement> allList = dao.listByProject(pid, currentOnly);
        
        int total = allList.size();
        int totalPages = (total + pageSize - 1) / pageSize;
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<UpstreamAgreement> pageList = fromIndex < total ? allList.subList(fromIndex, toIndex) : new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageList);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        ResponseUtil.ok(resp, result);
    }

    private void doGet(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        UpstreamAgreement a = dao.findById(id);
        if (a != null) {
            a.setTeamTargets(subDao.listUpstreamTeamTargets(id));
            a.setRemarkFiles(fillUrl(req, subDao.listUpstreamRemarkFiles(id)));
            a.setAttachFiles(fillUrl(req, subDao.listUpstreamAttaches(id)));
        }
        ResponseUtil.ok(resp, a);
    }

    private List<AttachFile> fillUrl(HttpServletRequest req, List<AttachFile> files) {
        if (files == null) return null;
        String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
        for (AttachFile f : files) {
            f.setDownloadUrl(base + "/api/file/download?path=" + f.getFilePath() + "&fileName=" + (f.getFileName() == null ? "" : java.net.URLEncoder.encode(f.getFileName(), java.nio.charset.StandardCharsets.UTF_8)));
        }
        return files;
    }

    private void doAdd(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        UpstreamAgreement a = parse(p);
        String mode = a.getCalcMode() == null || a.getCalcMode().isEmpty() ? "PROGRESSIVE" : a.getCalcMode();
        if ("PROGRESSIVE".equalsIgnoreCase(mode) && !com.rebate.service.RebateCalcService.sameBasis(a.getCalcBasis(), a.getRebateCalcBasis())) {
            ResponseUtil.fail(resp, "递进式计算返利要求指标核算依据与返利计算依据一致（金额与核算金额视为相同）");
            return;
        }
        a.setCreatedBy(u.getId());
        Integer v = dao.findCurrentByProject(a.getProjectId()) == null ? 1
                : dao.findCurrentByProject(a.getProjectId()).getVersion() + 1;
        a.setVersion(v);
        a.setIsCurrent(1);
        Long id = BaseDao.executeInTransaction(conn -> {
            Long newId = dao.insertWithConn(conn, a);
            if (newId != null) dao.markNotCurrentWithConn(conn, a.getProjectId(), newId);
            saveSubTables(conn, newId, p);
            return newId;
        });
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
    }

    private void doUpdate(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        UpstreamAgreement a = dao.findById(id);
        if (a == null) { ResponseUtil.fail(resp, "协议不存在"); return; }
        UpstreamAgreement upd = parse(p);
        upd.setId(id);
        String mode = upd.getCalcMode() == null || upd.getCalcMode().isEmpty() ? "PROGRESSIVE" : upd.getCalcMode();
        if ("PROGRESSIVE".equalsIgnoreCase(mode) && !com.rebate.service.RebateCalcService.sameBasis(upd.getCalcBasis(), upd.getRebateCalcBasis())) {
            ResponseUtil.fail(resp, "递进式计算返利要求指标核算依据与返利计算依据一致（金额与核算金额视为相同）");
            return;
        }
        dao.update(upd);
        subDao.clearUpstreamTeamTargets(id);
        ruleDao.deleteByAgreement(id);
        BaseDao.<Void>executeInTransaction((Connection conn) -> {
            saveSubTables(conn, id, p);
            return null;
        });
        ResponseUtil.ok(resp);
    }

    private void doDelete(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        dao.delete(WebUtil.getLong(p, "id", 0));
        ResponseUtil.ok(resp);
    }

    /**
     * 从BPM选择协议后，在项目下新增新版本协议，并将同样协议编号的旧版本作废
     */
    private void doImportBpm(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p,
                             com.rebate.model.UserContext u) {
        long projectId = WebUtil.getLong(p, "projectId", 0);
        String agreementNo = WebUtil.getSafeParam(p, "agreementNo");
        if (projectId <= 0) { ResponseUtil.fail(resp, "项目不能为空"); return; }
        if (agreementNo == null || agreementNo.isEmpty()) { ResponseUtil.fail(resp, "协议编号不能为空"); return; }

        // 1. 把同样协议编号的旧版本全部作废
        List<UpstreamAgreement> olds = dao.listByProjectAndAgreementNo(projectId, agreementNo);
        for (UpstreamAgreement old : olds) {
            if (old.getIsCurrent() != null && old.getIsCurrent() == 1) {
                dao.markNotCurrentById(old.getId());
            }
        }

        // 2. 计算新版本号
        int newVersion = 1;
        for (UpstreamAgreement old : olds) {
            if (old.getVersion() != null && old.getVersion() >= newVersion) {
                newVersion = old.getVersion() + 1;
            }
        }

        // 3. 构建新协议记录
        UpstreamAgreement a = new UpstreamAgreement();
        a.setProjectId(projectId);
        a.setVersion(newVersion);
        a.setIsCurrent(1);
        a.setAgreementName(WebUtil.getSafeParam(p, "agreementName"));
        a.setAgreementNo(agreementNo);
        a.setBpmAgreeId(agreementNo);
        String ps = WebUtil.getSafeParam(p, "periodStartDate");
        String pe = WebUtil.getSafeParam(p, "periodEndDate");
        if (ps != null && !ps.isEmpty()) a.setPeriodStartDate(Date.valueOf(ps));
        if (pe != null && !pe.isEmpty()) a.setPeriodEndDate(Date.valueOf(pe));
        a.setRegion(WebUtil.getSafeParam(p, "region"));
        a.setTargetTerminal(WebUtil.getSafeParam(p, "targetTerminal"));
        a.setSupplier(WebUtil.getSafeParam(p, "supplier"));
        a.setCreatedBy(u.getId());

        Long id = dao.insert(a);
        if (id != null) {
            ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
        } else {
            ResponseUtil.fail(resp, "新增上游协议失败");
        }
    }

    /**
     * 从BPM查询当前项目的上游协议列表（供用户选择）
     */
    private void doListBpmAgreements(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long projectId = WebUtil.getLong(p, "projectId", 0);
        if (projectId <= 0) { ResponseUtil.fail(resp, "请选择项目"); return; }
        Project project = projectDao.findById(projectId);
        if (project == null) { ResponseUtil.fail(resp, "项目不存在"); return; }
        String projectCode = project.getProjectCode();
        if (projectCode == null || projectCode.isEmpty()) {
            ResponseUtil.ok(resp, java.util.Collections.emptyList());
            return;
        }
        List<Map<String, Object>> list = dao.listBpmUpstreamAgreements(projectCode);
        // 标记本地已存在同样协议编号的协议
        for (Map<String, Object> row : list) {
            Object agreeNo = row.get("agreementNo");
            if (agreeNo != null && !agreeNo.toString().trim().isEmpty()) {
                List<UpstreamAgreement> local = dao.listByProjectAndAgreementNo(projectId, agreeNo.toString());
                row.put("localVersionCount", local.size());
                boolean hasCurrent = false;
                for (UpstreamAgreement u : local) {
                    if (u.getIsCurrent() != null && u.getIsCurrent() == 1) { hasCurrent = true; break; }
                }
                row.put("hasCurrent", hasCurrent);
            } else {
                row.put("localVersionCount", 0);
                row.put("hasCurrent", false);
            }
        }
        ResponseUtil.ok(resp, list);
    }

    private void doUploadAttach(HttpServletRequest req, HttpServletResponse resp, com.rebate.model.UserContext u) throws Exception {
        long agreementId = 0;
        try {
            String aid = req.getParameter("agreementId");
            if (aid != null) agreementId = Long.parseLong(aid);
        } catch (Exception ignore) {}
        if (agreementId <= 0) { ResponseUtil.fail(resp, "agreementId 必填"); return; }
        String attachType = req.getParameter("attachType");
        Part file = req.getPart("file");
        if (file == null) { ResponseUtil.fail(resp, "请选择文件"); return; }
        String rel = FileUtil.save(file.getInputStream(), "agreement/upstream", file.getSubmittedFileName());
        AttachFile f = new AttachFile();
        f.setAgreementId(agreementId);
        f.setFileName(file.getSubmittedFileName());
        f.setFilePath(rel);
        f.setFileSize(file.getSize());
        f.setUploadedBy(u.getId());
        Long id;
        if ("MAIN".equals(attachType) || "SUPP".equals(attachType)) {
            f.setAttachType(attachType);
            id = subDao.insertUpstreamAttach(f);
            f.setId(id);
            String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
            f.setFileUrl(base + "/api/file/" + id);
            ResponseUtil.ok(resp, f);
        } else {
            id = subDao.insertUpstreamRemarkFile(f);
            ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
        }
    }

    private void doDeleteAttach(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        String type = WebUtil.getSafeParam(p, "type");
        if ("REMARK".equals(type)) subDao.deleteUpstreamRemarkFile(id);
        else subDao.deleteUpstreamAttach(id);
        ResponseUtil.ok(resp);
    }

    private void doListAttachs(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long agreementId = WebUtil.getLong(p, "agreementId", 0);
        List<AttachFile> attachs = subDao.listUpstreamAttaches(agreementId);
        ResponseUtil.ok(resp, fillUrl(req, attachs));
    }

    private void saveSubTables(Connection conn, long agreementId, Map<String, Object> p) throws SQLException {
        // 保存团队目标
        Object teamObj = p.get("teamTargets");
        if (teamObj != null) {
            String json = new Gson().toJson(teamObj);
            Type t = new TypeToken<List<TeamTarget>>() {}.getType();
            List<TeamTarget> list = new Gson().fromJson(json, t);
            if (list != null) {
                for (TeamTarget tt : list) {
                    tt.setAgreementId(agreementId);
                    subDao.insertTeamTargetWithConn(conn, tt);
                }
            }
        }

        // 保存返利计算规则（支持按考核组分组的格式）
        Object rulesObj = p.get("rebateRules");
        if (rulesObj == null) return;
        String json = new Gson().toJson(rulesObj);
        if ("{}".equals(json) || "[]".equals(json)) return;
        // 尝试解析为按考核组ID分组的格式
        try {
            Type mapType = new TypeToken<Map<String, List<RebateRule>>>() {}.getType();
            Map<String, List<RebateRule>> rulesMap = new Gson().fromJson(json, mapType);
            if (rulesMap != null && !rulesMap.isEmpty()) {
                for (Map.Entry<String, List<RebateRule>> entry : rulesMap.entrySet()) {
                    String key = entry.getKey();
                    // 默认组（id=0或"default"）时，assessGroupId 设为 null
                    Long assessGroupId = null;
                    if (!"0".equals(key) && !"default".equals(key) && !"DEFAULT".equals(key)) {
                        try {
                            assessGroupId = Long.parseLong(key);
                        } catch (NumberFormatException ignore) {}
                    }
                    List<RebateRule> rules = entry.getValue();
                    for (int i = 0; i < rules.size(); i++) {
                        RebateRule rule = rules.get(i);
                        if (rule != null) {
                            rule.setAgreementId(agreementId);
                            rule.setAssessGroupId(assessGroupId);
                            rule.setSortNo(i + 1);
                            rule.setSharedGroupIds(normalizeSharedGroupIds(rule.getSharedGroupIds()));
                        ruleDao.insertRuleWithConn(conn, rule);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 兼容旧格式：直接是规则数组
            try {
                Type listType = new TypeToken<List<RebateRule>>() {}.getType();
                List<RebateRule> rules = new Gson().fromJson(json, listType);
                if (rules != null && !rules.isEmpty()) {
                    Long assessGroupId = WebUtil.getLong(p, "assessGroupId", 0);
                    for (int i = 0; i < rules.size(); i++) {
                        RebateRule rule = rules.get(i);
                        rule.setAgreementId(agreementId);
                        rule.setSortNo(i + 1);
                        rule.setSharedGroupIds(normalizeSharedGroupIds(rule.getSharedGroupIds()));
                        if (assessGroupId != null && assessGroupId > 0) {
                            rule.setAssessGroupId(assessGroupId);
                        }
                        ruleDao.insertRuleWithConn(conn, rule);
                    }
                }
            } catch (Exception ignore) {}
        }
    }

    private UpstreamAgreement parse(Map<String, Object> p) {
        UpstreamAgreement a = new UpstreamAgreement();
        a.setProjectId(WebUtil.getLong(p, "projectId", 0));
        a.setBpmAgreeId(WebUtil.getSafeParam(p, "bpmAgreeId"));
        a.setAgreementName(WebUtil.getSafeParam(p, "agreementName"));
        a.setAgreementNo(WebUtil.getSafeParam(p, "agreementNo"));
        String s = WebUtil.getSafeParam(p, "periodStartDate");
        String e = WebUtil.getSafeParam(p, "periodEndDate");
        if (s != null && !s.isEmpty()) a.setPeriodStartDate(Date.valueOf(s));
        if (e != null && !e.isEmpty()) a.setPeriodEndDate(Date.valueOf(e));
        a.setRegion(WebUtil.getSafeParam(p, "region"));
        a.setTargetTerminal(WebUtil.getSafeParam(p, "targetTerminal"));
        a.setCalcBasis(WebUtil.getSafeParam(p, "calcBasis"));
        a.setTargetScale(toBd(p.get("targetScale")));
        a.setCalcMethod(WebUtil.getSafeParam(p, "calcMethod"));
        String calcMode = WebUtil.getSafeParam(p, "calcMode");
        a.setCalcMode(calcMode == null || calcMode.isEmpty() ? "PROGRESSIVE" : calcMode);
        a.setRebateCalcBasis(WebUtil.getSafeParam(p, "rebateCalcBasis"));
        a.setSupplier(WebUtil.getSafeParam(p, "supplier"));
        a.setTargetDept(WebUtil.getSafeParam(p, "targetDept"));
        a.setFlowContact(WebUtil.getSafeParam(p, "flowContact"));
        a.setFlowPhone(WebUtil.getSafeParam(p, "flowPhone"));
        a.setFlowChannel(WebUtil.getSafeParam(p, "flowChannel"));
        a.setFlowProvideMethod(WebUtil.getSafeParam(p, "flowProvideMethod"));
        a.setStage1Target(toBd(p.get("stage1Target")));
        a.setStage2Target(toBd(p.get("stage2Target")));
        a.setStage3Target(toBd(p.get("stage3Target")));
        a.setStage4Target(toBd(p.get("stage4Target")));
        Long oid = WebUtil.getLong(p, "ownerUserId", 0);
        a.setOwnerUserId(oid == 0 ? null : oid);
        a.setPolicyDetail(WebUtil.getSafeParam(p, "policyDetail"));
        a.setRebateCalcRule(WebUtil.getSafeParam(p, "rebateCalcRule"));
        a.setSettleBasis(WebUtil.getSafeParam(p, "settleBasis"));
        Object settleRatioObj = p.get("settleRatio");
        if (settleRatioObj != null) {
            a.setSettleRatio(new Gson().toJson(settleRatioObj));
        }
        a.setRebatePayType(WebUtil.getSafeParam(p, "rebatePayType"));
        a.setRebatePayTime(WebUtil.getSafeParam(p, "rebatePayTime"));
        a.setTeamAssessSettle(WebUtil.getSafeParam(p, "teamAssessSettle"));
        a.setRequiredStaffNum(WebUtil.getInt(p, "requiredStaffNum", 0));
        a.setFormalCount(WebUtil.getInt(p, "formalCount", 0));
        a.setFormalNames(WebUtil.getSafeParam(p, "formalNames"));
        a.setInformalCount(WebUtil.getInt(p, "informalCount", 0));
        a.setInformalNames(WebUtil.getSafeParam(p, "informalNames"));
        return a;
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    /** sharedGroupIds 排序归一化：确保 "2,3" 和 "3,2" 存为同一字符串 "2,3" */
    static String normalizeSharedGroupIds(String val) {
        if (val == null || val.trim().isEmpty()) return "";
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (String p : val.split("[,，;； ]+")) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            try { ids.add(Long.parseLong(t)); } catch (NumberFormatException ignored) {}
        }
        java.util.Collections.sort(ids);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    private void doListRebateRules(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long agreementId = WebUtil.getLong(p, "agreementId", 0);
        ResponseUtil.ok(resp, ruleDao.listByAgreement(agreementId));
    }

    private void doListAssessGroups(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long projectId = WebUtil.getLong(p, "projectId", 0);
        ResponseUtil.ok(resp, ruleDao.listAssessGroups(projectId));
    }

    private void doGetAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        ResponseUtil.ok(resp, ruleDao.getAssessGroup(id));
    }

    private void doAddAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        AssessGroup g = new AssessGroup();
        g.setProjectId(WebUtil.getLong(p, "projectId", 0L));
        g.setGroupCode(WebUtil.getSafeParam(p, "groupCode"));
        g.setGroupName(WebUtil.getSafeParam(p, "groupName"));
        g.setDescription(WebUtil.getSafeParam(p, "description"));
        g.setTargetScale(toBd(p.get("targetScale")));
        g.setStage1Target(toBd(p.get("stage1Target")));
        g.setStage2Target(toBd(p.get("stage2Target")));
        g.setStage3Target(toBd(p.get("stage3Target")));
        g.setStage4Target(toBd(p.get("stage4Target")));
        g.setSharedGroupIds(normalizeSharedGroupIds(WebUtil.getSafeParam(p, "sharedGroupIds")));
        g.setCreatedBy(u.getId());
        Long id = ruleDao.insertAssessGroup(g);
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
    }

    private void doUpdateAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        AssessGroup g = new AssessGroup();
        g.setId(WebUtil.getLong(p, "id", 0));
        g.setGroupCode(WebUtil.getSafeParam(p, "groupCode"));
        g.setGroupName(WebUtil.getSafeParam(p, "groupName"));
        g.setDescription(WebUtil.getSafeParam(p, "description"));
        g.setTargetScale(toBd(p.get("targetScale")));
        g.setStage1Target(toBd(p.get("stage1Target")));
        g.setStage2Target(toBd(p.get("stage2Target")));
        g.setStage3Target(toBd(p.get("stage3Target")));
        g.setStage4Target(toBd(p.get("stage4Target")));
        g.setSharedGroupIds(normalizeSharedGroupIds(WebUtil.getSafeParam(p, "sharedGroupIds")));
        ruleDao.updateAssessGroup(g);
        ResponseUtil.ok(resp);
    }

    private void doDeleteAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long groupId = WebUtil.getLong(p, "id", 0);
        if (groupId > 0) {
            // 删除前，清理其他考核组对本组的共享引用，避免悬空引用
            AssessGroup target = ruleDao.getAssessGroup(groupId);
            if (target != null && target.getProjectId() != null) {
                cleanupSharedReferences(groupId, target.getProjectId());
            }
            ruleDao.deleteAssessGroup(groupId);
        }
        ResponseUtil.ok(resp);
    }

    /** 删除考核组时，从同项目其他考核组的 shared_group_ids 中移除对该组的引用 */
    private void cleanupSharedReferences(Long deletedGroupId, Long projectId) {
        try {
            List<AssessGroup> all = ruleDao.listAssessGroups(projectId);
            if (all == null) return;
            String deletedIdStr = String.valueOf(deletedGroupId);
            for (AssessGroup g : all) {
                String sids = g.getSharedGroupIds();
                if (sids == null || sids.trim().isEmpty()) continue;
                String[] parts = sids.split("[,，;； ]+");
                boolean changed = false;
                StringBuilder sb = new StringBuilder();
                for (String part : parts) {
                    String t = part.trim();
                    if (t.isEmpty()) continue;
                    if (t.equals(deletedIdStr)) { changed = true; continue; }
                    if (sb.length() > 0) sb.append(',');
                    sb.append(t);
                }
                if (changed) {
                    g.setSharedGroupIds(sb.toString());
                    ruleDao.updateAssessGroup(g);
                }
            }
        } catch (Exception e) {
            // 清理失败不阻断删除主流程
        }
    }
    
    private boolean checkPerm(com.rebate.model.UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "list":
            case "listAll":
            case "get":
            case "listAttachs":
            case "listRebateRules":
            case "listAssessGroups":
            case "getAssessGroup":
            case "listBpmAgreements":
                return u.hasPerm("agreement:view");
            case "add":
            case "update":
            case "delete":
            case "uploadAttach":
            case "deleteAttach":
            case "addAssessGroup":
            case "updateAssessGroup":
            case "deleteAssessGroup":
            case "importFromBpm":
                return u.hasPerm("agreement:edit");
            default:
                return true;
        }
    }
}
