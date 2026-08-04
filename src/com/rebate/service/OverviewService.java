package com.rebate.service;

import com.rebate.dao.*;
import com.rebate.model.*;
import com.rebate.util.ExcelUtil;
import org.apache.poi.ss.usermodel.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 项目概览 / 平衡表 服务
 */
public class OverviewService {

    private final ProjectDao projectDao = new ProjectDao();
    private final UpstreamAgreementDao upstreamDao = new UpstreamAgreementDao();
    private final DownstreamAgreementDao downstreamDao = new DownstreamAgreementDao();
    private final DownstreamFlowDao dsFlowDao = new DownstreamFlowDao();
    private final UpstreamFlowDao flowDao = new UpstreamFlowDao();
    private final CostDao costDao = new CostDao();
    private final ReceivablePayableDao rpDao = new ReceivablePayableDao();
    private final ReceivedPaidDao receivedPaidDao = new ReceivedPaidDao();
    private final AgreementSubDao subDao = new AgreementSubDao();

    /**
     * 项目概览
     */
    public Map<String, Object> overview(long projectId) {
        Project project = projectDao.findById(projectId);
        if (project == null) return Collections.emptyMap();
        UpstreamAgreement upstream = upstreamDao.findCurrentByProject(projectId);
        List<DownstreamAgreement> downs = downstreamDao.listByProject(projectId, true);
        
        // 加载下游协议的附件
        for (DownstreamAgreement down : downs) {
            down.setAttachFiles(subDao.listDownstreamAttaches(down.getId()));
            down.setRemarkFiles(subDao.listDownstreamRemarkFiles(down.getId()));
        }
        
        // 加载上游协议的返利规则和考核组
        RebateRuleDao rebateRuleDao = new RebateRuleDao();
        List<AssessGroup> assessGroups = rebateRuleDao.listAssessGroups(projectId);
        // 判断是否单考核组（用于流向汇总逻辑）
        boolean singleGroup = assessGroups.size() <= 1;
        if (upstream != null && upstream.getId() != null) {
            List<com.rebate.model.RebateRule> rules = rebateRuleDao.listByAgreement(upstream.getId());
            upstream.setRebateRules(rules);
            upstream.setAttachFiles(subDao.listUpstreamAttaches(upstream.getId()));
            upstream.setRemarkFiles(subDao.listUpstreamRemarkFiles(upstream.getId()));
        }

        // 获取上一年项目的数据（用于按销售增长计算）
        Map<String, Object> prevYearData = null;
        if (project.getCoYear() != null && !project.getCoYear().isEmpty()) {
            try {
                int prevYear = Integer.parseInt(project.getCoYear()) - 1;
                List<Project> prevProjects = projectDao.findByNameAndYear(project.getProjectName(), String.valueOf(prevYear));
                if (prevProjects != null && !prevProjects.isEmpty()) {
                    prevYearData = new LinkedHashMap<>();
                    Project prevProject = prevProjects.get(0);
                    
                    // 获取上一年的上游流向汇总
                    Map<String, BigDecimal> prevMonthScale = ProjectScaleService.loadMonthScale(prevProject.getId(),
                            upstream == null ? "AMT" : upstream.getCalcBasis());
                    Map<String, BigDecimal> prevScale = ProjectScaleService.computeScale(prevProject.getId(),
                            upstream == null ? "AMT" : upstream.getCalcBasis(),
                            prevMonthScale, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
                    
                    // 按阶段汇总
                    BigDecimal ps1 = BigDecimal.ZERO, ps2 = BigDecimal.ZERO, ps3 = BigDecimal.ZERO, ps4 = BigDecimal.ZERO;
                    for (Map.Entry<String, BigDecimal> e : prevMonthScale.entrySet()) {
                        String month = e.getKey();
                        if (month == null || month.length() < 6) continue;
                        int m = Integer.parseInt(month.substring(4, 6));
                        if (m >= 1 && m <= 3) ps1 = ps1.add(e.getValue());
                        else if (m >= 4 && m <= 6) ps2 = ps2.add(e.getValue());
                        else if (m >= 7 && m <= 9) ps3 = ps3.add(e.getValue());
                        else ps4 = ps4.add(e.getValue());
                    }
                    
                    prevYearData.put("project", prevProject);
                    prevYearData.put("stage1Actual", ps1);
                    prevYearData.put("stage2Actual", ps2);
                    prevYearData.put("stage3Actual", ps3);
                    prevYearData.put("stage4Actual", ps4);
                    prevYearData.put("totalActual", prevScale.get("totalActual"));
                    
                    // 获取上一年的考核组流向数据
                    // 单考核组时使用整体流向，多考核组时按考核组分别计算
                    // 注意：上下年考核组ID不同，需按名称匹配
                    RebateRuleDao prevRebateRuleDao = new RebateRuleDao();
                    List<AssessGroup> prevAssessGroups = prevRebateRuleDao.listAssessGroups(prevProject.getId());
                    Map<String, AssessGroup> prevGroupByName = new HashMap<>();
                    for (AssessGroup pag : prevAssessGroups) {
                        prevGroupByName.put(pag.getGroupName(), pag);
                    }
                    
                    List<Map<String, Object>> prevGroupFlows = new ArrayList<>();
                    for (AssessGroup ag : assessGroups) {
                        Map<String, Object> groupFlow = new HashMap<>();
                        groupFlow.put("groupId", ag.getId());
                        groupFlow.put("groupName", ag.getGroupName());
                        
                        // 查找名称相同的上年考核组
                        AssessGroup prevGroup = prevGroupByName.get(ag.getGroupName());
                        Long prevGroupId = null;
                        if (!singleGroup && prevGroup != null) {
                            prevGroupId = prevGroup.getId();
                        }
                        
                        // 单考核组使用整体流向（不按考核组过滤），多考核组按考核组过滤
                        Long groupIdForQuery = singleGroup ? null : prevGroupId;
                        Map<String, BigDecimal> stageFlows = getAllStageFlows(prevProject.getId(), groupIdForQuery);
                        groupFlow.put("stage1Actual", stageFlows.get("S1"));
                        groupFlow.put("stage2Actual", stageFlows.get("S2"));
                        groupFlow.put("stage3Actual", stageFlows.get("S3"));
                        groupFlow.put("stage4Actual", stageFlows.get("S4"));
                        prevGroupFlows.add(groupFlow);
                    }
                    prevYearData.put("groupFlows", prevGroupFlows);
                }
            } catch (Exception e) {
                // 忽略年份解析错误
            }
        }
        
        // 获取当前项目的考核组流向数据
        // 单考核组时使用整体流向，多考核组时按考核组分别计算
        List<Map<String, Object>> currentGroupFlows = new ArrayList<>();
        for (AssessGroup ag : assessGroups) {
            Map<String, Object> groupFlow = new HashMap<>();
            groupFlow.put("groupId", ag.getId());
            groupFlow.put("groupName", ag.getGroupName());
            groupFlow.put("stage1Target", ag.getStage1Target());
            groupFlow.put("stage2Target", ag.getStage2Target());
            groupFlow.put("stage3Target", ag.getStage3Target());
            groupFlow.put("stage4Target", ag.getStage4Target());
            // 单考核组使用整体流向（不按考核组过滤），多考核组按考核组过滤
            Long groupIdForQuery = singleGroup ? null : ag.getId();
            Map<String, BigDecimal> stageFlows = getAllStageFlows(projectId, groupIdForQuery);
            groupFlow.put("stage1Actual", stageFlows.get("S1"));
            groupFlow.put("stage2Actual", stageFlows.get("S2"));
            groupFlow.put("stage3Actual", stageFlows.get("S3"));
            groupFlow.put("stage4Actual", stageFlows.get("S4"));
            currentGroupFlows.add(groupFlow);
        }

        // 规模
        Map<String, BigDecimal> monthScale = ProjectScaleService.loadMonthScale(projectId,
                upstream == null ? "AMT" : upstream.getCalcBasis());
        Map<String, BigDecimal> scale = ProjectScaleService.computeScale(projectId,
                upstream == null ? "AMT" : upstream.getCalcBasis(),
                monthScale,
                upstream == null ? BigDecimal.ZERO : upstream.getStage1Target(),
                upstream == null ? BigDecimal.ZERO : upstream.getStage2Target(),
                upstream == null ? BigDecimal.ZERO : upstream.getStage3Target(),
                upstream == null ? BigDecimal.ZERO : upstream.getStage4Target());

        // 达成率
        BigDecimal totalTarget = upstream == null ? BigDecimal.ZERO : upstream.getTargetScale();
        BigDecimal totalRate = ProjectScaleService.rate(scale.get("totalActual"), totalTarget);

        BigDecimal s1a = scale.getOrDefault("stage1Actual", BigDecimal.ZERO);
        BigDecimal s2a = scale.getOrDefault("stage2Actual", BigDecimal.ZERO);
        BigDecimal s3a = scale.getOrDefault("stage3Actual", BigDecimal.ZERO);
        BigDecimal s4a = scale.getOrDefault("stage4Actual", BigDecimal.ZERO);
        BigDecimal s1t = upstream == null ? BigDecimal.ZERO : upstream.getStage1Target();
        BigDecimal s2t = upstream == null ? BigDecimal.ZERO : upstream.getStage2Target();
        BigDecimal s3t = upstream == null ? BigDecimal.ZERO : upstream.getStage3Target();
        BigDecimal s4t = upstream == null ? BigDecimal.ZERO : upstream.getStage4Target();

        // 应收/应付/实收/实付
        BigDecimal recvTotal = orZero(rpDao.sumReceivable(projectId));
        BigDecimal externalPayable = orZero(rpDao.sumPayable(projectId, "外部公司"));
        BigDecimal received = orZero(receivedPaidDao.sumReceived(projectId));
        BigDecimal externalPaid = orZero(receivedPaidDao.sumPaid(projectId, true));

        // 投入
        BigDecimal expense = orZero(costDao.sumExpenseByProject(projectId));
        BigDecimal labor = orZero(costDao.sumLaborByProject(projectId));
        BigDecimal invest = expense.add(labor);

        BigDecimal expectedRebate = project.getExpectedRebate() == null ? BigDecimal.ZERO : project.getExpectedRebate();
        BigDecimal receivableRate = expectedRebate.signum() == 0 ? BigDecimal.ZERO
                : recvTotal.subtract(invest).subtract(externalPayable)
                .multiply(BigDecimal.valueOf(100))
                .divide(expectedRebate, 2, RoundingMode.HALF_UP);
        BigDecimal actualIncomeRate = expectedRebate.signum() == 0 ? BigDecimal.ZERO
                : received.subtract(invest).subtract(externalPaid)
                .multiply(BigDecimal.valueOf(100))
                .divide(expectedRebate, 2, RoundingMode.HALF_UP);
        BigDecimal cashRate = recvTotal.signum() == 0 ? BigDecimal.ZERO
                : received.multiply(BigDecimal.valueOf(100)).divide(recvTotal, 2, RoundingMode.HALF_UP);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("project", project);
        r.put("upstream", upstream);
        r.put("downstreams", downs);
        r.put("monthScale", monthScale);
        r.put("totalActual", scale.get("totalActual"));
        r.put("totalTarget", totalTarget);
        r.put("totalRate", totalRate);
        r.put("stage1Actual", s1a);
        r.put("stage2Actual", s2a);
        r.put("stage3Actual", s3a);
        r.put("stage4Actual", s4a);
        r.put("stage1Target", s1t);
        r.put("stage2Target", s2t);
        r.put("stage3Target", s3t);
        r.put("stage4Target", s4t);
        r.put("stage1Rate", ProjectScaleService.rate(s1a, s1t));
        r.put("stage2Rate", ProjectScaleService.rate(s2a, s2t));
        r.put("stage3Rate", ProjectScaleService.rate(s3a, s3t));
        r.put("stage4Rate", ProjectScaleService.rate(s4a, s4t));
        r.put("receivableTotal", recvTotal);
        r.put("payableTotal", orZero(rpDao.sumPayable(projectId, null)));
        r.put("externalPayable", externalPayable);
        r.put("receivedTotal", received);
        r.put("paidTotal", orZero(receivedPaidDao.sumPaid(projectId, false)));
        r.put("externalPaid", externalPaid);
        r.put("expenseTotal", expense);
        r.put("laborTotal", labor);
        r.put("investTotal", invest);
        r.put("expectedRebate", expectedRebate);
        r.put("receivableRate", receivableRate);
        r.put("actualIncomeRate", actualIncomeRate);
        r.put("cashRate", cashRate);

        r.put("receivables", rpDao.listReceivableByProject(projectId));
        r.put("payables", rpDao.listPayableByProject(projectId, null, null, null));
        r.put("receivedList", receivedPaidDao.listReceivedByProject(projectId, null, null));
        r.put("paidList", receivedPaidDao.listPaidByProject(projectId, null, null));
        r.put("expenseList", costDao.listExpenses(projectId, null, null, null, null, null));
        r.put("laborList", costDao.listLabors(projectId, null));
        r.put("flowFinalMonths", flowDao.listFinalMonths(projectId));
        r.put("upstreamMonthSummary", flowDao.sumByMonth(projectId, upstream == null ? "AMT" : upstream.getCalcBasis()));
        r.put("expenseByMonth", costDao.sumExpenseByMonth(projectId));
        r.put("expenseByMonthAndType", costDao.sumExpenseByMonthAndType(projectId));
        r.put("laborByMonth", costDao.sumLaborByMonth(projectId));
        r.put("laborByMonthWithDetail", costDao.sumLaborByMonthWithDetail(projectId));
        
        // 添加下游流向按协议分组的数据
        List<Map<String, Object>> downstreamFlows = new ArrayList<>();
        for (DownstreamAgreement down : downs) {
            List<Map<String, Object>> flows = dsFlowDao.sumByAgreement(down.getId(), down.getCalcBasis() == null ? "AMT" : down.getCalcBasis());
            Map<String, Object> flowData = new HashMap<>();
            flowData.put("agreementId", down.getId());
            flowData.put("agreementName", down.getAgreementName());
            flowData.put("flows", flows);
            downstreamFlows.add(flowData);
        }
        r.put("downstreamFlows", downstreamFlows);
        
        // 添加考核组数据（包含流向汇总）
        r.put("assessGroups", assessGroups);
        r.put("currentGroupFlows", currentGroupFlows);
        r.put("prevYearData", prevYearData);
        
        return r;
    }
    
    /**
     * 获取指定项目所有阶段的流向数据（一次查询获取所有阶段）
     * @return Map: key="S1"/"S2"/"S3"/"S4", value=BigDecimal 金额
     */
    private Map<String, BigDecimal> getAllStageFlows(long projectId, Long assessGroupId) {
        // 获取上游协议以确定计算依据
        UpstreamAgreement upstream = upstreamDao.findCurrentByProject(projectId);
        String basis = upstream == null || upstream.getCalcBasis() == null ? "AMT" : upstream.getCalcBasis();
        return getAllStageFlows(projectId, assessGroupId, basis);
    }

    /**
     * 获取指定项目所有阶段的流向数据（一次查询获取所有阶段）
     * @param basis 计算依据 "AMT" 或 "QTY"
     * @return Map: key="S1"/"S2"/"S3"/"S4", value=BigDecimal 金额
     */
    private Map<String, BigDecimal> getAllStageFlows(long projectId, Long assessGroupId, String basis) {
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("S1", BigDecimal.ZERO);
        result.put("S2", BigDecimal.ZERO);
        result.put("S3", BigDecimal.ZERO);
        result.put("S4", BigDecimal.ZERO);
        
        Project project = projectDao.findById(projectId);
        if (project == null || project.getPeriodStartDate() == null) {
            return result;
        }
        
        // 根据计算依据选择列
        String sumCol = "AMT".equalsIgnoreCase(basis) ? "calc_amount" : "quantity";
        
        java.sql.Date startDate = project.getPeriodStartDate();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(startDate);
        int startYear = cal.get(java.util.Calendar.YEAR);
        int startMonth = cal.get(java.util.Calendar.MONTH) + 1;
        
        // 计算各阶段的 YYYYMM 范围
        int[][] stages = new int[4][2];
        for (int i = 0; i < 4; i++) {
            int stageStartMonth = startMonth + i * 3;
            int stageEndMonth = stageStartMonth + 2;
            int stageStartYear = startYear + (stageStartMonth - 1) / 12;
            int stageStartMonthOfYear = ((stageStartMonth - 1) % 12) + 1;
            int stageEndYear = startYear + (stageEndMonth - 1) / 12;
            int stageEndMonthOfYear = ((stageEndMonth - 1) % 12) + 1;
            stages[i][0] = stageStartYear * 100 + stageStartMonthOfYear;
            stages[i][1] = stageEndYear * 100 + stageEndMonthOfYear;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("  COALESCE(SUM(CASE WHEN r.month_yyyymm::int BETWEEN ? AND ? THEN r." + sumCol + " END), 0) as s1, ");
        sql.append("  COALESCE(SUM(CASE WHEN r.month_yyyymm::int BETWEEN ? AND ? THEN r." + sumCol + " END), 0) as s2, ");
        sql.append("  COALESCE(SUM(CASE WHEN r.month_yyyymm::int BETWEEN ? AND ? THEN r." + sumCol + " END), 0) as s3, ");
        sql.append("  COALESCE(SUM(CASE WHEN r.month_yyyymm::int BETWEEN ? AND ? THEN r." + sumCol + " END), 0) as s4 ");
        sql.append("FROM flow_upstream_record r ");
        sql.append("WHERE r.project_id = ? AND r.is_valid = 1 ");
        if (assessGroupId != null && assessGroupId > 0) {
            sql.append("AND r.assess_group_id = ?");
        }
        
        List<Object> params = new ArrayList<>();
        for (int[] stage : stages) {
            params.add(stage[0]);
            params.add(stage[1]);
        }
        params.add(projectId);
        if (assessGroupId != null && assessGroupId > 0) {
            params.add(assessGroupId);
        }
        
        BaseDao.queryOne(sql.toString(), (rs) -> {
            result.put("S1", rs.getBigDecimal(1));
            result.put("S2", rs.getBigDecimal(2));
            result.put("S3", rs.getBigDecimal(3));
            result.put("S4", rs.getBigDecimal(4));
            return null;
        }, params.toArray());
        
        return result;
    }

    /**
     * 下游协议概览（阶段达成情况）
     */
    public Map<String, Object> agreementOverview(long agreementId) {
        DownstreamAgreement agreement = downstreamDao.findById(agreementId);
        if (agreement == null) return Collections.emptyMap();
        
        // 获取下游协议的流向数据
        DownstreamFlowDao dsFlowDao = new DownstreamFlowDao();
        String basis = agreement.getCalcBasis() == null ? "AMT" : agreement.getCalcBasis();
        
        // 按月份聚合流向数据
        List<Map<String, Object>> monthData = dsFlowDao.sumByAgreement(agreementId, basis);
        
        // 计算总达成
        BigDecimal totalActual = BigDecimal.ZERO;
        Map<String, BigDecimal> monthScale = new LinkedHashMap<>();
        for (Map<String, Object> m : monthData) {
            BigDecimal scale = (BigDecimal) m.get("scale");
            if (scale != null) {
                totalActual = totalActual.add(scale);
                monthScale.put((String) m.get("month"), scale);
            }
        }
        
        // 阶段划分 (假设项目周期为12个月，按季度划分)
        BigDecimal s1a = BigDecimal.ZERO, s2a = BigDecimal.ZERO, s3a = BigDecimal.ZERO, s4a = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : monthScale.entrySet()) {
            String month = e.getKey();
            if (month == null || month.length() < 6) continue;
            int m = Integer.parseInt(month.substring(4, 6));
            if (m >= 1 && m <= 3) s1a = s1a.add(e.getValue());
            else if (m >= 4 && m <= 6) s2a = s2a.add(e.getValue());
            else if (m >= 7 && m <= 9) s3a = s3a.add(e.getValue());
            else s4a = s4a.add(e.getValue());
        }
        
        BigDecimal totalTarget = agreement.getTargetScale() == null ? BigDecimal.ZERO : agreement.getTargetScale();
        BigDecimal s1t = agreement.getStage1Target() == null ? BigDecimal.ZERO : agreement.getStage1Target();
        BigDecimal s2t = agreement.getStage2Target() == null ? BigDecimal.ZERO : agreement.getStage2Target();
        BigDecimal s3t = agreement.getStage3Target() == null ? BigDecimal.ZERO : agreement.getStage3Target();
        BigDecimal s4t = agreement.getStage4Target() == null ? BigDecimal.ZERO : agreement.getStage4Target();
        
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("agreement", agreement);
        r.put("totalActual", totalActual);
        r.put("totalTarget", totalTarget);
        r.put("totalRate", ProjectScaleService.rate(totalActual, totalTarget));
        r.put("stage1Actual", s1a);
        r.put("stage2Actual", s2a);
        r.put("stage3Actual", s3a);
        r.put("stage4Actual", s4a);
        r.put("stage1Target", s1t);
        r.put("stage2Target", s2t);
        r.put("stage3Target", s3t);
        r.put("stage4Target", s4t);
        r.put("stage1Rate", ProjectScaleService.rate(s1a, s1t));
        r.put("stage2Rate", ProjectScaleService.rate(s2a, s2t));
        r.put("stage3Rate", ProjectScaleService.rate(s3a, s3t));
        r.put("stage4Rate", ProjectScaleService.rate(s4a, s4t));
        return r;
    }

    /**
     * 应付估算数据（基于下游协议）
     */
    public Map<String, Object> payableEstimate(long agreementId) {
        DownstreamAgreement agreement = downstreamDao.findById(agreementId);
        if (agreement == null) return Collections.emptyMap();
        
        Project project = projectDao.findById(agreement.getProjectId());
        if (project == null) return Collections.emptyMap();
        
        // 获取下游流向数据
        DownstreamFlowDao dsFlowDao = new DownstreamFlowDao();
        String basis = agreement.getCalcBasis() == null ? "AMT" : agreement.getCalcBasis();
        List<Map<String, Object>> monthData = dsFlowDao.sumByAgreement(agreementId, basis);
        
        // 计算阶段达成额（相对于项目开始时间）
        Map<String, BigDecimal> stageActuals = new HashMap<>();
        stageActuals.put("S1", BigDecimal.ZERO);
        stageActuals.put("S2", BigDecimal.ZERO);
        stageActuals.put("S3", BigDecimal.ZERO);
        stageActuals.put("S4", BigDecimal.ZERO);
        
        if (project.getPeriodStartDate() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(project.getPeriodStartDate());
            int startYear = cal.get(java.util.Calendar.YEAR);
            int startMonth = cal.get(java.util.Calendar.MONTH) + 1;
            
            for (Map<String, Object> m : monthData) {
                BigDecimal scale = (BigDecimal) m.get("scale");
                if (scale == null) continue;
                String month = (String) m.get("month");
                if (month == null || month.length() < 6) continue;
                
                int yyyymm = Integer.parseInt(month);
                int y = yyyymm / 100;
                int mth = yyyymm % 100;
                
                int monthOffset = (y - startYear) * 12 + (mth - startMonth);
                int stageIndex = monthOffset / 3;
                if (stageIndex >= 0 && stageIndex < 4) {
                    String[] stages = {"S1", "S2", "S3", "S4"};
                    BigDecimal current = stageActuals.get(stages[stageIndex]);
                    stageActuals.put(stages[stageIndex], current.add(scale));
                }
            }
        }
        
        // 获取下游协议返利规则
        RebateRuleDao rebateRuleDao = new RebateRuleDao();
        List<RebateRule> rebateRules = rebateRuleDao.listDownstreamRebateRules(agreementId);
        
        // 获取下游协议的考核组目标数据
        AssessDownstreamTargetDao assessDownstreamTargetDao = new AssessDownstreamTargetDao();
        List<AssessDownstreamTarget> assessDownstreamTargets = assessDownstreamTargetDao.listByAgreement(agreementId);
        
        // 按考核组计算实际达成
        List<Map<String, Object>> assessGroups = new ArrayList<>();
        for (AssessDownstreamTarget target : assessDownstreamTargets) {
            Map<String, Object> group = new LinkedHashMap<>();
            Long agId = target.getAssessGroupId();
            group.put("id", agId == null ? 0 : agId);
            String gName = target.getGroupName();
            group.put("groupName", (gName == null || gName.isEmpty()) ? "默认" : gName);
            String gCode = target.getGroupCode();
            group.put("groupCode", (gCode == null || gCode.isEmpty()) ? "DEFAULT" : gCode);
            group.put("stage1Target", target.getStage1Target());
            group.put("stage2Target", target.getStage2Target());
            group.put("stage3Target", target.getStage3Target());
            group.put("stage4Target", target.getStage4Target());
            group.put("totalTarget", target.getTotalTarget());
            
            // 按考核组获取月度数据并计算阶段实际达成
            List<Map<String, Object>> groupMonthData = dsFlowDao.sumByMonth(project.getId(), basis, target.getAssessGroupId(), agreementId);
            Map<String, BigDecimal> groupStageActuals = new HashMap<>();
            groupStageActuals.put("S1", BigDecimal.ZERO);
            groupStageActuals.put("S2", BigDecimal.ZERO);
            groupStageActuals.put("S3", BigDecimal.ZERO);
            groupStageActuals.put("S4", BigDecimal.ZERO);
            
            if (project.getPeriodStartDate() != null) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(project.getPeriodStartDate());
                int startYear = cal.get(java.util.Calendar.YEAR);
                int startMonth = cal.get(java.util.Calendar.MONTH) + 1;
                
                for (Map<String, Object> m : groupMonthData) {
                    BigDecimal scale = (BigDecimal) m.get("scale");
                    if (scale == null) continue;
                    String month = (String) m.get("month");
                    if (month == null || month.length() < 6) continue;
                    
                    int yyyymm = Integer.parseInt(month);
                    int y = yyyymm / 100;
                    int mth = yyyymm % 100;
                    
                    int monthOffset = (y - startYear) * 12 + (mth - startMonth);
                    int stageIndex = monthOffset / 3;
                    if (stageIndex >= 0 && stageIndex < 4) {
                        String[] stages = {"S1", "S2", "S3", "S4"};
                        BigDecimal current = groupStageActuals.get(stages[stageIndex]);
                        groupStageActuals.put(stages[stageIndex], current.add(scale));
                    }
                }
            }
            group.put("stage1Actual", groupStageActuals.get("S1"));
            group.put("stage2Actual", groupStageActuals.get("S2"));
            group.put("stage3Actual", groupStageActuals.get("S3"));
            group.put("stage4Actual", groupStageActuals.get("S4"));
            assessGroups.add(group);
        }
        
        // 获取上一年项目数据（用于按销售增长计算）
        Map<String, Object> prevYearData = null;
        String projectName = project.getProjectName();
        String coYear = project.getCoYear();
        if (projectName != null && coYear != null && !coYear.isEmpty()) {
            try {
                int year = Integer.parseInt(coYear);
                if (year > 2000) {
                    List<Project> prevProjects = projectDao.findByNameAndYear(projectName, String.valueOf(year - 1));
                    if (prevProjects != null && !prevProjects.isEmpty()) {
                        Project prevProject = prevProjects.get(0);
                        List<DownstreamAgreement> agreements = downstreamDao.listByProject(prevProject.getId(), true);
                        if (!agreements.isEmpty()) {
                            DownstreamAgreement prevAgreement = agreements.get(0);
                            List<Map<String, Object>> prevMonthData = dsFlowDao.sumByAgreement(prevAgreement.getId(), basis);
                            Map<String, BigDecimal> prevStageActuals = new HashMap<>();
                            prevStageActuals.put("S1", BigDecimal.ZERO);
                            prevStageActuals.put("S2", BigDecimal.ZERO);
                            prevStageActuals.put("S3", BigDecimal.ZERO);
                            prevStageActuals.put("S4", BigDecimal.ZERO);
                            
                            if (prevProject.getPeriodStartDate() != null) {
                                java.util.Calendar cal = java.util.Calendar.getInstance();
                                cal.setTime(prevProject.getPeriodStartDate());
                                int startYear = cal.get(java.util.Calendar.YEAR);
                                int startMonth = cal.get(java.util.Calendar.MONTH) + 1;
                                
                                for (Map<String, Object> pm : prevMonthData) {
                                    BigDecimal scale = (BigDecimal) pm.get("scale");
                                    if (scale == null) continue;
                                    String month = (String) pm.get("month");
                                    if (month == null || month.length() < 6) continue;
                                    
                                    int yyyymm = Integer.parseInt(month);
                                    int y = yyyymm / 100;
                                    int mth = yyyymm % 100;
                                    
                                    int monthOffset = (y - startYear) * 12 + (mth - startMonth);
                                    int stageIndex = monthOffset / 3;
                                    if (stageIndex >= 0 && stageIndex < 4) {
                                        String[] stages = {"S1", "S2", "S3", "S4"};
                                        BigDecimal current = prevStageActuals.get(stages[stageIndex]);
                                        prevStageActuals.put(stages[stageIndex], current.add(scale));
                                    }
                                }
                            }
                            prevYearData = new HashMap<>();
                            prevYearData.put("S1", prevStageActuals.get("S1"));
                            prevYearData.put("S2", prevStageActuals.get("S2"));
                            prevYearData.put("S3", prevStageActuals.get("S3"));
                            prevYearData.put("S4", prevStageActuals.get("S4"));
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // ignore invalid year format
            }
        }
        
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("agreementId", agreementId);
        r.put("agreement", agreement);
        r.put("projectId", project.getId());
        r.put("stage1Actual", stageActuals.get("S1"));
        r.put("stage2Actual", stageActuals.get("S2"));
        r.put("stage3Actual", stageActuals.get("S3"));
        r.put("stage4Actual", stageActuals.get("S4"));
        r.put("stage1Target", agreement.getStage1Target());
        r.put("stage2Target", agreement.getStage2Target());
        r.put("stage3Target", agreement.getStage3Target());
        r.put("stage4Target", agreement.getStage4Target());
        r.put("rebateRules", rebateRules);
        r.put("prevYearData", prevYearData);
        r.put("assessGroups", assessGroups);
        return r;
    }

    /**
     * 平衡表：所有项目一行（优化版，使用批量查询）
     */
    public List<Map<String, Object>> balanceTable(String coYear) {
        List<Project> projects = (coYear == null || coYear.isEmpty()) ? projectDao.listAll() : projectDao.listByYear(coYear);
        if (projects.isEmpty()) return Collections.emptyList();
        
        // 提取项目ID列表
        List<Long> projectIds = projects.stream().map(Project::getId).collect(java.util.stream.Collectors.toList());
        
        // 批量查询应收总额（从应收台账表）
        Map<Long, Map<String, BigDecimal>> receivableMap = rpDao.sumReceivableBatch(projectIds);
        
        // 批量查询外部应付总额（从应付台账表，只统计外部公司）
        Map<Long, Map<String, BigDecimal>> payableMap = rpDao.sumPayableBatch(projectIds);
        
        // 批量查询实收总额（从实收表）
        Map<Long, Map<String, BigDecimal>> receivedMap = receivedPaidDao.sumReceivedBatch(projectIds);
        
        // 批量查询外部实付总额（从实付表，只统计外部公司）
        Map<Long, Map<String, BigDecimal>> paidMap = receivedPaidDao.sumPaidBatch(projectIds);
        
        // 批量查询投入汇总（费用+人工）
        Map<Long, Map<String, BigDecimal>> costMap = costDao.sumCostBatch(projectIds);
        
        // 批量查询上游协议目标（用于 totalTarget）
        Map<Long, BigDecimal> targetMap = new HashMap<>();
        List<UpstreamAgreement> upstreamList = upstreamDao.listByProjects(projectIds);
        for (UpstreamAgreement up : upstreamList) {
            targetMap.put(up.getProjectId(), up.getTargetScale());
        }
        
        // 批量查询流向汇总（用于 totalActual）
        Map<Long, BigDecimal> actualMap = new HashMap<>();
        for (Long pid : projectIds) {
            UpstreamAgreement up = upstreamList.stream().filter(u -> pid.equals(u.getProjectId())).findFirst().orElse(null);
            String basis = (up != null && up.getCalcBasis() != null) ? up.getCalcBasis() : "AMT";
            Map<String, BigDecimal> monthScale = ProjectScaleService.loadMonthScale(pid, basis, null);
            BigDecimal totalActual = monthScale.getOrDefault("totalActual", BigDecimal.ZERO);
            actualMap.put(pid, totalActual);
        }
        
        // 组装结果
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Project p : projects) {
            Long pid = p.getId();
            Map<String, BigDecimal> rec = receivableMap.getOrDefault(pid, Collections.emptyMap());
            Map<String, BigDecimal> pay = payableMap.getOrDefault(pid, Collections.emptyMap());
            Map<String, BigDecimal> recd = receivedMap.getOrDefault(pid, Collections.emptyMap());
            Map<String, BigDecimal> pd = paidMap.getOrDefault(pid, Collections.emptyMap());
            Map<String, BigDecimal> cost = costMap.getOrDefault(pid, Collections.emptyMap());
            
            BigDecimal receivable = rec.getOrDefault("receivableTotal", BigDecimal.ZERO);
            BigDecimal received = recd.getOrDefault("receivedTotal", BigDecimal.ZERO);
            BigDecimal invest = cost.getOrDefault("investTotal", BigDecimal.ZERO);
            BigDecimal externalPayable = pay.getOrDefault("externalPayable", BigDecimal.ZERO);
            BigDecimal externalPaid = pd.getOrDefault("externalPaid", BigDecimal.ZERO);
            BigDecimal totalTarget = targetMap.getOrDefault(pid, BigDecimal.ZERO);
            BigDecimal totalActual = actualMap.getOrDefault(pid, BigDecimal.ZERO);
            
            // 预计返利从项目表获取
            BigDecimal expectedRebate = p.getExpectedRebate() == null ? BigDecimal.ZERO : p.getExpectedRebate();
            
            // 应收收益 = 应收总额 - 投入总额 - 外部应付
            BigDecimal recvProfit = receivable.subtract(invest).subtract(externalPayable);
            // 实收收益 = 实收总额 - 投入总额 - 外部实付
            BigDecimal actualProfit = received.subtract(invest).subtract(externalPaid);
            
            // 应收达成率 = 应收收益 / 预计返利 * 100
            BigDecimal recvProfitRate = expectedRebate.signum() == 0 ? BigDecimal.ZERO
                    : recvProfit.multiply(BigDecimal.valueOf(100))
                    .divide(expectedRebate, 2, RoundingMode.HALF_UP);
            
            // 实收达成率 = 实收收益 / 预计返利 * 100
            BigDecimal profitRate = expectedRebate.signum() == 0 ? BigDecimal.ZERO
                    : actualProfit.multiply(BigDecimal.valueOf(100))
                    .divide(expectedRebate, 2, RoundingMode.HALF_UP);
            
            // 兑现率 = 实收总额 / 应收总额 * 100
            BigDecimal cashRate = receivable.signum() == 0 ? BigDecimal.ZERO
                    : received.multiply(BigDecimal.valueOf(100))
                    .divide(receivable, 2, RoundingMode.HALF_UP);
            
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("projectId", pid);
            row.put("projectName", p.getProjectName());
            row.put("coYear", p.getCoYear());
            row.put("totalTarget", totalTarget);
            row.put("totalActual", totalActual);
            row.put("receivableTotal", receivable);
            row.put("receivedTotal", received);
            row.put("investTotal", invest);
            row.put("externalPayable", externalPayable);
            row.put("externalPaid", externalPaid);
            row.put("recvProfit", recvProfit);
            row.put("actualProfit", actualProfit);
            row.put("expectedRebate", expectedRebate);
            row.put("recvProfitRate", recvProfitRate);
            row.put("profitRate", profitRate);
            row.put("cashRate", cashRate);
            rows.add(row);
        }
        return rows;
    }

    /**
     * 计算某项目按考核组的规模与达成
     * @param projectId 项目ID
     * @param assessGroupId 考核组ID (可以为null，返回全部考核组的)
     * @param basis 计算依据 ("AMT" 或 "QTY")
     * @return 考核组维度的计算结果
     */
    public Map<String, Object> overviewByAssessGroup(Long projectId, Long assessGroupId, String basis) {
        Project project = projectDao.findById(projectId);
        if (project == null) return Collections.emptyMap();
        UpstreamAgreement upstream = upstreamDao.findCurrentByProject(projectId);
        
        // 按考核组获取月度规模
        Map<String, BigDecimal> monthScale = ProjectScaleService.loadMonthScale(projectId, basis, assessGroupId);
        Map<String, BigDecimal> scale = ProjectScaleService.computeScale(projectId, basis, monthScale,
                upstream == null ? BigDecimal.ZERO : upstream.getStage1Target(),
                upstream == null ? BigDecimal.ZERO : upstream.getStage2Target(),
                upstream == null ? BigDecimal.ZERO : upstream.getStage3Target(),
                upstream == null ? BigDecimal.ZERO : upstream.getStage4Target());
        
        // 获取考核组信息
        AssessGroup group = null;
        if (assessGroupId != null) {
            group = new RebateRuleDao().getAssessGroup(assessGroupId);
        }
        
        // 计算达成率
        BigDecimal totalTarget = upstream == null ? BigDecimal.ZERO : upstream.getTargetScale();
        BigDecimal totalActual = scale.get("totalActual");
        BigDecimal totalRate = ProjectScaleService.rate(totalActual, totalTarget);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assessGroup", group);
        result.put("monthScale", monthScale);
        result.put("totalActual", totalActual);
        result.put("totalTarget", totalTarget);
        result.put("totalRate", totalRate);
        result.put("stage1Actual", scale.get("stage1Actual"));
        result.put("stage2Actual", scale.get("stage2Actual"));
        result.put("stage3Actual", scale.get("stage3Actual"));
        result.put("stage4Actual", scale.get("stage4Actual"));
        result.put("stage1Target", upstream == null ? null : upstream.getStage1Target());
        result.put("stage2Target", upstream == null ? null : upstream.getStage2Target());
        result.put("stage3Target", upstream == null ? null : upstream.getStage3Target());
        result.put("stage4Target", upstream == null ? null : upstream.getStage4Target());
        result.put("stage1Rate", ProjectScaleService.rate(scale.get("stage1Actual"), 
                upstream == null ? null : upstream.getStage1Target()));
        result.put("stage2Rate", ProjectScaleService.rate(scale.get("stage2Actual"), 
                upstream == null ? null : upstream.getStage2Target()));
        result.put("stage3Rate", ProjectScaleService.rate(scale.get("stage3Actual"), 
                upstream == null ? null : upstream.getStage3Target()));
        result.put("stage4Rate", ProjectScaleService.rate(scale.get("stage4Actual"), 
                upstream == null ? null : upstream.getStage4Target()));
        
        return result;
    }

    private BigDecimal orZero(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }

    private String mapStatus(String st) {
        if (st == null) return "草稿";
        switch (st) {
            case "DRAFT": return "草稿";
            case "AUDIT": return "待终稿";
            case "FINAL": return "已终稿";
            case "REJECTED": return "已驳回";
            default: return st;
        }
    }

    /**
     * 导出项目概览为多页签Excel
     */
    public Workbook exportExcel(Map<String, Object> data) {
        List<List<String>> projectInfo = new ArrayList<>();
        projectInfo.add(linkedRow("项目名称", "项目值"));
        Project p = (Project) data.get("project");
        if (p != null) {
            projectInfo.add(linkedRow("项目名称", str(p.getProjectName())));
            projectInfo.add(linkedRow("合作年度", str(p.getCoYear())));
            projectInfo.add(linkedRow("厂牌", str(p.getBrand())));
            projectInfo.add(linkedRow("覆盖地区", str(p.getRegion())));
            projectInfo.add(linkedRow("合作模式", str(p.getCoMode())));
        }
        projectInfo.add(linkedRow("指标规模", fmt(data.get("totalTarget"))));
        projectInfo.add(linkedRow("实际规模", fmt(data.get("totalActual"))));
        projectInfo.add(linkedRow("规模达成率", fmt(data.get("totalRate")) + "%"));
        projectInfo.add(linkedRow("应收达成率", fmt(data.get("receivableRate")) + "%"));
        projectInfo.add(linkedRow("实际收益", fmt(data.get("receivedTotal"))));
        projectInfo.add(linkedRow("实际收益达成率", fmt(data.get("profitRate")) + "%"));
        projectInfo.add(linkedRow("收益兑现率", fmt(data.get("cashRate")) + "%"));
        
        // 上游协议
        List<List<String>> upstreamAgree = new ArrayList<>();
        upstreamAgree.add(linkedRow("属性", "值"));
        UpstreamAgreement up = (UpstreamAgreement) data.get("upstream");
        if (up != null) {
            upstreamAgree.add(linkedRow("协议名称", str(up.getAgreementName())));
            upstreamAgree.add(linkedRow("协议编号", str(up.getAgreementNo())));
            upstreamAgree.add(linkedRow("上游供应商", str(up.getSupplier())));
            upstreamAgree.add(linkedRow("合作年度", str(up.getCoYear())));
            upstreamAgree.add(linkedRow("合作周期", str(up.getPeriodStartDate()) + " 至 " + str(up.getPeriodEndDate())));
            upstreamAgree.add(linkedRow("指标规模", fmt(up.getTargetScale())));
            upstreamAgree.add(linkedRow("阶段指标", "S1=" + fmt(up.getStage1Target()) + " S2=" + fmt(up.getStage2Target()) + " S3=" + fmt(up.getStage3Target()) + " S4=" + fmt(up.getStage4Target())));
            upstreamAgree.add(linkedRow("结算方式", str(up.getSettleBasis()) + " " + str(up.getSettleRatio())));
            upstreamAgree.add(linkedRow("专职团队考核", str(up.getTeamAssessSettle())));
        }
        
        // 下游协议
        List<List<String>> downstreamAgree = new ArrayList<>();
        downstreamAgree.add(linkedRow("协议名称", "协议编号", "分销商", "分销商类型", "合作年度", "合作周期"));
        List<DownstreamAgreement> downs = (List<DownstreamAgreement>) data.get("downstreams");
        if (downs != null) {
            for (DownstreamAgreement d : downs) {
                downstreamAgree.add(linkedRow(
                    d.getAgreementName(), d.getAgreementNo(), d.getDistributor(), d.getDistributorType(),
                    d.getCoYear(), str(d.getPeriodStartDate()) + " - " + str(d.getPeriodEndDate())
                ));
            }
        }
        
        List<List<String>> upstreamFlow = new ArrayList<>();
        upstreamFlow.add(linkedRow("月份", "数量", "核算金额", "记录数", "状态"));
        List<Map<String, Object>> upFlows = (List<Map<String, Object>>) data.get("upstreamMonthSummary");
        if (upFlows != null) {
            for (Map<String, Object> f : upFlows) {
                upstreamFlow.add(linkedRow(
                    str(f.get("month")),
                    fmt(f.get("qtyCount")),
                    fmt(f.get("scale")),
                    str(f.get("count")),
                    "Y".equals(f.get("isFinal")) ? "终稿" : "草稿"
                ));
            }
        }
        
        List<List<String>> downstreamFlow = new ArrayList<>();
        downstreamFlow.add(linkedRow("协议", "月份", "数量", "核算金额", "记录数", "状态"));
        List<Map<String, Object>> dsFlows = (List<Map<String, Object>>) data.get("downstreamFlows");
        if (dsFlows != null) {
            for (Map<String, Object> df : dsFlows) {
                List<Map<String, Object>> flows = (List<Map<String, Object>>) df.get("flows");
                if (flows != null) {
                    for (Map<String, Object> f : flows) {
                        downstreamFlow.add(linkedRow(
                            str(df.get("agreementName")),
                            str(f.get("month")),
                            fmt(f.get("qtyCount")),
                            fmt(f.get("scale")),
                            str(f.get("count")),
                            "Y".equals(f.get("isFinal")) ? "终稿" : "草稿"
                        ));
                    }
                }
            }
        }
        
        List<List<String>> receivables = new ArrayList<>();
        receivables.add(linkedRow("阶段", "依据规模应收", "依据考核应收", "合计应收", "系统估算", "状态"));
        List<Receivable> recs = (List<Receivable>) data.get("receivables");
        if (recs != null) {
            for (Receivable r : recs) {
                receivables.add(linkedRow(
                    r.getStage(), fmt(r.getScaleAmount()), fmt(r.getAssessAmount()),
                    fmt(r.getTotalAmount()), fmt(r.getEstimateAmount()), mapStatus(r.getStatus())
                ));
            }
        }
        
        List<List<String>> receivedList = new ArrayList<>();
        receivedList.add(linkedRow("阶段", "类型", "返利金额", "价税合计", "部门分摊", "发票号", "收款部门"));
        List<Received> recd = (List<Received>) data.get("receivedList");
        if (recd != null) {
            for (Received x : recd) {
                receivedList.add(linkedRow(
                    x.getStage(), x.getRebateType(), fmt(x.getRebateAmount()),
                    fmt(x.getTotalPriceTax()), fmt(x.getDeptShare()), x.getInvoiceNo(), x.getReceiveDept()
                ));
            }
        }
        
        List<List<String>> payables = new ArrayList<>();
        payables.add(linkedRow("阶段", "下游分销商", "分销商类型", "依据规模应付", "依据考核应付", "合计应付", "系统估算", "状态"));
        List<Payable> pays = (List<Payable>) data.get("payables");
        if (pays != null) {
            for (Payable p2 : pays) {
                payables.add(linkedRow(
                    p2.getStage(), p2.getDistributor(), p2.getDistributorType(),
                    fmt(p2.getScaleAmount()), fmt(p2.getAssessAmount()),
                    fmt(p2.getTotalAmount()), fmt(p2.getEstimateAmount()), mapStatus(p2.getStatus())
                ));
            }
        }
        
        List<List<String>> paidList = new ArrayList<>();
        paidList.add(linkedRow("阶段", "下游协议", "客户/分销商", "类型", "返利总额", "实际返利", "冲差价", "执行状态"));
        List<Paid> pd = (List<Paid>) data.get("paidList");
        if (pd != null) {
            for (Paid x : pd) {
                paidList.add(linkedRow(
                    x.getStage(), x.getAgreementName(), x.getCustomerName(), x.getRebateType(),
                    fmt(x.getTotalRebate()), fmt(x.getActualRebate()), fmt(x.getDiffAmount()), mapStatus(x.getExecuteStatus())
                ));
            }
        }
        
        List<List<String>> expenseList = new ArrayList<>();
        expenseList.add(linkedRow("月份", "费用类型", "发票金额", "本项目分摊金额"));
        List<Map<String, Object>> expenses = (List<Map<String, Object>>) data.get("expenseByMonthAndType");
        if (expenses != null) {
            for (Map<String, Object> m : expenses) {
                expenseList.add(linkedRow(
                    str(m.get("month")), str(m.get("expenseType")),
                    fmt(m.get("invoiceAmount")), fmt(m.get("amount"))
                ));
            }
        }
        
        List<List<String>> laborList = new ArrayList<>();
        laborList.add(linkedRow("月份", "费用类型", "费用合计", "本项目分摊金额"));
        List<Map<String, Object>> labors = (List<Map<String, Object>>) data.get("laborByMonthWithDetail");
        if (labors != null) {
            for (Map<String, Object> m : labors) {
                laborList.add(linkedRow(
                    str(m.get("month")), str(m.get("expenseType")),
                    fmt(m.get("invoiceAmount")), fmt(m.get("amount"))
                ));
            }
        }
        
        Map<String, List<List<String>>> sheets = new LinkedHashMap<>();
        sheets.put("项目信息与收益", projectInfo);
        sheets.put("上游协议", upstreamAgree);
        sheets.put("下游协议", downstreamAgree);
        sheets.put("上游流向", upstreamFlow);
        sheets.put("下游流向", downstreamFlow);
        sheets.put("应收台账", receivables);
        sheets.put("实收台账", receivedList);
        sheets.put("应付台账", payables);
        sheets.put("实付台账", paidList);
        sheets.put("费用投入", expenseList);
        sheets.put("人工投入", laborList);
        
        return ExcelUtil.exportMultiSheet(sheets);
    }
    
    private Map<String, String> toRow(String k, String v) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("k", k);
        m.put("v", v == null ? "" : v);
        return m;
    }
    
    private List<String> linkedRow(String... vals) {
        return Arrays.asList(vals);
    }
    
    private String str(Object o) { return o == null ? "" : String.valueOf(o); }
    
    private String fmt(Object o) {
        if (o == null) return "";
        if (o instanceof BigDecimal) {
            return ((BigDecimal) o).setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        return String.valueOf(o);
    }
    
    /**
     * 导出项目平衡表为Excel
     */
    public Workbook exportBalance(String coYear, String projectId) {
        List<Map<String, Object>> data;
        if (projectId != null && !projectId.isEmpty()) {
            // 导出单个项目
            long pid = Long.parseLong(projectId);
            Map<String, Object> overviewData = overview(pid);
            data = new ArrayList<>();
            Map<String, Object> row = new LinkedHashMap<>();
            Project p = (Project) overviewData.get("project");
            row.put("projectId", pid);
            row.put("projectName", p != null ? p.getProjectName() : "");
            row.put("coYear", p != null ? p.getCoYear() : "");
            row.put("totalTarget", overviewData.get("totalTarget"));
            row.put("totalActual", overviewData.get("totalActual"));
            row.put("receivableTotal", overviewData.get("receivableTotal"));
            row.put("receivedTotal", overviewData.get("receivedTotal"));
            row.put("investTotal", overviewData.get("investTotal"));
            row.put("externalPayable", overviewData.get("externalPayable"));
            row.put("externalPaid", overviewData.get("externalPaid"));
            
            BigDecimal receivableTotal = orZero((BigDecimal) overviewData.get("receivableTotal"));
            BigDecimal receivedTotal = orZero((BigDecimal) overviewData.get("receivedTotal"));
            BigDecimal investTotal = orZero((BigDecimal) overviewData.get("investTotal"));
            BigDecimal externalPayable = orZero((BigDecimal) overviewData.get("externalPayable"));
            BigDecimal externalPaid = orZero((BigDecimal) overviewData.get("externalPaid"));
            
            // 预计返利从项目表获取
            BigDecimal expectedRebate = p == null || p.getExpectedRebate() == null ? BigDecimal.ZERO : p.getExpectedRebate();
            
            // 应收收益 = 应收总额 - 投入总额 - 外部应付
            BigDecimal recvProfit = receivableTotal.subtract(investTotal).subtract(externalPayable);
            // 实收收益 = 实收总额 - 投入总额 - 外部实付
            BigDecimal actualProfit = receivedTotal.subtract(investTotal).subtract(externalPaid);
            
            row.put("recvProfit", recvProfit);
            row.put("actualProfit", actualProfit);
            row.put("expectedRebate", expectedRebate);
            
            // 应收达成率 = 应收收益 / 预计返利 * 100
            row.put("recvProfitRate", expectedRebate.signum() == 0 ? BigDecimal.ZERO
                    : recvProfit.multiply(BigDecimal.valueOf(100)).divide(expectedRebate, 2, RoundingMode.HALF_UP));
            // 实收达成率 = 实收收益 / 预计返利 * 100
            row.put("profitRate", expectedRebate.signum() == 0 ? BigDecimal.ZERO
                    : actualProfit.multiply(BigDecimal.valueOf(100)).divide(expectedRebate, 2, RoundingMode.HALF_UP));
            // 兑现率 = 实收总额 / 应收总额 * 100
            row.put("cashRate", receivableTotal.signum() == 0 ? BigDecimal.ZERO
                    : receivedTotal.multiply(BigDecimal.valueOf(100)).divide(receivableTotal, 2, RoundingMode.HALF_UP));
            data.add(row);
        } else {
            // 导出所有项目
            data = balanceTable(coYear);
        }
        
        List<List<String>> sheet = new ArrayList<>();
        sheet.add(linkedRow("项目名称", "合作年度", "指标规模", "实际规模", "应收总额", "实收总额", 
                "投入总额", "外部应付", "外部实付", "预计返利", "实际收益", 
                "预计达成率", "实际达成率", "兑现率"));
        
        for (Map<String, Object> row : data) {
            sheet.add(linkedRow(
                str(row.get("projectName")),
                str(row.get("coYear")),
                fmt(row.get("totalTarget")),
                fmt(row.get("totalActual")),
                fmt(row.get("receivableTotal")),
                fmt(row.get("receivedTotal")),
                fmt(row.get("investTotal")),
                fmt(row.get("externalPayable")),
                fmt(row.get("externalPaid")),
                fmt(row.get("recvProfit")),
                fmt(row.get("actualProfit")),
                fmt(row.get("recvProfitRate")) + "%",
                fmt(row.get("profitRate")) + "%",
                fmt(row.get("cashRate")) + "%"
            ));
        }
        
        Map<String, List<List<String>>> sheets = new LinkedHashMap<>();
        sheets.put("项目平衡表", sheet);
        return ExcelUtil.exportMultiSheet(sheets);
    }
}
