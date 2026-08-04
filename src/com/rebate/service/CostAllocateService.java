package com.rebate.service;

import com.rebate.dao.BaseDao;
import com.rebate.dao.CostDao;
import com.rebate.dao.ProjectDao;
import com.rebate.model.Project;
import com.rebate.model.ProjectExpense;
import com.rebate.model.ProjectLabor;
import com.rebate.model.ProjectStaff;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.*;

/**
 * 项目费用/人工 分摊服务
 * <p>规则：</p>
 * <ul>
 *   <li>如果该人员为全职或外包 → 分摊金额 = 费用金额</li>
 *   <li>如果该人员为兼职 → 分摊金额 = 费用金额 × 本项目投入比例 / ∑所有项目投入比例</li>
 * </ul>
 */
public class CostAllocateService {

    private final ProjectDao projectDao = new ProjectDao();
    private final CostDao costDao = new CostDao();

    /**
     * 分配一个项目费用记录到具体项目，并计算本项目分摊金额
     * 旧方法保留兼容
     */
    public ProjectExpense allocateExpense(ProjectExpense e) {
        e = matchExpenseProject(e);
        e.setAllocatedAmount(calcExpenseAlloc(e));
        return e;
    }

    /**
     * 根据报销日期和人员，按新规则分摊费用
     * 返回需要保存的费用记录列表
     */
    public List<ProjectExpense> allocateExpenseByRule(ProjectExpense e) {
        List<ProjectExpense> result = new ArrayList<>();
        
        // 1. 按报销日期与项目的起止时间的匹配关系来筛选项目，形成相关项目组
        if (e.getWorkNo() == null || e.getWorkNo().isEmpty() || e.getReimburseDate() == null) {
            // 如果没有人员或日期，直接保存原样
            result.add(e);
            return result;
        }

        List<ProjectStaff> matchedPersons = findStaffByDate(e.getWorkNo(), e.getReimburseDate());

        // 2. 如果报销人员仅在一个项目中存在，则其本项目分摊金额直接等于发票金额
        if (matchedPersons.size() == 1) {
            ProjectStaff p = matchedPersons.get(0);
            ProjectExpense newE = cloneExpense(e);
            newE.setProjectId(p.getProjectId());
            newE.setAllocatedAmount(e.getAmount());
            result.add(newE);
            return result;
        }

        // 3. 如果报销人员在多个项目中存在
        if (matchedPersons.size() > 1) {
            // 1）计算该人员在相关项目组各个项目中费用分摊比例之和
            BigDecimal sumRatio = BigDecimal.ZERO;
            for (ProjectStaff p : matchedPersons) {
                sumRatio = sumRatio.add(p.getExpenseRatio() == null ? BigDecimal.ZERO : p.getExpenseRatio());
            }

            // 2）在相关项目组中逐个项目添加费用投入记录
            for (ProjectStaff p : matchedPersons) {
                ProjectExpense newE = cloneExpense(e);
                newE.setProjectId(p.getProjectId());
                
                // 计算本项目分摊额
                BigDecimal thisRatio = p.getExpenseRatio() == null ? BigDecimal.ZERO : p.getExpenseRatio();
                if (sumRatio.signum() == 0) {
                    // 如果比例和为0，每个项目都分摊全部
                    newE.setAllocatedAmount(e.getAmount());
                } else {
                    // 分摊额=发票金额*本项目比例/总比例
                    BigDecimal allocated = e.getAmount().multiply(thisRatio).divide(sumRatio, 2, RoundingMode.HALF_UP);
                    newE.setAllocatedAmount(allocated);
                }
                result.add(newE);
            }
            return result;
        }

        // 如果没有匹配到任何项目，保存原样
        result.add(e);
        return result;
    }

    /**
     * 克隆一个费用记录
     */
    private ProjectExpense cloneExpense(ProjectExpense e) {
        ProjectExpense newE = new ProjectExpense();
        newE.setReimburseDate(e.getReimburseDate());
        newE.setExpenseType(e.getExpenseType());
        newE.setWorkNo(e.getWorkNo());
        newE.setName(e.getName());
        newE.setDescription(e.getDescription());
        newE.setAmount(e.getAmount());
        newE.setSource(e.getSource());
        newE.setRawProjectName(e.getRawProjectName());
        newE.setDocNo(e.getDocNo());
        newE.setRemark(e.getRemark());
        newE.setImportUser(e.getImportUser());
        return newE;
    }

    /**
     * 分配人工成本到具体项目，并计算本项目分摊金额
     */
    public ProjectLabor allocateLabor(ProjectLabor l) {
        l = matchLaborProject(l);
        l.setAllocatedAmount(calcLaborAlloc(l));
        return l;
    }

    /**
     * 根据月度（取月末）和人员，按新规则分摊人工成本
     * 返回需要保存的人工记录列表
     */
    public List<ProjectLabor> allocLaborByRule(ProjectLabor l) {
        List<ProjectLabor> result = new ArrayList<>();
        
        // 按月度与项目的起止时间的匹配关系来筛选项目，形成相关项目组
        if (l.getWorkNo() == null || l.getWorkNo().isEmpty() || l.getMonthYyyymm() == null) {
            result.add(l);
            return result;
        }

        // 计算月末日期
        String month = l.getMonthYyyymm();
        int y, m;
        if (month.contains("-")) {
            // 格式: YYYY-MM
            y = Integer.parseInt(month.substring(0, 4));
            m = Integer.parseInt(month.substring(5, 7));
        } else {
            // 格式: YYYYMM
            y = Integer.parseInt(month.substring(0, 4));
            m = Integer.parseInt(month.substring(4, 6));
        }
        Calendar c = Calendar.getInstance();
        c.set(y, m - 1, 1);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endDate = new Date(c.getTimeInMillis());

        List<ProjectStaff> matchedPersons = findStaffByDate(l.getWorkNo(), endDate);

        // 如果人员在只有一个项目中存在，则其本项目分摊金额直接等于费用合计
        if (matchedPersons.size() == 1) {
            ProjectStaff p = matchedPersons.get(0);
            ProjectLabor newL = cloneLabor(l);
            newL.setProjectId(p.getProjectId());
            newL.setAllocatedAmount(l.getTotalCost());
            newL.setAllocRatio(BigDecimal.ONE);
            result.add(newL);
            return result;
        }

        // 如果人员在多个项目中存在
        if (matchedPersons.size() > 1) {
            // 计算该人员在相关项目组各个项目中人工成本比例之和
            BigDecimal sumRatio = BigDecimal.ZERO;
            for (ProjectStaff p : matchedPersons) {
                sumRatio = sumRatio.add(p.getLaborCostRatio() == null ? BigDecimal.ZERO : p.getLaborCostRatio());
            }

            // 在相关项目组中逐个项目添加人工投入记录
            for (ProjectStaff p : matchedPersons) {
                ProjectLabor newL = cloneLabor(l);
                newL.setProjectId(p.getProjectId());
                
                // 计算本项目分摊额
                BigDecimal thisRatio = p.getLaborCostRatio() == null ? BigDecimal.ZERO : p.getLaborCostRatio();
                if (sumRatio.signum() == 0) {
                    // 如果比例和为0，每个项目都分摊全部
                    newL.setAllocatedAmount(l.getTotalCost());
                    newL.setAllocRatio(BigDecimal.ONE);
                } else {
                    // 分摊额 = 费用合计 * 本项目比例 / 总比例
                    BigDecimal allocRatio = thisRatio.divide(sumRatio, 4, RoundingMode.HALF_UP);
                    newL.setAllocRatio(allocRatio);
                    BigDecimal allocated = l.getTotalCost().multiply(thisRatio).divide(sumRatio, 2, RoundingMode.HALF_UP);
                    newL.setAllocatedAmount(allocated);
                }
                result.add(newL);
            }
            return result;
        }

        // 如果没有匹配到任何项目，保存原样
        result.add(l);
        return result;
    }

    /**
     * 克隆一个人工记录
     */
    private ProjectLabor cloneLabor(ProjectLabor l) {
        ProjectLabor newL = new ProjectLabor();
        newL.setMonthYyyymm(l.getMonthYyyymm());
        newL.setWorkNo(l.getWorkNo());
        newL.setName(l.getName());
        newL.setWorkType(l.getWorkType());
        newL.setSalary(l.getSalary());
        newL.setWelfare(l.getWelfare());
        newL.setTotalCost(l.getTotalCost());
        newL.setRemark(l.getRemark());
        newL.setSource(l.getSource());
        newL.setImportUser(l.getImportUser());
        return newL;
    }

    private ProjectExpense matchExpenseProject(ProjectExpense e) {
        if (e.getRawProjectName() != null && !e.getRawProjectName().isEmpty()) {
            String sql = "SELECT * FROM prj_project WHERE project_name=? AND (? IS NULL OR (COALESCE(period_start_date, '1900-01-01'::date) <= ? AND COALESCE(period_end_date, '9999-12-31'::date) >= ?)) LIMIT 1";
            Project p = BaseDao.queryOne(sql, rs -> {
                Project pp = new Project();
                pp.setId(rs.getLong("id"));
                pp.setProjectName(rs.getString("project_name"));
                return pp;
            }, e.getRawProjectName(), e.getReimburseDate(), e.getReimburseDate(), e.getReimburseDate());
            if (p != null) {
                e.setProjectId(p.getId());
                e.setMatchedType("PROJECT_NAME");
                return e;
            }
        }
        if (e.getWorkNo() != null && !e.getWorkNo().isEmpty() && e.getReimburseDate() != null) {
            List<ProjectStaff> matched = findStaffByDate(e.getWorkNo(), e.getReimburseDate());
            if (matched.size() == 1) {
                e.setProjectId(matched.get(0).getProjectId());
                e.setMatchedType("PERSON");
                return e;
            } else if (matched.size() > 1) {
                ProjectStaff top = matched.get(0);
                for (ProjectStaff pp : matched) {
                    if (pp.getExpenseRatio() != null && top.getExpenseRatio() != null
                            && pp.getExpenseRatio().compareTo(top.getExpenseRatio()) > 0) {
                        top = pp;
                    }
                }
                e.setProjectId(top.getProjectId());
                e.setMatchedType("PERSON_MULTI");
                return e;
            }
        }
        e.setMatchedType("UNMATCHED");
        return e;
    }

    private ProjectLabor matchLaborProject(ProjectLabor l) {
        if (l.getWorkNo() == null || l.getWorkNo().isEmpty() || l.getMonthYyyymm() == null) {
            l.setMatchedType("UNMATCHED");
            return l;
        }
        int y = Integer.parseInt(l.getMonthYyyymm().substring(0, 4));
        int m = Integer.parseInt(l.getMonthYyyymm().substring(4, 6));
        Calendar c = Calendar.getInstance();
        c.set(y, m - 1, 1);
        Date start = new Date(c.getTimeInMillis());
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date end = new Date(c.getTimeInMillis());
        List<ProjectStaff> matched = findStaffByDate(l.getWorkNo(), end);
        if (matched.size() == 1) {
            l.setProjectId(matched.get(0).getProjectId());
            l.setMatchedType("PERSON");
            return l;
        } else if (matched.size() > 1) {
            ProjectStaff top = matched.get(0);
            for (ProjectStaff pp : matched) {
                if (pp.getLaborCostRatio() != null && top.getLaborCostRatio() != null
                        && pp.getLaborCostRatio().compareTo(top.getLaborCostRatio()) > 0) {
                    top = pp;
                }
            }
            l.setProjectId(top.getProjectId());
            l.setMatchedType("PERSON_MULTI");
            return l;
        }
        l.setMatchedType("UNMATCHED");
        return l;
    }

    /**
     * 根据工号和日期查询 prj_project_staff 表
     */
    private List<ProjectStaff> findStaffByDate(String workNo, Date date) {
        // 使用 COALESCE 处理 NULL 值：如果日期为 NULL，则视为匹配任意日期
        String sql = "SELECT ps.* FROM prj_project_staff ps " +
                "JOIN prj_project p ON ps.project_id = p.id " +
                "WHERE ps.user_code = ? " +
                "AND COALESCE(p.period_start_date, '1900-01-01'::date) <= ? " +
                "AND COALESCE(p.period_end_date, '9999-12-31'::date) >= ?";
        return BaseDao.query(sql, (rs) -> {
            ProjectStaff pp = new ProjectStaff();
            pp.setId(rs.getLong("id"));
            pp.setProjectId(rs.getLong("project_id"));
            pp.setUserCode(rs.getString("user_code"));
            pp.setUserName(rs.getString("user_name"));
            pp.setWorkType(rs.getString("work_type"));
            pp.setLaborCostRatio(BaseDao.toBigDecimal(rs.getObject("labor_cost_ratio")));
            pp.setExpenseRatio(BaseDao.toBigDecimal(rs.getObject("expense_ratio")));
            return pp;
        }, workNo, date, date);
    }

    /**
     * 根据工号查询 prj_project_staff 表
     */
    private List<ProjectStaff> findStaffByWorkNo(String workNo) {
        String sql = "SELECT ps.*, p.project_name FROM prj_project_staff ps " +
                "LEFT JOIN prj_project p ON ps.project_id = p.id " +
                "WHERE ps.user_code = ?";
        return BaseDao.query(sql, (rs) -> {
            ProjectStaff pp = new ProjectStaff();
            pp.setId(rs.getLong("id"));
            pp.setProjectId(rs.getLong("project_id"));
            pp.setUserCode(rs.getString("user_code"));
            pp.setUserName(rs.getString("user_name"));
            pp.setWorkType(rs.getString("work_type"));
            pp.setLaborCostRatio(BaseDao.toBigDecimal(rs.getObject("labor_cost_ratio")));
            pp.setExpenseRatio(BaseDao.toBigDecimal(rs.getObject("expense_ratio")));
            return pp;
        }, workNo);
    }

    /**
     * 计算费用在本项目的分摊金额
     * 全职/外包：金额 = 费用金额
     * 兼职：金额 = 费用金额 × 本项目投入比例 / ∑所有项目投入比例
     */
    private BigDecimal calcExpenseAlloc(ProjectExpense e) {
        if (e.getWorkNo() == null || e.getWorkNo().isEmpty() || e.getAmount() == null) {
            return e.getAmount() == null ? BigDecimal.ZERO : e.getAmount();
        }
        List<ProjectStaff> persons = findStaffByWorkNo(e.getWorkNo());
        if (persons.isEmpty()) return e.getAmount();
        
        // 找到当前项目对应的分摊比例
        ProjectStaff targetPerson = null;
        if (e.getProjectId() != null) {
            for (ProjectStaff p : persons) {
                if (e.getProjectId().equals(p.getProjectId())) {
                    targetPerson = p;
                    break;
                }
            }
        }
        if (targetPerson == null) {
            targetPerson = persons.get(0);
        }
        
        String workType = targetPerson.getWorkType();
        if ("FULL".equals(workType) || "OUTSOURCE".equals(workType)) {
            return e.getAmount();
        }
        // 兼职：按比例分摊
        BigDecimal thisRatio = targetPerson.getExpenseRatio() == null ? BigDecimal.ZERO : targetPerson.getExpenseRatio();
        BigDecimal sumRatio = BigDecimal.ZERO;
        for (ProjectStaff p : persons) {
            sumRatio = sumRatio.add(p.getExpenseRatio() == null ? BigDecimal.ZERO : p.getExpenseRatio());
        }
        if (sumRatio.signum() == 0) return e.getAmount();
        return e.getAmount().multiply(thisRatio).divide(sumRatio, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcLaborAlloc(ProjectLabor l) {
        if (l.getWorkNo() == null || l.getWorkNo().isEmpty() || l.getTotalCost() == null) {
            return l.getTotalCost() == null ? BigDecimal.ZERO : l.getTotalCost();
        }
        List<ProjectStaff> persons = findStaffByWorkNo(l.getWorkNo());
        if (persons.isEmpty()) return l.getTotalCost();
        ProjectStaff person = persons.get(0);
        String workType = person.getWorkType();
        if ("FULL".equals(workType) || "OUTSOURCE".equals(workType)) {
            return l.getTotalCost();
        }
        BigDecimal thisRatio = person.getLaborCostRatio() == null ? BigDecimal.ZERO : person.getLaborCostRatio();
        BigDecimal sumRatio = BigDecimal.ZERO;
        for (ProjectStaff p : persons) {
            sumRatio = sumRatio.add(p.getLaborCostRatio() == null ? BigDecimal.ZERO : p.getLaborCostRatio());
        }
        if (sumRatio.signum() == 0) return l.getTotalCost();
        BigDecimal ratio = thisRatio.divide(sumRatio, 4, RoundingMode.HALF_UP);
        l.setAllocRatio(ratio);
        return l.getTotalCost().multiply(thisRatio).divide(sumRatio, 2, RoundingMode.HALF_UP);
    }

    public List<ProjectExpense> splitExpenseByRatio(long expenseId) {
        return Collections.emptyList();
    }
}
