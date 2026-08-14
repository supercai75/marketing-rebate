package com.rebate.dao;

import com.rebate.model.AssessItemDetail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 应收/应付考核明细 DAO
 */
public class AssessItemDetailDao {

    private AssessItemDetail map(ResultSet rs) throws SQLException {
        AssessItemDetail d = new AssessItemDetail();
        d.setId(rs.getLong("id"));
        Object rId = rs.getObject("receivable_id");
        d.setReceivableId(rId == null ? null : ((Number) rId).longValue());
        Object pId = rs.getObject("payable_id");
        d.setPayableId(pId == null ? null : ((Number) pId).longValue());
        d.setItemType(rs.getString("item_type"));
        d.setItemName(rs.getString("item_name"));
        d.setRemark(rs.getString("remark"));
        d.setTargetValue(BaseDao.toBigDecimal(rs.getObject("target_value")));
        d.setActualValue(BaseDao.toBigDecimal(rs.getObject("actual_value")));
        d.setRewardAmount(BaseDao.toBigDecimal(rs.getObject("reward_amount")));
        Object afId = rs.getObject("attach_file_id");
        d.setAttachFileId(afId == null ? null : ((Number) afId).longValue());
        d.setSortNo(rs.getInt("sort_no"));
        d.setCreatedAt(rs.getTimestamp("created_at"));
        return d;
    }

    public List<AssessItemDetail> listByReceivable(Long receivableId) {
        return BaseDao.query("SELECT * FROM prj_receivable_assess_item WHERE receivable_id=? ORDER BY sort_no, id",
                this::map, receivableId);
    }

    public List<AssessItemDetail> listByPayable(Long payableId) {
        return BaseDao.query("SELECT * FROM prj_payable_assess_item WHERE payable_id=? ORDER BY sort_no, id",
                this::map, payableId);
    }

    public void deleteByReceivable(Long receivableId) {
        BaseDao.update("DELETE FROM prj_receivable_assess_item WHERE receivable_id=?", receivableId);
    }

    public void deleteByPayable(Long payableId) {
        BaseDao.update("DELETE FROM prj_payable_assess_item WHERE payable_id=?", payableId);
    }

    public Long insertReceivableItem(AssessItemDetail item) {
        String sql = "INSERT INTO prj_receivable_assess_item(receivable_id, item_type, item_name, remark, " +
                "target_value, actual_value, reward_amount, attach_file_id, sort_no) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, item.getReceivableId(), item.getItemType(), item.getItemName(),
                item.getRemark(), item.getTargetValue(), item.getActualValue(), item.getRewardAmount(),
                item.getAttachFileId(), item.getSortNo());
    }

    public Long insertPayableItem(AssessItemDetail item) {
        String sql = "INSERT INTO prj_payable_assess_item(payable_id, item_type, item_name, remark, " +
                "target_value, actual_value, reward_amount, attach_file_id, sort_no) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, item.getPayableId(), item.getItemType(), item.getItemName(),
                item.getRemark(), item.getTargetValue(), item.getActualValue(), item.getRewardAmount(),
                item.getAttachFileId(), item.getSortNo());
    }
}
