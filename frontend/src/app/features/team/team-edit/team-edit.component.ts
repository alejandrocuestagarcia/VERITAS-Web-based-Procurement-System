import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  TeamDto,
  TeamEditDto,
  TeamsModuleService,
  UserDto,
  UserModuleService,
  DepartmentsModuleService,
  DepartmentDto
} from '../../../core/api';
import { ToastService } from '../../../core/services/toast.service';

interface TeamMemberOption {
  id: number;
  displayName: string;
  subtitle: string;
  initials: string;
  currentTeam: string | null;
}

@Component({
  selector: 'app-team-edit',
  templateUrl: './team-edit.component.html',
  styleUrls: ['./team-edit.component.scss']
})
export class TeamEditComponent implements OnInit {
  teamForm!: FormGroup;
  teamId!: number;
  teamName = '';
  team: TeamDto | null = null;

  currentLeaderId: number | null = null;
  previousLeaderId: number | null = null;
  currentMemberIds = new Set<number>();

  departments: DepartmentDto[] = [];

  leadOptions: TeamMemberOption[] = [];
  loading = false;
  submitting = false;
  error: string | null = null;

  membersPanelOpen = false;
  selectedMemberCandidateId: number | null = null;
  additionalMembers: TeamMemberOption[] = [];

  constructor(
    private userService: UserModuleService,
    private teamsService: TeamsModuleService,
    private departmentsService: DepartmentsModuleService,
    private router: Router,
    private route: ActivatedRoute,
    private toastService: ToastService,
    private fb: FormBuilder
  ) { }

  ngOnInit(): void {
    this.teamId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.teamId) {
      this.toastService.showError('Invalid team identifier.');
      this.router.navigate(['/teams']);
      return;
    }

    this.initForm();
    this.loadTeamData();
  }

  private initForm(): void {
    this.teamForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
      leaderId: [null],
      departmentId: [null, Validators.required],
      description: ['', [Validators.required, Validators.maxLength(500)]]
    });

    this.teamForm.get('leaderId')?.valueChanges.subscribe(() => {
      this.onLeadChanged();
    });
  }

  private loadTeamData(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      team: this.teamsService.getTeam(this.teamId),
      users: this.userService.getAllUsers({ page: 0, size: 100 }),
      departments: this.departmentsService.getAllDepartments()
    }).subscribe({
      next: ({ team, users, departments }) => {
        this.departments = this.toArray<DepartmentDto>(departments);
        const normalizedUsers = this.toArray<UserDto>(users);
        const seenIds = new Set<number>();

        this.leadOptions = normalizedUsers
          .map((user) => {
            const coercedId = this.normalizeLeaderId(user.id);
            if (coercedId === null) return null;
            return this.mapUserToOption({ ...user, id: coercedId });
          })
          .filter((option): option is TeamMemberOption => option !== null)
          .filter((option) => {
            if (seenIds.has(option.id)) {
              return false;
            }
            seenIds.add(option.id);
            return true;
          });

        this.applyTeamData(team);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Failed to load team data. Please try again.';
      }
    });
  }

  private applyTeamData(team: TeamDto): void {
    this.team = team;
    this.teamName = team.name?.trim() || 'Team';
    this.currentLeaderId = this.normalizeLeaderId(team.leaderId);
    this.previousLeaderId = this.currentLeaderId;

    const leaderId = this.currentLeaderId;

    const dept = this.departments.find(d => d.name === team.department);

    this.teamForm.patchValue({
      name: team.name ?? '',
      leaderId: leaderId,
      departmentId: dept?.id ?? null,
      description: team.description ?? ''
    });

    const members = Array.isArray(team.members) ? team.members : [];
    const uniqueMembers = new Map<number, UserDto & { id: number }>();

    members.forEach((member) => {
      const memberId = this.normalizeLeaderId(member.id);
      if (memberId === null) return;
      uniqueMembers.set(memberId, { ...member, id: memberId });
    });

    this.currentMemberIds = new Set(uniqueMembers.keys());

    this.additionalMembers = Array.from(uniqueMembers.values())
      .filter((member) => leaderId === null || member.id !== leaderId)
      .map((member) => this.mapUserToOption(member));
  }

  cancel(): void {
    this.router.navigate(['/teams']);
  }

  submit(): void {
    this.error = null;

    if (this.loading) {
      return;
    }

    if (this.teamForm.invalid) {
      this.error = 'Please complete all required fields before saving changes.';
      this.teamForm.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const formValues = this.teamForm.value;
    const leaderId = this.normalizeLeaderId(formValues.leaderId);

    const payload: TeamEditDto = {
      name: formValues.name.trim(),
      description: formValues.description.trim(),
      departmentId: formValues.departmentId,
      memberIds: this.additionalMembers.map((member) => member.id)
    };

    if (leaderId !== null) {
      payload.leaderId = leaderId;
    }

    if (leaderId === null && this.currentLeaderId !== null) {
      payload.clearLeader = true;
    }

    this.teamsService.editTeam(this.teamId, payload as TeamEditDto).subscribe({
      next: () => {
        this.submitting = false;
        this.toastService.showSuccess('Team updated successfully.');
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

    if (this.isAssignedElsewhere(candidate)) {
      this.error = `"${candidate.displayName}" is already assigned to another team. Remove them first.`;
      return;
    }

    if (this.normalizeLeaderId(this.teamForm.value.leaderId) === candidate.id) {
      this.error = 'The selected team lead is already assigned as leader.';
      return;
    }

    if (this.additionalMembers.some((member) => member.id === candidate.id)) {
      this.error = 'This member has already been added to the team composition.';
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
    const currentLeadId = this.normalizeLeaderId(this.teamForm.value.leaderId);

    if (this.previousLeaderId !== null && this.previousLeaderId !== currentLeadId) {
      const oldLeaderOption = this.leadOptions.find(o => o.id === this.previousLeaderId);
      if (oldLeaderOption && !this.additionalMembers.some(m => m.id === oldLeaderOption.id)) {
        this.additionalMembers = [...this.additionalMembers, oldLeaderOption];
      }
    }

    if (currentLeadId !== null) {
      this.additionalMembers = this.additionalMembers.filter((member) => member.id !== currentLeadId);
    }

    this.previousLeaderId = currentLeadId;
  }

  get leadMember(): TeamMemberOption | null {
    if (!this.teamForm) return null;
    const currentLeadId = this.normalizeLeaderId(this.teamForm.value.leaderId);
    if (currentLeadId === null) {
      return null;
    }

    return this.leadOptions.find((option) => option.id === currentLeadId) ?? null;
  }

  get availableMemberCandidates(): TeamMemberOption[] {
    if (!this.teamForm) return [];
    const currentLeadId = this.normalizeLeaderId(this.teamForm.value.leaderId);

    return this.leadOptions.filter((option) => {
      if (currentLeadId === option.id) {
        return false;
      }

      if (this.additionalMembers.some((member) => member.id === option.id)) {
        return false;
      }

      return !this.isAssignedElsewhere(option);
    });
  }

  get restrictedLeadOptions(): TeamMemberOption[] {
    return this.leadOptions.filter((option) => !this.isAssignedElsewhere(option));
  }

  get hasExistingLeader(): boolean {
    return this.currentLeaderId !== null;
  }

  trackByMemberId(_index: number, member: TeamMemberOption): number {
    return member.id;
  }

  get selectedLeadOption(): TeamMemberOption | null {
    const leaderId = this.normalizeLeaderId(this.teamForm?.value?.leaderId);
    if (leaderId === null) return null;
    return this.leadOptions.find(o => o.id === leaderId) ?? null;
  }

  get selectedMemberCandidateOption(): TeamMemberOption | null {
    if (this.selectedMemberCandidateId === null) return null;
    return this.availableMemberCandidates.find(c => c.id === this.selectedMemberCandidateId) ?? null;
  }

  private normalizeLeaderId(raw: any): number | null {
    if (raw === null || raw === undefined || raw === '') {
      return null;
    }
    const parsed = Number(raw);
    return isNaN(parsed) ? null : parsed;
  }

  private isAssignedElsewhere(option: TeamMemberOption): boolean {
    if (!option.currentTeam) {
      return false;
    }

    if (this.currentMemberIds.has(option.id)) {
      return false;
    }

    if (this.currentLeaderId === option.id) {
      return false;
    }

    return true;
  }

  private mapUserToOption(user: UserDto & { id: number }): TeamMemberOption {
    const fallbackName = user.email ? user.email.split('@')[0] : 'Unknown User';
    const displayName = (user.name ?? '').trim() || this.humanizeFallback(fallbackName);
    const subtitle = this.humanizeRole(user.role ?? 'TEAM_MEMBER');

    return {
      id: user.id,
      displayName,
      subtitle,
      initials: this.getInitials(displayName),
      currentTeam: user.teamName ?? null
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
    if (typeof err?.error === 'string' && err.error.length > 0) {
      return err.error;
    }

    const backendMessage = err?.error?.message;
    if (backendMessage && typeof backendMessage === 'string') {
      return backendMessage;
    }

    return 'Failed to update the team. Please review the form and try again.';
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
