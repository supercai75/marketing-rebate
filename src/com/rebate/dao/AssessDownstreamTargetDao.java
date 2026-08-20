package com.rebate.dao;

import com.rebate.model.AssessDownstreamTarget;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 下游协议考核目标 DAO
 * 表: prj_assess_downstream_target
 */
public class AssessDownstreamTargetDao {

    private AssessDownstreamTarget map(ResultSet rs) throws SQLException {
        AssessDownstreamTarget t = new AssessDownstreamTarget();
        t.setId(rs.getLong("id"));
        t.setAgreementId(rs.getLong("agreement_id"));
        // 正确处理 NULL: rs.getLong() 对 NULL 返回 0，需要额外判断
        Object agIdObj = rs.getObject("assess_group_id");
        t.setAssessGroupId(agIdObj == null ? null : ((Number) agIdObj).longValue());
        t.setGroupName(rs.getString("group_name"));
        t.setGroupCode(rs.getString("group_code"));
        t.setTotalTarget(toBd(rs, "total_target"));
        t.setStage1Target(toBd(rs, "stage1_target"));
        t.setStage2Target(toBd(rs, "stage2_target"));
        t.setStage3Target(toBd(rs, "stage3_target"));
        t.setStage4Target(toBd(rs, "stage4_target"));
        t.setRemark(rs.getString("remark"));
        return t;
    }

    private BigDecimal toBd(ResultSet rs, String col) throws SQLException {
        var v = rs.getBigDecimal(col);
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 按协议查询所有考核目标
     */
    public List<AssessDownstreamTarget> listByAgreement(Long agreementId) {
        return BaseDao.query(
            "SELECT t.*, g.group_name, g.group_code FROM prj_assess_downstream_target t " +
            "LEFT JOIN prj_assess_group g ON t.assess_group_id = g.id " +
            "WHERE t.agreement_id = ? ORDER BY t.assess_group_id",
            this::map, agreementId
        );
    }

    /**
     * 按协议+考核组查询目标
     */
    public AssessDownstreamTarget findByAgreementAndGroup(Long agreementId, Long assessGroupId) {
        return BaseDao.queryOne(
            "SELECT t.*, g.group_name, g.group_code FROM prj_assess_downstream_target t " +
            "LEFT JOIN prj_assess_group g ON t.assess_group_id = g.id " +
            "WHERE t.agreement_id = ? AND t.assess_group_id = ?",
            this::map, agreementId, assessGroupId
        );
    }

    /**
     * 新增或更新（按协议+考核组唯一）
     */
    public void upsert(AssessDownstreamTarget t) {
        AssessDownstreamTarget existing = findByAgreementAndGroup(t.getAgreementId(), t.getAssessGroupId());
        if (existing != null) {
            BaseDao.update(
                "UPDATE prj_assess_downstream_target SET group_name=?, group_code=?, total_target=?, stage1_target=?, stage2_target=?, stage3_target=?, stage4_target=?, remark=? WHERE id=?",
                t.getGroupName(), t.getGroupCode(), t.getTotalTarget(), t.getStage1Target(),
                t.getStage2Target(), t.getStage3Target(), t.getStage4Target(), t.getRemark(), existing.getId()
            );
        } else {
            BaseDao.update(
                "INSERT INTO prj_assess_downstream_target(agreement_id, assess_group_id, group_name, group_code, total_target, stage1_target, stage2_target, stage3_target, stage4_target, remark) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                t.getAgreementId(), t.getAssessGroupId(), t.getGroupName(), t.getGroupCode(),
                t.getTotalTarget(), t.getStage1Target(), t.getStage2Target(), t.getStage3Target(),
                t.getStage4Target(), t.getRemark()
            );
        }
    }

    /**
     * 新增或更新（按协议+考核组唯一）—— 事务内版本，使用外部 Connection。
     * 查询与写入均走同一 Connection，确保事务内可见未提交数据。
     */
    public void upsertWithConn(Connection conn, AssessDownstreamTarget t) throws SQLException {
        AssessDownstreamTarget existing = BaseDao.queryOneWithConn(conn,
            "SELECT t.*, g.group_name, g.group_code FROM prj_assess_downstream_target t " +
            "LEFT JOIN prj_assess_group g ON t.assess_group_id = g.id " +
            "WHERE t.agreement_id = ? AND t.assess_group_id = ?",
            this::map, t.getAgreementId(), t.getAssessGroupId());
        if (existing != null) {
            BaseDao.updateWithConn(conn,
                "UPDATE prj_assess_downstream_target SET group_name=?, group_code=?, total_target=?, stage1_target=?, stage2_target=?, stage3_target=?, stage4_target=?, remark=? WHERE id=?",
                t.getGroupName(), t.getGroupCode(), t.getTotalTarget(), t.getStage1Target(),
                t.getStage2Target(), t.getStage3Target(), t.getStage4Target(), t.getRemark(), existing.getId()
            );
        } else {
            BaseDao.updateWithConn(conn,
                "INSERT INTO prj_assess_downstream_target(agreement_id, assess_group_id, group_name, group_code, total_target, stage1_target, stage2_target, stage3_target, stage4_target, remark) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                t.getAgreementId(), t.getAssessGroupId(), t.getGroupName(), t.getGroupCode(),
                t.getTotalTarget(), t.getStage1Target(), t.getStage2Target(), t.getStage3Target(),
                t.getStage4Target(), t.getRemark()
            );
        }
    }

    /**
     * 删除某协议的所有目标
     */
    public void deleteByAgreement(Long agreementId) {
        BaseDao.update("DELETE FROM prj_assess_downstream_target WHERE agreement_id = ?", agreementId);
    }

    /**
     * 删除某协议+考核组的目标
     */
    public void deleteByAgreementAndGroup(Long agreementId, Long assessGroupId) {
        BaseDao.update("DELETE FROM prj_assess_downstream_target WHERE agreement_id = ? AND assess_group_id = ?",
            agreementId, assessGroupId);
    }
}
