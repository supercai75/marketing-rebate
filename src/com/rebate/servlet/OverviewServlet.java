package com.rebate.servlet;

import com.rebate.dao.AgreementSubDao;
import com.rebate.model.AttachFile;
import com.rebate.model.DownstreamAgreement;
import com.rebate.model.Project;
import com.rebate.model.UpstreamAgreement;
import com.rebate.service.OverviewService;
import com.rebate.service.RebateCalcService;
import com.rebate.util.ExcelUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 项目概览 / 平衡表
 */
public class OverviewServlet extends BaseServlet {

    private final OverviewService service = new OverviewService();
    private final RebateCalcService rebateCalcService = new RebateCalcService();
    private final AgreementSubDao subDao = new AgreementSubDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, com.rebate.model.UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "overview";
        
        if (!checkPerm(u, op)) {
            ResponseUtil.forbidden(resp);
            return;
        }
        
        switch (op) {
            case "overview": doOverview(req, resp, p); break;
            case "agreementOverview": doAgreementOverview(req, resp, p); break;
            case "overviewByAssessGroup": doOverviewByAssessGroup(req, resp, p); break;
            case "balanceTable": {
                String coYear = WebUtil.getSafeParam(p, "coYear");
                String undertakingDept = WebUtil.getSafeParam(p, "undertakingDept");
                String projectGroupId = WebUtil.getSafeParam(p, "projectGroupId");
                ResponseUtil.ok(resp, service.balanceTable(coYear, undertakingDept, projectGroupId));
                break;
            }
            case "payableEstimate": doPayableEstimate(req, resp, p); break;
            case "recalcRebate":
            case "calcRebate": doRecalcRebate(req, resp, p); break;
            case "calcDownstreamRebate": doCalcDownstreamRebate(req, resp, p); break;
            case "exportExcel": doExportExcel(req, resp, p); break;
            case "exportBalance": doExportBalance(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }

    private boolean checkPerm(com.rebate.model.UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "overview":
            case "agreementOverview":
            case "overviewByAssessGroup":
            case "balanceTable":
            case "payableEstimate":
            case "recalcRebate":
            case "calcRebate":
            case "calcDownstreamRebate":
            case "exportExcel":
            case "exportBalance":
                return u.hasPerm("overview:view");
            default:
                return false;
        }
    }

    private void doOverview(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        Map<String, Object> data = service.overview(pid);
        
        // 填充附件的下载 URL（包含上下文路径）
        if (data.get("upstream") instanceof UpstreamAgreement) {
            UpstreamAgreement up = (UpstreamAgreement) data.get("upstream");
            up.setAttachFiles(fillUrl(req, up.getAttachFiles()));
            up.setRemarkFiles(fillUrl(req, up.getRemarkFiles()));
        }
        if (data.get("downstreams") instanceof List) {
            List<?> downs = (List<?>) data.get("downstreams");
            for (Object obj : downs) {
                if (obj instanceof DownstreamAgreement) {
                    DownstreamAgreement down = (DownstreamAgreement) obj;
                    down.setAttachFiles(fillUrl(req, down.getAttachFiles()));
                    down.setRemarkFiles(fillUrl(req, down.getRemarkFiles()));
                }
            }
        }
        
        ResponseUtil.ok(resp, data);
    }
    
    private List<AttachFile> fillUrl(HttpServletRequest req, List<AttachFile> files) {
        if (files == null) return null;
        String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
        for (AttachFile f : files) {
            f.setDownloadUrl(base + "/api/file/download?path=" + f.getFilePath() + "&fileName=" + (f.getFileName() == null ? "" : java.net.URLEncoder.encode(f.getFileName(), java.nio.charset.StandardCharsets.UTF_8)));
        }
        return files;
    }
    
    private void doAgreementOverview(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long aid = WebUtil.getLong(p, "agreementId", 0);
        ResponseUtil.ok(resp, service.agreementOverview(aid));
    }
    
    private void doOverviewByAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String assessGroupIdStr = WebUtil.getSafeParam(p, "assessGroupId");
        Long assessGroupId = (assessGroupIdStr == null || assessGroupIdStr.isEmpty()) ? null : Long.parseLong(assessGroupIdStr);
        String basis = WebUtil.getSafeParam(p, "basis");
        if (basis == null || basis.isEmpty()) basis = "AMT";
        ResponseUtil.ok(resp, service.overviewByAssessGroup(pid, assessGroupId, basis));
    }
    
    private void doPayableEstimate(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long agreementId = WebUtil.getLong(p, "agreementId", 0);
        ResponseUtil.ok(resp, service.payableEstimate(agreementId));
    }

    private void doRecalcRebate(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String type = WebUtil.getSafeParam(p, "type");
        boolean isUpstream = !"downstream".equalsIgnoreCase(type);
        ResponseUtil.ok(resp, rebateCalcService.calcProjectRebate(pid, isUpstream));
    }

    /** 单个下游协议的返利估算（应付台账用） */
    private void doCalcDownstreamRebate(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long aid = WebUtil.getLong(p, "agreementId", 0);
        ResponseUtil.ok(resp, rebateCalcService.calcSingleDownstreamAgreementRebate(aid));
    }
    
    private void doExportExcel(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        Map<String, Object> data = service.overview(pid);
        Workbook wb = service.exportExcel(data);
        
        // 生成文件名：项目名称 + 项目年度 + 当前日期
        String fileName = "项目概览";
        Project pj = (Project) data.get("project");
        if (pj != null) {
            String projectName = pj.getProjectName() != null ? pj.getProjectName() : "";
            String coYear = pj.getCoYear() != null ? pj.getCoYear() : "";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String today = sdf.format(new Date());
            fileName = projectName + coYear + today;
            // 移除文件名中不允许的字符
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        }
        fileName += ".xlsx";
        
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }
    
    private void doExportBalance(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        String coYear = WebUtil.getSafeParam(p, "coYear");
        String projectId = WebUtil.getSafeParam(p, "projectId");
        String undertakingDept = WebUtil.getSafeParam(p, "undertakingDept");
        String projectGroupId = WebUtil.getSafeParam(p, "projectGroupId");
        
        Workbook wb = service.exportBalance(coYear, projectId, undertakingDept, projectGroupId);
        
        String fileName = "项目平衡表";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String today = sdf.format(new Date());
        if (coYear != null && !coYear.isEmpty()) {
            fileName += "_" + coYear;
        }
        fileName += "_" + today + ".xlsx";
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Expires", "0");
        
        try (java.io.OutputStream out = resp.getOutputStream()) {
            wb.write(out);
            out.flush();
        } finally {
            wb.close();
        }
    }
}
