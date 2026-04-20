import {
  AfterContentInit,
  Component,
  ContentChildren,
  EventEmitter,
  Input,
  Output,
  QueryList,
  ViewChild
} from '@angular/core';
import {MatColumnDef, MatTable, MatTableDataSource} from "@angular/material/table";
import {PageEvent} from "@angular/material/paginator";

@Component({
  selector: 'app-shared-table',
  templateUrl: './shared-table.component.html',
  styleUrls: ['./shared-table.component.scss']
})
export class SharedTableComponent implements AfterContentInit {
  @Input() title: string = '';
  @Input() subtitle: string = '';
  @Input() searchPlaceholder: string = 'Search...';
  @Input() loading: boolean = false;

  @Input() dataSource = new MatTableDataSource<any>();
  @Input() displayedColumns: string[] = [];
  @Input() totalElements: number = 0;
  @Input() pageSize: number = 10;

  @Output() search = new EventEmitter<string>();
  @Output() pageChanged = new EventEmitter<PageEvent>();

  @ViewChild(MatTable, { static: true }) table!: MatTable<any>;

  @ContentChildren(MatColumnDef) columnDefs!: QueryList<MatColumnDef>;

  ngAfterContentInit() {
    this.columnDefs.forEach(columnDef => this.table.addColumnDef(columnDef));
  }

  onSearch(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.search.emit(value);
  }
}
