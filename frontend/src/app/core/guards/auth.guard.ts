import { CanActivateFn, Router } from '@angular/router';
import { inject } from "@angular/core";
import { AuthService } from "../services/auth.service";

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  if (authService.isLoggedIn()) {
    if (authService.isPasswordChangeRequired() && state.url !== '/force-password-reset') {
      router.navigate(['/force-password-reset']);
      return false;
    }
    return true;
  } else {
    router.navigate(['/login'], {queryParams: {returnTo: state.url}});
    return false;
  }
};

