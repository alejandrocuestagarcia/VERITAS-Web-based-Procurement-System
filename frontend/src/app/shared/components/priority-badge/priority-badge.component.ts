import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-priority-badge',
  templateUrl: './priority-badge.component.html',
})
export class PriorityBadgeComponent {
  @Input() priority: string | null | undefined = '';

  getPriorityColor(): string {
    switch (this.priority) {
      case 'LOW': return 'text-slate-500';
      case 'MEDIUM': return 'text-blue-600';
      case 'HIGH': return 'text-orange-600';
      case 'CRITICAL': return 'text-red-600 font-bold';
      default: return 'text-slate-500';
    }
  }

  getIcon(): string {
    switch (this.priority) {
      case 'LOW': return 'arrow_downward';
      case 'MEDIUM': return 'remove';
      case 'HIGH': return 'arrow_upward';
      case 'CRITICAL': return 'warning';
      default: return 'priority_high';
    }
  }
}
