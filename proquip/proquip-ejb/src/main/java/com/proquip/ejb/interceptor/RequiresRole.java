package com.proquip.ejb.interceptor;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ロールベースアクセス制御用のアノテーション。
 *
 * <p>このアノテーションを付与したメソッドまたはクラスに対して、
 * {@link SecurityInterceptor} による認可チェックが自動的に適用される。
 * {@code value} に許可されるロール名の配列を指定する。</p>
 *
 * <p>使用例:
 * <pre>
 * {@code @RequiresRole({"ADMIN", "MANAGER"})}
 * public void approveOrder(Long orderId) { ... }
 * </pre>
 * </p>
 *
 * @author ProQuip開発チーム
 * @see SecurityInterceptor
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresRole {

    /**
     * 許可されるロール名の配列。
     * いずれか1つのロールを持っていればアクセスが許可される。
     *
     * @return 許可ロール名の配列
     */
    @Nonbinding
    String[] value() default {};
}
