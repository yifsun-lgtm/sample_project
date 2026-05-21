package com.proquip.ejb.entity.organization;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "delegation_rule")
@NamedQueries({
    @NamedQuery(
        name = "DelegationRule.findByDelegateFrom",
        query = "SELECT dr FROM DelegationRule dr WHERE dr.delegateFrom.id = :userId ORDER BY dr.validFrom DESC"
    ),
    @NamedQuery(
        name = "DelegationRule.findActiveDelegations",
        query = "SELECT dr FROM DelegationRule dr WHERE dr.delegateTo.id = :userId AND dr.validFrom <= :currentDate AND dr.validTo >= :currentDate"
    )
})
public class DelegationRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "valid_from", nullable = false)
    private Date validFrom;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "valid_until", nullable = false)
    private Date validUntil;

    @NotNull
    @Size(max = 50)
    @Column(name = "delegation_type", nullable = false, length = 50)
    private String delegationType;

    @Column(name = "max_amount", precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "reason")
    private String reason;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @NotNull
    @Size(max = 50)
    @Column(name = "scope", nullable = false, length = 50)
    private String scope;

    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "valid_to", nullable = false)
    private Date validTo;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delegator_id", nullable = false)
    private UserProfile delegateFrom;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delegate_id", nullable = false)
    private UserProfile delegateTo;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delegate_from_user_id", nullable = false)
    private UserProfile delegateFromUser;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delegate_to_user_id", nullable = false)
    private UserProfile delegateToUser;

    /**
     * デフォルトコンストラクタ。
     */
    public DelegationRule() {
        super();
    }

    // --- Getter / Setter ---

    public Date getValidFrom() { return validFrom; }
    public void setValidFrom(Date validFrom) { this.validFrom = validFrom; }

    public Date getValidUntil() { return validUntil; }
    public void setValidUntil(Date validUntil) { this.validUntil = validUntil; }

    public String getDelegationType() { return delegationType; }
    public void setDelegationType(String delegationType) { this.delegationType = delegationType; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public Date getValidTo() { return validTo; }
    public void setValidTo(Date validTo) { this.validTo = validTo; }

    public UserProfile getDelegateFrom() { return delegateFrom; }
    public void setDelegateFrom(UserProfile delegateFrom) { this.delegateFrom = delegateFrom; }

    public UserProfile getDelegateTo() { return delegateTo; }
    public void setDelegateTo(UserProfile delegateTo) { this.delegateTo = delegateTo; }

    public UserProfile getDelegateFromUser() { return delegateFromUser; }
    public void setDelegateFromUser(UserProfile delegateFromUser) { this.delegateFromUser = delegateFromUser; }

    public UserProfile getDelegateToUser() { return delegateToUser; }
    public void setDelegateToUser(UserProfile delegateToUser) { this.delegateToUser = delegateToUser; }

    @Override
    public String toString() {
        return "DelegationRule{id=" + getId() + ", scope='" + scope + "', validFrom=" + validFrom + ", validTo=" + validTo + '}';
    }
}
