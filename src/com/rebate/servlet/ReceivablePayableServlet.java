package com.rebate.servlet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rebate.dao.AssessItemDetailDao;
import com.rebate.dao.ProjectDao;
import com.rebate.dao.ReceivablePayableDao;
import com.rebate.dao.UpstreamFlowDao;
import com.rebate.model.AssessItemDetail;
import com.rebate.model.AttachFile;
import com.rebate.model.Payable;
import com.rebate.model.Project;
import com.rebate.model.Receivable;
import com.rebate.util.ExcelUtil;
import com.rebate.util.FileUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.stream.Collectors;

/**
 * 应收/应付
 */
public class ReceivablePayableServlet extends BaseServlet {

    private final ReceivablePayableDao dao = new ReceivablePayableDao();
    private final AssessItemDetailDao assessItemDao = new AssessItemDetailDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, com.rebate.model.UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "listReceivable";
        
        if (!checkPerm(u, op)) {
            ResponseUtil.forbidden(resp);
            return;
        }
        
        switch (op) {
            case "listReceivable": doListRecv(req, resp, p); break;
            case "getReceivable": doGetRecv(req, resp, p); break;
            case "saveReceivable": doSaveRecv(req, resp, p, u); break;
            case "auditReceivable": doAuditRecv(req, resp, p, u); break;
            case "deleteReceivable": doDeleteRecv(req, resp, p); break;
            case "listReceivableAssessItems": doListRecvAssessItems(req, resp, p); break;
            case "uploadAssessAttach": doUploadRecvAssessAttach(req, resp, u); break;
            case "listPayable": doListPay(req, resp, p); break;
            case "savePayable": doSavePay(req, resp, p, u); break;
            case "auditPayable": doAuditPay(req, resp, p, u); break;
            case "confirmPayable": doConfirmPay(req, resp, p, u); break;
            case "listPayableAssessItems": doListPayAssessItems(req, resp, p); break;
            case "uploadPayAssessAttach": doUploadPayAssessAttach(req, resp, u); break;
            case "deleteAssessAttach": doDeleteAssessAttach(req, resp, p); break;
            case "sumScaleAmount": doSumScaleAmount(req, resp, p); break;
            case "exportReceivable": doExportReceivable(req, resp, p); break;
            case "exportPayable": doExportPayable(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }
    
    private boolean checkPerm(com.rebate.model.UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "listReceivable":
            case "getReceivable":
            case "listReceivableAssessItems":
            case "exportReceivable":
                return u.hasPerm("receivable:view");
            case "saveReceivable":
            case "deleteReceivable":
            case "uploadAssessAttach":
                return u.hasPerm("receivable:edit");
            case "deleteAssessAttach":
                // 具体在方法内二次检查：应收附件需要 receivable:edit，应付附件需要 payable:edit
                return u.hasPerm("receivable:edit") || u.hasPerm("payable:edit");
            case "auditReceivable":
                return u.hasPerm("receivable:audit");
            case "listPayable":
            case "listPayableAssessItems":
            case "exportPayable":
                return u.hasPerm("payable:view");
            case "savePayable":
            case "uploadPayAssessAttach":
                return u.hasPerm("payable:edit");
            case "sumScaleAmount":
                return u.hasPerm("receivable:view") || u.hasPerm("payable:view");
            case "auditPayable":
            case "confirmPayable":
                return u.hasPerm("payable:audit");
            default:
                return true;
        }
    }

    /** 为附件填充下载URL */
    private List<AttachFile> fillUrl(HttpServletRequest req, List<AttachFile> files) {
        if (files == null) return null;
        String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
        for (AttachFile f : files) f.setDownloadUrl(base + "/api/file/download?path=" + f.getFilePath());
        return files;
    }

    private void doListRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        List<Receivable> list = dao.listReceivableByProject(pid);
        for (Receivable r : list) {
            r.setAssessItems(assessItemDao.listByReceivable(r.getId()));
        }
        ResponseUtil.ok(resp, list);
    }

    private void doListRecvAssessItems(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long receivableId = WebUtil.getLong(p, "receivableId", 0);
        List<AssessItemDetail> list = assessItemDao.listByReceivable(receivableId);
        if (list != null) {
            for (AssessItemDetail item : list) {
                item.setAttachFiles(fillUrl(req, assessItemDao.listReceivableAttachsByItem(item.getId())));
            }
        }
        ResponseUtil.ok(resp, list);
    }

    private void doGetRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        Receivable r = dao.findReceivable(id);
        if (r != null) {
            r.setAssessItems(assessItemDao.listByReceivable(r.getId()));
        }
        ResponseUtil.ok(resp, r);
    }

    private void doSaveRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        Receivable r = new Receivable();
        r.setId(WebUtil.getLong(p, "id", 0) == 0 ? null : WebUtil.getLong(p, "id", 0));
        r.setProjectId(WebUtil.getLong(p, "projectId", 0));
        r.setStage(WebUtil.getSafeParam(p, "stage"));
        r.setScaleAmount(toBd(p.get("scaleAmount")));
        r.setAssessAmount(toBd(p.get("assessAmount")));
        r.setTotalAmount(r.getScaleAmount().add(r.getAssessAmount()));
        r.setEstimateAmount(toBd(p.get("estimateAmount")));
        r.setTaxRate(toBd(p.get("taxRate")));
        r.setStatus("DRAFT");
        r.setFillUser(u.getId());
        r.setFillTime(new Timestamp(System.currentTimeMillis()));
        r.setRemark(WebUtil.getSafeParam(p, "remark"));
        boolean isNew = r.getId() == null;
        if (isNew) {
            r.setId(dao.insertReceivable(r));
        } else {
            dao.updateReceivable(r);
            // 编辑模式：先清掉旧附件关联（assess_item_id 会被重新建立），不删除附件文件，只清空 assess_item_id 临时关联
        }
        Map<String, Long> itemIdRowKeyMap = saveReceivableAssessItems(r.getId(), p);
        // 处理 rowAttachMap：将附件关联到新的 assessItemId
        handleRowAttachMap(r.getId(), p, itemIdRowKeyMap, "RECEIVABLE");
        Map<String, Object> ret = new HashMap<>();
        ret.put("id", r.getId());
        ret.put("itemIdRowKeyMap", itemIdRowKeyMap);
        ResponseUtil.ok(resp, ret);
    }

    private Map<String, Long> saveReceivableAssessItems(Long receivableId, Map<String, Object> p) {
        Map<String, Long> rowKeyToItemId = new HashMap<>();
        if (receivableId == null) return rowKeyToItemId;
        assessItemDao.deleteByReceivable(receivableId);
        Object obj = p.get("assessItems");
        if (obj == null) return rowKeyToItemId;
        String json = new Gson().toJson(obj);
        if ("[]".equals(json) || "{}".equals(json)) return rowKeyToItemId;
        Type t = new TypeToken<List<AssessItemDetail>>() {}.getType();
        List<AssessItemDetail> list = new Gson().fromJson(json, t);
        if (list == null) return rowKeyToItemId;
        for (int i = 0; i < list.size(); i++) {
            AssessItemDetail d = list.get(i);
            if (d == null) continue;
            String rowKey = null;
            try {
                // 前端在 assessItems 中额外放入 rowKey 字段
                java.lang.reflect.Field f = AssessItemDetail.class.getDeclaredField("rowKey");
                f.setAccessible(true);
                rowKey = (String) f.get(d);
            } catch (Exception ignore) {}
            // 也可能通过 map 方式传入，尝试从 Gson 反序列化后的对象中没有 rowKey，这里通过 assessItems 的每个元素读取 rowKey 字段（如果是JsonObject 的话）
            // 兼容：assessItems 中每个 item 是 Map 时，尝试读 rowKey
            Object origItem = null;
            if (obj instanceof List) {
                try { origItem = ((List<?>) obj).get(i); } catch (Exception ignore) {}
            }
            if (origItem instanceof Map) {
                Object rk = ((Map<?, ?>) origItem).get("rowKey");
                if (rk != null) rowKey = String.valueOf(rk);
            }
            d.setId(null);
            d.setReceivableId(receivableId);
            d.setPayableId(null);
            d.setSortNo(i + 1);
            Long newId = assessItemDao.insertReceivableItem(d);
            if (rowKey != null) rowKeyToItemId.put(rowKey, newId);
        }
        return rowKeyToItemId;
    }

    /** 处理 rowAttachMap：根据 rowKey -> attachIds 的映射，更新附件的 assess_item_id */
    private void handleRowAttachMap(Long mainId, Map<String, Object> p, Map<String, Long> rowKeyToItemId, String type) {
        Object ram = p.get("rowAttachMap");
        if (ram == null) return;
        String json = new Gson().toJson(ram);
        if ("[]".equals(json) || "{}".equals(json)) return;
        Type t = new TypeToken<Map<String, List<Long>>>() {}.getType();
        Map<String, List<Long>> rowAttachMap;
        try {
            rowAttachMap = new Gson().fromJson(json, t);
        } catch (Exception e) { return; }
        if (rowAttachMap == null) return;
        for (Map.Entry<String, List<Long>> e : rowAttachMap.entrySet()) {
            String rowKey = e.getKey();
            List<Long> attachIds = e.getValue();
            Long itemId = rowKeyToItemId.get(rowKey);
            if (itemId == null || attachIds == null) continue;
            for (Long aid : attachIds) {
                if (aid == null) continue;
                if ("RECEIVABLE".equals(type)) {
                    assessItemDao.updateReceivableAttachItemId(aid, itemId, mainId);
                } else {
                    assessItemDao.updatePayableAttachItemId(aid, itemId, mainId);
                }
            }
        }
    }

    private void doAuditRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        String action = WebUtil.getSafeParam(p, "action"); // PASS / REJECT
        String newStatus = "PASS".equals(action) ? "FINAL" : "REJECTED";
        dao.auditReceivable(id, u.getId(), newStatus);
        
        // 如果审核通过，将上游流向对应月份置为终稿
        if ("PASS".equals(action)) {
            Receivable r = dao.findReceivable(id);
            if (r != null && r.getStage() != null) {
                markUpstreamFlowFinal(r.getProjectId(), r.getStage(), u.getId());
            }
        }
        
        ResponseUtil.ok(resp);
    }

    /**
     * 将上游流向对应月份置为终稿
     * 月份计算规则：项目起始时间+阶段开始月份到项目起始时间+阶段截止月份
     */
    private void markUpstreamFlowFinal(long projectId, String stage, long userId) {
        ProjectDao projectDao = new ProjectDao();
        Project project = projectDao.findById(projectId);
        if (project == null || project.getPeriodStartDate() == null) return;
        
        // 计算阶段对应的月份范围
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(project.getPeriodStartDate());
        int startYear = cal.get(java.util.Calendar.YEAR);
        int startMonth = cal.get(java.util.Calendar.MONTH) + 1;
        
        // 阶段对应的相对月份范围（每个阶段3个月）
        int stageIndex = 0;
        if ("阶段一".equals(stage)) stageIndex = 0;
        else if ("阶段二".equals(stage)) stageIndex = 1;
        else if ("阶段三".equals(stage)) stageIndex = 2;
        else if ("阶段四".equals(stage)) stageIndex = 3;
        else return; // 全年或其他不处理
        
        int stageStartMonth = stageIndex * 3; // 相对月份偏移
        int stageEndMonth = stageStartMonth + 2; // 阶段结束相对月份
        
        // 计算实际的年月
        int actualStartYear = startYear + (startMonth + stageStartMonth - 1) / 12;
        int actualStartMonth = (startMonth + stageStartMonth - 1) % 12 + 1;
        int actualEndYear = startYear + (startMonth + stageEndMonth - 1) / 12;
        int actualEndMonth = (startMonth + stageEndMonth - 1) % 12 + 1;
        
        // 生成月份列表
        List<String> months = new ArrayList<>();
        int y = actualStartYear, m = actualStartMonth;
        while (y < actualEndYear || (y == actualEndYear && m <= actualEndMonth)) {
            months.add(String.format("%04d%02d", y, m));
            m++;
            if (m > 12) { m = 1; y++; }
        }
        
        // 将这些月份的上游流向置为终稿
        UpstreamFlowDao upstreamFlowDao = new UpstreamFlowDao();
        for (String month : months) {
            upstreamFlowDao.setFinalMonth(projectId, month, userId);
            upstreamFlowDao.markFinalInRecords(projectId, month);
        }
    }

    private void doDeleteRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        assessItemDao.deleteReceivableAttachsByReceivable(id);
        assessItemDao.deleteByReceivable(id);
        dao.deleteReceivable(id);
        ResponseUtil.ok(resp);
    }

    private void doListPay(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        Long agId = WebUtil.getLong(p, "agreementId", 0);
        if (agId != null && agId == 0) agId = null;
        List<Payable> list = dao.listPayableByProject(pid, agId, WebUtil.getSafeParam(p, "stage"), WebUtil.getSafeParam(p, "status"));
        for (Payable r : list) {
            r.setAssessItems(assessItemDao.listByPayable(r.getId()));
        }
        ResponseUtil.ok(resp, list);
    }

    private void doListPayAssessItems(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long payableId = WebUtil.getLong(p, "payableId", 0);
        List<AssessItemDetail> list = assessItemDao.listByPayable(payableId);
        if (list != null) {
            for (AssessItemDetail item : list) {
                item.setAttachFiles(fillUrl(req, assessItemDao.listPayableAttachsByItem(item.getId())));
            }
        }
        ResponseUtil.ok(resp, list);
    }

    private void doSavePay(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        Payable r = new Payable();
        r.setId(WebUtil.getLong(p, "id", 0) == 0 ? null : WebUtil.getLong(p, "id", 0));
        r.setProjectId(WebUtil.getLong(p, "projectId", 0));
        r.setAgreementId(WebUtil.getLong(p, "agreementId", 0));
        r.setStage(WebUtil.getSafeParam(p, "stage"));
        r.setScaleAmount(toBd(p.get("scaleAmount")));
        r.setAssessAmount(toBd(p.get("assessAmount")));
        r.setTotalAmount(r.getScaleAmount().add(r.getAssessAmount()));
        r.setEstimateAmount(toBd(p.get("estimateAmount")));
        r.setTaxRate(toBd(p.get("taxRate")));
        r.setStatus("DRAFT");
        r.setFillUser(u.getId());
        r.setFillTime(new Timestamp(System.currentTimeMillis()));
        r.setRemark(WebUtil.getSafeParam(p, "remark"));
        if (r.getId() == null) {
            r.setId(dao.insertPayable(r));
        } else {
            dao.updatePayable(r);
        }
        Map<String, Long> itemIdRowKeyMap = savePayableAssessItems(r.getId(), p);
        handleRowAttachMap(r.getId(), p, itemIdRowKeyMap, "PAYABLE");
        Map<String, Object> ret = new HashMap<>();
        ret.put("id", r.getId());
        ret.put("itemIdRowKeyMap", itemIdRowKeyMap);
        ResponseUtil.ok(resp, ret);
    }

    private Map<String, Long> savePayableAssessItems(Long payableId, Map<String, Object> p) {
        Map<String, Long> rowKeyToItemId = new HashMap<>();
        if (payableId == null) return rowKeyToItemId;
        assessItemDao.deleteByPayable(payableId);
        Object obj = p.get("assessItems");
        if (obj == null) return rowKeyToItemId;
        String json = new Gson().toJson(obj);
        if ("[]".equals(json) || "{}".equals(json)) return rowKeyToItemId;
        Type t = new TypeToken<List<AssessItemDetail>>() {}.getType();
        List<AssessItemDetail> list = new Gson().fromJson(json, t);
        if (list == null) return rowKeyToItemId;
        for (int i = 0; i < list.size(); i++) {
            AssessItemDetail d = list.get(i);
            if (d == null) continue;
            String rowKey = null;
            Object origItem = null;
            if (obj instanceof List) {
                try { origItem = ((List<?>) obj).get(i); } catch (Exception ignore) {}
            }
            if (origItem instanceof Map) {
                Object rk = ((Map<?, ?>) origItem).get("rowKey");
                if (rk != null) rowKey = String.valueOf(rk);
            }
            d.setId(null);
            d.setPayableId(payableId);
            d.setReceivableId(null);
            d.setSortNo(i + 1);
            Long newId = assessItemDao.insertPayableItem(d);
            if (rowKey != null) rowKeyToItemId.put(rowKey, newId);
        }
        return rowKeyToItemId;
    }

    private void doAuditPay(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        String action = WebUtil.getSafeParam(p, "action");
        String newStatus = "PASS".equals(action) ? "AUDIT" : "REJECTED";
        dao.auditPayable(id, u.getId(), newStatus);
        ResponseUtil.ok(resp);
    }

    private void doConfirmPay(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        dao.confirmPayable(id, u.getId(), "FINAL");
        ResponseUtil.ok(resp);
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private void doSumScaleAmount(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        String type = WebUtil.getSafeParam(p, "type");
        if (type == null || type.isEmpty()) type = "receivable";
        long pid = WebUtil.getLong(p, "projectId", 0);
        String stage = WebUtil.getSafeParam(p, "stage");
        Long excludeId = WebUtil.getLong(p, "excludeId", 0L);
        if (excludeId == 0) excludeId = null;

        java.math.BigDecimal sum;
        if ("payable".equalsIgnoreCase(type)) {
            long aid = WebUtil.getLong(p, "agreementId", 0L);
            sum = dao.sumPayableScaleAmount(pid, aid > 0 ? aid : null, stage, excludeId);
        } else {
            sum = dao.sumReceivableScaleAmount(pid, stage, excludeId);
        }
        if (sum == null) sum = java.math.BigDecimal.ZERO;
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("sum", sum);
        ResponseUtil.ok(resp, result);
    }

    private void doExportReceivable(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        List<Receivable> list = dao.listReceivableByProject(pid);

        List<String> headers = Arrays.asList("阶段", "依据规模应收", "依据考核应收", "合计应收",
                "税率", "无税金额", "系统估算", "状态", "填报人", "填报时间", "审核人", "审核时间", "备注");
        List<List<String>> rows = new ArrayList<>();
        for (Receivable r : list) {
            java.math.BigDecimal rate = r.getTaxRate() != null ? r.getTaxRate() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal total = r.getTotalAmount() != null ? r.getTotalAmount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal taxEx = (rate != null && rate.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? total.divide(java.math.BigDecimal.ONE.add(rate.divide(new java.math.BigDecimal(100), 4, java.math.BigDecimal.ROUND_HALF_UP)), 2, java.math.BigDecimal.ROUND_HALF_UP)
                : total;
            rows.add(Arrays.asList(
                r.getStage() != null ? r.getStage() : "",
                r.getScaleAmount() != null ? r.getScaleAmount().toString() : "",
                r.getAssessAmount() != null ? r.getAssessAmount().toString() : "",
                r.getTotalAmount() != null ? r.getTotalAmount().toString() : "",
                rate != null ? rate.toString() : "",
                taxEx != null ? taxEx.toString() : "",
                r.getEstimateAmount() != null ? r.getEstimateAmount().toString() : "",
                r.getStatus() != null ? r.getStatus() : "",
                r.getFillUser() != null ? r.getFillUser().toString() : "",
                r.getFillTime() != null ? r.getFillTime().toString() : "",
                r.getAuditUser() != null ? r.getAuditUser().toString() : "",
                r.getAuditTime() != null ? r.getAuditTime().toString() : "",
                r.getRemark() != null ? r.getRemark() : ""
            ));
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileName = "项目应收_" + sdf.format(new Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }

    private void doExportPayable(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        List<Payable> list = dao.listPayableByProject(pid, null, null, null);

        List<String> headers = Arrays.asList("阶段", "依据规模应付", "依据考核应付", "合计应付",
                "税率", "无税金额", "系统估算", "状态", "填报人", "填报时间", "审核人", "审核时间", "备注");
        List<List<String>> rows = new ArrayList<>();
        for (Payable r : list) {
            java.math.BigDecimal rate = r.getTaxRate() != null ? r.getTaxRate() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal total = r.getTotalAmount() != null ? r.getTotalAmount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal taxEx = (rate != null && rate.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? total.divide(java.math.BigDecimal.ONE.add(rate.divide(new java.math.BigDecimal(100), 4, java.math.BigDecimal.ROUND_HALF_UP)), 2, java.math.BigDecimal.ROUND_HALF_UP)
                : total;
            rows.add(Arrays.asList(
                r.getStage() != null ? r.getStage() : "",
                r.getScaleAmount() != null ? r.getScaleAmount().toString() : "",
                r.getAssessAmount() != null ? r.getAssessAmount().toString() : "",
                r.getTotalAmount() != null ? r.getTotalAmount().toString() : "",
                rate != null ? rate.toString() : "",
                taxEx != null ? taxEx.toString() : "",
                r.getEstimateAmount() != null ? r.getEstimateAmount().toString() : "",
                r.getStatus() != null ? r.getStatus() : "",
                r.getFillUser() != null ? r.getFillUser().toString() : "",
                r.getFillTime() != null ? r.getFillTime().toString() : "",
                r.getAuditUser() != null ? r.getAuditUser().toString() : "",
                r.getAuditTime() != null ? r.getAuditTime().toString() : "",
                r.getRemark() != null ? r.getRemark() : ""
            ));
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileName = "项目应付_" + sdf.format(new Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }

    // ========== 应收考核明细附件上传 ==========
    private void doUploadRecvAssessAttach(HttpServletRequest req, HttpServletResponse resp, com.rebate.model.UserContext u) throws Exception {
        long receivableId = 0;
        try { receivableId = Long.parseLong(req.getParameter("receivableId")); } catch (Exception ignore) {}
        if (receivableId <= 0) { ResponseUtil.fail(resp, "receivableId 必填"); return; }
        long assessItemId = 0;
        try { assessItemId = Long.parseLong(req.getParameter("assessItemId")); } catch (Exception ignore) {}
        Part file = req.getPart("file");
        if (file == null) { ResponseUtil.fail(resp, "请选择文件"); return; }
        String rel = FileUtil.save(file.getInputStream(), "receivable/assess", file.getSubmittedFileName());
        AttachFile f = new AttachFile();
        f.setFileName(file.getSubmittedFileName());
        f.setFilePath(rel);
        f.setFileSize(file.getSize());
        f.setUploadedBy(u.getId());
        Long attachId = assessItemDao.insertReceivableAssessAttach(
                assessItemId > 0 ? assessItemId : null, receivableId, f);
        f.setId(attachId);
        String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
        f.setDownloadUrl(base + "/api/file/download?path=" + f.getFilePath());
        Map<String, Object> ret = new HashMap<>();
        ret.put("file", f);
        if (assessItemId > 0) ret.put("assessItemId", assessItemId);
        ResponseUtil.ok(resp, ret);
    }

    // ========== 应付考核明细附件上传 ==========
    private void doUploadPayAssessAttach(HttpServletRequest req, HttpServletResponse resp, com.rebate.model.UserContext u) throws Exception {
        long payableId = 0;
        try { payableId = Long.parseLong(req.getParameter("payableId")); } catch (Exception ignore) {}
        if (payableId <= 0) { ResponseUtil.fail(resp, "payableId 必填"); return; }
        long assessItemId = 0;
        try { assessItemId = Long.parseLong(req.getParameter("assessItemId")); } catch (Exception ignore) {}
        Part file = req.getPart("file");
        if (file == null) { ResponseUtil.fail(resp, "请选择文件"); return; }
        String rel = FileUtil.save(file.getInputStream(), "payable/assess", file.getSubmittedFileName());
        AttachFile f = new AttachFile();
        f.setFileName(file.getSubmittedFileName());
        f.setFilePath(rel);
        f.setFileSize(file.getSize());
        f.setUploadedBy(u.getId());
        Long attachId = assessItemDao.insertPayableAssessAttach(
                assessItemId > 0 ? assessItemId : null, payableId, f);
        f.setId(attachId);
        String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
        f.setDownloadUrl(base + "/api/file/download?path=" + f.getFilePath());
        Map<String, Object> ret = new HashMap<>();
        ret.put("file", f);
        if (assessItemId > 0) ret.put("assessItemId", assessItemId);
        ResponseUtil.ok(resp, ret);
    }

    // ========== 删除考核明细附件（应收/应付通用）==========
    private void doDeleteAssessAttach(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long id = WebUtil.getLong(p, "id", 0);
        if (id <= 0) { ResponseUtil.fail(resp, "id 必填"); return; }
        var u = TokenUtil.getLoginUser(req, com.rebate.model.UserContext.class);
        // 先查应收附件表
        AttachFile recvAttach = assessItemDao.findReceivableAttach(id);
        if (recvAttach != null) {
            if (u != null && !u.isAdmin() && !u.hasPerm("receivable:edit")) {
                ResponseUtil.forbidden(resp); return;
            }
            assessItemDao.deleteReceivableAttach(id);
            ResponseUtil.ok(resp);
            return;
        }
        // 再查应付附件表
        AttachFile payAttach = assessItemDao.findPayableAttach(id);
        if (payAttach != null) {
            if (u != null && !u.isAdmin() && !u.hasPerm("payable:edit")) {
                ResponseUtil.forbidden(resp); return;
            }
            assessItemDao.deletePayableAttach(id);
            ResponseUtil.ok(resp);
            return;
        }
        ResponseUtil.fail(resp, "附件不存在");
    }
}
