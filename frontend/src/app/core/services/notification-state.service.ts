import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { NotificationModuleService } from '../api';

@Injectable({
  providedIn: 'root'
})
export class NotificationStateService {
  private unreadCount$ = new BehaviorSubject<number>(0);

  constructor(private notificationService: NotificationModuleService) {}

  getUnreadCountObservable(): Observable<number> {
    return this.unreadCount$.asObservable();
  }

  refreshUnreadCount(): void {
    this.notificationService.getUnreadCount().subscribe({
      next: (count) => {
        this.unreadCount$.next(count);
      },
      error: () => {}
    });
  }

  getNotificationIcon(type: string | undefined): string {
    switch (type) {
      case 'SUBMITTED': return 'send';
      case 'APPROVED': return 'check_circle';
      case 'REJECTED': return 'cancel';
      case 'FINISHED': return 'task_alt';
      case 'ASSIGNED': return 'assignment_ind';
      case 'REASSIGNED': return 'swap_horiz';
      case 'PAID': return 'payments';
      case 'REVERTED': return 'undo';
      default: return 'notifications';
    }
  }

  getNotificationColorClass(type: string | undefined): string {
    switch (type) {
      case 'APPROVED': return 'bg-emerald-50 text-emerald-600 border-emerald-200';
      case 'REJECTED': return 'bg-rose-50 text-rose-600 border-rose-200';
      case 'SUBMITTED': return 'bg-blue-50 text-blue-600 border-blue-200';
      case 'FINISHED': return 'bg-purple-50 text-purple-600 border-purple-200';
      case 'ASSIGNED': return 'bg-amber-50 text-amber-600 border-amber-200';
      case 'REASSIGNED': return 'bg-indigo-50 text-indigo-600 border-indigo-200';
      case 'PAID': return 'bg-emerald-50 text-emerald-600 border-emerald-200';
      case 'REVERTED': return 'bg-amber-50 text-amber-600 border-amber-200';
      default: return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  }
}
