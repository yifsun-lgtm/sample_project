package com.proquip.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * 日付操作に関するユーティリティクラス。
 *
 * <p>【技術的負債 - 重大】このクラスには以下の深刻な問題がある：
 * <ul>
 *   <li>java.util.Date / Calendar を使用しており、Java 8のjava.time APIに移行すべき</li>
 *   <li>SimpleDateFormatをstaticフィールドで保持しており、スレッドセーフではない</li>
 *   <li>parseDateがParseExceptionを握りつぶしてnullを返す（サイレント失敗）</li>
 *   <li>後から追加されたtoLocalDate/fromLocalDateメソッドが他のメソッドと一貫性がない</li>
 * </ul>
 * </p>
 *
 * <p>【対応方針】新規開発ではjava.time APIを直接使用すること。
 * このクラスはレガシーコードとの互換性のために残しているが、
 * 段階的にjava.time APIへ移行する予定。</p>
 *
 * @author ProQuip開発チーム（初期実装: 2018年、最終更新: 2024年）
 * @since 1.0.0
 */
public final class DateUtils {

    // 【技術的負債】SimpleDateFormatはスレッドセーフではない。
    // マルチスレッド環境（サーブレットコンテナ等）では日付の誤変換が発生しうる。
    // DateTimeFormatterに置き換えるべき。
    private static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("yyyy/MM/dd");

    private static final SimpleDateFormat DATETIME_FORMATTER =
            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    // 2023年に追加。ISO形式のフォーマッタだが、上と同じスレッドセーフ問題あり
    private static final SimpleDateFormat ISO_FORMATTER =
            new SimpleDateFormat("yyyy-MM-dd");

    /** 1日のミリ秒数（daysBetweenの計算で使用） */
    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

    /** インスタンス化を防止 */
    private DateUtils() {
    }

    /**
     * Dateを「yyyy/MM/dd」形式の文字列にフォーマットする。
     *
     * <p>【技術的負債】スレッドセーフではない。</p>
     *
     * @param date フォーマット対象の日付
     * @return フォーマットされた日付文字列。dateがnullの場合は空文字列
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_FORMATTER.format(date);
    }

    /**
     * Dateを「yyyy/MM/dd HH:mm:ss」形式の文字列にフォーマットする。
     *
     * <p>【技術的負債】スレッドセーフではない。</p>
     *
     * @param date フォーマット対象の日付
     * @return フォーマットされた日時文字列。dateがnullの場合は空文字列
     */
    public static String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return DATETIME_FORMATTER.format(date);
    }

    /**
     * 「yyyy/MM/dd」形式の文字列をDateに変換する。
     *
     * <p>【技術的負債】ParseExceptionをキャッチしてnullを返しており、
     * 呼び出し元でのエラーハンドリングが困難。例外をスローすべき。</p>
     *
     * @param dateStr 日付文字列
     * @return 変換されたDate。変換失敗時はnull
     */
    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return DATE_FORMATTER.parse(dateStr.trim());
        } catch (ParseException e) {
            // TODO: 例外を握りつぶしている。ログ出力もされない。
            return null;
        }
    }

    /**
     * 「yyyy-MM-dd」形式（ISO 8601）の文字列をDateに変換する。
     *
     * <p>2023年にAPI対応で追加。他のメソッドと同じ問題を持つ。</p>
     *
     * @param dateStr ISO形式の日付文字列
     * @return 変換されたDate。変換失敗時はnull
     */
    public static Date parseIsoDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return ISO_FORMATTER.parse(dateStr.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 指定した日数を加算した日付を返す。
     *
     * <p>【技術的負債】Calendarを使っている。LocalDate.plusDays()を使うべき。</p>
     *
     * @param date 基準日
     * @param days 加算する日数（負の値で減算）
     * @return 日数を加算したDate
     */
    public static Date addDays(Date date, int days) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    /**
     * 指定した月数を加算した日付を返す。
     *
     * @param date 基準日
     * @param months 加算する月数（負の値で減算）
     * @return 月数を加算したDate
     */
    public static Date addMonths(Date date, int months) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, months);
        return cal.getTime();
    }

    /**
     * 2つの日付間の日数を計算する。
     *
     * <p>【技術的負債】ミリ秒の差分を手動で計算しており、
     * 夏時間（DST）の切替日やうるう秒で誤差が出る可能性がある。
     * ChronoUnit.DAYS.between()を使用すべき。</p>
     *
     * @param from 開始日
     * @param to 終了日
     * @return 日数（toがfromより前の場合は負の値）
     */
    public static long daysBetween(Date from, Date to) {
        if (from == null || to == null) {
            return 0;
        }
        long diffMillis = to.getTime() - from.getTime();
        return diffMillis / MILLIS_PER_DAY;
    }

    /**
     * 指定した日付が週末（土曜日または日曜日）かどうかを判定する。
     *
     * @param date 判定対象の日付
     * @return 週末の場合true
     */
    public static boolean isWeekend(Date date) {
        if (date == null) {
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
    }

    /**
     * 指定した日付の開始時刻（00:00:00.000）を返す。
     *
     * @param date 対象の日付
     * @return その日の開始時刻
     */
    public static Date getStartOfDay(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 指定した日付の終了時刻（23:59:59.999）を返す。
     *
     * @param date 対象の日付
     * @return その日の終了時刻
     */
    public static Date getEndOfDay(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * 指定した日付の月初日を返す。
     *
     * @param date 対象の日付
     * @return その月の初日（00:00:00）
     */
    public static Date getStartOfMonth(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 指定した日付の月末日を返す。
     *
     * @param date 対象の日付
     * @return その月の末日（23:59:59.999）
     */
    public static Date getEndOfMonth(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * 指定した日付の会計年度を返す。
     *
     * <p>日本の会計年度（4月始まり）を使用する。
     * 例: 2024年3月 → 2023年度、2024年4月 → 2024年度</p>
     *
     * <p>【技術的負債】会計年度の開始月（4月）がハードコードされている。
     * 設定で変更可能にすべき。</p>
     *
     * @param date 対象の日付
     * @return 会計年度（西暦）
     */
    public static int getFiscalYear(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("日付がnullです。");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH); // 0始まり

        // 4月始まり: 1月〜3月は前年度
        if (month < Calendar.APRIL) {
            return year - 1;
        }
        return year;
    }

    /**
     * java.util.Dateをjava.time.LocalDateに変換する。
     *
     * <p>2024年に追加。新しいjava.time APIとの橋渡し用。
     * 【注意】このクラスの他のメソッドと設計方針が異なる（後付け）。</p>
     *
     * @param date 変換対象のDate
     * @return 対応するLocalDate。dateがnullの場合はnull
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * java.time.LocalDateをjava.util.Dateに変換する。
     *
     * <p>2024年に追加。レガシーコードとの連携用。</p>
     *
     * @param localDate 変換対象のLocalDate
     * @return 対応するDate。localDateがnullの場合はnull
     */
    public static Date fromLocalDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
