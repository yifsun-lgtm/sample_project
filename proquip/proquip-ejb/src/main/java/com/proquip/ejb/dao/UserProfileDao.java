package com.proquip.ejb.dao;

import com.proquip.ejb.entity.organization.UserProfile;

import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ユーザープロファイルエンティティに対するデータアクセスオブジェクト。
 *
 * <p>ユーザー情報の検索に関するデータベースアクセスを提供する。
 * NamedQueryとインラインJPQLの両方を使用している。</p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class UserProfileDao extends AbstractBaseDao<UserProfile, Long> {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(UserProfileDao.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public UserProfileDao() {
        super();
    }

    /**
     * ユーザー名（Keycloak ID）でユーザーを検索する。
     *
     * @param username Keycloak ユーザーID
     * @return 該当ユーザー（見つからない場合は {@code null}）
     */
    public UserProfile findByUsername(String username) {
        LOG.log(Level.FINE, "ユーザー名検索: username={0}", username);

        TypedQuery<UserProfile> query = em.createNamedQuery(
            "UserProfile.findByKeycloakId", UserProfile.class
        );
        query.setParameter("keycloakId", username);
        List<UserProfile> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * メールアドレスでユーザーを検索する。
     *
     * @param email メールアドレス
     * @return 該当ユーザー（見つからない場合は {@code null}）
     */
    public UserProfile findByEmail(String email) {
        LOG.log(Level.FINE, "メールアドレス検索: email={0}", email);

        TypedQuery<UserProfile> query = em.createNamedQuery(
            "UserProfile.findByEmail", UserProfile.class
        );
        query.setParameter("email", email);
        List<UserProfile> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 部門IDでユーザーを検索する。
     *
     * @param departmentId 部門ID
     * @return 該当部門に所属するユーザーのリスト
     */
    public List<UserProfile> findByDepartment(Long departmentId) {
        LOG.log(Level.FINE, "部門別ユーザー検索: departmentId={0}", departmentId);

        TypedQuery<UserProfile> query = em.createNamedQuery(
            "UserProfile.findByDepartment", UserProfile.class
        );
        query.setParameter("departmentId", departmentId);
        return query.getResultList();
    }

    /**
     * 有効なユーザーのみを取得する。
     *
     * <p>ステータスが "ACTIVE" のユーザーを返す。</p>
     *
     * @return 有効なユーザーのリスト
     */
    public List<UserProfile> findActiveUsers() {
        LOG.log(Level.FINE, "有効ユーザー取得");

        TypedQuery<UserProfile> query = em.createNamedQuery(
            "UserProfile.findByStatus", UserProfile.class
        );
        query.setParameter("status", "ACTIVE");
        return query.getResultList();
    }

    /**
     * 社員番号でユーザーを検索する。
     *
     * @param employeeNumber 社員番号
     * @return 該当ユーザー（見つからない場合は {@code null}）
     */
    public UserProfile findByEmployeeNumber(String employeeNumber) {
        LOG.log(Level.FINE, "社員番号検索: employeeNumber={0}", employeeNumber);

        TypedQuery<UserProfile> query = em.createNamedQuery(
            "UserProfile.findByEmployeeNumber", UserProfile.class
        );
        query.setParameter("employeeNumber", employeeNumber);
        List<UserProfile> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
