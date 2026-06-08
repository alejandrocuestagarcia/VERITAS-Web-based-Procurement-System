import {Component, OnDestroy, OnInit} from '@angular/core';
import {AuthService} from "../../services/auth.service";
import {NavigationService, NavItem} from "../../services/navigation.service";
import {ToastService} from "../../services/toast.service";
import {NotificationStateService} from "../../services/notification-state.service";
import {Subscription, interval} from "rxjs";

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit, OnDestroy {

  constructor(
    protected authService: AuthService,
    protected navService: NavigationService,
    private notificationStateService: NotificationStateService,
    private toastService: ToastService
  ) {
  }

  navLinks: NavItem[] = [];
  isOpen: boolean = false;
  unreadCount: number = 0;
  private isInitialCheck: boolean = true;
  private pollingSubscription?: Subscription;
  private stateSubscription?: Subscription;

  ngOnInit(): void {
    const role = this.authService.getRole();
    if (role) {
      this.navLinks = this.navService.getLinksForRole(role);
    }

    if (this.authService.isLoggedIn()) {
      this.stateSubscription = this.notificationStateService.getUnreadCountObservable().subscribe({
        next: (count) => {
          if (this.isInitialCheck) {
            if (count > 0) {
              this.toastService.showInfo(`You have ${count} unread notification${count > 1 ? 's' : ''}`);
            }
            this.isInitialCheck = false;
          } else if (count > this.unreadCount) {
            const newCount = count - this.unreadCount;
            this.toastService.showInfo(`You have ${newCount} new notification${newCount > 1 ? 's' : ''}`);
          }
          this.unreadCount = count;
        },
        error: () => {}
      });
      this.notificationStateService.refreshUnreadCount();
      this.startPolling();
    }
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
    this.stateSubscription?.unsubscribe();
  }

  onLogout() {
    this.pollingSubscription?.unsubscribe();
    this.stateSubscription?.unsubscribe();
    this.authService.logout();
  }

  toggleMenu(): void {
    this.isOpen = !this.isOpen;
  }

  closeMenu(): void {
    this.isOpen = false;
  }

  private startPolling(): void {
    this.pollingSubscription = interval(30000).subscribe({
      next: () => {
        this.notificationStateService.refreshUnreadCount();
      },
      error: () => {}
    });
  }
}
