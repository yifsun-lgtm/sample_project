package com.proquip.ejb.dao;

import com.proquip.ejb.entity.product.Product;

import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 商品エンティティに対するデータアクセスオブジェクト。
 *
 * <p>商品の検索・登録・更新・削除に関するデータベースアクセスを提供する。
 * Criteria APIとJPQLの両方を使用しており、クエリ方式に一貫性がない。</p>
 *
 * <p>技術的負債 #18: 一部のメソッドはCriteria APIを使用し、
 * 他のメソッドはJPQLを使用している。本来はどちらか一方に統一すべき。</p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class ProductDao extends AbstractBaseDao<Product, Long> {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(ProductDao.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public ProductDao() {
        super();
    }

    /**
     * カテゴリIDで商品を検索する。
     *
     * <p>Criteria APIを使用して検索を実行する。</p>
     *
     * @param categoryId カテゴリID
     * @return 該当カテゴリに属する商品のリスト
     */
    public List<Product> findByCategory(Long categoryId) {
        LOG.log(Level.FINE, "カテゴリ別商品検索: categoryId={0}", categoryId);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);

        cq.select(root)
          .where(cb.equal(root.get("category").get("id"), categoryId))
          .orderBy(cb.asc(root.get("name")));

        return em.createQuery(cq).getResultList();
    }

    /**
     * キーワードで商品を検索する（Criteria API版）。
     *
     * <p>商品名および説明文に対して部分一致検索を行う。
     * Criteria APIを使用した動的クエリ構築を行っている。</p>
     *
     * <p>技術的負債: 前方一致ではなく部分一致（LIKE '%keyword%'）を使用しており、
     * インデックスが効かないため大量データでパフォーマンス問題が発生する。</p>
     *
     * @param keyword 検索キーワード
     * @return 検索結果の商品リスト
     */
    public List<Product> searchByKeyword(String keyword) {
        LOG.log(Level.FINE, "キーワード検索: keyword={0}", keyword);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            Predicate namePredicate = cb.like(cb.lower(root.get("name")), pattern);
            Predicate descPredicate = cb.like(cb.lower(root.get("description")), pattern);
            predicates.add(cb.or(namePredicate, descPredicate));
        }

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }
        cq.orderBy(cb.asc(root.get("name")));

        return em.createQuery(cq).getResultList();
    }

    /**
     * 有効な商品のみを取得する。
     *
     * <p>技術的負債: searchByKeyword() がCriteria APIなのに対し、
     * このメソッドはJPQLを使用している。クエリ方式に一貫性がない。</p>
     *
     * @return 有効な商品のリスト
     */
    public List<Product> findActiveProducts() {
        LOG.log(Level.FINE, "有効商品取得");
        TypedQuery<Product> query = em.createNamedQuery("Product.findActiveProducts", Product.class);
        return query.getResultList();
    }

    /**
     * 製造元IDで商品を検索する。
     *
     * <p>JPQLを使用して検索を実行する（Criteria APIと不統一）。</p>
     *
     * @param manufacturerId 製造元ID
     * @return 該当製造元の商品リスト
     */
    public List<Product> findByManufacturer(Long manufacturerId) {
        LOG.log(Level.FINE, "製造元別商品検索: manufacturerId={0}", manufacturerId);

        // 技術的負債: findByCategory()はCriteria APIなのにこちらはJPQL
        TypedQuery<Product> query = em.createQuery(
            "SELECT p FROM Product p WHERE p.manufacturer.id = :manufacturerId ORDER BY p.name",
            Product.class
        );
        query.setParameter("manufacturerId", manufacturerId);
        return query.getResultList();
    }

    /**
     * SKUコードで商品を検索する。
     *
     * <p>NamedQueryを使用して検索を実行する（さらに別のクエリ方式）。</p>
     *
     * @param skuCode SKUコード
     * @return 該当商品（存在しない場合は {@code null}）
     */
    public Product findBySkuCode(String skuCode) {
        LOG.log(Level.FINE, "SKUコード検索: sku={0}", skuCode);

        // 技術的負債: 他のメソッドとクエリ方式が異なる（NamedQuery使用）
        TypedQuery<Product> query = em.createNamedQuery("Product.findBySku", Product.class);
        query.setParameter("sku", skuCode);

        List<Product> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * ステータスと価格帯で商品を検索する（Criteria API版）。
     *
     * <p>複合条件による動的検索。Criteria APIの特性を活かしたメソッドだが、
     * 他のシンプルな検索メソッドとのAPI一貫性が欠けている。</p>
     *
     * @param status   商品ステータス（nullの場合は条件に含めない）
     * @param minPrice 最低価格（nullの場合は条件に含めない）
     * @param maxPrice 最高価格（nullの場合は条件に含めない）
     * @return 検索結果の商品リスト
     */
    public List<Product> findByStatusAndPriceRange(String status,
                                                   java.math.BigDecimal minPrice,
                                                   java.math.BigDecimal maxPrice) {
        LOG.log(Level.FINE, "ステータス・価格帯検索: status={0}, min={1}, max={2}",
                new Object[]{status, minPrice, maxPrice});

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();

        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("unitPrice"), minPrice));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("unitPrice"), maxPrice));
        }

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }
        cq.orderBy(cb.asc(root.get("unitPrice")));

        return em.createQuery(cq).getResultList();
    }
}
