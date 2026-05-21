package com.proquip.ejb.interceptor;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * パフォーマンス監視用のインターセプターバインディングアノテーション。
 *
 * <p>このアノテーションを付与したメソッドまたはクラスに対して、
 * {@link PerformanceInterceptor} による実行時間計測が自動的に適用される。</p>
 *
 * @author ProQuip開発チーム
 * @see PerformanceInterceptor
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Monitored {
}
