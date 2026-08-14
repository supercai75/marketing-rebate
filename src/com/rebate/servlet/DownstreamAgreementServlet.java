package com.rebate.servlet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rebate.dao.AgreementSubDao;
import com.rebate.dao.AssessDownstreamTargetDao;
import com.rebate.dao.DownstreamAgreementDao;
import com.rebate.dao.RebateRuleDao;
import com.rebate.model.AssessDownstreamTarget;
import com.rebate.model.AssessGroup;
import com.rebate.model.AttachFile;
import com.rebate.model.DownstreamAgreement;
import com.rebate.model.RebateRule;
import com.rebate.model.TeamTarget;
import com.rebate.util.FileUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;

/**
 * 下游协议管理
 */
@MultipartConfig
public class DownstreamAgreementServlet extends BaseServlet {

    private final DownstreamAgreementDao dao = new DownstreamAgreementDao();
    private final AgreementSubDao subDao = new AgreementSubDao();
    private final RebateRuleDao ruleDao = new RebateRuleDao();
    private final AssessDownstreamTargetDao targetDao = new AssessDownstreamTargetDao();

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
            case "get": doGet(req, resp, p); break;
            case "add": doAdd(req, resp, p, u); break;
            case "update": doUpdate(req, resp, p); break;
            case "delete": doDelete(req, resp, p); break;
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
    
    private boolean checkPerm(com.rebate.model.UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "list":
            case "get":
            case "listAttachs":
            case "listRebateRules":
            case "listAssessGroups":
            case "getAssessGroup":
                return u.hasPerm("agreement:view");
            case "add":
            case "update":
            case "delete":
            case "uploadAttach":
            case "deleteAttach":
            case "addAssessGroup":
            case "updateAssessGroup":
            case "deleteAssessGroup":
                return u.hasPerm("agreement:edit");
            default:
                return false;
        }
    }

    private void doList(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        boolean currentOnly = !"0".equals(WebUtil.getSafeParam(p, "showHistory"));
        ResponseUtil.ok(resp, dao.listByProject(pid, currentOnly));
    }

    private void doGet(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        DownstreamAgreement a = dao.findById(id);
        if (a != null) {
            a.setTeamTargets(subDao.listDownstreamTeamTargets(id));
            a.setRemarkFiles(fillUrl(req, subDao.listDownstreamRemarkFiles(id)));
            a.setAttachFiles(fillUrl(req, subDao.listDownstreamAttaches(id)));
            a.setRebateRules(ruleDao.listDownstreamRebateRules(id));
            // 加载下游协议考核目标
            a.setAssessTargets(targetDao.listByAgreement(id));
        }
        ResponseUtil.ok(resp, a);
    }

    private List<AttachFile> fillUrl(HttpServletRequest req, List<AttachFile> files) {
        if (files == null) return null;
        String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
        for (AttachFile f : files) f.setDownloadUrl(base + "/api/file/download?path=" + f.getFilePath());
        return files;
    }

    private void doAdd(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        DownstreamAgreement a = parse(p);
        a.setCreatedBy(u.getId());
        // 记录旧的生效版本ID（新版本创建前的 current 版本）—— 插入前查，因为 markNotCurrent 会把旧的设为 0
        Long oldCurrentId = dao.findCurrentId(a.getProjectId(), a.getAgreementNo());
        int maxVersion = dao.findMaxVersion(a.getProjectId(), a.getAgreementNo());
        a.setVersion(maxVersion + 1);
        a.setIsCurrent(1);
        Long id = dao.insert(a);
        if (id != null) dao.markNotCurrent(a.getProjectId(), a.getAgreementNo(), id);
        saveSubTables(id, p);
        // 将旧 current 版本下的业务数据（流向/应付/实付/分解/定案等）迁移到新版本
        if (oldCurrentId != null && oldCurrentId > 0 && id != null && !oldCurrentId.equals(id)) {
            dao.migrateAssociatedData(oldCurrentId, id);
        }
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
    }

    private void doUpdate(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        DownstreamAgreement a = dao.findById(id);
        if (a == null) { ResponseUtil.fail(resp, "协议不存在"); return; }
        DownstreamAgreement upd = parse(p);
        upd.setId(id);
        dao.update(upd);
        subDao.clearDownstreamTeamTargets(id);
        ruleDao.deleteDownstreamRebateRules(id);
        saveSubTables(id, p);
        ResponseUtil.ok(resp);
    }

    private void doDelete(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        dao.delete(WebUtil.getLong(p, "id", 0));
        ResponseUtil.ok(resp);
    }

    private void doUploadAttach(HttpServletRequest req, HttpServletResponse resp, com.rebate.model.UserContext u) throws Exception {
        long agreementId = 0;
        try { agreementId = Long.parseLong(req.getParameter("agreementId")); } catch (Exception ignore) {}
        if (agreementId <= 0) { ResponseUtil.fail(resp, "agreementId 必填"); return; }
        String attachType = req.getParameter("attachType");
        Part file = req.getPart("file");
        if (file == null) { ResponseUtil.fail(resp, "请选择文件"); return; }
        String rel = FileUtil.save(file.getInputStream(), "agreement/downstream", file.getSubmittedFileName());
        AttachFile f = new AttachFile();
        f.setAgreementId(agreementId);
        f.setFileName(file.getSubmittedFileName());
        f.setFilePath(rel);
        f.setFileSize(file.getSize());
        f.setUploadedBy(u.getId());
        Long id;
        if ("MAIN".equals(attachType) || "SUPP".equals(attachType)) {
            f.setAttachType(attachType);
            id = subDao.insertDownstreamAttach(f);
            f.setId(id);
            String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
            f.setDownloadUrl(base + "/api/file/download?path=" + f.getFilePath());
            ResponseUtil.ok(resp, f);
        } else {
            f.setFileType(attachType);
            id = subDao.insertDownstreamRemarkFile(f);
            ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
        }
    }

    private void doDeleteAttach(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        String type = WebUtil.getSafeParam(p, "type");
        if ("REMARK".equals(type)) subDao.deleteDownstreamRemarkFile(id);
        else subDao.deleteDownstreamAttach(id);
        ResponseUtil.ok(resp);
    }

    private void doListAttachs(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long agreementId = WebUtil.getLong(p, "agreementId", 0);
        List<AttachFile> attachs = subDao.listDownstreamAttaches(agreementId);
        ResponseUtil.ok(resp, fillUrl(req, attachs));
    }

    private void doListRebateRules(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long agreementId = WebUtil.getLong(p, "agreementId", 0);
        ResponseUtil.ok(resp, ruleDao.listDownstreamRebateRules(agreementId));
    }

    private void saveSubTables(long agreementId, Map<String, Object> p) {
        Object teamObj = p.get("teamTargets");
        if (teamObj != null) {
            String json = new Gson().toJson(teamObj);
            Type t = new TypeToken<List<TeamTarget>>() {}.getType();
            List<TeamTarget> list = new Gson().fromJson(json, t);
            if (list != null) {
                for (TeamTarget tt : list) {
                    tt.setAgreementId(agreementId);
                    subDao.insertDownstreamTeamTarget(tt);
                }
            }
        }

        // 保存考核组目标
        Object assessTargetsObj = p.get("assessTargets");
        if (assessTargetsObj != null) {
            String json = new Gson().toJson(assessTargetsObj);
            if (!"{}".equals(json) && !"[]".equals(json)) {
                try {
                    Type listType = new TypeToken<List<AssessDownstreamTarget>>() {}.getType();
                    List<AssessDownstreamTarget> list = new Gson().fromJson(json, listType);
                    if (list != null) {
                        for (AssessDownstreamTarget t : list) {
                            t.setAgreementId(agreementId);
                            // 考核组ID为0时设为null（默认组）
                            if (t.getAssessGroupId() != null && t.getAssessGroupId() == 0) {
                                t.setAssessGroupId(null);
                            }
                            targetDao.upsert(t);
                        }
                    }
                } catch (Exception ignore) {}
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
                            ruleDao.insertDownstreamRebateRule(rule);
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
                        ruleDao.insertDownstreamRebateRule(rule);
                    }
                }
            } catch (Exception ignore) {}
        }
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

    private DownstreamAgreement parse(Map<String, Object> p) {
        DownstreamAgreement a = new DownstreamAgreement();
        a.setProjectId(WebUtil.getLong(p, "projectId", 0));
        a.setUpstreamId(WebUtil.getLong(p, "upstreamId", 0));
        a.setBpmAgreeId(WebUtil.getSafeParam(p, "bpmAgreeId"));
        a.setUpstreamName(WebUtil.getSafeParam(p, "upstreamName"));
        a.setUpstreamNo(WebUtil.getSafeParam(p, "upstreamNo"));
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
        a.setDistributor(WebUtil.getSafeParam(p, "distributor"));
        a.setDistributorType(WebUtil.getSafeParam(p, "distributorType"));
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

    private void doListAssessGroups(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long projectId = WebUtil.getLong(p, "projectId", 0);
        long agreementId = WebUtil.getLong(p, "agreementId", 0);
        List<AssessGroup> groups = ruleDao.listAssessGroups(projectId);
        
        // 先清空所有考核组的目标数据，避免显示项目级考核组的默认目标（可能是上游协议设置的）
        if (groups != null) {
            for (AssessGroup g : groups) {
                g.setTargetScale(null);
                g.setStage1Target(null);
                g.setStage2Target(null);
                g.setStage3Target(null);
                g.setStage4Target(null);
            }
        }
        
        if (agreementId > 0) {
            List<AssessDownstreamTarget> targets = targetDao.listByAgreement(agreementId);
            if (groups == null || groups.isEmpty()) {
                // 没有项目级考核组时，查找协议级目标（assess_group_id 为 null 或 0）
                AssessDownstreamTarget protocolTarget = null;
                for (AssessDownstreamTarget t : targets) {
                    Long agId = t.getAssessGroupId();
                    if (agId == null || agId == 0) {
                        protocolTarget = t;
                        break;
                    }
                }
                if (protocolTarget != null) {
                    groups = new java.util.ArrayList<>();
                    AssessGroup g = new AssessGroup();
                    g.setId(0L);
                    g.setGroupName("默认组");
                    g.setGroupCode("DEFAULT");
                    g.setTargetScale(protocolTarget.getTotalTarget());
                    g.setStage1Target(protocolTarget.getStage1Target());
                    g.setStage2Target(protocolTarget.getStage2Target());
                    g.setStage3Target(protocolTarget.getStage3Target());
                    g.setStage4Target(protocolTarget.getStage4Target());
                    groups.add(g);
                } else {
                    groups = new java.util.ArrayList<>();
                }
            } else {
                // 有项目级考核组时，合并目标到组
                Map<Long, AssessDownstreamTarget> targetMap = new java.util.HashMap<>();
                for (AssessDownstreamTarget t : targets) {
                    // 确保目标确实属于当前下游协议（而非错误关联的其他协议数据）
                    if (t.getAgreementId() != null && t.getAgreementId() == agreementId) {
                        targetMap.put(t.getAssessGroupId(), t);
                    }
                }
                for (AssessGroup g : groups) {
                    AssessDownstreamTarget t = targetMap.get(g.getId());
                    if (t != null) {
                        g.setTargetScale(t.getTotalTarget());
                        g.setStage1Target(t.getStage1Target());
                        g.setStage2Target(t.getStage2Target());
                        g.setStage3Target(t.getStage3Target());
                        g.setStage4Target(t.getStage4Target());
                    }
                    // 如果没有对应的目标记录，AssessGroup 的目标字段保持为 null，前端会显示空白
                }
            }
        }
        if (groups == null) groups = new java.util.ArrayList<>();
        ResponseUtil.ok(resp, groups);
    }

    private void doGetAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        ResponseUtil.ok(resp, ruleDao.getAssessGroup(id));
    }

    private void doAddAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long projectId = WebUtil.getLong(p, "projectId", 0L);
        long agreementId = WebUtil.getLong(p, "agreementId", 0L);
        String groupCode = WebUtil.getSafeParam(p, "groupCode");
        String groupName = WebUtil.getSafeParam(p, "groupName");
        String description = WebUtil.getSafeParam(p, "description");
        String sharedGroupIds = normalizeSharedGroupIds(WebUtil.getSafeParam(p, "sharedGroupIds"));

        // 检查同项目下是否已有相同编码的组
        AssessGroup existing = ruleDao.getAssessGroupByProjectAndCode(projectId, groupCode);
        Long groupId;
        if (existing != null) {
            groupId = existing.getId();
            // 更新共享组（已存在组时也同步更新）
            existing.setSharedGroupIds(sharedGroupIds);
            ruleDao.updateAssessGroup(existing);
        } else {
            // 创建组定义（只存名称/编码/描述，不存目标字段）
            AssessGroup g = new AssessGroup();
            g.setProjectId(projectId);
            g.setGroupCode(groupCode);
            g.setGroupName(groupName);
            g.setDescription(description);
            g.setSharedGroupIds(sharedGroupIds);
            g.setCreatedBy(u.getId());
            groupId = ruleDao.insertAssessGroup(g);
        }

        // 保存下游协议专属目标到 prj_assess_downstream_target
        if (agreementId > 0 && groupId != null) {
            AssessDownstreamTarget t = new AssessDownstreamTarget();
            t.setAgreementId(agreementId);
            t.setAssessGroupId(groupId);
            t.setGroupName(groupName);
            t.setGroupCode(groupCode);
            t.setTotalTarget(toBd(p.get("targetScale")));
            t.setStage1Target(toBd(p.get("stage1Target")));
            t.setStage2Target(toBd(p.get("stage2Target")));
            t.setStage3Target(toBd(p.get("stage3Target")));
            t.setStage4Target(toBd(p.get("stage4Target")));
            t.setRemark(description);
            targetDao.upsert(t);
        }

        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", groupId));
    }

    private void doUpdateAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long agreementId = WebUtil.getLong(p, "agreementId", 0L);
        long groupId = WebUtil.getLong(p, "id", 0);
        if (agreementId > 0 && groupId > 0) {
            // 获取组信息用于存储目标
            AssessGroup grp = ruleDao.getAssessGroup(groupId);
            String groupName = WebUtil.getSafeParam(p, "groupName");
            String groupCode = WebUtil.getSafeParam(p, "groupCode");
            String description = WebUtil.getSafeParam(p, "description");
            String sharedGroupIds = normalizeSharedGroupIds(WebUtil.getSafeParam(p, "sharedGroupIds"));

            // 同步更新考核组定义（含共享组）
            if (grp != null) {
                grp.setGroupCode(groupCode);
                grp.setGroupName(groupName);
                grp.setDescription(description);
                grp.setSharedGroupIds(sharedGroupIds);
                ruleDao.updateAssessGroup(grp);
            }

            AssessDownstreamTarget t = new AssessDownstreamTarget();
            t.setAgreementId(agreementId);
            t.setAssessGroupId(groupId);
            t.setGroupName(grp != null ? grp.getGroupName() : groupName);
            t.setGroupCode(grp != null ? grp.getGroupCode() : groupCode);
            t.setTotalTarget(toBd(p.get("targetScale")));
            t.setStage1Target(toBd(p.get("stage1Target")));
            t.setStage2Target(toBd(p.get("stage2Target")));
            t.setStage3Target(toBd(p.get("stage3Target")));
            t.setStage4Target(toBd(p.get("stage4Target")));
            t.setRemark(description);
            targetDao.upsert(t);
        }
        ResponseUtil.ok(resp);
    }

    private void doDeleteAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long agreementId = WebUtil.getLong(p, "agreementId", 0L);
        long groupId = WebUtil.getLong(p, "id", 0);
        if (agreementId > 0 && groupId > 0) {
            targetDao.deleteByAgreementAndGroup(agreementId, groupId);
        }
        ResponseUtil.ok(resp);
    }
}
