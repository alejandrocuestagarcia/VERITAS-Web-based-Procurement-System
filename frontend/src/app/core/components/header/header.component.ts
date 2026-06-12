import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, map, catchError, tap } from 'rxjs/operators';
import {
  RequisitionModuleService,
  RequisitionDto,
  ProjectModuleService,
  UserModuleService,
  ProjectDto,
  UserDto
} from 'src/app/core/api';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit, OnDestroy {
  searchCtrl = new FormControl('');

  showAutocomplete = false;
  loadingAutocomplete = false;
  showFilterPopup = false;

  suggestions: RequisitionDto[] = [];
  projects: ProjectDto[] = [];
  creators: UserDto[] = [];

  selectedProjectId: number | '' = '';
  createdFrom = '';
  createdTo = '';
  selectedCreatorId: number | '' = '';
  status = '';

  readonly statuses = [
    { value: 'OPEN', label: 'Open' },
    { value: 'AWAITING_PAYMENT', label: 'Awaiting Payment' },
    { value: 'CLOSED', label: 'Closed' }
  ];

  private searchSub?: Subscription;

  constructor(
    private requisitionService: RequisitionModuleService,
    private projectService: ProjectModuleService,
    private userService: UserModuleService,
    public authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Setup autocomplete typeahead inside the search control
    this.searchSub = this.searchCtrl.valueChanges.pipe(
      debounceTime(250),
      distinctUntilChanged(),
      tap((value) => {
        const val = typeof value === 'string' ? value.trim() : '';
        if (val.length >= 2) {
          this.showAutocomplete = true;
          this.loadingAutocomplete = true;
        } else if (val.length === 1) {
          this.showAutocomplete = true;
          this.suggestions = [];
          this.loadingAutocomplete = false;
        } else {
          this.showAutocomplete = false;
          this.suggestions = [];
          this.loadingAutocomplete = false;
        }
      }),
      switchMap(value => {
        const val = typeof value === 'string' ? value.trim() : '';
        if (val.length < 2) {
          return of([]);
        }

        return this.requisitionService.getRequests(
          undefined,
          val,
          undefined,
          undefined,
          undefined,
          undefined,
          0,
          5
        ).pipe(
          map(res => res.content || []),
          catchError(() => of([]))
        );
      })
    ).subscribe({
      next: (results) => {
        this.suggestions = results;
        this.loadingAutocomplete = false;
      },
      error: (err) => {
        console.error('Error fetching suggestions', err);
        this.suggestions = [];
        this.loadingAutocomplete = false;
      }
    });

    // Sync search input with URL query parameters
    this.route.queryParams.subscribe(params => {
      const q = params['q'] || '';
      this.searchCtrl.setValue(q, { emitEvent: false });
    });
  }

  ngOnDestroy(): void {
    if (this.searchSub) {
      this.searchSub.unsubscribe();
    }
  }

  triggerSearch(): void {
    const queryParams: any = {
      q: this.searchCtrl.value?.trim() || undefined
    };
    this.showAutocomplete = false;
    this.showFilterPopup = false;
    this.router.navigate(['/requisitions'], { queryParams });
  }

  selectSuggestion(req: RequisitionDto): void {
    this.showAutocomplete = false;
    this.searchCtrl.setValue('', { emitEvent: false });
    this.router.navigate(['/requisitions', req.id]);
  }

  toggleFilterPopup(): void {
    this.showFilterPopup = !this.showFilterPopup;
    if (this.showFilterPopup) {
      this.loadProjects();
      this.loadCreators();
    }
  }

  loadProjects(): void {
    if (this.projects.length === 0) {
      this.projectService.getAllProjects().subscribe({
        next: (projects) => this.projects = projects,
        error: (err) => console.error('Failed to load projects', err)
      });
    }
  }

  loadCreators(): void {
    if (this.creators.length === 0 && !this.isRequester) {
      this.userService.getAllUsers({ page: 0, size: 1000 }, "").subscribe({
        next: (response) => this.creators = response.content || [],
        error: (err) => console.error('Failed to load creators', err)
      });
    }
  }

  get isRequester(): boolean {
    return this.authService.hasRole('REQUESTER');
  }

  applyFilters(): void {
    this.showFilterPopup = false;
    const queryParams: any = {
      q: this.searchCtrl.value?.trim() || undefined,
      status: this.status || undefined,
      projectId: this.selectedProjectId || undefined,
      createdFrom: this.createdFrom || undefined,
      createdTo: this.createdTo || undefined,
      creatorId: this.selectedCreatorId || undefined
    };
    this.router.navigate(['/requisitions'], { queryParams });
  }

  clearFilters(): void {
    this.status = '';
    this.selectedProjectId = '';
    this.createdFrom = '';
    this.createdTo = '';
    this.selectedCreatorId = '';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const searchContainer = document.getElementById('search-container');
    if (searchContainer && !searchContainer.contains(target)) {
      if (target.closest('.cdk-overlay-container')) {
        return;
      }
      this.showAutocomplete = false;
      this.showFilterPopup = false;
    }
  }

  get currentUserEmail(): string {
    const decoded = this.authService.getDecodedToken();
    return decoded?.sub || decoded?.email || '';
  }

  get currentUserName(): string {
    const decoded = this.authService.getDecodedToken();
    return decoded?.name || 'User';
  }

  get currentUserRole(): string {
    return this.authService.getRole() || '';
  }
}
