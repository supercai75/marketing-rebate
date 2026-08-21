package com.rebate.service;

import com.rebate.dao.DownstreamAgreementDao;
import com.rebate.model.AssessGroup;
import com.rebate.model.DownstreamAgreement;
import com.rebate.model.Project;
import com.rebate.model.RebateRule;
import com.rebate.model.UpstreamAgreement;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 返利计算服务：替代前端 rebate-calc.js 的估算逻辑。
 * 复用 OverviewService 加载协议规则、考核组、流向数据，调用 RebateCalcUtil 完成金额计算。
 *
 * 双口径（需求4）：
 *   - calcBasis     指标计算依据：用于判定达成率（匹配区间阈值、计算X变量、计算达成率百分比）
 *   - rebateCalcBasis 返利计算依据：用于计算返利基数 baseAmount 和 Y 变量（完成核算数量/金额）
 *
 * 示例（calcBasis=AMT, rebateCalcBasis=BID_AMT）：
 *   目标 100 万，达成后返利 3%。实际核算金额 105 万（calcBasis），实际中标价金额 88 万（rebateCalcBasis）。
 *   判定：105 万 >= 100 万 → 达成。返利 = 88 万 × 3% = 2.64 万。
 */
public class RebateCalcService {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final String[] STAGE_CODES = {"S1", "S2", "S3", "S4"};
    private static final String[] STAGE_NAMES = {"阶段一", "阶段二", "阶段三", "阶段四"};

    private final DownstreamAgreementDao downstreamDao = new DownstreamAgreementDao();
    private final OverviewService overviewService = new OverviewService();

    public Map<String, Object> calcProjectRebate(Long projectId, boolean isUpstream) {
        return isUpstream ? calcUpstreamRebate(projectId) : calcDownstreamRebate(projectId);
    }

    /** 单个下游协议的返利估算（应付台账用） */
    @SuppressWarnings("unchecked")
    public Map<String, Object> calcSingleDownstreamAgreementRebate(Long agreementId) {
        if (agreementId == null || agreementId <= 0) return emptyResult();
        DownstreamAgreement down = downstreamDao.findById(agreementId);
        if (down == null) return emptyResult();

        Map<String, Object> est = overviewService.payableEstimate(agreementId);
        List<RebateRule> rules = (List<RebateRule>) est.get("rebateRules");
        if (rules == null) rules = Collections.emptyList();

        BigDecimal[] stageActuals = new BigDecimal[]{
                bd(est.get("stage1Actual")), bd(est.get("stage2Actual")),
                bd(est.get("stage3Actual")), bd(est.get("stage4Actual"))
        };
        BigDecimal[] stageTargets = new BigDecimal[]{
                bd(est.get("stage1Target")), bd(est.get("stage2Target")),
                bd(est.get("stage3Target")), bd(est.get("stage4Target"))
        };
        DownstreamAgreement agreement = (DownstreamAgreement) est.get("agreement");
        BigDecimal totalTarget = agreement == null ? BigDecimal.ZERO : nz(agreement.getTargetScale());
        Map<String, Object> prevYearData = (Map<String, Object>) est.get("prevYearData");
        List<Map<String, Object>> assessGroups = (List<Map<String, Object>>) est.get("assessGroups");

        String calcBasis = agreement == null ? "AMT" : agreement.getCalcBasis();
        String rebateBasis = normalizeBasis(agreement == null ? null : agreement.getRebateCalcBasis(), calcBasis);
        Project project = (Project) est.get("project");
        if (project == null) project = overviewService.findProjectByAgreement(down);
        BigDecimal[] stageRebateActuals = loadDownstreamStageRebateActuals(
                project, down, rebateBasis, calcBasis, stageActuals);

        List<GroupStageData> groupDataList = buildDownstreamGroupData(
                assessGroups, prevYearData,
                stageActuals, stageRebateActuals, stageTargets, totalTarget,
                project, down, calcBasis, rebateBasis);

        BigDecimal[] fullYearHolder = new BigDecimal[]{BigDecimal.ZERO};
        List<List<Map<String, Object>>> stageDetails = new ArrayList<>();
        BigDecimal[] stageRebates = computeStageRebateAmounts(
                rules, groupDataList, stageActuals, stageRebateActuals,
                stageTargets, totalTarget, prevYearData, fullYearHolder, agreement == null ? "PROGRESSIVE" : agreement.getCalcMode(), stageDetails, isQtyBasis(rebateBasis));

        List<List<RebateRule>> projStageRules = new ArrayList<>();
        for (int i = 0; i < 4; i++) projStageRules.add(filterStageRules(rules, STAGE_CODES[i]));
        return buildResult(stageActuals, stageTargets, stageRebates, projStageRules, fullYearHolder[0], stageDetails);
    }

    // ================================================================
    // 上游协议返利（应收估算）
    // ================================================================
    @SuppressWarnings("unchecked")
    private Map<String, Object> calcUpstreamRebate(Long projectId) {
        Map<String, Object> overview = overviewService.overview(projectId);
        UpstreamAgreement up = (UpstreamAgreement) overview.get("upstream");
        if (up == null) return emptyResult();

        List<RebateRule> allRules = up.getRebateRules();
        if (allRules == null) allRules = new ArrayList<>();

        List<AssessGroup> assessGroups = (List<AssessGroup>) overview.get("assessGroups");
        List<Map<String, Object>> currentGroupFlows = (List<Map<String, Object>>) overview.get("currentGroupFlows");
        Map<String, Object> prevYearData = (Map<String, Object>) overview.get("prevYearData");

        // calcBasis 口径 actual（指标计算依据）
        BigDecimal[] stageActuals = new BigDecimal[]{
                bd(overview.get("stage1Actual")), bd(overview.get("stage2Actual")),
                bd(overview.get("stage3Actual")), bd(overview.get("stage4Actual"))
        };
        BigDecimal[] stageTargets = new BigDecimal[]{
                bd(overview.get("stage1Target")), bd(overview.get("stage2Target")),
                bd(overview.get("stage3Target")), bd(overview.get("stage4Target"))
        };
        BigDecimal totalTarget = bd(overview.get("totalTarget"));

        // rebateCalcBasis 口径 actual（返利计算依据）
        String calcBasis = up.getCalcBasis();
        String rebateBasis = normalizeBasis(up.getRebateCalcBasis(), calcBasis);
        BigDecimal[] stageRebateActuals = loadUpstreamStageRebateActuals(
                projectId, rebateBasis, calcBasis, stageActuals,
                up.getStage1Target(), up.getStage2Target(), up.getStage3Target(), up.getStage4Target());

        List<GroupStageData> groupDataList = buildUpstreamGroupData(
                assessGroups, currentGroupFlows, prevYearData,
                stageActuals, stageRebateActuals, stageTargets, totalTarget,
                projectId, calcBasis, rebateBasis, up);

        BigDecimal[] fullYearHolder = new BigDecimal[]{BigDecimal.ZERO};
        List<List<Map<String, Object>>> stageDetails = new ArrayList<>();
        BigDecimal[] stageRebateAmounts = computeStageRebateAmounts(
                allRules, groupDataList, stageActuals, stageRebateActuals,
                stageTargets, totalTarget, prevYearData, fullYearHolder, up.getCalcMode(), stageDetails, isQtyBasis(rebateBasis));

        List<List<RebateRule>> projStageRules = new ArrayList<>();
        for (int i = 0; i < 4; i++) projStageRules.add(filterStageRules(allRules, STAGE_CODES[i]));
        return buildResult(stageActuals, stageTargets, stageRebateAmounts, projStageRules, fullYearHolder[0], stageDetails);
    }

    // ================================================================
    // 下游协议返利（应付估算）
    // ================================================================
    @SuppressWarnings("unchecked")
    private Map<String, Object> calcDownstreamRebate(Long projectId) {
        List<DownstreamAgreement> downs = downstreamDao.listByProject(projectId, true);
        if (downs == null || downs.isEmpty()) return emptyResult();

        BigDecimal[] projStageActuals = zeros();
        BigDecimal[] projStageTargets = zeros();
        BigDecimal[] projStageRebates = zeros();
        List<List<RebateRule>> projStageRules = new ArrayList<>();
        for (int i = 0; i < 4; i++) projStageRules.add(new ArrayList<>());
        BigDecimal projFullYearRebate = BigDecimal.ZERO;
        List<List<Map<String, Object>>> projStageDetails = new ArrayList<>();
        for (int i = 0; i < 4; i++) projStageDetails.add(new ArrayList<>());

        for (DownstreamAgreement down : downs) {
            Map<String, Object> est = overviewService.payableEstimate(down.getId());
            List<RebateRule> rules = (List<RebateRule>) est.get("rebateRules");
            if (rules == null) rules = Collections.emptyList();

            BigDecimal[] stageActuals = new BigDecimal[]{
                    bd(est.get("stage1Actual")), bd(est.get("stage2Actual")),
                    bd(est.get("stage3Actual")), bd(est.get("stage4Actual"))
            };
            BigDecimal[] stageTargets = new BigDecimal[]{
                    bd(est.get("stage1Target")), bd(est.get("stage2Target")),
                    bd(est.get("stage3Target")), bd(est.get("stage4Target"))
            };
            DownstreamAgreement agreement = (DownstreamAgreement) est.get("agreement");
            BigDecimal totalTarget = agreement == null ? BigDecimal.ZERO : nz(agreement.getTargetScale());
            Map<String, Object> prevYearData = (Map<String, Object>) est.get("prevYearData");
            List<Map<String, Object>> assessGroups = (List<Map<String, Object>>) est.get("assessGroups");

            String calcBasis = agreement == null ? "AMT" : agreement.getCalcBasis();
            String rebateBasis = normalizeBasis(agreement == null ? null : agreement.getRebateCalcBasis(), calcBasis);
            Project project = (Project) est.get("project");
            if (project == null) {
                project = overviewService.findProjectByAgreement(down);
            }
            BigDecimal[] stageRebateActuals = loadDownstreamStageRebateActuals(
                    project, down, rebateBasis, calcBasis, stageActuals);

            List<GroupStageData> groupDataList = buildDownstreamGroupData(
                    assessGroups, prevYearData,
                    stageActuals, stageRebateActuals, stageTargets, totalTarget,
                    project, down, calcBasis, rebateBasis);

            BigDecimal[] fullYearHolder = new BigDecimal[]{BigDecimal.ZERO};
            List<List<Map<String, Object>>> stageDetails = new ArrayList<>();
            BigDecimal[] stageRebates = computeStageRebateAmounts(
                    rules, groupDataList, stageActuals, stageRebateActuals,
                    stageTargets, totalTarget, prevYearData, fullYearHolder, agreement == null ? "PROGRESSIVE" : agreement.getCalcMode(), stageDetails, isQtyBasis(rebateBasis));

            for (int i = 0; i < 4; i++) {
                projStageActuals[i] = projStageActuals[i].add(stageActuals[i]);
                projStageTargets[i] = projStageTargets[i].add(stageTargets[i]);
                projStageRebates[i] = projStageRebates[i].add(stageRebates[i]);
                projStageRules.get(i).addAll(filterStageRules(rules, STAGE_CODES[i]));
                if (i < stageDetails.size()) projStageDetails.get(i).addAll(stageDetails.get(i));
            }
            projFullYearRebate = projFullYearRebate.add(fullYearHolder[0]);
        }

        return buildResult(projStageActuals, projStageTargets, projStageRebates, projStageRules, projFullYearRebate, projStageDetails);
    }

    // ================================================================
    // 双口径口径标准化 & 专用加载
    // ================================================================
    private static String normalizeBasis(String basis, String fallback) {
        if (basis == null || basis.trim().isEmpty()) {
            return fallback == null ? "AMT" : fallback;
        }
        return basis;
    }

    private BigDecimal[] loadUpstreamStageRebateActuals(Long projectId, String rebateBasis, String calcBasis,
                                                        BigDecimal[] calcStageActuals,
                                                        BigDecimal s1, BigDecimal s2, BigDecimal s3, BigDecimal s4) {
        if (sameBasis(rebateBasis, calcBasis)) return calcStageActuals;
        Map<String, BigDecimal> rebateMonth = ProjectScaleService.loadMonthScale(projectId, rebateBasis);
        Map<String, BigDecimal> rebateScale = ProjectScaleService.computeScale(
                projectId, rebateBasis, rebateMonth, nz(s1), nz(s2), nz(s3), nz(s4));
        return new BigDecimal[]{
                nz(rebateScale.get("stage1Actual")), nz(rebateScale.get("stage2Actual")),
                nz(rebateScale.get("stage3Actual")), nz(rebateScale.get("stage4Actual"))
        };
    }

    @SuppressWarnings("unchecked")
    private BigDecimal[] loadDownstreamStageRebateActuals(Project project, DownstreamAgreement down,
                                                          String rebateBasis, String calcBasis,
                                                          BigDecimal[] calcStageActuals) {
        if (sameBasis(rebateBasis, calcBasis) || project == null || down == null) return calcStageActuals;
        try {
            com.rebate.dao.DownstreamFlowDao dsFlowDao = new com.rebate.dao.DownstreamFlowDao();
            List<Map<String, Object>> monthData = dsFlowDao.sumByAgreement(down.getId(), rebateBasis);
            Map<String, BigDecimal> monthScale = new HashMap<>();
            for (Map<String, Object> m : monthData) {
                BigDecimal scale = (BigDecimal) m.get("scale");
                if (scale == null) continue;
                String month = (String) m.get("month");
                if (month == null || month.length() < 6) continue;
                monthScale.put(month, scale);
            }
            // 阶段-月份区间: 按项目阶段配置(整12个月走默认, 非12个月读 prj_stage_month_config)
            Map<String, int[]> ranges = StageMonthService.getStageRanges(project.getId());
            return new BigDecimal[]{
                StageMonthService.sumByRange(monthScale, ranges.get("S1")),
                StageMonthService.sumByRange(monthScale, ranges.get("S2")),
                StageMonthService.sumByRange(monthScale, ranges.get("S3")),
                StageMonthService.sumByRange(monthScale, ranges.get("S4"))
            };
        } catch (Exception ex) {
            return calcStageActuals;
        }
    }

    public static boolean sameBasis(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        String aa = a.equalsIgnoreCase("AMT") ? "CALC_AMT" : a.toUpperCase();
        String bb = b.equalsIgnoreCase("AMT") ? "CALC_AMT" : b.toUpperCase();
        return aa.equalsIgnoreCase(bb);
    }

    // ================================================================
    // 按阶段汇总返利计算
    // ================================================================
    private BigDecimal[] computeStageRebateAmounts(List<RebateRule> allRules, List<GroupStageData> groupDataList,
                                                    BigDecimal[] stageActuals, BigDecimal[] stageRebateActuals,
                                                    BigDecimal[] stageTargets,
                                                    BigDecimal totalTarget, Map<String, Object> prevYearData,
                                                    BigDecimal[] outFullYear, String calcMode, boolean qtyBased) {
        return computeStageRebateAmounts(allRules, groupDataList, stageActuals, stageRebateActuals,
                stageTargets, totalTarget, prevYearData, outFullYear, calcMode, null, qtyBased);
    }

    private BigDecimal[] computeStageRebateAmounts(List<RebateRule> allRules, List<GroupStageData> groupDataList,
                                                    BigDecimal[] stageActuals, BigDecimal[] stageRebateActuals,
                                                    BigDecimal[] stageTargets,
                                                    BigDecimal totalTarget, Map<String, Object> prevYearData,
                                                    BigDecimal[] outFullYear, String calcMode,
                                                    List<List<Map<String, Object>>> outStageDetails, boolean qtyBased) {
        BigDecimal[] stageRebateAmounts = zeros();
        for (int i = 0; i < 4; i++) {
            List<RebateRule> stageRules = filterStageRules(allRules, STAGE_CODES[i]);
            BigDecimal prevActual = extractPrevStage(prevYearData, i);
            List<Map<String, Object>> stageDetails = outStageDetails != null ? new ArrayList<>() : null;
            stageRebateAmounts[i] = calcRebateForGroups(
                    stageRules, groupDataList,
                    stageActuals[i], stageRebateActuals[i],
                    stageTargets[i], prevActual, i, calcMode, stageDetails, qtyBased);
            if (outStageDetails != null) outStageDetails.add(stageDetails);
        }
        if (outFullYear != null) {
            List<RebateRule> fullYearRules = filterFullYearRules(allRules);
            if (!fullYearRules.isEmpty()) {
                BigDecimal totalActual = sum(stageActuals);
                BigDecimal totalRebateActual = sum(stageRebateActuals);
                BigDecimal prevTotalActual = extractPrevTotal(prevYearData);
                outFullYear[0] = calcRebateForGroups(
                        fullYearRules, groupDataList,
                        totalActual, totalRebateActual, totalTarget, prevTotalActual, -1, calcMode, qtyBased);
            }
        }
        return stageRebateAmounts;
    }

    // ================================================================
    // 构建 GroupStageData（上游）
    // ================================================================
    @SuppressWarnings("unchecked")
    private List<GroupStageData> buildUpstreamGroupData(List<AssessGroup> assessGroups,
                                                        List<Map<String, Object>> currentGroupFlows,
                                                        Map<String, Object> prevYearData,
                                                        BigDecimal[] stageActuals, BigDecimal[] stageRebateActuals,
                                                        BigDecimal[] stageTargets, BigDecimal totalTarget,
                                                        Long projectId, String calcBasis, String rebateBasis,
                                                        UpstreamAgreement up) {
        List<GroupStageData> list = new ArrayList<>();
        boolean singleGroup = assessGroups == null || assessGroups.size() <= 1;

        Map<String, Map<String, Object>> prevFlowByName = new HashMap<>();
        if (prevYearData != null) {
            Object gf = prevYearData.get("groupFlows");
            if (gf instanceof List) {
                for (Object o : (List<?>) gf) {
                    if (o instanceof Map) {
                        Map<String, Object> m = (Map<String, Object>) o;
                        Object name = m.get("groupName");
                        if (name != null) prevFlowByName.put(name.toString(), m);
                    }
                }
            }
        }
        BigDecimal[] prevOverall = new BigDecimal[4];
        for (int i = 0; i < 4; i++) prevOverall[i] = extractPrevStage(prevYearData, i);

        if (singleGroup) {
            GroupStageData gd = new GroupStageData();
            gd.groupId = (assessGroups == null || assessGroups.isEmpty()) ? 0L
                    : (assessGroups.get(0).getId() == null ? 0L : assessGroups.get(0).getId());
            gd.groupName = (assessGroups == null || assessGroups.isEmpty()) ? "默认" : assessGroups.get(0).getGroupName();
            for (int i = 0; i < 4; i++) {
                gd.stageActuals[i] = stageActuals[i];
                gd.stageRebateActuals[i] = stageRebateActuals[i];
                gd.stageTargets[i] = stageTargets[i];
                gd.prevStageActuals[i] = prevOverall[i];
            }
            gd.totalActual = sum(stageActuals);
            gd.totalRebateActual = sum(stageRebateActuals);
            gd.totalTarget = totalTarget;
            gd.prevTotalActual = sum(prevOverall);
            list.add(gd);
            return list;
        }

        // 多考核组时，针对每个考核组按 rebateBasis 重新加载流向（如果口径不一致）
        Map<Long, BigDecimal[]> groupRebateStageMap = null;
        if (!sameBasis(rebateBasis, calcBasis)) {
            groupRebateStageMap = new HashMap<>();
            for (AssessGroup ag : assessGroups) {
                Long gid = ag.getId() == null ? 0L : ag.getId();
                Map<String, BigDecimal> ms = ProjectScaleService.loadMonthScale(projectId, rebateBasis, gid);
                Map<String, BigDecimal> sc = ProjectScaleService.computeScale(
                        projectId, rebateBasis, ms,
                        up.getStage1Target(), up.getStage2Target(), up.getStage3Target(), up.getStage4Target());
                groupRebateStageMap.put(gid, new BigDecimal[]{
                        nz(sc.get("stage1Actual")), nz(sc.get("stage2Actual")),
                        nz(sc.get("stage3Actual")), nz(sc.get("stage4Actual"))});
            }
        }

        for (AssessGroup ag : assessGroups) {
            GroupStageData gd = new GroupStageData();
            gd.groupId = ag.getId() == null ? 0L : ag.getId();
            gd.groupName = ag.getGroupName();
            gd.sharedGroupIds = ag.getSharedGroupIds();
            Map<String, Object> flow = findFlowByGroupId(currentGroupFlows, gd.groupId);
            for (int i = 0; i < 4; i++) {
                Object rawA = flow != null ? flow.get("stage" + (i + 1) + "Actual") : null;
                gd.stageActuals[i] = rawA == null
                        ? (stageActuals[i].signum() > 0 ? stageActuals[i] : BigDecimal.ZERO) : bd(rawA);
                Object rawT = flow != null ? flow.get("stage" + (i + 1) + "Target") : null;
                gd.stageTargets[i] = rawT == null
                        ? (stageTargets[i].signum() > 0 ? stageTargets[i] : BigDecimal.ZERO) : bd(rawT);
            }
            // rebateBasis 口径阶段实际值
            if (groupRebateStageMap != null && groupRebateStageMap.containsKey(gd.groupId)) {
                BigDecimal[] rbs = groupRebateStageMap.get(gd.groupId);
                System.arraycopy(rbs, 0, gd.stageRebateActuals, 0, 4);
            } else {
                System.arraycopy(gd.stageActuals, 0, gd.stageRebateActuals, 0, 4);
            }
            Map<String, Object> prevFlow = prevFlowByName.get(gd.groupName);
            for (int i = 0; i < 4; i++) {
                gd.prevStageActuals[i] = prevFlow != null ? bd(prevFlow.get("stage" + (i + 1) + "Actual")) : BigDecimal.ZERO;
            }
            gd.totalActual = sum(gd.stageActuals);
            gd.totalRebateActual = sum(gd.stageRebateActuals);
            gd.totalTarget = ag.getTargetScale() == null ? totalTarget : ag.getTargetScale();
            gd.prevTotalActual = sum(gd.prevStageActuals);
            list.add(gd);
        }
        return list;
    }

    // ================================================================
    // 构建 GroupStageData（下游）
    // ================================================================
    @SuppressWarnings("unchecked")
    private List<GroupStageData> buildDownstreamGroupData(List<Map<String, Object>> assessGroups,
                                                          Map<String, Object> prevYearData,
                                                          BigDecimal[] stageActuals, BigDecimal[] stageRebateActuals,
                                                          BigDecimal[] stageTargets, BigDecimal totalTarget,
                                                          Project project, DownstreamAgreement down,
                                                          String calcBasis, String rebateBasis) {
        List<GroupStageData> list = new ArrayList<>();
        boolean singleGroup = assessGroups == null || assessGroups.size() <= 1;
        BigDecimal[] prevStageActuals = new BigDecimal[4];
        for (int i = 0; i < 4; i++) prevStageActuals[i] = extractPrevStage(prevYearData, i);

        if (singleGroup) {
            GroupStageData gd = new GroupStageData();
            gd.groupId = 0L;
            gd.groupName = "默认";
            for (int i = 0; i < 4; i++) {
                gd.stageActuals[i] = stageActuals[i];
                gd.stageRebateActuals[i] = stageRebateActuals[i];
                gd.stageTargets[i] = stageTargets[i];
                gd.prevStageActuals[i] = prevStageActuals[i];
            }
            gd.totalActual = sum(stageActuals);
            gd.totalRebateActual = sum(stageRebateActuals);
            gd.totalTarget = totalTarget;
            gd.prevTotalActual = sum(prevStageActuals);
            list.add(gd);
            return list;
        }

        // 多考核组：对每个考核组单独加载 rebateBasis 流向
        Map<Long, BigDecimal[]> groupRebateStageMap = null;
        if (!sameBasis(rebateBasis, calcBasis) && project != null && down != null) {
            groupRebateStageMap = new HashMap<>();
            com.rebate.dao.DownstreamFlowDao dsFlowDao = new com.rebate.dao.DownstreamFlowDao();
            for (Map<String, Object> ag : assessGroups) {
                Long gid = toLong(ag.get("id"));
                Map<String, BigDecimal> groupStage = new HashMap<>();
                groupStage.put("S1", BigDecimal.ZERO); groupStage.put("S2", BigDecimal.ZERO);
                groupStage.put("S3", BigDecimal.ZERO); groupStage.put("S4", BigDecimal.ZERO);
                try {
                    List<Map<String, Object>> gm = dsFlowDao.sumByMonth(project.getId(), rebateBasis,
                            gid > 0 ? gid : null, down.getId());
                    if (project.getPeriodStartDate() != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(project.getPeriodStartDate());
                        int startYear = cal.get(Calendar.YEAR);
                        int startMonth = cal.get(Calendar.MONTH) + 1;
                        for (Map<String, Object> m : gm) {
                            BigDecimal scale = (BigDecimal) m.get("scale");
                            if (scale == null) continue;
                            String month = (String) m.get("month");
                            if (month == null || month.length() < 6) continue;
                            int yyyymm = Integer.parseInt(month);
                            int monthOffset = (yyyymm / 100 - startYear) * 12 + (yyyymm % 100 - startMonth);
                            int stageIdx = monthOffset / 3;
                            if (stageIdx >= 0 && stageIdx < 4) {
                                String[] stages = {"S1", "S2", "S3", "S4"};
                                groupStage.put(stages[stageIdx], groupStage.get(stages[stageIdx]).add(scale));
                            }
                        }
                    }
                } catch (Exception ignored) {}
                groupRebateStageMap.put(gid, new BigDecimal[]{
                        groupStage.get("S1"), groupStage.get("S2"),
                        groupStage.get("S3"), groupStage.get("S4")});
            }
        }

        for (Map<String, Object> ag : assessGroups) {
            GroupStageData gd = new GroupStageData();
            gd.groupId = toLong(ag.get("id"));
            gd.groupName = ag.get("groupName") == null ? "默认" : String.valueOf(ag.get("groupName"));
            Object sg = ag.get("sharedGroupIds");
            gd.sharedGroupIds = sg == null ? null : String.valueOf(sg);
            for (int i = 0; i < 4; i++) {
                Object rawA = ag.get("stage" + (i + 1) + "Actual");
                gd.stageActuals[i] = rawA == null
                        ? (stageActuals[i].signum() > 0 ? stageActuals[i] : BigDecimal.ZERO) : bd(rawA);
                Object rawT = ag.get("stage" + (i + 1) + "Target");
                gd.stageTargets[i] = rawT == null
                        ? (stageTargets[i].signum() > 0 ? stageTargets[i] : BigDecimal.ZERO) : bd(rawT);
                gd.prevStageActuals[i] = prevStageActuals[i];
            }
            if (groupRebateStageMap != null && groupRebateStageMap.containsKey(gd.groupId)) {
                System.arraycopy(groupRebateStageMap.get(gd.groupId), 0, gd.stageRebateActuals, 0, 4);
            } else {
                System.arraycopy(gd.stageActuals, 0, gd.stageRebateActuals, 0, 4);
            }
            gd.totalActual = sum(gd.stageActuals);
            gd.totalRebateActual = sum(gd.stageRebateActuals);
            Object rawTT = ag.get("totalTarget");
            gd.totalTarget = rawTT == null ? totalTarget : bd(rawTT);
            gd.prevTotalActual = sum(prevStageActuals);
            list.add(gd);
        }
        return list;
    }

    // ================================================================
    // 计算：按考核组/共享组分组聚合后调用 calcRulesByType
    // ================================================================
    private BigDecimal calcRebateForGroups(List<RebateRule> rules, List<GroupStageData> groups,
                                           BigDecimal fallbackCalcActual, BigDecimal fallbackRebateActual,
                                           BigDecimal fallbackTarget,
                                           BigDecimal fallbackPrev, int stageIdx, String calcMode,
                                           boolean qtyBased) {
        return calcRebateForGroups(rules, groups, fallbackCalcActual, fallbackRebateActual,
                fallbackTarget, fallbackPrev, stageIdx, calcMode, null, qtyBased);
    }

    private BigDecimal calcRebateForGroups(List<RebateRule> rules, List<GroupStageData> groups,
                                           BigDecimal fallbackCalcActual, BigDecimal fallbackRebateActual,
                                           BigDecimal fallbackTarget,
                                           BigDecimal fallbackPrev, int stageIdx, String calcMode,
                                           List<Map<String, Object>> details, boolean qtyBased) {
        if (rules == null || rules.isEmpty()) return BigDecimal.ZERO;
        if (groups == null || groups.isEmpty()) {
            return calcRulesByType(rules, fallbackCalcActual, fallbackRebateActual,
                    fallbackTarget, fallbackPrev, calcMode, details, "项目汇总", qtyBased);
        }
        BigDecimal total = BigDecimal.ZERO;

        // 收集所有参与共享的考核组ID（用于在普通分组时跳过）
        java.util.Set<Long> sharedGroupIds = new java.util.HashSet<>();
        for (GroupStageData gd : groups) {
            if (hasSharedGroups(gd)) {
                sharedGroupIds.add(gd.groupId);
                // 共享组配置中的其他组也加入
                for (Long id : parseGroupIds(gd.sharedGroupIds)) {
                    sharedGroupIds.add(id);
                }
            }
        }

        // 1) 普通考核组（未配置共享组）：按 assessGroupId 分组，各组独立计算
        for (GroupStageData gd : groups) {
            if (sharedGroupIds.contains(gd.groupId)) continue; // 共享组在后面统一处理
            List<RebateRule> rulesForGroup = new ArrayList<>();
            for (RebateRule r : rules) {
                // 兼容旧数据：如果规则自身仍带有 sharedGroupIds，跳过（由共享逻辑处理）
                if (hasSharedGroups(r)) continue;
                if (ruleMatchesGroup(r, gd.groupId)) rulesForGroup.add(r);
            }
            if (!rulesForGroup.isEmpty()) {
                BigDecimal calcA = valForStage(gd.stageActuals, stageIdx, gd.totalActual);
                BigDecimal rebA = valForStage(gd.stageRebateActuals, stageIdx, gd.totalRebateActual);
                BigDecimal t = valForStage(gd.stageTargets, stageIdx, gd.totalTarget);
                BigDecimal p = valForStage(gd.prevStageActuals, stageIdx, gd.prevTotalActual);
                total = total.add(calcRulesByType(rulesForGroup, calcA, rebA, t, p, calcMode, details, gd.groupName, qtyBased));
            }
        }
        // ================================================================
        // 共享考核组（目标共享）：
        //   - 多个考核组共享一组目标 → 达成率 = Σ(各组实际) / Σ(各组目标)
        //   - 但每个考核组有各自的返利规则 → 用共享达成率匹配自己的规则区间
        //   - 各考核组的返利基数 = 自己的实际值（rebateActual）
        //   例：组2+组3共享150万目标，组2实际80万，组3实际100万
        //       共享达成率 = (80+100)/150 = 120%
        //       组2用自己的规则(120以上→8%)，基数=80万 → 返利=80×8%=6.4万
        //       组3用自己的规则(110~130%→9%)，基数=100万 → 返利=100×9%=9万
        //   共享关系来源：AssessGroup.sharedGroupIds（非 RebateRule.sharedGroupIds）
        // ================================================================

        // 2) 按共享组合（sharedKey）聚合：同一共享键下的多个考核组共用达成率
        //    每个 sharedKey 下，每个考核组用自己的规则 + 自己的实际值
        //    sharedKey = 本组ID + 所选共享组ID（归一化排序），确保双向选择时键一致
        //    例：组2选3 → sharedKey="2,3"；组3选2 → sharedKey="2,3" → 聚合到同一桶
        Map<String, List<GroupStageData>> sharedBuckets = new LinkedHashMap<>();
        for (GroupStageData gd : groups) {
            if (!hasSharedGroups(gd)) continue;
            String sharedKey = buildSharedKeyWithSelf(gd.sharedGroupIds, gd.groupId);
            sharedBuckets.computeIfAbsent(sharedKey, k -> new ArrayList<>()).add(gd);
        }

        // 缓存每个 sharedKey 的汇总值
        Map<String, BigDecimal> sharedCalcCache = new HashMap<>();
        Map<String, BigDecimal> sharedTargetCache = new HashMap<>();
        Map<String, BigDecimal> sharedPrevCache = new HashMap<>();

        for (Map.Entry<String, List<GroupStageData>> entry : sharedBuckets.entrySet()) {
            String sharedKey = entry.getKey();
            List<GroupStageData> sharedGroups = entry.getValue();
            List<Long> sharedGIds = parseGroupIds(sharedKey);

            // 共享达成率用的汇总值
            BigDecimal sharedCalc = sharedCalcCache.computeIfAbsent(sharedKey, k -> {
                BigDecimal s = BigDecimal.ZERO;
                for (Long id : sharedGIds) {
                    GroupStageData gd = findGroupById(groups, id);
                    if (gd != null) s = s.add(valForStage(gd.stageActuals, stageIdx, gd.totalActual));
                }
                return s;
            });
            BigDecimal sharedTarget = sharedTargetCache.computeIfAbsent(sharedKey, k -> {
                BigDecimal s = BigDecimal.ZERO;
                for (Long id : sharedGIds) {
                    GroupStageData gd = findGroupById(groups, id);
                    if (gd != null) s = s.add(valForStage(gd.stageTargets, stageIdx, gd.totalTarget));
                }
                return s;
            });
            BigDecimal sharedPrev = sharedPrevCache.computeIfAbsent(sharedKey, k -> {
                BigDecimal s = BigDecimal.ZERO;
                for (Long id : sharedGIds) {
                    GroupStageData gd = findGroupById(groups, id);
                    if (gd != null) s = s.add(valForStage(gd.prevStageActuals, stageIdx, gd.prevTotalActual));
                }
                return s;
            });

            // 每个考核组用自己的规则 + 自己的实际值，但用共享达成率
            for (GroupStageData gd : sharedGroups) {
                List<RebateRule> groupRules = new ArrayList<>();
                for (RebateRule r : rules) {
                    if (ruleMatchesGroup(r, gd.groupId)) groupRules.add(r);
                }
                if (groupRules.isEmpty()) continue;
                BigDecimal ownRebateActual = valForStage(gd.stageRebateActuals, stageIdx, gd.totalRebateActual);
                // calcActual = 共享汇总值；rebateActual = 本组实际值；target = 共享目标
                total = total.add(calcRulesByType(groupRules, sharedCalc, ownRebateActual, sharedTarget, sharedPrev, calcMode, details, gd.groupName + "(共享)", qtyBased));
            }
        }

        // 3) 兜底：规则自身仍带有 sharedGroupIds 但考核组未配置共享（旧数据兼容）
        //    用规则上的 sharedGroupIds 聚合，按汇总实际值计算
        Map<String, List<RebateRule>> legacySharedBuckets = new LinkedHashMap<>();
        for (RebateRule r : rules) {
            if (!hasSharedGroups(r)) continue;
            // 如果该规则所属考核组已在新共享逻辑中处理，则跳过
            Long rgid = r.getAssessGroupId();
            if (rgid != null && sharedGroupIds.contains(rgid)) continue;
            String key = normalizeSharedGroupIds(r.getSharedGroupIds());
            legacySharedBuckets.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<String, List<RebateRule>> e : legacySharedBuckets.entrySet()) {
            List<Long> gIds = parseGroupIds(e.getKey());
            BigDecimal aggCalc = BigDecimal.ZERO, aggRebate = BigDecimal.ZERO, aggTarget = BigDecimal.ZERO, aggPrev = BigDecimal.ZERO;
            for (Long id : gIds) {
                GroupStageData gd = findGroupById(groups, id);
                if (gd != null) {
                    aggCalc = aggCalc.add(valForStage(gd.stageActuals, stageIdx, gd.totalActual));
                    aggRebate = aggRebate.add(valForStage(gd.stageRebateActuals, stageIdx, gd.totalRebateActual));
                    aggTarget = aggTarget.add(valForStage(gd.stageTargets, stageIdx, gd.totalTarget));
                    aggPrev = aggPrev.add(valForStage(gd.prevStageActuals, stageIdx, gd.prevTotalActual));
                }
            }
            total = total.add(calcRulesByType(e.getValue(), aggCalc, aggRebate, aggTarget, aggPrev, calcMode, details, "共享组" + e.getKey(), qtyBased));
        }
        return total;
    }

    /**
     * 按 rewardType 计算一组规则的返利金额。
     *
     * @param calcActual   calcBasis 口径实际值（用于区间匹配 + 计算X）
     * @param rebateActual rebateCalcBasis 口径实际值（用于Y变量 + base）
     * @param calcMode     返利计算模式 PROGRESSIVE / FLAT（来自协议）
     */
    private BigDecimal calcRulesByType(List<RebateRule> rules, BigDecimal calcActual, BigDecimal rebateActual,
                                       BigDecimal target, BigDecimal prevActual, String calcMode, boolean qtyBased) {
        return calcRulesByType(rules, calcActual, rebateActual, target, prevActual, calcMode, null, null, qtyBased);
    }

    private BigDecimal calcRulesByType(List<RebateRule> rules, BigDecimal calcActual, BigDecimal rebateActual,
                                       BigDecimal target, BigDecimal prevActual, String calcMode,
                                       List<Map<String, Object>> details, String groupName, boolean qtyBased) {
        if (rules == null || rules.isEmpty()) return BigDecimal.ZERO;
        Map<String, List<RebateRule>> byType = new LinkedHashMap<>();
        for (RebateRule r : rules) {
            String t = r.getRewardType() == null ? "PERSENT" : r.getRewardType().toUpperCase();
            byType.computeIfAbsent(t, k -> new ArrayList<>()).add(r);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, List<RebateRule>> e : byType.entrySet()) {
            List<RebateRule> typeRules = new ArrayList<>(e.getValue());
            typeRules.sort(Comparator.comparing(r -> nz(r.getThresholdLow())));
            // X: 按 calcBasis 计算达成率/达成额/增长率（匹配区间阈值）
            BigDecimal x = computeX(e.getKey(), calcActual, target, prevActual);
            // 返利计算基数：统一使用 rebateActual（返利计算依据口径的实际值）
            BigDecimal base = nz(rebateActual);
            // actualY（Y变量） = rebateBasis 实际值（完成核算数量/金额）
            BigDecimal rebateAmt = RebateCalcUtil.calcRebateAmount(typeRules, x, nz(rebateActual), base, nz(target), calcMode, qtyBased);
            total = total.add(rebateAmt);

            // 收集明细
            if (details != null) {
                RebateRule matchedRule = RebateCalcUtil.matchRule(typeRules, x);
                BigDecimal effectiveRatio = matchedRule != null
                        ? RebateCalcUtil.calcRatioByRule(matchedRule, x, nz(rebateActual), qtyBased)
                        : BigDecimal.ZERO;
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("groupName", groupName != null ? groupName : "默认");
                detail.put("rewardType", e.getKey());
                detail.put("x", x.setScale(2, RoundingMode.HALF_UP));
                detail.put("base", base.setScale(2, RoundingMode.HALF_UP));
                detail.put("effectiveRatio", effectiveRatio.setScale(4, RoundingMode.HALF_UP));
                detail.put("rebateAmount", rebateAmt.setScale(2, RoundingMode.HALF_UP));
                detail.put("calcMode", calcMode != null ? calcMode : "PROGRESSIVE");
                detail.put("qtyBased", qtyBased);
                detail.put("matchedRule", matchedRule);
                detail.put("target", target != null ? target.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                detail.put("calcActual", calcActual != null ? calcActual.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                details.add(detail);
            }
        }
        return total;
    }

    /** 返利计算依据为 数量/销售数量 时，返利比例视同每数量单位的金额 */
    private static boolean isQtyBasis(String rebateBasis) {
        if (rebateBasis == null) return false;
        String b = rebateBasis.toUpperCase();
        return b.equals("QTY") || b.equals("SALE_QTY");
    }

    // ================================================================
    // 辅助：区间匹配变量 X
    // ================================================================
    private BigDecimal computeX(String rewardType, BigDecimal actual, BigDecimal target, BigDecimal prevActual) {
        if ("SCALE".equalsIgnoreCase(rewardType)) return nz(actual);
        if ("GROWTH".equalsIgnoreCase(rewardType)) {
            BigDecimal p = nz(prevActual);
            if (p.signum() <= 0) return BigDecimal.ZERO;
            return nz(actual).divide(p, MC).subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100), MC);
        }
        BigDecimal t = nz(target);
        if (t.signum() <= 0) return BigDecimal.ZERO;
        return nz(actual).multiply(BigDecimal.valueOf(100), MC).divide(t, MC);
    }

    // ================================================================
    // 规则过滤
    // ================================================================
    private List<RebateRule> filterStageRules(List<RebateRule> all, String stageCode) {
        List<RebateRule> list = new ArrayList<>();
        for (RebateRule r : all) {
            String sc = r.getStageCode();
            if (sc == null) continue;
            if (sc.equalsIgnoreCase(stageCode) || sc.equalsIgnoreCase("ALL")) list.add(r);
        }
        return list;
    }

    private List<RebateRule> filterFullYearRules(List<RebateRule> all) {
        List<RebateRule> list = new ArrayList<>();
        for (RebateRule r : all) {
            String sc = r.getStageCode();
            if (sc != null && sc.equalsIgnoreCase("FULL_YEAR")) list.add(r);
        }
        return list;
    }

    // ================================================================
    // 辅助：共享组
    // ================================================================
    private boolean hasSharedGroups(RebateRule r) {
        String s = r.getSharedGroupIds();
        return s != null && !s.trim().isEmpty();
    }

    private boolean hasSharedGroups(GroupStageData gd) {
        String s = gd.sharedGroupIds;
        return s != null && !s.trim().isEmpty();
    }

    private boolean ruleMatchesGroup(RebateRule r, Long groupId) {
        Long rid = r.getAssessGroupId();
        if (rid == null || rid == 0L) return true;
        return rid.equals(groupId);
    }

    private List<Long> parseGroupIds(String s) {
        List<Long> list = new ArrayList<>();
        for (String p : s.split("[,，;； ]+")) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            try { list.add(Long.parseLong(t)); } catch (NumberFormatException ignored) {}
        }
        Collections.sort(list);
        return list;
    }

    private String normalizeSharedGroupIds(String s) {
        List<Long> ids = parseGroupIds(s);
        if (ids.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    /**
     * 构建包含本组自身的共享键：本组ID + 所选共享组ID，归一化排序后拼接。
     * 确保双向选择时（组2选3、组3选2）产生相同的 sharedKey="2,3"，
     * 从而聚合到同一桶，且汇总实际值/目标时包含双方。
     */
    private String buildSharedKeyWithSelf(String sharedGroupIds, Long selfId) {
        String combined = (selfId == null ? "" : selfId.toString()) + "," + (sharedGroupIds == null ? "" : sharedGroupIds);
        return normalizeSharedGroupIds(combined);
    }

    private GroupStageData findGroupById(List<GroupStageData> groups, Long id) {
        for (GroupStageData g : groups) {
            if (id == null ? g.groupId == null : id.equals(g.groupId)) return g;
        }
        return null;
    }

    private Map<String, Object> findFlowByGroupId(List<Map<String, Object>> flows, Long groupId) {
        if (flows == null) return null;
        for (Map<String, Object> f : flows) {
            Long id = toLong(f.get("groupId"));
            if (groupId == null ? id == null : groupId.equals(id)) return f;
        }
        return null;
    }

    // ================================================================
    // 通用辅助
    // ================================================================
    private Map<String, Object> buildResult(List<RebateRule> allRules, BigDecimal[] stageActuals,
                                            BigDecimal[] stageTargets, BigDecimal[] stageRebateAmounts,
                                            BigDecimal fullYearRebate) {
        List<List<RebateRule>> projStageRules = new ArrayList<>();
        for (int i = 0; i < 4; i++) projStageRules.add(filterStageRules(allRules, STAGE_CODES[i]));
        return buildResult(stageActuals, stageTargets, stageRebateAmounts, projStageRules, fullYearRebate, null);
    }

    private Map<String, Object> buildResult(BigDecimal[] stageActuals, BigDecimal[] stageTargets,
                                            BigDecimal[] stageRebateAmounts, List<List<RebateRule>> stageRules,
                                            BigDecimal fullYearRebate,
                                            List<List<Map<String, Object>>> stageDetails) {
        BigDecimal totalRebate = BigDecimal.ZERO;
        List<Map<String, Object>> stageRebates = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Map<String, Object> sr = new LinkedHashMap<>();
            sr.put("stageCode", STAGE_CODES[i]);
            sr.put("stageName", STAGE_NAMES[i]);
            sr.put("target", stageTargets[i]);
            sr.put("actual", stageActuals[i]);
            sr.put("rebateAmount", stageRebateAmounts[i].setScale(2, RoundingMode.HALF_UP));
            sr.put("rules", stageRules.get(i));
            if (stageDetails != null && i < stageDetails.size()) {
                sr.put("details", stageDetails.get(i));
            }
            stageRebates.add(sr);
            totalRebate = totalRebate.add(stageRebateAmounts[i]);
        }
        totalRebate = totalRebate.add(fullYearRebate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stageRebates", stageRebates);
        result.put("totalRebate", totalRebate.setScale(2, RoundingMode.HALF_UP));
        result.put("fullYearRebate", fullYearRebate.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    private BigDecimal valForStage(BigDecimal[] stageVals, int stageIdx, BigDecimal totalVal) {
        if (stageIdx < 0) return nz(totalVal);
        return nz(stageVals[stageIdx]);
    }

    private BigDecimal extractPrevStage(Map<String, Object> prevYearData, int i) {
        if (prevYearData == null) return BigDecimal.ZERO;
        Object o = prevYearData.get("stage" + (i + 1) + "Actual");
        if (o == null) o = prevYearData.get("S" + (i + 1));
        return bd(o);
    }

    private BigDecimal extractPrevTotal(Map<String, Object> prevYearData) {
        if (prevYearData == null) return BigDecimal.ZERO;
        Object t = prevYearData.get("totalActual");
        if (t != null) return bd(t);
        BigDecimal s = BigDecimal.ZERO;
        for (int i = 0; i < 4; i++) s = s.add(extractPrevStage(prevYearData, i));
        return s;
    }

    private Map<String, Object> emptyResult() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("stageRebates", Collections.emptyList());
        r.put("totalRebate", BigDecimal.ZERO);
        r.put("fullYearRebate", BigDecimal.ZERO);
        return r;
    }

    private static BigDecimal bd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(((Number) o).toString());
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private static Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static BigDecimal sum(BigDecimal[] arr) {
        BigDecimal s = BigDecimal.ZERO;
        for (BigDecimal v : arr) s = s.add(nz(v));
        return s;
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] a = new BigDecimal[4];
        Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }

    private static class GroupStageData {
        Long groupId;
        String groupName;
        String sharedGroupIds;                          // 共享考核组ID列表（来自AssessGroup，逗号分隔）
        BigDecimal[] stageActuals = zeros();           // calcBasis口径：用于判定达成率
        BigDecimal[] stageRebateActuals = zeros();     // rebateCalcBasis口径：用于返利基数和Y变量
        BigDecimal[] stageTargets = zeros();
        BigDecimal[] prevStageActuals = zeros();       // calcBasis口径（上年）
        BigDecimal totalActual = BigDecimal.ZERO;       // calcBasis口径
        BigDecimal totalRebateActual = BigDecimal.ZERO;// rebateCalcBasis口径
        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal prevTotalActual = BigDecimal.ZERO;
    }
}
