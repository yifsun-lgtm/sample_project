import { Component, OnInit } from '@angular/core';
import { NotificationTemplateService } from '@shared/services/notification-template.service';

@Component({
  selector: 'app-notification-settings',
  templateUrl: './notification-settings.component.html',
  styleUrls: ['./notification-settings.component.scss']
})
export class NotificationSettingsComponent implements OnInit {

  templates: any[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  showEditModal = false;
  editItem: any = {};
  isSaving = false;

  channelOptions = [
    { value: 'EMAIL', label: 'メール' },
    { value: 'IN_APP', label: 'アプリ内' },
    { value: 'BOTH', label: '両方' },
    { value: 'SMS', label: 'SMS' },
    { value: 'WEBHOOK', label: 'Webhook' }
  ];

  eventTypeLabels: { [key: string]: string } = {
    'PO_CREATED': '発注作成',
    'PO_APPROVED': '発注承認',
    'PO_REJECTED': '発注却下',
    'PO_RECEIVED': '発注受領',
    'PR_SUBMITTED': '購買申請提出',
    'PR_APPROVED': '購買申請承認',
    'PR_REJECTED': '購買申請却下',
    'APPROVAL_REQUIRED': '承認依頼',
    'APPROVAL_REMINDER': '承認リマインダー',
    'APPROVAL_ESCALATED': '承認エスカレーション',
    'LOW_STOCK_ALERT': '在庫不足警告',
    'REORDER_SUGGESTION': '発注提案',
    'CONTRACT_EXPIRING': '契約期限',
    'CERTIFICATION_EXPIRING': '認証期限',
    'BUDGET_THRESHOLD': '予算閾値',
    'BUDGET_EXCEEDED': '予算超過',
    'GOODS_RECEIVED': '入荷完了',
    'GOODS_REJECTED': '入荷却下',
    'SYSTEM_ANNOUNCEMENT': 'システム通知',
    'IMPORT_COMPLETED': 'インポート完了',
    'IMPORT_FAILED': 'インポート失敗'
  };

  constructor(private templateService: NotificationTemplateService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.clearMessages();
    this.templateService.getTemplates().subscribe({
      next: (data) => { this.templates = data; this.isLoading = false; },
      error: () => { this.errorMessage = '通知テンプレート一覧の取得に失敗しました。'; this.isLoading = false; }
    });
  }

  toggleActive(item: any): void {
    this.clearMessages();
    this.templateService.toggleActive(item.id).subscribe({
      next: (updated) => {
        item.active = updated.active;
        this.successMessage = updated.active ? '有効にしました。' : '無効にしました。';
      },
      error: () => { this.errorMessage = '更新に失敗しました。'; }
    });
  }

  openEditModal(item: any): void {
    this.editItem = { ...item };
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editItem = {};
  }

  save(): void {
    this.isSaving = true;
    this.clearMessages();

    const payload = {
      name: this.editItem.name,
      subject: this.editItem.subject,
      bodyText: this.editItem.bodyText,
      channel: this.editItem.channel,
      active: this.editItem.active
    };

    this.templateService.updateTemplate(this.editItem.id, payload).subscribe({
      next: () => {
        this.successMessage = '更新しました。';
        this.isSaving = false;
        this.closeEditModal();
        this.loadData();
      },
      error: () => {
        this.errorMessage = '保存に失敗しました。';
        this.isSaving = false;
      }
    });
  }

  getEventTypeLabel(eventType: string): string {
    return this.eventTypeLabels[eventType] || eventType;
  }

  getChannelLabel(channel: string): string {
    const found = this.channelOptions.find(o => o.value === channel);
    return found ? found.label : channel;
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
