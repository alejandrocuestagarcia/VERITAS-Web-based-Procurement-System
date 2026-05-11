import {Component, OnInit, ViewChild, ElementRef, OnDestroy} from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  debounceTime,
  distinctUntilChanged,
  forkJoin,
  Observable,
  of,
  startWith,
  Subject,
  Subscription,
  switchMap
} from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  ProjectModuleService,
  RequisitionModuleService,
  WorkflowModuleService,
  ProjectDto,
  WorkflowDto,
  RequisitionCreateDto, RequisitionCreateDtoPriorityEnum
} from '../../../core/api';
import { MatDialog } from '@angular/material/dialog';
import { WorkflowEditorComponent } from '../../workflow/workflow-editor/workflow-editor.component';

@Component({
  selector: 'app-requisition-create',
  templateUrl: './requisition-create.component.html'
})
export class RequisitionCreateComponent implements OnInit,OnDestroy {
  basicInfoForm!: FormGroup;
  lineItemsForm!: FormGroup;
  loading = false;

  projects: ProjectDto[] = [];
  projectSearch: string = '';
  workflows: WorkflowDto[] = [];
  workflowSearch: string = '';
  priorities = Object.values(RequisitionCreateDtoPriorityEnum)
  uploadedFiles: File[] = [];
  isDragging = false;

  @ViewChild('projectSearchInput') projectSearchInput!: ElementRef<HTMLInputElement>;
  @ViewChild('workflowSearchInput') workflowSearchInput!: ElementRef<HTMLInputElement>;
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private projectService: ProjectModuleService,
    private workflowService: WorkflowModuleService,
    private requisitionService: RequisitionModuleService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) { }

  private workflowSearch$ = new Subject<string>();
  private searchSubscription?: Subscription;

  ngOnInit(): void {
    this.initForms();
    this.loadData();

    this.searchSubscription = this.workflowSearch$.pipe(
      startWith(''),
      debounceTime(400),
      distinctUntilChanged(),
      switchMap(search => {
        const pageable = { page: 0, size: 10, sort: ['name,asc'] };
        return this.workflowService.getAllWorkflows(pageable, search, true);
      })
    ).subscribe({
      next: (response) => {
        this.workflows = response.content || [];
      },
      error: () => {
        this.snackBar.open('Error searching workflows', 'Close', { duration: 3000 });
      }
    });
  }

  getFilteredProjects(): ProjectDto[] {
    if (!this.projectSearch) return this.projects;
    const search = this.projectSearch.toLowerCase();
    return this.projects.filter(p => p.name?.toLowerCase().includes(search));
  }

  onWorkflowSearchChange(value: string): void{
    this.workflowSearch = value;
    this.workflowSearch$.next(value);
  }

  getSelectedProjectName(): string {
    const id = this.basicInfoForm.get('projectId')?.value;
    const project = this.projects.find(p => p.id === id);
    return project?.name || 'Not selected';
  }

  getSelectedWorkflowName(): string {
    const id = this.basicInfoForm.get('workflowDefinitionId')?.value;
    const workflow = this.workflows.find(w => w.id === id);
    return workflow?.name || 'Not selected';
  }


  onOpenedChange(opened: boolean): void {
    if (!opened) {
      this.projectSearch = '';
    } else {
      setTimeout(() => {
        if (this.projectSearchInput) {
          this.projectSearchInput.nativeElement.focus();
        }
      }, 100);
    }
  }
  viewWorkflow(id: number | undefined): void {
    if (id) {
      // TODO: Change this to a read-only component, once viewing workflows by ID is implemented.
      // Currently using WorkflowEditorComponent as a placeholder.
      this.dialog.open(WorkflowEditorComponent, {
        width: '90vw',
        height: '90vh',
        maxWidth: '1200px',
        panelClass: 'overflow-hidden'
      });
    }
  }

  onFileSelected(event: any): void {
    const files = event.target.files;
    this.addFiles(files);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    const files = event.dataTransfer?.files;
    this.addFiles(files);
  }

  private addFiles(files: FileList | null | undefined): void {
    if (files) {
      for (let i = 0; i < files.length; i++) {
        this.uploadedFiles.push(files[i]);
      }
    }
  }

  removeFile(index: number): void {
    this.uploadedFiles.splice(index, 1);
  }

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  private initForms(): void {
    this.basicInfoForm = this.fb.group({
      requestName: ['', Validators.required],
      projectId: [null, Validators.required],
      priority: ['MEDIUM', Validators.required],
      workflowDefinitionId: [null, Validators.required],
      description: ['']
    });

    this.lineItemsForm = this.fb.group({
      items: this.fb.array([this.createItem()])
    });
  }

  private createItem(): FormGroup {
    return this.fb.group({
      name: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unit: ['', Validators.required],
      description: ['']
    });
  }

  get items(): FormArray {
    return this.lineItemsForm.get('items') as FormArray;
  }

  addItem(): void {
    this.items.push(this.createItem());
  }

  removeItem(index: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(index);
    }
  }

  private loadData(): void {
    this.projectService.getAllProjects().subscribe({
      next: (projects) => this.projects = projects,
      error: () => this.snackBar.open('Failed to load projects', 'Close', { duration: 3000 })
    });
  }

  onSubmit(): void {
    if (this.basicInfoForm.valid && this.lineItemsForm.valid) {
      this.loading = true;
      const request: RequisitionCreateDto = {
        requestName: this.basicInfoForm.value.requestName,
        projectId: this.basicInfoForm.value.projectId,
        workflowDefinitionId: this.basicInfoForm.value.workflowDefinitionId,
        priority: this.basicInfoForm.value.priority as any,
        description: this.basicInfoForm.value.description,
        items: this.items.value.map((item: any) => ({
          name: item.name,
          quantity: item.quantity,
          unit: item.unit,
          description: item.description
        }))
      };

      this.requisitionService.createRequest(request).subscribe({
        next: (createdRequest) => {
          if (this.uploadedFiles.length > 0 && createdRequest.id) {
            const uploadTasks: Observable<any>[] = this.uploadedFiles.map(file => {
              return this.requisitionService.uploadQuotes(createdRequest.id!, file as any).pipe(
                catchError(err => {
                  return of(null);
                })
              );
            });

            forkJoin(uploadTasks).subscribe({
              next: (results) => {
                this.loading = false;
                this.snackBar.open('Procurement request & attachments saved successfully', 'Close', { duration: 3000 });
                this.router.navigate(['/dashboard']);
              },
              error: (err) => {
                this.loading = false;
              }
            });
          } else {
            this.loading = false;
            this.snackBar.open('Procurement request created successfully', 'Close', { duration: 3000 });
            this.router.navigate(['/dashboard']);
          }
        },
        error: (err) => {
          this.loading = false;
          this.snackBar.open('Failed to create request: ' + (err.error?.message || 'Unknown error'), 'Close', { duration: 5000 });
        }
      });
    } else {
      this.snackBar.open('Please fill out all required fields properly.', 'Close', { duration: 3000 });
      this.basicInfoForm.markAllAsTouched();
      this.lineItemsForm.markAllAsTouched();
    }
  }

  onCancel(): void {
    this.router.navigate(['/dashboard']);
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe()
  }
}
