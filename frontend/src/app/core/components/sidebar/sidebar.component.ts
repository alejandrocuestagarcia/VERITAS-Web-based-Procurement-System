import {Component, OnInit} from '@angular/core';
import {AuthService} from "../../services/auth.service";
import {NavigationService,NavItem} from "../../services/navigation.service";

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit{

  constructor(
    protected authService: AuthService,
    protected navService: NavigationService
  ) {
  }
  navLinks: NavItem[] = [];
  ngOnInit(): void {
    const role = this.authService.getRole()
    if (role) {
      this.navLinks = this.navService.getLinksForRole(role);
    }
  }
  onLogout() {
    this.authService.logout();
  }
}
