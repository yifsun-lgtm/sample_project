package com.proquip.ejb.entity.organization;

import com.proquip.ejb.entity.base.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/**
 * ユーザープロファイルエンティティ。
 * <p>
 * システム利用者の基本情報を保持する。Keycloak の外部IDと社員番号で
 * 一意に識別される。所属部門および割り当てられたロールへの関連を持つ。
 * </p>
 * <p>
 * 【技術的負債】toString() が遅延ロード対象のリレーション（roles）を含んでおり、
 * トランザクション外で呼ばれた場合に LazyInitializationException が発生する可能性がある。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "user_profile")
@NamedQueries({
    @NamedQuery(
        name = "UserProfile.findByKeycloakId",
        query = "SELECT u FROM UserProfile u WHERE u.keycloakId = :keycloakId"
    ),
    @NamedQuery(
        name = "UserProfile.findByEmployeeNumber",
        query = "SELECT u FROM UserProfile u WHERE u.employeeNumber = :employeeNumber"
    ),
    @NamedQuery(
        name = "UserProfile.findByDepartment",
        query = "SELECT u FROM UserProfile u WHERE u.department.id = :departmentId ORDER BY u.lastName, u.firstName"
    ),
    @NamedQuery(
        name = "UserProfile.findByStatus",
        query = "SELECT u FROM UserProfile u WHERE u.active = :active ORDER BY u.lastName, u.firstName"
    ),
    @NamedQuery(
        name = "UserProfile.findByEmail",
        query = "SELECT u FROM UserProfile u WHERE u.email = :email"
    )
})
public class UserProfile extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** Keycloak のユーザーID（外部ID、一意制約） */
    @NotNull
    @Size(max = 100)
    @Column(name = "keycloak_id", unique = true, nullable = false, length = 100)
    private String keycloakId;

    /** 社員番号（一意制約） */
    @NotNull
    @Size(max = 20)
    @Column(name = "employee_number", unique = true, nullable = false, length = 20)
    private String employeeNumber;

    /** 名（ファーストネーム） */
    @NotNull
    @Size(max = 50)
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    /** 姓（ラストネーム） */
    @NotNull
    @Size(max = 50)
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    /** メールアドレス */
    @NotNull
    @Email
    @Size(max = 150)
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /** 電話番号 */
    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * ステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: ACTIVE, INACTIVE, SUSPENDED
     * </p>
     */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** 所属部門への参照 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** 割り当てられたロールのセット（多対多） */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_role_mapping",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<Role>();

    /**
     * デフォルトコンストラクタ。
     */
    public UserProfile() {
        super();
    }

    // --- Getter / Setter ---

    public String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return active ? "ACTIVE" : "INACTIVE";
    }

    public void setStatus(String status) {
        this.active = "ACTIVE".equals(status);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    /**
     * 技術的負債: toString() がすべてのフィールドを含んでおり、
     * 遅延ロード対象の roles コレクションにアクセスする。
     * トランザクション外で呼ばれた場合に LazyInitializationException が発生する。
     */
    @Override
    public String toString() {
        return "UserProfile{" +
                "keycloakId='" + keycloakId + '\'' +
                ", employeeNumber='" + employeeNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", active=" + active +
                '}';
    }
}
