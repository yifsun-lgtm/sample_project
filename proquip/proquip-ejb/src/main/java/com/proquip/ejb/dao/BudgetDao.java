package com.proquip.ejb.dao;

import com.proquip.ejb.entity.pricing.Budget;

import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 予算エンティティに対するデータアクセスオブジェクト。
 *
 * <p>予算マスタの検索に関するデータベースアクセスを提供する。
 * エンティティに定義された {@code @NamedQuery} を使用するアプローチを取っている。
 * これは {@link ProductDao} のCriteria APIや {@link SupplierDao} のインラインJPQLとは
 * 異なる方式であり、プロジェクト内のクエリ方式の不統一を助長している。</p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class BudgetDao extends AbstractBaseDao<Budget, Long> {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(BudgetDao.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public BudgetDao() {
        super();
    }

    /**
     * 部門IDで予算を検索する。
     *
     * <p>技術的負債: Budget エンティティの departmentId が生のIDであるため、
     * 部門名の取得には別途クエリが必要。</p>
     *
     * @param departmentId 部門ID
     * @return 該当部門の予算リスト
     */
    public List<Budget> findByDepartment(Long departmentId) {
        LOG.log(Level.FINE, "部門別予算検索: departmentId={0}", departmentId);

        TypedQuery<Budget> query = em.createQuery(
            "SELECT b FROM Budget b WHERE b.departmentId = :departmentId ORDER BY b.fiscalYear DESC",
            Budget.class
        );
        query.setParameter("departmentId", departmentId);
        return query.getResultList();
    }

    /**
     * 会計年度で予算を検索する。
     *
     * <p>エンティティの @NamedQuery を使用する。</p>
     *
     * @param fiscalYear 会計年度
     * @return 該当年度の予算リスト
     */
    public List<Budget> findByFiscalYear(Integer fiscalYear) {
        LOG.log(Level.FINE, "会計年度別予算検索: fiscalYear={0}", fiscalYear);

        TypedQuery<Budget> query = em.createNamedQuery("Budget.findByFiscalYear", Budget.class);
        query.setParameter("fiscalYear", fiscalYear);
        return query.getResultList();
    }

    /**
     * 有効な予算のみを取得する。
     *
     * <p>ステータスが "ACTIVE" または "APPROVED" の予算を返す。
     * @NamedQuery ではなくインラインJPQLを使用（方式の不統一）。</p>
     *
     * @return 有効な予算のリスト
     */
    public List<Budget> findActiveBudgets() {
        LOG.log(Level.FINE, "有効予算取得");

        // 技術的負債: NamedQueryを使うメソッドとインラインJPQLが混在
        TypedQuery<Budget> query = em.createQuery(
            "SELECT b FROM Budget b " +
            "WHERE b.status IN ('ACTIVE', 'APPROVED') " +
            "ORDER BY b.fiscalYear DESC, b.name",
            Budget.class
        );
        return query.getResultList();
    }

    /**
     * 部門IDと会計年度で予算を検索する。
     *
     * <p>@NamedQuery を使用する。</p>
     *
     * @param departmentId 部門ID
     * @param fiscalYear   会計年度
     * @return 該当する予算リスト
     */
    public List<Budget> findByDepartmentAndYear(Long departmentId, Integer fiscalYear) {
        LOG.log(Level.FINE, "部門・年度別予算検索: departmentId={0}, fiscalYear={1}",
                new Object[]{departmentId, fiscalYear});

        TypedQuery<Budget> query = em.createNamedQuery(
            "Budget.findByDepartmentAndYear", Budget.class
        );
        query.setParameter("departmentId", departmentId);
        query.setParameter("fiscalYear", fiscalYear);
        return query.getResultList();
    }
}
