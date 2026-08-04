package com.rebate.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 登录态上下文（存于 Session）
 */
public class UserContext implements Serializable {

    private Long id;
    private String workNo;
    private String name;
    private String loginName;
    private Long deptId;
    private String deptName;
    private Long companyId;
    private String companyName;
    private Integer isAdmin;
    private Set<String> permCodes = new HashSet<>();
    private Set<String> roleCodes = new HashSet<>();

    public boolean hasPerm(String perm) {
        if (isAdmin != null && isAdmin == 1) return true;
        return permCodes.contains(perm);
    }

    public boolean isAdmin() {
        return isAdmin != null && isAdmin == 1;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWorkNo() { return workNo; }
    public void setWorkNo(String workNo) { this.workNo = workNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public Integer getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Integer isAdmin) { this.isAdmin = isAdmin; }
    public Set<String> getPermCodes() { return permCodes; }
    public void setPermCodes(Set<String> permCodes) { this.permCodes = permCodes; }
    public Set<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(Set<String> roleCodes) { this.roleCodes = roleCodes; }
}
