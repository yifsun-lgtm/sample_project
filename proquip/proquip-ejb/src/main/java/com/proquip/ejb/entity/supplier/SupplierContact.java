package com.proquip.ejb.entity.supplier;

import com.proquip.ejb.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * 仕入先の連絡先担当者を表すエンティティ。
 *
 * <p>仕入先に所属する担当者の連絡先情報を管理する。
 * {@code isPrimary} フラグにより、主担当者を識別する。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "supplier_contact")
@NamedQueries({
    @NamedQuery(
        name = "SupplierContact.findBySupplier",
        query = "SELECT sc FROM SupplierContact sc WHERE sc.supplier.id = :supplierId ORDER BY sc.isPrimary DESC, sc.lastName"
    ),
    @NamedQuery(
        name = "SupplierContact.findPrimaryBySupplier",
        query = "SELECT sc FROM SupplierContact sc WHERE sc.supplier.id = :supplierId AND sc.isPrimary = true"
    )
})
public class SupplierContact extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 担当者名（名） */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /** 担当者名（姓） */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** メールアドレス */
    @Column(name = "email", length = 200)
    private String email;

    /** 電話番号 */
    @Column(name = "phone", length = 30)
    private String phone;

    /** 主担当者フラグ */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    /** 所属部署 */
    @Column(name = "department", length = 100)
    private String department;

    /** 所属仕入先 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierContact() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 担当者名（名）を返す。
     *
     * @return 名
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * 担当者名（名）を設定する。
     *
     * @param firstName 名
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * 担当者名（姓）を返す。
     *
     * @return 姓
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * 担当者名（姓）を設定する。
     *
     * @param lastName 姓
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * メールアドレスを返す。
     *
     * @return メールアドレス
     */
    public String getEmail() {
        return email;
    }

    /**
     * メールアドレスを設定する。
     *
     * @param email メールアドレス
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 電話番号を返す。
     *
     * @return 電話番号
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 電話番号を設定する。
     *
     * @param phone 電話番号
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 主担当者かどうかを返す。
     *
     * @return 主担当者の場合 {@code true}
     */
    public boolean isPrimary() {
        return isPrimary;
    }

    /**
     * 主担当者フラグを設定する。
     *
     * @param primary 主担当者フラグ
     */
    public void setPrimary(boolean primary) {
        this.isPrimary = primary;
    }

    /**
     * 所属部署を返す。
     *
     * @return 所属部署
     */
    public String getDepartment() {
        return department;
    }

    /**
     * 所属部署を設定する。
     *
     * @param department 所属部署
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * 所属仕入先を返す。
     *
     * @return 仕入先
     */
    public Supplier getSupplier() {
        return supplier;
    }

    /**
     * 所属仕入先を設定する。
     *
     * @param supplier 仕入先
     */
    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    @Override
    public String toString() {
        return "SupplierContact{" +
                "id=" + getId() +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", isPrimary=" + isPrimary +
                ", department='" + department + '\'' +
                '}';
    }
}
