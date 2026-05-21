package com.proquip.ejb.entity.organization;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/**
 * ロールエンティティ。
 * <p>
 * システム内のユーザーロールを定義する。
 * ロール名で一意に識別され、複数のパーミッションを保持する。
 * ユーザーとの多対多関係は {@link UserProfile} 側で管理される。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "role")
@NamedQueries({
    @NamedQuery(
        name = "Role.findByName",
        query = "SELECT r FROM Role r WHERE r.name = :name"
    ),
    @NamedQuery(
        name = "Role.findAll",
        query = "SELECT r FROM Role r ORDER BY r.name"
    )
})
public class Role extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** ロール名（一意制約） */
    @NotNull
    @Size(max = 50)
    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name;

    /** ロールの説明 */
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    /** このロールに割り当てられたパーミッションのセット（多対多） */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permission_mapping",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<Permission>();

    /** このロールが割り当てられたユーザーのセット（多対多、逆側） */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<UserProfile> users = new HashSet<UserProfile>();

    /**
     * デフォルトコンストラクタ。
     */
    public Role() {
        super();
    }

    // --- Getter / Setter ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<UserProfile> getUsers() {
        return users;
    }

    public void setUsers(Set<UserProfile> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "Role{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
