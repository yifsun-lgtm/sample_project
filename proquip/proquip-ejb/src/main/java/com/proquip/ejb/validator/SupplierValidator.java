package com.proquip.ejb.validator;

import com.proquip.ejb.entity.supplier.Supplier;
import com.proquip.ejb.entity.supplier.SupplierAddress;
import com.proquip.ejb.entity.supplier.SupplierContact;

import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 仕入先バリデータ。
 *
 * <p>仕入先エンティティに対するビジネスルールの検証を行う。
 * 連絡先情報、住所情報を含む総合的なバリデーションを提供する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>電話番号・メールアドレスの正規表現がハードコードされている。</li>
 *   <li>JPA {@code @Column} や Bean Validation アノテーションで既に検証されている
 *       フィールドに対して重複したバリデーションを行っている。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class SupplierValidator {

    private static final Logger logger = Logger.getLogger(SupplierValidator.class.getName());

    /**
     * 電話番号の正規表現パターン。
     * 技術的負債: ハードコードされた正規表現。国際電話番号フォーマットへの対応が不十分。
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+?[0-9]{1,4}[\\s-]?)?\\(?[0-9]{1,5}\\)?[\\s-]?[0-9]{1,5}[\\s-]?[0-9]{1,5}$");

    /**
     * メールアドレスの正規表現パターン。
     * 技術的負債: 簡易的な正規表現。RFC 5322準拠のバリデーションに移行すべき。
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 仕入先エンティティの総合バリデーションを実行する。
     *
     * @param supplier バリデーション対象の仕入先エンティティ
     * @return エラーメッセージのリスト。バリデーション成功時は空リスト
     */
    public List<String> validate(Supplier supplier) {
        List<String> errors = new ArrayList<>();

        if (supplier == null) {
            errors.add("仕入先オブジェクトがnullです");
            return errors;
        }

        // === 仕入先コードの検証 ===
        // 技術的負債: @Column(nullable = false) で既にDB側で制約あり（重複バリデーション）
        if (supplier.getCode() == null || supplier.getCode().trim().isEmpty()) {
            errors.add("仕入先コードは必須です");
        } else {
            if (supplier.getCode().length() > 50) {
                errors.add("仕入先コードは50文字以内で入力してください");
            }
            if (!supplier.getCode().matches("[A-Z0-9_-]+")) {
                errors.add("仕入先コードは大文字英数字、ハイフン、アンダースコアのみ使用可能です");
            }
        }

        // === 仕入先名の検証 ===
        // 技術的負債: @Column(nullable = false) で既にDB側で制約あり（重複バリデーション）
        if (supplier.getName() == null || supplier.getName().trim().isEmpty()) {
            errors.add("仕入先名は必須です");
        } else {
            if (supplier.getName().length() > 200) {
                errors.add("仕入先名は200文字以内で入力してください");
            }
        }

        // === ステータスの検証 ===
        if (supplier.getStatus() == null || supplier.getStatus().trim().isEmpty()) {
            errors.add("ステータスは必須です");
        } else {
            String status = supplier.getStatus();
            if (!status.equals("ACTIVE") && !status.equals("INACTIVE")
                    && !status.equals("SUSPENDED") && !status.equals("PENDING_APPROVAL")) {
                errors.add("無効なステータスです: " + status);
            }
        }

        // rating and paymentTermDays fields were removed from Supplier entity (DDL alignment).
        // Rating validation should be done on SupplierRating entity instead.

        // === 連絡先の検証 ===
        if (supplier.getContacts() != null && !supplier.getContacts().isEmpty()) {
            for (int i = 0; i < supplier.getContacts().size(); i++) {
                errors.addAll(validateContact(supplier.getContacts().get(i), i + 1));
            }

            // 主担当者の存在チェック
            boolean hasPrimary = false;
            for (SupplierContact contact : supplier.getContacts()) {
                if (contact.isPrimary()) {
                    hasPrimary = true;
                    break;
                }
            }
            if (!hasPrimary) {
                errors.add("主担当者が設定されていません。少なくとも1名の主担当者が必要です");
            }
        }

        // === 住所の検証 ===
        if (supplier.getAddresses() != null && !supplier.getAddresses().isEmpty()) {
            for (int i = 0; i < supplier.getAddresses().size(); i++) {
                errors.addAll(validateAddress(supplier.getAddresses().get(i), i + 1));
            }
        }

        logger.info("仕入先バリデーション完了: " + supplier.getCode()
                + " エラー数: " + errors.size());

        return errors;
    }

    /**
     * 仕入先連絡先のバリデーションを実行する。
     *
     * <p>技術的負債: 電話番号・メールアドレスの正規表現がハードコードされている。</p>
     *
     * @param contact 連絡先エンティティ
     * @param index   連絡先のインデックス（エラーメッセージ用）
     * @return エラーメッセージのリスト
     */
    private List<String> validateContact(SupplierContact contact, int index) {
        List<String> errors = new ArrayList<>();
        String prefix = "連絡先 " + index + ": ";

        if (contact.getFirstName() == null || contact.getFirstName().trim().isEmpty()) {
            errors.add(prefix + "名は必須です");
        }
        if (contact.getLastName() == null || contact.getLastName().trim().isEmpty()) {
            errors.add(prefix + "姓は必須です");
        }

        // メールアドレスの検証
        if (contact.getEmail() != null && !contact.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(contact.getEmail()).matches()) {
                errors.add(prefix + "メールアドレスのフォーマットが不正です: " + contact.getEmail());
            }
        }

        // 電話番号の検証
        if (contact.getPhone() != null && !contact.getPhone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(contact.getPhone()).matches()) {
                errors.add(prefix + "電話番号のフォーマットが不正です: " + contact.getPhone());
            }
        }

        return errors;
    }

    /**
     * 仕入先住所のバリデーションを実行する。
     *
     * @param address 住所エンティティ
     * @param index   住所のインデックス（エラーメッセージ用）
     * @return エラーメッセージのリスト
     */
    private List<String> validateAddress(SupplierAddress address, int index) {
        List<String> errors = new ArrayList<>();
        String prefix = "住所 " + index + ": ";

        if (address.getAddressType() == null || address.getAddressType().trim().isEmpty()) {
            errors.add(prefix + "住所種別は必須です");
        } else {
            String type = address.getAddressType();
            if (!type.equals("HEAD_OFFICE") && !type.equals("FACTORY")
                    && !type.equals("WAREHOUSE") && !type.equals("BILLING")) {
                errors.add(prefix + "無効な住所種別です: " + type);
            }
        }

        if (address.getCountry() == null || address.getCountry().trim().isEmpty()) {
            errors.add(prefix + "国は必須です");
        }

        if (address.getPostalCode() != null && !address.getPostalCode().trim().isEmpty()) {
            // 日本の郵便番号フォーマット
            if (address.getCountry() != null && address.getCountry().equals("日本")) {
                if (!address.getPostalCode().matches("[0-9]{3}-[0-9]{4}")) {
                    errors.add(prefix + "郵便番号のフォーマットが不正です（日本の場合: NNN-NNNN）");
                }
            }
        }

        return errors;
    }
}
