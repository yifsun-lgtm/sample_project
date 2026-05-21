package com.proquip.ejb.interceptor;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 監査ログ記録用のインターセプターバインディングアノテーション。
 *
 * <p>このアノテーションを付与したメソッドまたはクラスに対して、
 * {@link AuditInterceptor} による監査ログ記録が自動的に適用される。</p>
 *
 * @author ProQuip開発チーム
 * @see AuditInterceptor
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Audited {
}
