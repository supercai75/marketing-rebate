package com.rebate.dao;

import com.rebate.model.TeamTarget;
import com.rebate.model.AttachFile;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 协议子表：考核目标、备注附件、协议附件
 */
public class AgreementSubDao {

    public void insertTeamTarget(TeamTarget t) {
        BaseDao.update("INSERT INTO prj_upstream_team_target(agreement_id, target_name, owner, requirement, calc_standard, reward_standard, sort_no) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)", t.getAgreementId(), t.getTargetName(), t.getOwner(), t.getRequirement(),
                t.getCalcStandard(), t.getRewardStandard(), t.getSortNo());
    }

    public void clearUpstreamTeamTargets(long agreementId) {
        BaseDao.update("DELETE FROM prj_upstream_team_target WHERE agreement_id=?", agreementId);
    }

    public List<TeamTarget> listUpstreamTeamTargets(long agreementId) {
        return BaseDao.query("SELECT * FROM prj_upstream_team_target WHERE agreement_id=? ORDER BY sort_no", this::mapTarget, agreementId);
    }

    public void insertDownstreamTeamTarget(TeamTarget t) {
        BaseDao.update("INSERT INTO prj_downstream_team_target(agreement_id, target_name, owner, requirement, calc_standard, reward_standard, sort_no) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)", t.getAgreementId(), t.getTargetName(), t.getOwner(), t.getRequirement(),
                t.getCalcStandard(), t.getRewardStandard(), t.getSortNo());
    }

    public void clearDownstreamTeamTargets(long agreementId) {
        BaseDao.update("DELETE FROM prj_downstream_team_target WHERE agreement_id=?", agreementId);
    }

    public List<TeamTarget> listDownstreamTeamTargets(long agreementId) {
        return BaseDao.query("SELECT * FROM prj_downstream_team_target WHERE agreement_id=? ORDER BY sort_no", this::mapTarget, agreementId);
    }

    public Long insertUpstreamRemarkFile(AttachFile f) {
        return BaseDao.insertReturnId("INSERT INTO prj_upstream_remark_file(agreement_id, file_type, file_name, file_path, file_size, uploaded_by) " +
                "VALUES (?, ?, ?, ?, ?, ?)", f.getAgreementId(), f.getFileType(), f.getFileName(), f.getFilePath(), f.getFileSize(), f.getUploadedBy());
    }

    public Long insertUpstreamAttach(AttachFile f) {
        return BaseDao.insertReturnId("INSERT INTO prj_upstream_attach(agreement_id, attach_type, file_name, file_path, file_size, uploaded_by) " +
                "VALUES (?, ?, ?, ?, ?, ?)", f.getAgreementId(), f.getAttachType(), f.getFileName(), f.getFilePath(), f.getFileSize(), f.getUploadedBy());
    }

    public Long insertDownstreamRemarkFile(AttachFile f) {
        return BaseDao.insertReturnId("INSERT INTO prj_downstream_remark_file(agreement_id, file_type, file_name, file_path, file_size, uploaded_by) " +
                "VALUES (?, ?, ?, ?, ?, ?)", f.getAgreementId(), f.getFileType(), f.getFileName(), f.getFilePath(), f.getFileSize(), f.getUploadedBy());
    }

    public Long insertDownstreamAttach(AttachFile f) {
        return BaseDao.insertReturnId("INSERT INTO prj_downstream_attach(agreement_id, attach_type, file_name, file_path, file_size, uploaded_by) " +
                "VALUES (?, ?, ?, ?, ?, ?)", f.getAgreementId(), f.getAttachType(), f.getFileName(), f.getFilePath(), f.getFileSize(), f.getUploadedBy());
    }

    public List<AttachFile> listUpstreamRemarkFiles(long agreementId) {
        return BaseDao.query("SELECT * FROM prj_upstream_remark_file WHERE agreement_id=? ORDER BY uploaded_at DESC", this::mapFile, agreementId);
    }

    public List<AttachFile> listUpstreamAttaches(long agreementId) {
        return BaseDao.query("SELECT * FROM prj_upstream_attach WHERE agreement_id=? ORDER BY uploaded_at DESC", this::mapFile, agreementId);
    }

    public List<AttachFile> listDownstreamRemarkFiles(long agreementId) {
        return BaseDao.query("SELECT * FROM prj_downstream_remark_file WHERE agreement_id=? ORDER BY uploaded_at DESC", this::mapFile, agreementId);
    }

    public List<AttachFile> listDownstreamAttaches(long agreementId) {
        return BaseDao.query("SELECT * FROM prj_downstream_attach WHERE agreement_id=? ORDER BY uploaded_at DESC", this::mapFile, agreementId);
    }

    public int deleteUpstreamRemarkFile(long id) { return BaseDao.update("DELETE FROM prj_upstream_remark_file WHERE id=?", id); }
    public int deleteUpstreamAttach(long id) { return BaseDao.update("DELETE FROM prj_upstream_attach WHERE id=?", id); }
    public int deleteDownstreamRemarkFile(long id) { return BaseDao.update("DELETE FROM prj_downstream_remark_file WHERE id=?", id); }
    public int deleteDownstreamAttach(long id) { return BaseDao.update("DELETE FROM prj_downstream_attach WHERE id=?", id); }

    private TeamTarget mapTarget(ResultSet rs) throws SQLException {
        TeamTarget t = new TeamTarget();
        t.setId(rs.getLong("id"));
        t.setAgreementId(rs.getLong("agreement_id"));
        t.setTargetName(rs.getString("target_name"));
        t.setOwner(rs.getString("owner"));
        t.setRequirement(rs.getString("requirement"));
        t.setCalcStandard(rs.getString("calc_standard"));
        t.setRewardStandard(rs.getString("reward_standard"));
        t.setSortNo(rs.getInt("sort_no"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        return t;
    }

    private AttachFile mapFile(ResultSet rs) throws SQLException {
        AttachFile f = new AttachFile();
        f.setId(rs.getLong("id"));
        f.setAgreementId(rs.getLong("agreement_id"));
        try { f.setFileType(rs.getString("file_type")); } catch (Exception ignore) {}
        try { f.setAttachType(rs.getString("attach_type")); } catch (Exception ignore) {}
        f.setFileName(rs.getString("file_name"));
        f.setFilePath(rs.getString("file_path"));
        f.setFileSize(rs.getObject("file_size") == null ? null : rs.getLong("file_size"));
        f.setUploadedBy(rs.getObject("uploaded_by") == null ? null : rs.getLong("uploaded_by"));
        f.setUploadedAt(rs.getTimestamp("uploaded_at"));
        return f;
    }
}
