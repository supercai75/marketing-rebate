package com.rebate.dao;

import com.rebate.model.AssessGroup;
import com.rebate.model.AssessItem;
import com.rebate.model.RebateRule;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 返利规则与考核组 DAO
 */
public class RebateRuleDao {

    private RebateRule mapRule(ResultSet rs) throws SQLException {
        RebateRule r = new RebateRule();
        r.setId(rs.getLong("id"));
        r.setAgreementId(rs.getLong("agreement_id"));
        r.setStageCode(rs.getString("stage_code"));
        r.setThresholdLow(rs.getBigDecimal("threshold_low"));
        r.setThresholdHigh(rs.getBigDecimal("threshold_high"));
        r.setRebateRatio(rs.getBigDecimal("rebate_ratio"));
        r.setRewardType(rs.getString("reward_type"));
        // 正确处理 NULL: rs.getLong() 对 NULL 返回 0，需要额外判断
        Object agIdObj = rs.getObject("assess_group_id");
        r.setAssessGroupId(agIdObj == null ? null : ((Number) agIdObj).longValue());
        r.setSortNo(rs.getInt("sort_no"));
        r.setExpression(rs.getString("expression"));
        return r;
    }

    private AssessGroup mapGroup(ResultSet rs) throws SQLException {
        AssessGroup g = new AssessGroup();
        g.setId(rs.getLong("id"));
        g.setProjectId(rs.getLong("project_id"));
        g.setGroupCode(rs.getString("group_code"));
        g.setGroupName(rs.getString("group_name"));
        g.setDescription(rs.getString("description"));
        g.setTargetScale(rs.getBigDecimal("target_scale"));
        g.setStage1Target(rs.getBigDecimal("stage1_target"));
        g.setStage2Target(rs.getBigDecimal("stage2_target"));
        g.setStage3Target(rs.getBigDecimal("stage3_target"));
        g.setStage4Target(rs.getBigDecimal("stage4_target"));
        return g;
    }

    private AssessItem mapItem(ResultSet rs) throws SQLException {
        AssessItem i = new AssessItem();
        i.setId(rs.getLong("id"));
        i.setGroupId(rs.getLong("group_id"));
        i.setItemCode(rs.getString("item_code"));
        i.setItemName(rs.getString("item_name"));
        i.setCalcBasis(rs.getString("calc_basis"));
        i.setTargetValue(rs.getBigDecimal("target_value"));
        i.setWeight(rs.getBigDecimal("weight"));
        i.setSortNo(rs.getInt("sort_no"));
        return i;
    }

    public List<RebateRule> listByAgreement(Long agreementId) {
        return BaseDao.query("SELECT * FROM prj_upstream_rebate_rule WHERE agreement_id=? ORDER BY sort_no", this::mapRule, agreementId);
    }

    public void deleteByAgreement(Long agreementId) {
        BaseDao.update("DELETE FROM prj_upstream_rebate_rule WHERE agreement_id=?", agreementId);
    }

    public Long insertRule(RebateRule rule) {
        String sql = "INSERT INTO prj_upstream_rebate_rule(agreement_id, stage_code, threshold_low, threshold_high, rebate_ratio, reward_type, assess_group_id, sort_no, expression) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, rule.getAgreementId(), rule.getStageCode(), rule.getThresholdLow(),
                rule.getThresholdHigh(), rule.getRebateRatio(), rule.getRewardType(), rule.getAssessGroupId(), rule.getSortNo(), rule.getExpression());
    }

    public List<RebateRule> listDownstreamRebateRules(Long agreementId) {
        return BaseDao.query("SELECT * FROM prj_downstream_rebate_rule WHERE agreement_id=? ORDER BY sort_no", this::mapRule, agreementId);
    }

    public void deleteDownstreamRebateRules(Long agreementId) {
        BaseDao.update("DELETE FROM prj_downstream_rebate_rule WHERE agreement_id=?", agreementId);
    }

    public Long insertDownstreamRebateRule(RebateRule rule) {
        String sql = "INSERT INTO prj_downstream_rebate_rule(agreement_id, stage_code, threshold_low, threshold_high, rebate_ratio, reward_type, assess_group_id, sort_no, expression) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, rule.getAgreementId(), rule.getStageCode(), rule.getThresholdLow(),
                rule.getThresholdHigh(), rule.getRebateRatio(), rule.getRewardType(), rule.getAssessGroupId(), rule.getSortNo(), rule.getExpression());
    }

    public List<AssessGroup> listAssessGroups(Long projectId) {
        return BaseDao.query("SELECT * FROM prj_assess_group WHERE project_id=? ORDER BY id", this::mapGroup, projectId);
    }

    public AssessGroup getAssessGroup(Long id) {
        return BaseDao.queryOne("SELECT * FROM prj_assess_group WHERE id=?", this::mapGroup, id);
    }
    
    public AssessGroup getAssessGroupByProjectAndName(Long projectId, String name) {
        return BaseDao.queryOne("SELECT * FROM prj_assess_group WHERE project_id=? AND group_name=?", this::mapGroup, projectId, name);
    }
    
    public AssessGroup getAssessGroupByProjectAndCode(Long projectId, String code) {
        return BaseDao.queryOne("SELECT * FROM prj_assess_group WHERE project_id=? AND group_code=?", this::mapGroup, projectId, code);
    }

    public Long insertAssessGroup(AssessGroup group) {
        String sql = "INSERT INTO prj_assess_group(project_id, group_code, group_name, description, target_scale, stage1_target, stage2_target, stage3_target, stage4_target, created_by) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, group.getProjectId(), group.getGroupCode(), group.getGroupName(), group.getDescription(),
                group.getTargetScale(), group.getStage1Target(), group.getStage2Target(), group.getStage3Target(), group.getStage4Target(), group.getCreatedBy());
    }

    public int updateAssessGroup(AssessGroup group) {
        String sql = "UPDATE prj_assess_group SET group_code=?, group_name=?, description=?, target_scale=?, stage1_target=?, stage2_target=?, stage3_target=?, stage4_target=? WHERE id=?";
        return BaseDao.update(sql, group.getGroupCode(), group.getGroupName(), group.getDescription(),
                group.getTargetScale(), group.getStage1Target(), group.getStage2Target(), group.getStage3Target(), group.getStage4Target(), group.getId());
    }

    public int deleteAssessGroup(Long id) {
        return BaseDao.update("DELETE FROM prj_assess_group WHERE id=?", id);
    }

    public List<AssessItem> listAssessItems(Long groupId) {
        return BaseDao.query("SELECT * FROM prj_assess_item WHERE group_id=? ORDER BY sort_no", this::mapItem, groupId);
    }

    public Long insertAssessItem(AssessItem item) {
        String sql = "INSERT INTO prj_assess_item(group_id, item_code, item_name, calc_basis, target_value, weight, sort_no) VALUES(?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, item.getGroupId(), item.getItemCode(), item.getItemName(),
                item.getCalcBasis(), item.getTargetValue(), item.getWeight(), item.getSortNo());
    }

    public int deleteAssessItems(Long groupId) {
        return BaseDao.update("DELETE FROM prj_assess_item WHERE group_id=?", groupId);
    }
}
