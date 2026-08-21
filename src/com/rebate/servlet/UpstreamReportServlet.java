package com.rebate.servlet;

import com.rebate.dao.UpstreamReportDao;
import com.rebate.model.UserContext;
import com.rebate.util.ExcelUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 上游流向达成报表
 *
 *  - list:   主表查询( 按年度/分组/部门筛选，返回项目维度达成汇总 )
 *  - detail: 明细查询( 按项目ID返回阶段+月份维度明细 )
 *  - export: 导出Excel( 2个页签: 主表 + 随动表 )
 */
public class UpstreamReportServlet extends BaseServlet {

    private final UpstreamReportDao dao = new UpstreamReportDao();

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
            case "detail": doDetail(req, resp, p); break;
            case "export": doExport(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }

    private boolean checkPerm(UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "list":
            case "detail":
            case "export":
                return u.hasPerm("upstream_report:view");
            default:
                return false;
        }
    }

    private void doList(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        String coYear = WebUtil.getSafeParam(p, "coYear");
        String projectGroup = WebUtil.getSafeParam(p, "projectGroup");
        String deptName = WebUtil.getSafeParam(p, "deptName");
        if (coYear == null || coYear.isEmpty()) {
            ResponseUtil.fail(resp, "合作年度不能为空");
            return;
        }
        ResponseUtil.ok(resp, dao.listMain(coYear, projectGroup, deptName));
    }

    private void doDetail(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long projectId = WebUtil.getLong(p, "projectId", 0);
        if (projectId <= 0) {
            ResponseUtil.fail(resp, "项目ID不能为空");
            return;
        }
        ResponseUtil.ok(resp, dao.listDetail(projectId));
    }

    /**
     * 导出Excel: 2个页签
     *   - "上游达成主表": 主表查询结果( 中文表头按 spec 顺序，39列 )
     *   - "阶段月份明细": 随动表查询结果( 按主表相同筛选，列名取自 SQL 中文别名 )
     */
    private void doExport(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        String coYear = WebUtil.getSafeParam(p, "coYear");
        String projectGroup = WebUtil.getSafeParam(p, "projectGroup");
        String deptName = WebUtil.getSafeParam(p, "deptName");
        if (coYear == null || coYear.isEmpty()) {
            ResponseUtil.fail(resp, "合作年度不能为空");
            return;
        }

        // ---- 页签1: 主表 ----
        // 中文表头与英文别名的对应( 顺序与前端表头一致 )
        String[][] mainCols = {
            // 项目概况
            {"项目名称", "project_name"}, {"合作厂牌", "brand"}, {"负责部门", "undertaking_dept"}, {"合作年度", "co_year"}, {"协议周期", "period_date"},
            // 项目整体
            {"协议指标", "target_scale"}, {"达成数量", "total_qty"}, {"达成核算金额", "total_amt"}, {"达成率", "total_rate"}, {"达成规模", "total_scale"}, {"达成含税金额", "total_tax"},
            // 阶段一
            {"阶段一指标", "stage1_target"}, {"阶段一达成数量", "s1_qty"}, {"阶段一达成核算金额", "s1_amt"}, {"阶段一达成率", "s1_rate"}, {"阶段一达成规模", "s1_scale"}, {"阶段一含税金额", "s1_tax"}, {"阶段一中标价金额", "s1_bid"},
            // 阶段二
            {"阶段二指标", "stage2_target"}, {"阶段二达成数量", "s2_qty"}, {"阶段二达成核算金额", "s2_amt"}, {"阶段二达成率", "s2_rate"}, {"阶段二达成规模", "s2_scale"}, {"阶段二含税金额", "s2_tax"}, {"阶段二中标价金额", "s2_bid"},
            // 阶段三
            {"阶段三指标", "stage3_target"}, {"阶段三达成数量", "s3_qty"}, {"阶段三达成核算金额", "s3_amt"}, {"阶段三达成率", "s3_rate"}, {"阶段三达成规模", "s3_scale"}, {"阶段三含税金额", "s3_tax"}, {"阶段三中标价金额", "s3_bid"},
            // 阶段四
            {"阶段四指标", "stage4_target"}, {"阶段四达成数量", "s4_qty"}, {"阶段四达成核算金额", "s4_amt"}, {"阶段四达成率", "s4_rate"}, {"阶段四达成规模", "s4_scale"}, {"阶段四含税金额", "s4_tax"}, {"阶段四中标价金额", "s4_bid"}
        };
        List<List<String>> mainRows = new ArrayList<>();
        // 表头行
        List<String> mainHeader = new ArrayList<>();
        for (String[] c : mainCols) mainHeader.add(c[0]);
        mainRows.add(mainHeader);
        // 数据行
        List<Map<String, Object>> mainData = dao.listMain(coYear, projectGroup, deptName);
        for (Map<String, Object> r : mainData) {
            List<String> row = new ArrayList<>();
            for (String[] c : mainCols) row.add(fmt(r.get(c[1])));
            mainRows.add(row);
        }

        // ---- 页签2: 随动表( 列名直接取自 SQL 中文别名 ) ----
        List<List<String>> detailRows = new ArrayList<>();
        List<Map<String, Object>> detailData = dao.listDetailMulti(coYear, projectGroup, deptName);
        if (!detailData.isEmpty()) {
            // 表头: 第一行的所有 key( 保持 SQL select 顺序 )
            List<String> detailHeader = new ArrayList<>(detailData.get(0).keySet());
            detailRows.add(detailHeader);
            for (Map<String, Object> r : detailData) {
                List<String> row = new ArrayList<>();
                for (String k : detailHeader) row.add(fmt(r.get(k)));
                detailRows.add(row);
            }
        }

        // ---- 写出多页签 Excel ----
        Map<String, List<List<String>>> sheets = new LinkedHashMap<>();
        sheets.put("上游达成主表", mainRows);
        sheets.put("阶段月份明细", detailRows);
        Workbook wb = ExcelUtil.exportMultiSheet(sheets);

        String fileName = "上游达成报表";
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        fileName += "_" + coYear + "_" + today + ".xlsx";
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

    /** 数值格式化: BigDecimal 保留2位，null 返回空串 */
    private static String fmt(Object o) {
        if (o == null) return "";
        if (o instanceof BigDecimal) {
            return ((BigDecimal) o).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        return String.valueOf(o);
    }
}
