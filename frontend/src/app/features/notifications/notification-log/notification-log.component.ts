import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationModuleService, NotificationDto, PageNotificationDto } from '../../../core/api';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../shared/error-utils';
import { NotificationStateService } from '../../../core/services/notification-state.service';
import { PageEvent } from '@angular/material/paginator';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification-log',
  templateUrl: './notification-log.component.html'
})
export class NotificationLogComponent implements OnInit, OnDestroy {
  notifications: NotificationDto[] = [];
  loading = false;
  page = 0;
  size = 10;
  totalElements = 0;
  unreadCount = 0;
  emailNotificationsEnabled = true;
  private stateSubscription?: Subscription;

  constructor(
    private notificationService: NotificationModuleService,
    public notificationStateService: NotificationStateService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.stateSubscription = this.notificationStateService.getUnreadCountObservable().subscribe({
      next: (count: number) => {
        this.unreadCount = count;
      }
    });

    this.loadNotifications();
    this.loadUnreadCount();
    this.loadEmailPreference();
  }

  ngOnDestroy(): void {
    this.stateSubscription?.unsubscribe();
  }

  loadNotifications(): void {
    this.loading = true;
    this.notificationService.getAllNotifications({ page: this.page, size: this.size }).subscribe({
      next: (response: PageNotificationDto) => {
        this.notifications = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Failed to load notifications', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load notifications'));
        this.loading = false;
      }
    });
  }

  loadUnreadCount(): void {
    this.notificationStateService.refreshUnreadCount();
  }

  onPageChange(event: PageEvent): void {
    this.page = event.pageIndex;
    this.size = event.pageSize;
    this.loadNotifications();
  }

  markAsRead(notification: NotificationDto): void {
    if (notification.read || !notification.id) return;
    this.notificationService.markAsRead(notification.id).subscribe({
      next: () => {
        notification.read = true;
        this.notificationStateService.refreshUnreadCount();
        this.toastService.showSuccess('Notification marked as read');
      },
      error: (err: any) => {
        console.error('Failed to mark notification as read', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to mark notification as read'));
      }
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.read = true);
        this.notificationStateService.refreshUnreadCount();
        this.toastService.showSuccess('All notifications marked as read');
      },
      error: (err: any) => {
        console.error('Failed to mark all notifications as read', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to mark all notifications as read'));
      }
    });
  }

  deleteNotification(event: Event, id: number | undefined): void {
    event.stopPropagation();
    if (!id) return;
    this.notificationService.deleteNotification(id).subscribe({
      next: () => {
        this.notifications = this.notifications.filter(n => n.id !== id);
        this.totalElements--;
        this.notificationStateService.refreshUnreadCount();
        this.toastService.showSuccess('Notification deleted');
        if (this.notifications.length === 0 && this.page > 0) {
          this.page--;
          this.loadNotifications();
        }
      },
      error: (err: any) => {
        console.error('Failed to delete notification', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to delete notification'));
      }
    });
  }

  navigateToRequest(requestId: number | undefined): void {
    if (requestId) {
      this.router.navigate(['/requisitions', requestId]);
    }
  }

  loadEmailPreference(): void {
    this.notificationService.getEmailPreference().subscribe({
      next: (enabled: boolean) => {
        this.emailNotificationsEnabled = enabled;
      },
      error: (err) => {
        this.toastService.showError(extractErrorMessage(err, 'Failed to load email preferences'));
      }
    });
  }

  onEmailToggleChange(event: any): void {
    const enabled = event.checked;
    this.notificationService.updateEmailPreference(enabled).subscribe({
      next: () => {
        this.emailNotificationsEnabled = enabled;
        this.toastService.showSuccess(
          enabled ? 'Email notifications enabled' : 'Email notifications disabled'
        );
      },
      error: (err) => {
        this.toastService.showError(extractErrorMessage(err, 'Failed to update email preferences'));
        this.emailNotificationsEnabled = !enabled;
      }
    });
  }
}
