package com.proquip.ejb.entity.organization;

import com.proquip.ejb.entity.base.AuditableEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * 部門エンティティ。
 * <p>
 * 組織の部門階層を表す。自己参照による親子関係を持ち、
 * ツリー構造で組織図を構成する。各部門にはコストセンターが割り当てられ、
 * 所属ユーザーのリストを保持する。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "department")
@NamedQueries({
    @NamedQuery(
        name = "Department.findByCode",
        query = "SELECT d FROM Department d WHERE d.code = :code"
    ),
    @NamedQuery(
        name = "Department.findRootDepartments",
        query = "SELECT d FROM Department d WHERE d.parent IS NULL ORDER BY d.name"
    ),
    @NamedQuery(
        name = "Department.findByParent",
        query = "SELECT d FROM Department d WHERE d.parent.id = :parentId ORDER BY d.name"
    )
})
public class Department extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 部門コード（一意制約） */
    @NotNull
    @Size(max = 20)
    @Column(name = "department_code", unique = true, nullable = false, length = 20)
    private String code;

    /** 部門名 */
    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** コストセンター番号 */
    @Size(max = 20)
    @Column(name = "cost_center", length = 20)
    private String costCenter;

    @Column(name = "level", nullable = false)
    private int level = 0;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** 親部門への参照（自己参照） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    /** 子部門のリスト（自己参照） */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Department> children = new ArrayList<Department>();

    /** 所属ユーザーのリスト */
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<UserProfile> users = new ArrayList<UserProfile>();

    /**
     * デフォルトコンストラクタ。
     */
    public Department() {
        super();
    }

    // --- Getter / Setter ---

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public Department getParent() {
        return parent;
    }

    public void setParent(Department parent) {
        this.parent = parent;
    }

    public List<Department> getChildren() {
        return children;
    }

    public void setChildren(List<Department> children) {
        this.children = children;
    }

    public List<UserProfile> getUsers() {
        return users;
    }

    public void setUsers(List<UserProfile> users) {
        this.users = users;
    }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "Department{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", costCenter='" + costCenter + '\'' +
                '}';
    }
}
