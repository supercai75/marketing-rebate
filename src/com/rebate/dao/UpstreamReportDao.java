package com.rebate.dao;

import com.rebate.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 上游流向达成报表 DAO
 *
 * 主表: 按项目维度聚合各阶段( S1..S4 )的达成指标，复表头展示项目概况/整体/阶段一~四。
 * 明细表: 点击主表行后，按阶段+月份展示该项目上游流向的明细汇总。
 *
 * 阶段-月份区间统一来自视图 project_stage_view( 自定义配置优先，否则按 period_start_date 每三个月一阶段 )。
 */
public class UpstreamReportDao {

    /**
     * 主表查询: 返回每个项目一行，含项目概况、整体达成、四阶段达成。
     * 列别名( 英文 snake_case )与前端表头按顺序对应。
     *
     * 参数:
     *   coYear       - 合作年度( 必填 )
     *   projectGroup - 项目分组名称( 可空，空时不筛选 )
     *   deptName     - 负责部门( 可空，空时不筛选；非空时 LIKE %deptName% )
     */
    public List<Map<String, Object>> listMain(String coYear, String projectGroup, String deptName) {
        StringBuilder baseWhere = new StringBuilder(" where a.co_year = ?");
        List<Object> params = new ArrayList<>();
        params.add(coYear);
        if (projectGroup != null && !projectGroup.isEmpty()) {
            baseWhere.append(" and b.name = ?");
            params.add(projectGroup);
        }
        if (deptName != null && !deptName.isEmpty()) {
            baseWhere.append(" and a.undertaking_dept like ?");
            params.add("%" + deptName + "%");
        }

        String sql =
            "select " +
            "  base.id, base.project_name, base.brand, base.undertaking_dept, base.co_year, base.period_date, " +
            // 项目整体 (6列)
            "  tg.target_scale, " +
            "  coalesce(a1.acq1,0)+coalesce(a2.acq2,0)+coalesce(a3.acq3,0)+coalesce(a4.acq4,0) as total_qty, " +
            "  coalesce(a1.acm1,0)+coalesce(a2.acm2,0)+coalesce(a3.acm3,0)+coalesce(a4.acm4,0) as total_amt, " +
            "  round(case when tg.calc_basis='QTY' and tg.target_scale<>0 then (coalesce(a1.acq1,0)+coalesce(a2.acq2,0)+coalesce(a3.acq3,0)+coalesce(a4.acq4,0))*100/tg.target_scale " +
            "             when tg.calc_basis='AMT' and tg.target_scale<>0 then (coalesce(a1.acm1,0)+coalesce(a2.acm2,0)+coalesce(a3.acm3,0)+coalesce(a4.acm4,0))*100/tg.target_scale " +
            "             else null end, 2) as total_rate, " +
            "  coalesce(a1.antm1,0)+coalesce(a2.antm2,0)+coalesce(a3.antm3,0)+coalesce(a4.antm4,0) as total_scale, " +
            "  coalesce(a1.atm1,0)+coalesce(a2.atm2,0)+coalesce(a3.atm3,0)+coalesce(a4.atm4,0) as total_tax, " +
            "  r1.recv_amt, r2.recved_amt, coalesce(r1.recv_amt,0) - coalesce(r2.recved_amt,0) as torecv_amt, " + 
            // 阶段一 (7列): 指标/达成数量/达成核算金额/达成率/达成规模/含税金额/中标价金额
            "  tg.stage1_target, a1.acq1 as s1_qty, a1.acm1 as s1_amt, " +
            "  round(case when tg.calc_basis='QTY' and tg.stage1_target<>0 then coalesce(a1.acq1,0)*100/tg.stage1_target when tg.calc_basis='AMT' and tg.stage1_target<>0 then coalesce(a1.acm1,0)*100/tg.stage1_target else null end,2) as s1_rate, " +
            "  a1.antm1 as s1_scale, a1.atm1 as s1_tax, a1.abm1 as s1_bid, r1.recv_amt1, r2.recved_amt1, coalesce(r1.recv_amt1,0) - coalesce(r2.recved_amt1,0) as torecv_amt1, " +
            // 阶段二
            "  tg.stage2_target, a2.acq2 as s2_qty, a2.acm2 as s2_amt, " +
            "  round(case when tg.calc_basis='QTY' and tg.stage2_target<>0 then coalesce(a2.acq2,0)*100/tg.stage2_target when tg.calc_basis='AMT' and tg.stage2_target<>0 then coalesce(a2.acm2,0)*100/tg.stage2_target else null end,2) as s2_rate, " +
            "  a2.antm2 as s2_scale, a2.atm2 as s2_tax, a2.abm2 as s2_bid, r1.recv_amt2, r2.recved_amt2, coalesce(r1.recv_amt2,0) - coalesce(r2.recved_amt2,0) as torecv_amt2, " +
            // 阶段三
            "  tg.stage3_target, a3.acq3 as s3_qty, a3.acm3 as s3_amt, " +
            "  round(case when tg.calc_basis='QTY' and tg.stage3_target<>0 then coalesce(a3.acq3,0)*100/tg.stage3_target when tg.calc_basis='AMT' and tg.stage3_target<>0 then coalesce(a3.acm3,0)*100/tg.stage3_target else null end,2) as s3_rate, " +
            "  a3.antm3 as s3_scale, a3.atm3 as s3_tax, a3.abm3 as s3_bid, r1.recv_amt3, r2.recved_amt3, coalesce(r1.recv_amt3,0) - coalesce(r2.recved_amt3,0) as torecv_amt3, " +
            // 阶段四
            "  tg.stage4_target, a4.acq4 as s4_qty, a4.acm4 as s4_amt, " +
            "  round(case when tg.calc_basis='QTY' and tg.stage4_target<>0 then coalesce(a4.acq4,0)*100/tg.stage4_target when tg.calc_basis='AMT' and tg.stage4_target<>0 then coalesce(a4.acm4,0)*100/tg.stage4_target else null end,2) as s4_rate, " +
            "  a4.antm4 as s4_scale, a4.atm4 as s4_tax, a4.abm4 as s4_bid, r1.recv_amt4, r2.recved_amt4, coalesce(r1.recv_amt4,0) - coalesce(r2.recved_amt4,0) as torecv_amt4 " +
            "from (select a.id, a.project_name, a.brand, b.name as project_group, a.undertaking_dept, a.co_year, " +
            "        to_char(a.period_start_date,'yyyy-MM-dd') || '到' || to_char(a.period_end_date,'yyyy-MM-dd') as period_date " +
            "      from prj_project a left join prj_project_group b on a.project_group_id = b.id " +
            baseWhere + ") base " +
            "left join (select project_id, target_scale, stage1_target, stage2_target, stage3_target, stage4_target, calc_basis from prj_upstream_agreement where is_current = 1) tg on base.id = tg.project_id " +
            "left join (select a.project_id, sum(quantity) as acq1, sum(calc_amount) as acm1, sum(no_tax_amount) as antm1, sum(bid_amount) as abm1, sum(tax_Amount) as atm1 from flow_upstream_record a inner join project_stage_view b on a.project_id = b.project_id and a.month_yyyymm between b.startmonth and b.endmonth and b.stage_code = 'S1' and a.is_valid = 1 group by a.project_id) a1 on base.id = a1.project_id " +
            "left join (select a.project_id, sum(quantity) as acq2, sum(calc_amount) as acm2, sum(no_tax_amount) as antm2, sum(bid_amount) as abm2, sum(tax_Amount) as atm2 from flow_upstream_record a inner join project_stage_view b on a.project_id = b.project_id and a.month_yyyymm between b.startmonth and b.endmonth and b.stage_code = 'S2' and a.is_valid = 1 group by a.project_id) a2 on base.id = a2.project_id " +
            "left join (select a.project_id, sum(quantity) as acq3, sum(calc_amount) as acm3, sum(no_tax_amount) as antm3, sum(bid_amount) as abm3, sum(tax_Amount) as atm3 from flow_upstream_record a inner join project_stage_view b on a.project_id = b.project_id and a.month_yyyymm between b.startmonth and b.endmonth and b.stage_code = 'S3' and a.is_valid = 1 group by a.project_id) a3 on base.id = a3.project_id " +
            "left join (select a.project_id, sum(quantity) as acq4, sum(calc_amount) as acm4, sum(no_tax_amount) as antm4, sum(bid_amount) as abm4, sum(tax_Amount) as atm4 from flow_upstream_record a inner join project_stage_view b on a.project_id = b.project_id and a.month_yyyymm between b.startmonth and b.endmonth and b.stage_code = 'S4' and a.is_valid = 1 group by a.project_id) a4 on base.id = a4.project_id " +
            "left join (select project_id, sum(total_amount) as recv_amt, sum(case when stage = '阶段一' then total_amount else 0 end) as recv_amt1, sum(case when stage = '阶段二' then total_amount else 0 end) as recv_amt2, sum(case when stage = '阶段三' then total_amount else 0 end) as recv_amt3, sum(case when stage = '阶段四' then total_amount else 0 end) as recv_amt4 from prj_receivable group by project_id) r1 on base.id = r1.project_id " + 
            "left join (select project_id, sum(dept_share) as recved_amt, sum(case when stage = '阶段一' then dept_share else 0 end) as recved_amt1, sum(case when stage = '阶段二' then dept_share else 0 end) as recved_amt2, sum(case when stage = '阶段三' then dept_share else 0 end) as recved_amt3, sum(case when stage = '阶段四' then dept_share else 0 end) as recved_amt4 from prj_received group by project_id) r2 on base.id = r2.project_id " + 
            "order by base.co_year, base.project_name";

        return queryToMaps(sql, params.toArray());
    }

    /**
     * 明细查询: 点击主表行后，按阶段+月份展示该项目上游流向的明细汇总。
     * 列别名: stage(阶段) / month(月份) / scale(营销规模) / lerent_scale(乐仁堂体系内规模)
     *         / qty(数量) / calc_amt(核算金额) / sale_qty(销售数量) / bid_amt(中标价金额) / tax_amt(含税金额)
     */
    public List<Map<String, Object>> listDetail(long projectId) {
        String sql =
            "select replace(b.stage_code, 'S', '阶段') as stage, a.month_yyyymm as month, " +
            "       sum(no_tax_amount) as scale, " +
            "       sum(case when a.buyer_name like '%国药乐仁堂%' then no_tax_amount else 0 end) as lerent_scale, " +
            "       sum(quantity) as qty, sum(calc_amount) as calc_amt, " +
            "       sum(sale_qty) as sale_qty, sum(bid_amount) as bid_amt, sum(tax_Amount) as tax_amt " +
            "from flow_upstream_record a " +
            "inner join project_stage_view b on a.project_id = b.project_id and a.month_yyyymm between b.startmonth and b.endmonth and a.is_valid = 1 " +
            "where a.project_id = ? " +
            "group by replace(b.stage_code, 'S', '阶段'), a.month_yyyymm " +
            "order by replace(b.stage_code, 'S', '阶段'), a.month_yyyymm";
        return queryToMaps(sql, projectId);
    }

    /**
     * 随动表查询( 导出用 ): 按主表相同的筛选条件，返回所有匹配项目的阶段+月份明细。
     * 列别名沿用用户 SQL 中的中文别名( 合作年度/项目名称/阶段/月份/... )，可直接作为 Excel 表头。
     */
    public List<Map<String, Object>> listDetailMulti(String coYear, String projectGroup, String deptName) {
        StringBuilder where = new StringBuilder(" where c.co_year = ?");
        List<Object> params = new ArrayList<>();
        params.add(coYear);
        if (projectGroup != null && !projectGroup.isEmpty()) {
            where.append(" and d.name = ?");
            params.add(projectGroup);
        }
        if (deptName != null && !deptName.isEmpty()) {
            where.append(" and c.undertaking_dept like ?");
            params.add("%" + deptName + "%");
        }
        String sql =
            "select c.co_year as 合作年度, c.project_name as 项目名称, " +
            "       replace(b.stage_code, 'S', '阶段') as 阶段, a.month_yyyymm as 月份, " +
            "       sum(no_tax_amount) as 营销规模, " +
            "       sum(case when a.buyer_name like '%国药乐仁堂%' then no_tax_amount else 0 end) as 乐仁堂体系内规模, " +
            "       sum(quantity) as 数量, sum(calc_amount) as 核算金额, " +
            "       sum(sale_qty) as 销售数量, sum(bid_amount) as 中标价金额, sum(tax_Amount) as 含税金额 " +
            "from flow_upstream_record a " +
            "inner join project_stage_view b on a.project_id = b.project_id and a.month_yyyymm between b.startmonth and b.endmonth and a.is_valid = 1 " +
            "inner join prj_project c on a.project_id = c.id " +
            "left join prj_project_group d on c.project_group_id = d.id " +
            where +
            " group by c.co_year, c.project_name, replace(b.stage_code, 'S', '阶段'), a.month_yyyymm " +
            "order by c.project_name, replace(b.stage_code, 'S', '阶段'), a.month_yyyymm";
        return queryToMaps(sql, params.toArray());
    }

    // ---- 通用: 将 ResultSet 转为 List<Map> ( 按 column label 取键，保持插入顺序 ) ----
    private List<Map<String, Object>> queryToMaps(String sql, Object... params) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            c = DBUtil.getConnection();
            ps = c.prepareStatement(sql);
            BaseDao.bindParams(ps, params);
            rs = ps.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= n; i++) {
                    String key = md.getColumnLabel(i);
                    if (key == null || key.isEmpty()) key = "col" + i;
                    row.put(key, rs.getObject(i));
                }
                list.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("queryToMaps failed: " + sql, e);
        } finally {
            DBUtil.close(c, ps, rs);
        }
        return list;
    }
}
