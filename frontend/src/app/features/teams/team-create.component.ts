import { Component, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { NgForm } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  TeamCreateDto,
  TeamCreateDtoDepartmentEnum,
  TeamsModuleService,
  UserDto,
  UserModuleService
} from '../../core/api';

interface TeamMemberOption {
  id: number;
  displayName: string;
  subtitle: string;
  initials: string;
}

@Component({
  selector: 'app-team-create',
  templateUrl: './team-create.component.html',
  styleUrls: ['./team-create.component.scss']
})
export class TeamCreateComponent implements OnInit {
  @ViewChild('teamForm') teamForm!: NgForm;

  readonly departmentOptions: Array<{ value: TeamCreateDtoDepartmentEnum; label: string }> = [
    { value: TeamCreateDtoDepartmentEnum.It, label: 'Technology' },
    { value: TeamCreateDtoDepartmentEnum.Rd, label: 'Research and Development' },
    { value: TeamCreateDtoDepartmentEnum.Hr, label: 'Human Resources' },
    { value: TeamCreateDtoDepartmentEnum.Sales, label: 'Sales' },
    { value: TeamCreateDtoDepartmentEnum.Legal, label: 'Legal' }
  ];

  model: {
    name: string;
    leaderId: number | null;
    department: TeamCreateDtoDepartmentEnum | null;
    description: string;
  } = {
      name: '',
      leaderId: null,
      department: null,
      description: ''
    };

  leadOptions: TeamMemberOption[] = [];
  loadingLeads = false;
  submitting = false;
  error: string | null = null;

  membersPanelOpen = false;
  selectedMemberCandidateId: number | null = null;
  additionalMembers: TeamMemberOption[] = [];

  constructor(
    private userService: UserModuleService,
    private teamsService: TeamsModuleService,
    private router: Router,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.loadLeadOptions();
  }

  cancel(): void {
    this.router.navigate(['/teams']);
  }

  submit(): void {
    this.error = null;

    if (this.teamForm.invalid || this.model.department === null) {
      this.error = 'Please complete all required fields before creating the team.';
      this.teamForm.form.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const payload: TeamCreateDto = {
      name: this.model.name.trim(),
      description: this.model.description.trim(),
      department: this.model.department
    };

    const leaderId = this.normalizeLeaderId(this.model.leaderId);
    if (leaderId !== null) {
      payload.leaderId = leaderId;
    }

    this.teamsService.createTeam(payload).subscribe({
      next: () => {
        this.submitting = false;
        this.snackBar.open('Team created successfully.', 'Close', {
          duration: 3500
        });
        this.router.navigate(['/teams']);
      },
      error: (err) => {
        this.submitting = false;
        this.error = this.extractErrorMessage(err);
      }
    });
  }

  toggleMembersPanel(): void {
    this.membersPanelOpen = !this.membersPanelOpen;
  }

  addMember(): void {
    if (this.selectedMemberCandidateId === null) {
      return;
    }

    const candidateId = this.normalizeLeaderId(this.selectedMemberCandidateId);
    const candidate = this.leadOptions.find((option) => option.id === candidateId);

    if (!candidate) {
      return;
    }

    if (this.normalizeLeaderId(this.model.leaderId) === candidate.id) {
      this.error = 'The selected team lead is already assigned as owner.';
      return;
    }

    if (this.additionalMembers.some((member) => member.id === candidate.id)) {
      this.error = 'This member has already been added to the local composition list.';
      return;
    }

    this.error = null;
    this.additionalMembers = [...this.additionalMembers, candidate];
    this.selectedMemberCandidateId = null;
  }

  removeMember(memberId: number): void {
    this.additionalMembers = this.additionalMembers.filter((member) => member.id !== memberId);
  }

  onLeadChanged(): void {
    const currentLeadId = this.normalizeLeaderId(this.model.leaderId);
    if (currentLeadId === null) {
      return;
    }

    this.additionalMembers = this.additionalMembers.filter((member) => member.id !== currentLeadId);
  }

  get leadMember(): TeamMemberOption | null {
    const currentLeadId = this.normalizeLeaderId(this.model.leaderId);
    if (currentLeadId === null) {
      return null;
    }

    return this.leadOptions.find((option) => option.id === currentLeadId) ?? null;
  }

  get availableMemberCandidates(): TeamMemberOption[] {
    const currentLeadId = this.normalizeLeaderId(this.model.leaderId);
    return this.leadOptions.filter((option) => {
      if (currentLeadId === option.id) {
        return false;
      }
      return !this.additionalMembers.some((member) => member.id === option.id);
    });
  }

  trackByMemberId(_index: number, member: TeamMemberOption): number {
    return member.id;
  }

  private normalizeLeaderId(raw: any): number | null {
    if (raw === null || raw === undefined || raw === '') {
      return null;
    }
    const parsed = Number(raw);
    return isNaN(parsed) ? null : parsed;
  }

  private loadLeadOptions(): void {
    this.loadingLeads = true;
    this.error = null;

    this.userService.getAllUsers({ page: 0, size: 100 }).subscribe({
      next: (users) => {
        const normalizedUsers = this.toArray<UserDto>(users);
        const seenIds = new Set<number>();

        this.leadOptions = normalizedUsers
          .map((user) => {
            const coercedId = this.normalizeLeaderId(user.id);
            if (coercedId === null) return null;

            const userWithId = { ...user, id: coercedId };
            return this.mapUserToOption(userWithId);
          })
          .filter((option): option is TeamMemberOption => option !== null)
          .filter((option) => {
            if (seenIds.has(option.id)) {
              return false;
            }
            seenIds.add(option.id);
            return true;
          });

        this.loadingLeads = false;
      },
      error: () => {
        this.loadingLeads = false;
        this.error = 'Failed to load team lead options. Ensure you are logged in as finance or administrator.';
      }
    });
  }

  private mapUserToOption(user: UserDto & { id: number }): TeamMemberOption {
    const fallbackName = user.email ? user.email.split('@')[0] : 'Unknown User';
    const displayName = (user.name ?? '').trim() || this.humanizeFallback(fallbackName);
    const subtitle = this.humanizeRole(user.role ?? 'TEAM_MEMBER');

    return {
      id: user.id,
      displayName,
      subtitle,
      initials: this.getInitials(displayName)
    };
  }

  private humanizeFallback(value: string): string {
    return value
      .split(/[._-]/)
      .filter((part) => part.length > 0)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private humanizeRole(role: string): string {
    return role
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private getInitials(value: string): string {
    const parts = value.trim().split(/\s+/).filter((part) => part.length > 0);
    if (parts.length === 0) {
      return 'NA';
    }

    if (parts.length === 1) {
      return parts[0].substring(0, 2).toUpperCase();
    }

    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  private extractErrorMessage(err: any): string {
    const backendMessage = err?.error?.message;
    if (backendMessage && typeof backendMessage === 'string') {
      return backendMessage;
    }

    return 'Failed to create team. Please review the form and try again.';
  }

  private toArray<T>(value: unknown): T[] {
    if (Array.isArray(value)) {
      return value as T[];
    }

    if (!value || typeof value !== 'object') {
      return [];
    }

    const wrappers = ['content', 'items', 'data', 'results', 'body'];
    for (const key of wrappers) {
      const candidate = (value as Record<string, unknown>)[key];
      if (Array.isArray(candidate)) {
        return candidate as T[];
      }
    }

    return [];
  }
}