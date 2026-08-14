package com.rebate.dao;

import com.rebate.model.AssessItemDetail;
import com.rebate.model.AttachFile;

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

    private AttachFile mapAttach(ResultSet rs) throws SQLException {
        AttachFile f = new AttachFile();
        f.setId(rs.getLong("id"));
        f.setFileName(rs.getString("file_name"));
        f.setFilePath(rs.getString("file_path"));
        f.setFileSize(rs.getObject("file_size") == null ? null : rs.getLong("file_size"));
        f.setUploadedBy(rs.getObject("uploaded_by") == null ? null : rs.getLong("uploaded_by"));
        f.setUploadedAt(rs.getTimestamp("uploaded_at"));
        return f;
    }

    /** 通过id查询应收附件，用于确认归属 */
    public AttachFile findReceivableAttach(Long id) {
        List<AttachFile> list = BaseDao.query("SELECT * FROM prj_receivable_assess_attach WHERE id=?", this::mapAttach, id);
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }
    /** 通过id查询应付附件，用于确认归属 */
    public AttachFile findPayableAttach(Long id) {
        List<AttachFile> list = BaseDao.query("SELECT * FROM prj_payable_assess_attach WHERE id=?", this::mapAttach, id);
        return list != null && !list.isEmpty() ? list.get(0) : null;
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

    // --- 应收明细附件 ---
    public Long insertReceivableAssessAttach(Long assessItemId, Long receivableId, AttachFile f) {
        return BaseDao.insertReturnId("INSERT INTO prj_receivable_assess_attach(assess_item_id, receivable_id, file_name, file_path, file_size, uploaded_by) " +
                "VALUES (?, ?, ?, ?, ?, ?)", assessItemId, receivableId, f.getFileName(), f.getFilePath(), f.getFileSize(), f.getUploadedBy());
    }
    public List<AttachFile> listReceivableAttachsByItem(Long assessItemId) {
        return BaseDao.query("SELECT * FROM prj_receivable_assess_attach WHERE assess_item_id=? ORDER BY uploaded_at DESC, id DESC",
                this::mapAttach, assessItemId);
    }
    public List<AttachFile> listReceivableAttachsByReceivable(Long receivableId) {
        return BaseDao.query("SELECT * FROM prj_receivable_assess_attach WHERE receivable_id=? ORDER BY uploaded_at DESC, id DESC",
                this::mapAttach, receivableId);
    }
    public int deleteReceivableAttach(Long id) {
        return BaseDao.update("DELETE FROM prj_receivable_assess_attach WHERE id=?", id);
    }
    public int deleteReceivableAttachsByReceivable(Long receivableId) {
        return BaseDao.update("DELETE FROM prj_receivable_assess_attach WHERE receivable_id=?", receivableId);
    }
    public int updateReceivableAttachItemId(Long id, Long assessItemId, Long receivableId) {
        return BaseDao.update("UPDATE prj_receivable_assess_attach SET assess_item_id=?, receivable_id=? WHERE id=?",
                assessItemId, receivableId, id);
    }

    // --- 应付明细附件 ---
    public Long insertPayableAssessAttach(Long assessItemId, Long payableId, AttachFile f) {
        return BaseDao.insertReturnId("INSERT INTO prj_payable_assess_attach(assess_item_id, payable_id, file_name, file_path, file_size, uploaded_by) " +
                "VALUES (?, ?, ?, ?, ?, ?)", assessItemId, payableId, f.getFileName(), f.getFilePath(), f.getFileSize(), f.getUploadedBy());
    }
    public List<AttachFile> listPayableAttachsByItem(Long assessItemId) {
        return BaseDao.query("SELECT * FROM prj_payable_assess_attach WHERE assess_item_id=? ORDER BY uploaded_at DESC, id DESC",
                this::mapAttach, assessItemId);
    }
    public List<AttachFile> listPayableAttachsByPayable(Long payableId) {
        return BaseDao.query("SELECT * FROM prj_payable_assess_attach WHERE payable_id=? ORDER BY uploaded_at DESC, id DESC",
                this::mapAttach, payableId);
    }
    public int deletePayableAttach(Long id) {
        return BaseDao.update("DELETE FROM prj_payable_assess_attach WHERE id=?", id);
    }
    public int deletePayableAttachsByPayable(Long payableId) {
        return BaseDao.update("DELETE FROM prj_payable_assess_attach WHERE payable_id=?", payableId);
    }
    public int updatePayableAttachItemId(Long id, Long assessItemId, Long payableId) {
        return BaseDao.update("UPDATE prj_payable_assess_attach SET assess_item_id=?, payable_id=? WHERE id=?",
                assessItemId, payableId, id);
    }
}
