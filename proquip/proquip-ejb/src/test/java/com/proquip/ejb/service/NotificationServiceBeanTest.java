package com.proquip.ejb.service;

import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.ejb.entity.system.Notification;
import com.proquip.ejb.entity.system.NotificationTemplate;
import com.proquip.ejb.service.notification.AbstractNotificationSender;
import com.proquip.ejb.service.notification.NotificationSenderFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationServiceBeanの単体テスト。
 *
 * <p>技術的負債 #13: 一括通知、クリーンアップ処理のテストが欠如。
 * テンプレート処理のテストは最低限のプレースホルダー置換のみ検証。</p>
 *
 * @author ProQuip開発チーム
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceBeanTest {

    @Mock
    private EntityManager em;

    @Mock
    private NotificationSenderFactory senderFactory;

    @Mock
    private AbstractNotificationSender inAppSender;

    @InjectMocks
    private NotificationServiceBean service;

    // ========================================================================
    // 通知送信テスト
    // ========================================================================

    @Test
    @DisplayName("通知送信 - 正常系: 通知がアプリ内通知として送信されること")
    void testSendNotification() {
        // Arrange
        when(senderFactory.getSender("IN_APP")).thenReturn(inAppSender);
        doNothing().when(inAppSender).send(any(Notification.class));

        // Act
        service.sendNotification(
                1L, "テスト通知", "テストメッセージです", "INFO",
                "PurchaseOrder", 100L);

        // Assert
        verify(em).persist(any(Notification.class));
        verify(senderFactory).getSender("IN_APP");
        verify(inAppSender).send(any(Notification.class));
    }

    @Test
    @DisplayName("通知送信 - 異常系: ユーザーIDがnullの場合は送信されないこと")
    void testSendNotification_nullUserId() {
        // Act
        service.sendNotification(null, "テスト", "メッセージ", "INFO", null, null);

        // Assert — persistが呼ばれないことを確認
        verify(em, never()).persist(any());
    }

    // ========================================================================
    // 既読管理テスト
    // ========================================================================

    @Test
    @DisplayName("既読 - 正常系: 通知が既読に変更されること")
    void testMarkAsRead() {
        // Arrange
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(100L);
        notification.setStatus("UNREAD");

        when(em.find(Notification.class, 1L)).thenReturn(notification);

        // Act
        service.markAsRead(1L, 100L);

        // Assert
        assertEquals("READ", notification.getStatus());
        assertNotNull(notification.getReadAt());
        verify(em).merge(notification);
    }

    @Test
    @DisplayName("既読 - 異常系: 別ユーザーの通知は既読にできないこと")
    void testMarkAsRead_wrongUser() {
        // Arrange
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(100L);
        notification.setStatus("UNREAD");

        when(em.find(Notification.class, 1L)).thenReturn(notification);

        // Act — 異なるユーザーID(200L)で既読を試行
        service.markAsRead(1L, 200L);

        // Assert — ステータスが変更されていないことを確認
        assertEquals("UNREAD", notification.getStatus());
        verify(em, never()).merge(notification);
    }

    // ========================================================================
    // テンプレート処理テスト
    // ========================================================================

    @Test
    @DisplayName("テンプレート処理 - 正常系: プレースホルダーが正しく置換されること")
    void testProcessTemplate() {
        // Arrange
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode("ORDER_APPROVED");
        template.setBodyText(
                "発注番号 {{poNumber}} が{{approverName}}によって承認されました。金額: {{amount}}円");

        Query templateQuery = mock(Query.class);
        when(em.createNamedQuery("NotificationTemplate.findByCode")).thenReturn(templateQuery);
        when(templateQuery.setParameter(anyString(), any())).thenReturn(templateQuery);
        when(templateQuery.getResultList()).thenReturn(Arrays.asList(template));

        Map<String, String> params = new HashMap<>();
        params.put("poNumber", "PO-20240315-0001");
        params.put("approverName", "山田太郎");
        params.put("amount", "150000");

        // Act
        String result = service.processTemplate("ORDER_APPROVED", params);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("PO-20240315-0001"));
        assertTrue(result.contains("山田太郎"));
        assertTrue(result.contains("150000"));
        assertFalse(result.contains("{{"));
    }

    @Test
    @DisplayName("テンプレート処理 - 異常系: 存在しないテンプレートコードで空文字が返ること")
    void testProcessTemplate_notFound() {
        // Arrange
        Query templateQuery = mock(Query.class);
        when(em.createNamedQuery("NotificationTemplate.findByCode")).thenReturn(templateQuery);
        when(templateQuery.setParameter(anyString(), any())).thenReturn(templateQuery);
        when(templateQuery.getResultList()).thenReturn(new ArrayList<>());

        // Act
        String result = service.processTemplate("NONEXISTENT", null);

        // Assert
        assertEquals("", result);
    }

    // ========================================================================
    // 未読通知取得テスト
    // ========================================================================

    @Test
    @DisplayName("未読通知取得 - 正常系: ユーザーの未読通知が取得できること")
    void testGetUnreadNotifications() {
        // Arrange
        Notification n1 = new Notification();
        n1.setId(1L);
        n1.setTitle("通知1");
        n1.setStatus("UNREAD");

        Notification n2 = new Notification();
        n2.setId(2L);
        n2.setTitle("通知2");
        n2.setStatus("UNREAD");

        Query unreadQuery = mock(Query.class);
        when(em.createNamedQuery("Notification.findUnreadByUserId")).thenReturn(unreadQuery);
        when(unreadQuery.setParameter(anyString(), any())).thenReturn(unreadQuery);
        when(unreadQuery.getResultList()).thenReturn(Arrays.asList(n1, n2));

        // Act
        List<Notification> result = service.getUnreadNotifications(100L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("未読通知取得 - 正常系: ユーザーIDがnullの場合は空リストが返ること")
    void testGetUnreadNotifications_nullUserId() {
        // Act
        List<Notification> result = service.getUnreadNotifications(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
