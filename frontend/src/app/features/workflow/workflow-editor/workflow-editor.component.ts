import { Component, ElementRef, OnInit, ViewChild, OnDestroy } from '@angular/core';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer';
import { editorModules, viewerModules } from '../custom-renderer';
import { WorkflowModuleService, WorkflowSaveDto, UserDtoRoleEnum } from 'src/app/core/api';
import { ToastService } from 'src/app/core/services/toast.service';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { dummyBpmnXml } from './workflow-editor.constants';
import {AuthService} from "../../../core/services/auth.service";

export type WorkflowMode = 'create' | 'edit' | 'view';

@Component({
  selector: 'app-workflow-editor',
  templateUrl: './workflow-editor.component.html',
  styleUrls: ['./workflow-editor.component.scss']
})
export class WorkflowEditorComponent implements OnInit, OnDestroy {
  @ViewChild('canvas', { static: true }) private canvas!: ElementRef;

  private bpmnInstance: any;

  mode: WorkflowMode = 'create';
  workflowForm: FormGroup;
  workflowId: number | null = null;
  workflowVersion: number | null = null;
  workflowName: string | null = null;
  workflowDescription: string | null = null;
  workflowIsActive: boolean | null = null;

  public showPropertiesPanelTransition = false;
  public showPropertiesPanelTask = false;
  public selectedElementId = '';
  public currentTask: any = {
    role: '',
    description: ''
  };
  public roles = Object.values(UserDtoRoleEnum);
  public currentRule: any = {
    isPdfRequired: false,
    isCsvRequired: false,
    isImageRequired: false,
    minRequiredVendors: 0,
    optionalFailureMessage: '',
    description: ''
  };

  get isEditable(): boolean {
    return this.mode !== 'view';
  }

  get isFinanceOfficer(): boolean {
    return this.authService.hasRole('FINANCE_OFFICER');
  }

  get isAdministrator(): boolean {
    return this.authService.hasRole('ADMINISTRATOR');
  }

  constructor(
    private workflowService: WorkflowModuleService,
    public router: Router,
    public authService: AuthService,
    private route: ActivatedRoute,
    private toastService: ToastService,
    private fb: FormBuilder
  ) {
    this.workflowForm = this.fb.group({
      title: ['', Validators.required],
      description: ['']
    });
  }

  async ngOnInit() {
    this.mode = (this.route.snapshot.data['mode'] as WorkflowMode) ?? 'create';

    const BpmnClass = this.mode === 'view' ? BpmnViewer : BpmnModeler;
    const modules = this.mode === 'view' ? viewerModules : editorModules;

    this.bpmnInstance = new BpmnClass({
      container: this.canvas.nativeElement,
      additionalModules: [modules]
    });

    this.bpmnInstance.on('selection.changed', (event: any) => {
      const selection = event.newSelection[0];
      if (selection) {
        if (selection.type === 'bpmn:SequenceFlow') {
          this.showPropertiesPanelTask = false;
          this.showPropertiesPanelTransition = true;
          this.loadTransitionRules(selection);
        } else if (selection.type === 'bpmn:Task') {
          this.showPropertiesPanelTransition = false;
          this.showPropertiesPanelTask = true;
          this.selectedElementId = selection.id;
          this.loadTaskDetails(selection);
        } else {
          this.showPropertiesPanelTransition = false;
          this.showPropertiesPanelTask = false;
        }
      } else {
        this.showPropertiesPanelTransition = false;
        this.showPropertiesPanelTask = false;
        this.resetRule();
        this.resetTask();
      }
    });

    if (this.mode !== 'view') {
      this.bpmnInstance.on('commandStack.shape.create.postExecuted', (event: any) => {
        const { context } = event;
        const { shape } = context;
        if (shape.type === 'bpmn:ExclusiveGateway') {
          setTimeout(() => {
            this.bpmnInstance.get('directEditing').activate(shape);
          }, 50);
        }
      });
    }

    if (this.mode === 'create') {
      await this.loadXml(dummyBpmnXml);
    } else {
      const id = Number(this.route.snapshot.paramMap.get('id'));
      this.workflowId = id;
      this.workflowService.getWorkflow(id).subscribe({
        next: async (workflow) => {
          this.workflowVersion = workflow.version ?? 0;
          this.workflowName = workflow.name ?? '';
          this.workflowDescription = workflow.description ?? '';
          this.workflowIsActive = workflow.isActive ?? false;
          this.workflowForm.patchValue({ title: this.workflowName, description: this.workflowDescription });
          await this.loadXml(workflow.bpmnXml ?? '');
        },
        error: (err) => {
          console.error('Failed to load workflow', err);
          this.toastService.showError('Failed to load workflow');
          this.router.navigate(['/workflows']);
        }
      });
    }
  }

  private async loadXml(xml: string): Promise<void> {
    try {
      await this.bpmnInstance.importXML(xml);
      const canvas = this.bpmnInstance.get('canvas');

      canvas.zoom('fit-viewport', 'auto');
      setTimeout(() => {
        canvas.resized();
        canvas.zoom('fit-viewport', 'auto');
      }, 1);

      this.applyTransitionRuleCss();
    } catch (err) {
      console.error('Failed to render workflow', err);
    }
  }

  zoomIn() { this.bpmnInstance.get('zoomScroll').stepZoom(1); }
  zoomOut() { this.bpmnInstance.get('zoomScroll').stepZoom(-1); }
  resetZoom() { this.bpmnInstance.get('canvas').zoom('fit-viewport', 'auto'); }

  private async getUpdatedBpmnXml(): Promise<string> {
    const modeling = this.bpmnInstance.get('modeling');
    const bpmnFactory = this.bpmnInstance.get('bpmnFactory');
    const rootElement = this.bpmnInstance.get('canvas').getRootElement();

    const title = this.workflowForm.value.title;
    const description = this.workflowForm.value.description;

    const documentation = bpmnFactory.create('bpmn:Documentation', { text: description || '' });
    modeling.updateProperties(rootElement, {
      name: title,
      documentation: [documentation]
    });

    const { xml } = await this.bpmnInstance.saveXML({ format: true });
    return xml;
  }

  async submitWorkflow() {
    if (this.workflowForm.invalid) {
      this.workflowForm.markAllAsTouched();
      return;
    }

    try {
      const xml = await this.getUpdatedBpmnXml();
      const payload: WorkflowSaveDto = { bpmnXml: xml };

      if (this.mode === 'edit') {
        this.workflowService.editWorkflow(this.workflowId ?? 0, payload).subscribe({
          next: (workflow) => {
            this.toastService.showSuccess('Workflow updated successfully!');
            this.router.navigate(['/workflows/view/', workflow.id]);
          },
          error: (err) => {
            console.error('Failed to update workflow', err);
            this.toastService.showError('Failed to update workflow');
          }
        });
      } else {
        this.workflowService.saveWorkflow(payload).subscribe({
          next: () => {
            this.toastService.showSuccess('Workflow saved successfully!');
            this.router.navigate(['/workflows']);
          },
          error: (err) => {
            console.error('Failed to save workflow', err);
            this.toastService.showError('Failed to save workflow');
          }
        });
      }
    } catch (err) {
      console.error('Failed to process workflow', err);
      this.toastService.showError('Failed to process workflow');
    }
  }

  async exportXML() {
    try {
      const xml = await this.getUpdatedBpmnXml();
      const blob = new Blob([xml], { type: 'application/xml' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      const title = this.workflowForm.value.title;
      a.download = `${title || 'workflow'}.bpmn`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Failed to export XML', err);
    }
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = async (e: any) => {
        await this.loadXml(e.target.result);
      };
      reader.readAsText(file);
    }
    event.target.value = '';
  }

  navigateToEdit() {
    if (this.workflowId) {
      this.router.navigate(['/workflows/edit', this.workflowId]);
    }
  }

  ngOnDestroy() { this.bpmnInstance?.destroy(); }

  loadTransitionRules(selection: any) {
    this.selectedElementId = selection.id;
    const elementRegistry = this.bpmnInstance.get('elementRegistry');
    const element = elementRegistry.get(selection.id);

    if (!element?.businessObject) {
      console.warn(`Could not find businessObject for ID: ${selection.id}`);
      this.resetRule();
      return;
    }

    const bo = element.businessObject;
    const doc = bo.get('documentation')?.[0]?.text || '';
    const extensions = bo.extensionElements;
    if (extensions?.values) {
      const rule = extensions.values.find((e: any) =>
        e.$type === 'veritas:transitionRule' || e.type === 'veritas:transitionRule'
      );
      if (rule) {
        this.currentRule = {
          isPdfRequired: String(rule.isPdfRequired) === 'true',
          isCsvRequired: String(rule.isCsvRequired) === 'true',
          isImageRequired: String(rule.isImageRequired) === 'true',
          minRequiredVendors: parseInt(rule.minRequiredVendors || '0'),
          optionalFailureMessage: rule.optionalFailureMessage || '',
          description: doc
        };
        return;
      }
    }
    this.currentRule = {
      isPdfRequired: false,
      isCsvRequired: false,
      isImageRequired: false,
      minRequiredVendors: 0,
      optionalFailureMessage: '',
      description: doc
    };
  }

  private resetRule() {
    this.currentRule = {
      isPdfRequired: false,
      isCsvRequired: false,
      isImageRequired: false,
      minRequiredVendors: 0,
      optionalFailureMessage: '',
      description: ''
    };
  }

  loadTaskDetails(selection: any) {
    this.selectedElementId = selection.id;
    const elementRegistry = this.bpmnInstance.get('elementRegistry');
    const element = elementRegistry.get(selection.id);
    if (!element?.businessObject) {
      this.resetTask();
      return;
    }
    const bo = element.businessObject;
    const docs = bo.get('documentation') || [];

    const assigneeDoc = docs.find((d: any) => d.text && d.text.startsWith('[ASSIGNEE]'));
    const assignee = assigneeDoc ? assigneeDoc.text.substring(10) : '';

    const descDoc = docs.find((d: any) => !d.text || !d.text.startsWith('[ASSIGNEE]'));
    const description = descDoc ? descDoc.text : '';

    this.currentTask = {
      role: assignee,
      description: description
    };
  }

  private resetTask() {
    this.currentTask = { role: '', description: '' };
  }

  updateTaskProperty(key: 'role' | 'description', value: string) {
    const directEditing = this.bpmnInstance.get('directEditing');
    if (directEditing.isActive()) {
      directEditing.complete();
    }

    const modeling = this.bpmnInstance.get('modeling');
    const elementRegistry = this.bpmnInstance.get('elementRegistry');
    const element = elementRegistry.get(this.selectedElementId);
    if (!element) return;

    const bo = element.businessObject;
    const bpmnFactory = this.bpmnInstance.get('bpmnFactory');
    let docs = bo.get('documentation') || [];

    if (key === 'role') {
      docs = docs.filter((d: any) => !d.text || !d.text.startsWith('[ASSIGNEE]'));
      if (value) {
        const doc = bpmnFactory.create('bpmn:Documentation', { text: `[ASSIGNEE]${value}` });
        docs.push(doc);
      }
    } else if (key === 'description') {
      docs = docs.filter((d: any) => d.text && d.text.startsWith('[ASSIGNEE]'));
      if (value) {
        const doc = bpmnFactory.create('bpmn:Documentation', { text: value });
        docs.push(doc);
      }
    }

    modeling.updateProperties(element, { documentation: docs });
    this.currentTask[key] = value;
  }

  updateRuleProperty(key: 'description' | 'minRequiredVendors' | 'isPdfRequired' | 'isCsvRequired' | 'isImageRequired' | 'optionalFailureMessage', value: any) {
    const directEditing = this.bpmnInstance.get('directEditing');
    if (directEditing.isActive()) {
      directEditing.complete();
    }

    const modeling = this.bpmnInstance.get('modeling');
    const elementRegistry = this.bpmnInstance.get('elementRegistry');
    const element = elementRegistry.get(this.selectedElementId);
    if (!element) return;

    if (key === 'description') {
      const bpmnFactory = this.bpmnInstance.get('bpmnFactory');
      const documentation = bpmnFactory.create('bpmn:Documentation', { text: value });
      modeling.updateProperties(element, { documentation: [documentation] });
    } else if (key === 'minRequiredVendors' || key === 'isPdfRequired' || key === 'isCsvRequired' || key === 'isImageRequired' || key === 'optionalFailureMessage') {
      const moddle = this.bpmnInstance.get('moddle');
      let extensionElements = element.businessObject.extensionElements;
      if (!extensionElements) {
        extensionElements = moddle.create('bpmn:ExtensionElements', { values: [] });
      }

      let rule = extensionElements.values?.find((e: any) =>
        e.$type === 'veritas:transitionRule' || e.type === 'veritas:transitionRule'
      );

      if (!rule) {
        try {
          rule = moddle.create('veritas:transitionRule');
        } catch (e) {
          rule = moddle.createAny('veritas:transitionRule', 'http://veritas', {
            $type: 'veritas:transitionRule',
            type: 'veritas:transitionRule'
          });
        }
        if (!extensionElements.values) {
          extensionElements.values = [];
        }
        extensionElements.values.push(rule);
      }

      rule[key] = value;
      modeling.updateProperties(element, { extensionElements });
    }
    this.currentRule[key] = value;
  }

  private applyTransitionRuleCss() {
    const canvas = this.bpmnInstance.get('canvas');
    const elementRegistry = this.bpmnInstance.get('elementRegistry');

    elementRegistry.forEach((element: any) => {
      if (element.type === 'bpmn:SequenceFlow') {
        const extensions = element.businessObject.extensionElements;
        const hasConstraint = extensions?.values?.some((val: any) =>
          [val.$type, val.type].includes('veritas:transitionRule')
        );
        if (hasConstraint) {
          canvas.addMarker(element.id, 'highlight');
        }
      }
    });
  }
}


