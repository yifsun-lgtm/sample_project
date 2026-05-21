package com.proquip.ejb.integration;

import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.supplier.Supplier;
import com.proquip.ejb.entity.system.ImportJob;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * データインポートの統合テスト。
 *
 * <p>WildFly上で実行するArquillian統合テスト。CSVファイルによる
 * 製品・仕入先データの一括インポート処理を検証する。</p>
 *
 * <p>技術的負債 #13: 統合テストが未実装。WildFlyの統合テスト環境が
 * 整備されるまで全テストが@Disabledとなっている。</p>
 *
 * @author ProQuip開発チーム
 */
@Disabled("統合テスト環境のセットアップ待ち")
@ExtendWith(ArquillianExtension.class)
public class DataImportIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    // @Inject
    // private DataImportService importService;

    /**
     * テスト用WARアーカイブを作成する。
     *
     * @return テスト用WebArchive
     */
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "proquip-import-test.war")
            .addPackages(true, "com.proquip.ejb.entity")
            .addPackages(true, "com.proquip.ejb.service")
            .addPackages(true, "com.proquip.ejb.dao")
            .addPackages(true, "com.proquip.common")
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * 各テスト前にテストデータをクリーンアップする。
     *
     * @throws Exception トランザクション操作で例外が発生した場合
     */
    @BeforeEach
    void setUp() throws Exception {
        utx.begin();
        // インポートテスト用の既存データをクリーンアップ
        em.createQuery("DELETE FROM Product p WHERE p.sku LIKE 'IMP-TEST-%'").executeUpdate();
        em.createQuery("DELETE FROM Supplier s WHERE s.code LIKE 'IMP-TEST-%'").executeUpdate();
        utx.commit();
    }

    // ========================================================================
    // 製品CSVインポートテスト
    // ========================================================================

    @Test
    @DisplayName("正常な製品CSVファイルをインポートできること")
    void shouldImportValidProductCsv() {
        // given: 3件の製品データを含むCSV
        // String csvContent = "sku,name,unitPrice,status\n"
        //     + "IMP-TEST-001,インポートテスト製品1,1000,ACTIVE\n"
        //     + "IMP-TEST-002,インポートテスト製品2,2000,ACTIVE\n"
        //     + "IMP-TEST-003,インポートテスト製品3,3000,ACTIVE\n";
        // InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        // when: CSVインポートを実行
        // ImportJob job = importService.importProducts(csvStream, "test-products.csv", "admin");

        // then: 3件すべてインポートされていること
        // assertEquals("COMPLETED", job.getStatus());
        // assertEquals(3, job.getSuccessCount());
        // assertEquals(0, job.getErrorCount());
        //
        // Long count = em.createQuery(
        //     "SELECT COUNT(p) FROM Product p WHERE p.sku LIKE 'IMP-TEST-%'", Long.class)
        //     .getSingleResult();
        // assertEquals(3L, count);
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("重複SKUを含むCSVのインポート時にエラーが記録されること")
    void shouldReportDuplicateSkuError() {
        // given: 既存製品と重複するSKUを含むCSV
        // まず1件登録
        // String csvContent1 = "sku,name,unitPrice,status\n"
        //     + "IMP-TEST-DUP,重複テスト製品,1000,ACTIVE\n";
        // importService.importProducts(
        //     new ByteArrayInputStream(csvContent1.getBytes(StandardCharsets.UTF_8)),
        //     "first.csv", "admin");

        // when: 同じSKUで再度インポート
        // String csvContent2 = "sku,name,unitPrice,status\n"
        //     + "IMP-TEST-DUP,重複テスト製品（重複）,2000,ACTIVE\n";
        // ImportJob job = importService.importProducts(
        //     new ByteArrayInputStream(csvContent2.getBytes(StandardCharsets.UTF_8)),
        //     "duplicate.csv", "admin");

        // then: エラーが1件記録されていること
        // assertEquals(1, job.getErrorCount());
        // assertTrue(job.getErrors().stream().anyMatch(e -> e.contains("重複")));
        fail("統合テスト環境のセットアップ待ち");
    }

    // ========================================================================
    // 仕入先CSVインポートテスト
    // ========================================================================

    @Test
    @DisplayName("正常な仕入先CSVファイルをインポートできること")
    void shouldImportValidSupplierCsv() {
        // given: 2件の仕入先データを含むCSV
        // String csvContent = "code,name,status,contactEmail\n"
        //     + "IMP-TEST-SUP-001,テストサプライヤーA,ACTIVE,a@example.com\n"
        //     + "IMP-TEST-SUP-002,テストサプライヤーB,ACTIVE,b@example.com\n";
        // InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        // when: CSVインポートを実行
        // ImportJob job = importService.importSuppliers(csvStream, "test-suppliers.csv", "admin");

        // then: 2件すべてインポートされていること
        // assertEquals("COMPLETED", job.getStatus());
        // assertEquals(2, job.getSuccessCount());
        fail("統合テスト環境のセットアップ待ち");
    }

    // ========================================================================
    // エラーハンドリングテスト
    // ========================================================================

    @Test
    @DisplayName("不正なCSV形式のファイルはエラーとなること")
    void shouldRejectMalformedCsv() {
        // given: ヘッダーが不正なCSV
        // String csvContent = "invalid_header_1,invalid_header_2\n"
        //     + "value1,value2\n";
        // InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        // when: CSVインポートを実行
        // ImportJob job = importService.importProducts(csvStream, "malformed.csv", "admin");

        // then: ジョブがFAILEDステータスになること
        // assertEquals("FAILED", job.getStatus());
        // assertTrue(job.getErrors().size() > 0, "エラーメッセージが記録されていること");
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("空のCSVファイルはエラーとなること")
    void shouldRejectEmptyCsv() {
        // given: 空のCSVファイル
        // String csvContent = "";
        // InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        // when & then: インポート時にValidationExceptionが発生
        // assertThrows(ValidationException.class, () -> {
        //     importService.importProducts(csvStream, "empty.csv", "admin");
        // });
        fail("統合テスト環境のセットアップ待ち");
    }
}
