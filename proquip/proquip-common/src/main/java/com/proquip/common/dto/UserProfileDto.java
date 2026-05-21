package com.proquip.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ユーザープロファイルデータ転送オブジェクト。
 *
 * <p>ユーザーの基本情報をプレゼンテーション層やAPI応答として転送するためのクラス。
 * セキュリティ上、パスワードやKeycloak内部情報は含まない。</p>
 *
 * @author ProQuip開発チーム
 */
public class UserProfileDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ユーザーID */
    private Long id;

    /** ユーザー名（Keycloak ID） */
    private String username;

    /** 名 */
    private String firstName;

    /** 姓 */
    private String lastName;

    /** 表示名 */
    private String displayName;

    /** メールアドレス */
    private String email;

    /** 部門ID */
    private Long departmentId;

    /** 部門名 */
    private String departmentName;

    /** 部門名（フロントエンド互換） */
    private String department;

    /** ロール一覧（文字列） */
    private List<String> roleNames = new ArrayList<>();

    /** ロール一覧（オブジェクト形式、フロントエンド互換） */
    private List<Map<String, Object>> roles = new ArrayList<>();

    /** 有効フラグ */
    private boolean active;

    /** 有効フラグ（フロントエンド互換） */
    private boolean enabled;

    /** 作成日時 */
    private Date createdAt;

    /**
     * デフォルトコンストラクタ。
     */
    public UserProfileDto() {
    }

    // --- Getter / Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public List<Map<String, Object>> getRoles() {
        return roles;
    }

    public void setRoles(List<Map<String, Object>> roles) {
        this.roles = roles;
    }

    public List<String> getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(List<String> roleNames) {
        this.roleNames = roleNames;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
