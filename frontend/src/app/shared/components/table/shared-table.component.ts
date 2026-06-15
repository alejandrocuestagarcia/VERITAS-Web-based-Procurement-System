import {
  AfterContentInit,
  Component,
  ContentChildren,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  QueryList,
  ViewChild
} from '@angular/core';
import { MatColumnDef, MatTable, MatTableDataSource } from "@angular/material/table";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { debounceTime, distinctUntilChanged, Subject } from "rxjs";

@Component({
  selector: 'app-shared-table',
  templateUrl: './shared-table.component.html',
  styleUrls: ['./shared-table.component.scss']
})
export class SharedTableComponent implements AfterContentInit, OnDestroy {
  @Input() title: string = '';
  @Input() subtitle: string = '';
  @Input() entity: string = '';
  @Input() searchPlaceholder: string = 'Search name, key, project...';
  @Input() loading: boolean = false;
  @Input() searchValue: string = '';

  @Input() dataSource = new MatTableDataSource<any>();
  @Input() displayedColumns: string[] = [];
  @Input() totalElements: number = 0;
  @Input() pageSize: number = 10;

  @Output() pageChanged = new EventEmitter<PageEvent>();
  @Output() searchChanged = new EventEmitter<string>();


  @ViewChild(MatTable, { static: true }) table!: MatTable<any>;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  @ContentChildren(MatColumnDef) columnDefs!: QueryList<MatColumnDef>;

  private searchSubject = new Subject<string>();

  constructor() {
    this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged(),
    ).subscribe(value => {
      this.searchChanged.emit(value)
    })
  }

  public resetToFirstPage(): void {
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
  }

  ngAfterContentInit() {
    this.columnDefs.forEach(columnDef => this.table.addColumnDef(columnDef));
  }

  onSearch(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }


  ngOnDestroy() {
    this.searchSubject.complete();
  }
}
