package com.proquip.ejb.entity.organization;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/**
 * パーミッションエンティティ。
 * <p>
 * システム内のアクセス制御に使用される権限を定義する。
 * パーミッションコードで一意に識別され、リソースとアクションの組み合わせで
 * 具体的な操作権限を表す。ロールとの多対多関係は {@link Role} 側で管理される。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "permission")
@NamedQueries({
    @NamedQuery(
        name = "Permission.findByCode",
        query = "SELECT p FROM Permission p WHERE p.code = :code"
    ),
    @NamedQuery(
        name = "Permission.findByResource",
        query = "SELECT p FROM Permission p WHERE p.resource = :resource ORDER BY p.action"
    ),
    @NamedQuery(
        name = "Permission.findAll",
        query = "SELECT p FROM Permission p ORDER BY p.resource, p.action"
    )
})
public class Permission extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** パーミッションコード（一意制約、例: PURCHASE_ORDER_CREATE） */
    @NotNull
    @Size(max = 100)
    @Column(name = "permission_code", unique = true, nullable = false, length = 100)
    private String code;

    /** パーミッション名称（説明） */
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String name;

    /** 対象リソース（例: PURCHASE_ORDER, INVENTORY, SUPPLIER） */
    @Size(max = 100)
    @Column(name = "resource", length = 100)
    private String resource;

    /** 許可するアクション（例: CREATE, READ, UPDATE, DELETE, APPROVE） */
    @Size(max = 50)
    @Column(name = "action", length = 50)
    private String action;

    /** このパーミッションが割り当てられたロールのセット（多対多、逆側） */
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<Role> roles = new HashSet<Role>();

    /**
     * デフォルトコンストラクタ。
     */
    public Permission() {
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

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", resource='" + resource + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}
