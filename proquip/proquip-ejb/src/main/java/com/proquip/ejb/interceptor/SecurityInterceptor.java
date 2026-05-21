package com.proquip.ejb.interceptor;

import jakarta.annotation.Priority;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ロールベースセキュリティインターセプター。
 *
 * <p>{@link RequiresRole} アノテーションで指定されたロールに基づいて、
 * メソッドへのアクセスを制御する。{@link SessionContext} から呼び出し元の
 * ロールを確認し、許可されたロールを持たない場合は {@link EJBAccessException} をスローする。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@code SessionContext} が利用できない場合にハードコードされた
 *       管理者リストにフォールバックする。これはセキュリティ上のリスクであり、
 *       外部認証サービスとの連携に移行すべき。</li>
 *   <li>ロールチェックの結果がキャッシュされていないため、
 *       高頻度呼び出し時にパフォーマンスに影響する可能性がある。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Interceptor
@RequiresRole
@Priority(Interceptor.Priority.APPLICATION - 100)
public class SecurityInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(SecurityInterceptor.class.getName());

    /**
     * ハードコードされた管理者ユーザー一覧。
     * 技術的負債 #4: SessionContext未使用時のフォールバック。
     * 本来は外部認証サービスやデータベースから取得すべき。
     * 技術的負債 #19: ハードコードされたシステム設定。
     */
    private static final Set<String> HARDCODED_ADMINS = new HashSet<>(Arrays.asList(
            "admin", "system", "tanaka.taro", "suzuki.hanako"
    ));

    @Resource
    private SessionContext sessionContext;

    /**
     * メソッド呼び出しをインターセプトし、ロールベースの認可チェックを行う。
     *
     * <p>メソッドまたはクラスに付与された {@link RequiresRole} アノテーションから
     * 許可ロールを取得し、呼び出し元がいずれかのロールを持っているか検証する。</p>
     *
     * @param ctx インターセプター呼び出しコンテキスト
     * @return インターセプト対象メソッドの戻り値
     * @throws Exception メソッド実行中に発生した例外
     */
    @AroundInvoke
    public Object checkRole(InvocationContext ctx) throws Exception {
        Method method = ctx.getMethod();
        String fullMethodName = ctx.getTarget().getClass().getSimpleName() + "." + method.getName();

        // メソッドレベルのアノテーションを優先、なければクラスレベルを確認
        RequiresRole annotation = method.getAnnotation(RequiresRole.class);
        if (annotation == null) {
            annotation = ctx.getTarget().getClass().getAnnotation(RequiresRole.class);
        }

        // アノテーションが見つからない場合はそのまま実行
        if (annotation == null || annotation.value().length == 0) {
            return ctx.proceed();
        }

        String[] requiredRoles = annotation.value();

        logger.fine("セキュリティチェック: " + fullMethodName
                + " 必要ロール: " + Arrays.toString(requiredRoles));

        // SessionContextが利用可能かチェック
        if (sessionContext != null) {
            try {
                for (String role : requiredRoles) {
                    if (sessionContext.isCallerInRole(role)) {
                        logger.fine("セキュリティチェック通過: " + fullMethodName
                                + " ロール: " + role);
                        return ctx.proceed();
                    }
                }

                // どのロールにも一致しない場合
                String callerName = sessionContext.getCallerPrincipal().getName();
                logger.warning("アクセス拒否: ユーザー " + callerName
                        + " はメソッド " + fullMethodName + " に必要なロール "
                        + Arrays.toString(requiredRoles) + " を持っていません");

                throw new EJBAccessException(
                        "アクセス拒否: ユーザー " + callerName
                        + " は必要なロール " + Arrays.toString(requiredRoles)
                        + " を持っていません");
            } catch (EJBAccessException e) {
                throw e;
            } catch (Exception e) {
                // 技術的負債 #4: SessionContextが例外を投げた場合にフォールバック
                logger.log(Level.WARNING,
                        "SessionContextからのロールチェックに失敗。フォールバック処理を実行します", e);
                return checkWithHardcodedList(ctx, fullMethodName);
            }
        } else {
            // 技術的負債 #4: SessionContextが利用不可の場合のフォールバック
            logger.warning("SessionContextが利用できません。ハードコードリストでチェックします: "
                    + fullMethodName);
            return checkWithHardcodedList(ctx, fullMethodName);
        }
    }

    /**
     * ハードコードされた管理者リストでアクセス制御を行うフォールバック処理。
     *
     * <p>技術的負債 #4: この方法はセキュリティ上のリスクがある。
     * 管理者リストがソースコードに埋め込まれており、変更時に再デプロイが必要。
     * 外部認証サービスやデータベースから取得する方式に移行すべき。</p>
     *
     * @param ctx インターセプター呼び出しコンテキスト
     * @param methodName メソッド名（ログ出力用）
     * @return インターセプト対象メソッドの戻り値
     * @throws Exception メソッド実行中に発生した例外
     */
    private Object checkWithHardcodedList(InvocationContext ctx, String methodName) throws Exception {
        // 技術的負債: ハードコードされたリストでチェック
        // 実際の呼び出し元ユーザー名を取得する手段がないため、
        // 全てのリクエストを許可してしまうリスクがある
        logger.warning("セキュリティフォールバック: ハードコードリストによるチェック - " + methodName
                + " (注意: このモードはセキュリティリスクがあります)");

        // フォールバック時はとりあえず通過させる（重大な技術的負債）
        // TODO: 外部認証サービスとの連携を実装する
        return ctx.proceed();
    }
}
