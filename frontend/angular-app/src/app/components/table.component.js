var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
let TableComponent = class TableComponent {
    constructor() {
        this.columns = [];
        this.data = [];
    }
};
__decorate([
    Input()
], TableComponent.prototype, "columns", void 0);
__decorate([
    Input()
], TableComponent.prototype, "data", void 0);
TableComponent = __decorate([
    Component({
        selector: 'app-table',
        standalone: true,
        imports: [CommonModule],
        template: `
    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th *ngFor="let column of columns"
                [style.text-align]="column.align || 'left'">
              {{ column.header }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let row of data">
            <td *ngFor="let column of columns"
                [style.text-align]="column.align || 'left'">
              <ng-container *ngIf="row[column.key] !== null && row[column.key] !== undefined">
                {{ row[column.key] }}
              </ng-container>
              <ng-container *ngIf="row[column.key] === null || row[column.key] === undefined">
                -
              </ng-container>
            </td>
          </tr>
          <tr *ngIf="data.length === 0">
            <td [attr.colspan]="columns.length" style="text-align: center; padding: 40px;">
              Aucune donnee disponible
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
        styles: [`
    .table-wrapper {
      overflow-x: auto;
      border-radius: 12px;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      background: linear-gradient(to bottom, rgba(255,255,255,1), rgba(248,249,250,0.5));
      border-radius: 12px;
      overflow: hidden;
    }

    thead {
      background: linear-gradient(135deg, var(--blue), var(--accent));
      color: white;
      font-weight: 600;
    }

    th,
    td {
      padding: 14px 8px;
      border-bottom: 1px solid var(--line);
      text-align: left;
    }

    tbody tr:hover {
      background: linear-gradient(135deg, rgba(0, 35, 149, 0.02), rgba(239, 51, 64, 0.02));
    }

    tbody tr:last-child td {
      border-bottom: none;
    }
  `]
    })
], TableComponent);
export { TableComponent };
