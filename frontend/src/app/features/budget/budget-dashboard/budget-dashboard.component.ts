import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import {
  FinancialGovernanceModuleService,
  DepartmentsModuleService,
  TeamsModuleService,
  ProjectModuleService,
  BudgetDashboardDto,
  DepartmentDto,
  TeamDto,
  ProjectDto
} from '../../../core/api';
import { EditBudgetDialogComponent } from '../edit-budget-dialog/edit-budget-dialog.component';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../shared/utils/error-utils';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexYAxis,
  ApexPlotOptions,
  ApexDataLabels,
  ApexTooltip,
  ApexGrid,
  ApexLegend,
  ApexStroke,
  ApexFill
} from 'ng-apexcharts';

export type ChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  plotOptions: ApexPlotOptions;
  dataLabels: ApexDataLabels;
  colors: string[];
  tooltip: ApexTooltip;
  grid: ApexGrid;
  legend: ApexLegend;
  stroke: ApexStroke;
  fill: ApexFill;
};

interface DepartmentDisplay extends DepartmentDto {
  leadName?: string;
  utilizationRate: number;
  status: 'IN RANGE' | 'WARNING' | 'OVER BUDGET';
  displayStatus: string;
}

interface BudgetAlert {
  departmentName: string;
  type: 'CRITICAL' | 'WARNING';
  message: string;
  details: string;
}

@Component({
  selector: 'app-budget-dashboard',
  templateUrl: './budget-dashboard.component.html',
  styleUrls: ['./budget-dashboard.component.scss']
})
export class BudgetDashboardComponent implements OnInit {
  loading = false;
  dashboardStats: BudgetDashboardDto = {
    totalBudget: 0,
    committedFunds: 0,
    actualSpend: 0,
    safetyBuffer: 0,
    exists: false,
    burndownData: []
  };

  departments: DepartmentDisplay[] = [];
  alerts: BudgetAlert[] = [];
  projectedBurn = 0;
  fiscalRunway = 0;
  emergencyReserve = 0;
  selectedPeriod = 'all';
  selectedYear = new Date().getFullYear();
  selectedDepartmentId = 0;
  allProjects: ProjectDto[] = [];
  selectedProjectDeptId = 0;

  projectDonutChartOptions: any = {
    series: [],
    chart: { type: 'donut' },
    labels: [],
    colors: [],
    legend: { show: false },
    dataLabels: { enabled: false },
    tooltip: {}
  };

  trendChartOptions: any = {
    series: [],
    chart: { type: 'bar' },
    xaxis: {},
    yaxis: {},
    colors: [],
    legend: { show: false },
    dataLabels: { enabled: false },
    tooltip: {}
  };

  chartOptions: ChartOptions = {
    series: [],
    chart: { type: 'area' },
    xaxis: {},
    yaxis: {},
    plotOptions: {},
    dataLabels: { enabled: false },
    colors: [],
    tooltip: {},
    grid: {},
    legend: { show: false },
    stroke: {},
    fill: {}
  };

  donutChartOptions: any = {
    series: [],
    chart: { type: 'donut' },
    labels: [],
    colors: [],
    legend: { show: false },
    dataLabels: { enabled: false },
    tooltip: {}
  };

  barChartOptions: any = {
    series: [],
    chart: { type: 'bar' },
    xaxis: {},
    yaxis: {},
    colors: [],
    legend: { show: false },
    dataLabels: { enabled: false },
    stroke: {},
    plotOptions: {},
    tooltip: {}
  };

  constructor(
    private financialService: FinancialGovernanceModuleService,
    private departmentService: DepartmentsModuleService,
    private teamsService: TeamsModuleService,
    private projectService: ProjectModuleService,
    private dialog: MatDialog,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;

    forkJoin({
      stats: this.financialService.getFinanceDashboard(this.selectedDepartmentId || undefined),
      depts: this.departmentService.getAllDepartments(true),
      teams: this.teamsService.getAllTeams(true),
      projects: this.projectService.getAllProjects(true)
    }).subscribe({
      next: (data) => {
        console.log('Dashboard stats from server:', data.stats);
        this.dashboardStats = data.stats;
        this.allProjects = data.projects;
        this.processDepartmentsAndTeams(data.depts, data.teams);
        this.calculateSecondaryMetrics();
        this.generateAlerts();
        this.generateChartData();
        this.generateTrendChartData();
        this.updateProjectChartData();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading data:', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load financial governance data'));
        this.loading = false;
      }
    });
  }

  onPeriodChange(): void {
    this.generateChartData();
    this.generateTrendChartData();
  }

  onYearChange(): void {
    this.loadData();
  }

  onDepartmentChange(): void {
    this.loadData();
  }

  processDepartmentsAndTeams(depts: DepartmentDto[], teams: TeamDto[]): void {
    const deptLeads = new Map<string, string>();
    teams.forEach(team => {
      if (team.department && team.leaderId && team.members) {
        const leaderUser = team.members.find(m => m.id === team.leaderId);
        if (leaderUser?.name) {
          deptLeads.set(team.department, leaderUser.name);
        }
      }
    });

    const currentMonth = new Date().getMonth() + 1;
    const targetSpentPercent = (currentMonth / 12) * 100;

    this.departments = depts.map(d => {
      const budget = d.budget || 0;
      const spent = (d.actualSpend || 0) + (d.committedSpend || 0);
      const utilizationRate = budget > 0 ? (spent / budget) * 100 : 0;

      let status: 'IN RANGE' | 'WARNING' | 'OVER BUDGET' = 'IN RANGE';
      let displayStatus = 'IN RANGE';

      if (utilizationRate > 100) {
        status = 'OVER BUDGET';
        displayStatus = 'OVER BUDGET';
      } else if (utilizationRate > targetSpentPercent + 15) {
        status = 'WARNING';
        displayStatus = 'OVER UTILIZED';
      } else if (utilizationRate < targetSpentPercent - 15) {
        status = 'IN RANGE';
        displayStatus = 'UNDER UTILIZED';
      } else {
        status = 'IN RANGE';
        displayStatus = 'IN RANGE';
      }

      return {
        ...d,
        leadName: d.name ? deptLeads.get(d.name) : undefined,
        utilizationRate: Math.round(utilizationRate * 10) / 10,
        status,
        displayStatus
      };
    });

    this.donutChartOptions = {
      series: this.departments.map(d => (d.actualSpend || 0) + (d.committedSpend || 0)),
      chart: {
        type: 'donut',
        height: 220,
        fontFamily: 'Inter, Roboto, Helvetica, Arial, sans-serif'
      },
      labels: this.departments.map(d => d.name || 'Unnamed'),
      colors: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'],
      legend: {
        position: 'bottom',
        fontSize: '11px',
        fontWeight: 500,
        labels: {
          colors: '#4b5563'
        }
      },
      dataLabels: {
        enabled: false
      },
      tooltip: {
        y: {
          formatter: (val: number) => {
            return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val);
          }
        }
      }
    };

    this.barChartOptions = {
      series: [
        {
          name: 'Committed Funds',
          data: this.departments.map(d => d.committedSpend || 0)
        },
        {
          name: 'Actual Spend',
          data: this.departments.map(d => d.actualSpend || 0)
        }
      ],
      chart: {
        type: 'bar',
        height: 220,
        stacked: true,
        stackType: '100%',
        toolbar: { show: false },
        fontFamily: 'Inter, Roboto, Helvetica, Arial, sans-serif'
      },
      plotOptions: {
        bar: {
          horizontal: true,
          barHeight: '70%',
          endingShape: 'rounded'
        }
      },
      dataLabels: {
        enabled: true,
        formatter: (val: number) => {
          return val ? Math.round(val) + '%' : '';
        },
        style: {
          fontSize: '10px',
          fontWeight: 600,
          colors: ['#fff']
        }
      },
      stroke: {
        show: true,
        width: 1,
        colors: ['#fff']
      },
      xaxis: {
        categories: this.departments.map(d => d.name || 'Unnamed'),
        labels: {
          formatter: (val: any) => {
            return val + '%';
          },
          style: {
            colors: '#9ca3af',
            fontSize: '10px',
            fontWeight: 600
          }
        }
      },
      yaxis: {
        labels: {
          formatter: (val: any) => {
            const str = String(val || '');
            return str.length > 15 ? str.substring(0, 15) + '...' : str;
          },
          style: {
            colors: '#9ca3af',
            fontSize: '10px',
            fontWeight: 600
          }
        }
      },
      colors: ['#a855f7', '#3b82f6'], // purple for committed, blue for actual
      legend: {
        position: 'bottom',
        fontSize: '11px',
        fontWeight: 500,
        labels: { colors: '#4b5563' }
      },
      tooltip: {
        x: {
          formatter: (val: any, opts: any) => {
            const idx = opts?.dataPointIndex;
            const dept = this.departments[idx];
            return dept ? dept.name : val;
          }
        },
        y: {
          formatter: (val: number) => {
            return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val);
          }
        }
      }
    };
  }

  calculateSecondaryMetrics(): void {
    const totalBudget = this.dashboardStats.totalBudget || 0;
    const bufferPercent = this.dashboardStats.safetyBuffer || 0;

    this.projectedBurn = this.dashboardStats.projectedBurn || 0;
    this.fiscalRunway = this.dashboardStats.fiscalRunway || 52;
    this.emergencyReserve = totalBudget * (bufferPercent / 100);
  }

  generateAlerts(): void {
    this.alerts = [];
    const currentMonth = new Date().getMonth() + 1;
    const targetSpentPercent = (currentMonth / 12) * 100;

    const globalBudget = this.dashboardStats.totalBudget || 0;
    const globalSpent = (this.dashboardStats.actualSpend || 0) + (this.dashboardStats.committedFunds || 0);
    const globalUtilizationRate = globalBudget > 0 ? (globalSpent / globalBudget) * 100 : 0;
    const globalSafetyBufferPercent = this.dashboardStats.safetyBuffer || 0;

    if (globalUtilizationRate > 100) {
      this.alerts.push({
        departmentName: 'GLOBAL',
        type: 'CRITICAL',
        message: `Exceeded Global Budget Limit (${Math.round(globalUtilizationRate * 10) / 10}%)`,
        details: 'Immediate global spending freeze or budget expansion required.'
      });
    } else if (globalUtilizationRate > (100 - globalSafetyBufferPercent)) {
      this.alerts.push({
        departmentName: 'GLOBAL',
        type: 'WARNING',
        message: 'Global Safety Buffer Breached',
        details: `Global spending is at ${Math.round(globalUtilizationRate)}% of total capacity.`
      });
    }

    this.departments.forEach(d => {
      const budget = d.budget || 0;
      const spent = (d.actualSpend || 0) + (d.committedSpend || 0);
      const utilizationRate = budget > 0 ? (spent / budget) * 100 : 0;
      const safetyBufferPercent = d.safetyBuffer || this.dashboardStats.safetyBuffer || 0;

      if (utilizationRate > 100) {
        this.alerts.push({
          departmentName: d.name,
          type: 'CRITICAL',
          message: `Exceeded Budget Limit (${Math.round(utilizationRate * 10) / 10}%)`,
          details: 'Immediate reallocation or freeze required for upcoming requisitions.'
        });
      } else if (utilizationRate > (100 - safetyBufferPercent)) {
        this.alerts.push({
          departmentName: d.name,
          type: 'WARNING',
          message: 'Approaching Safety Buffer',
          details: `Spending is at ${Math.round(utilizationRate)}% of total department capacity.`
        });
      } else if (utilizationRate > targetSpentPercent + 15) {
        this.alerts.push({
          departmentName: d.name,
          type: 'WARNING',
          message: 'Exceeded Target Burndown',
          details: `Spending is at ${Math.round(utilizationRate)}% which is more than 15% above the target burndown of ${Math.round(targetSpentPercent)}%.`
        });
      }
    });
  }

  generateChartData(): void {
    const globalTotalBudget = this.dashboardStats.totalBudget || 0;
    const globalSafetyBufferPercent = this.dashboardStats.safetyBuffer || 0;

    let targetBudget = globalTotalBudget;
    let safetyBufferPercent = globalSafetyBufferPercent;

    if (this.selectedDepartmentId !== 0) {
      const selectedDept = this.departments.find(d => d.id === this.selectedDepartmentId);
      if (selectedDept) {
        targetBudget = selectedDept.budget || 0;
        safetyBufferPercent = selectedDept.safetyBuffer || globalSafetyBufferPercent;
      }
    }

    const reserve = targetBudget * (safetyBufferPercent / 100);
    const usableBudget = targetBudget - reserve;

    let categories: string[] = [];
    let targetData: (number | null)[] = [];
    let actualData: (number | null)[] = [];

    // Current month is June 2026 (index 5)
    const currentMonthIndex = 5;

    const burndown = this.dashboardStats.burndownData || Array(12).fill(null);

    if (this.selectedPeriod === 'all') {
      categories = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

      // Target Burndown line: linearly decreases from targetBudget down to reserve at Dec
      for (let i = 0; i < 12; i++) {
        targetData.push(targetBudget - (usableBudget * ((i + 1) / 12)));
      }

      actualData = [...burndown];
    } else if (this.selectedPeriod === 'q1') {
      categories = ['January', 'February', 'March'];

      // Target: Q1 covers first 3 months (Jan-Mar)
      for (let i = 0; i < 3; i++) {
        targetData.push(targetBudget - (usableBudget * ((i + 1) / 12)));
      }

      actualData = burndown.slice(0, 3);
    } else if (this.selectedPeriod === 'q2') {
      categories = ['April', 'May', 'June'];

      // Target: Q2 covers months 4-6 (Apr-Jun)
      for (let i = 3; i < 6; i++) {
        targetData.push(targetBudget - (usableBudget * ((i + 1) / 12)));
      }

      actualData = burndown.slice(3, 6);
    } else if (this.selectedPeriod === 'q3') {
      categories = ['July', 'August', 'September'];

      // Target: Q3 covers months 7-9 (Jul-Sep)
      for (let i = 6; i < 9; i++) {
        targetData.push(targetBudget - (usableBudget * ((i + 1) / 12)));
      }

      actualData = burndown.slice(6, 9);
    } else if (this.selectedPeriod === 'q4') {
      categories = ['October', 'November', 'December'];

      // Target: Q4 covers months 10-12 (Oct-Dec)
      for (let i = 9; i < 12; i++) {
        targetData.push(targetBudget - (usableBudget * ((i + 1) / 12)));
      }

      actualData = burndown.slice(9, 12);
    }

    this.chartOptions = {
      series: [
        {
          name: 'Target Burndown',
          data: targetData as any
        },
        {
          name: 'Actual Remaining',
          data: actualData as any
        }
      ],
      chart: {
        type: 'area',
        height: 250,
        toolbar: {
          show: false
        },
        fontFamily: 'Inter, Roboto, Helvetica, Arial, sans-serif'
      },
      colors: ['#a855f7', '#3b82f6'], // purple-500 for target, blue-500 for actual
      stroke: {
        curve: 'smooth',
        width: [2, 3],
        dashArray: [5, 0] // dashed line for Target, solid line for Actual
      },
      fill: {
        type: 'gradient',
        gradient: {
          shadeIntensity: 1,
          opacityFrom: 0.3,
          opacityTo: 0.05,
          stops: [0, 90, 100]
        }
      },
      plotOptions: {},
      dataLabels: {
        enabled: false
      },
      xaxis: {
        categories: categories,
        labels: {
          style: {
            colors: '#9ca3af',
            fontSize: '10px',
            fontWeight: 600
          }
        },
        axisBorder: {
          show: false
        },
        axisTicks: {
          show: false
        }
      },
      yaxis: {
        labels: {
          formatter: (val) => {
            return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val);
          },
          style: {
            colors: '#9ca3af',
            fontSize: '10px',
            fontWeight: 600
          }
        }
      },
      grid: {
        borderColor: '#f3f4f6',
        strokeDashArray: 4,
        padding: {
          top: 10,
          right: 20,
          bottom: 15,
          left: 20
        },
        yaxis: {
          lines: {
            show: true
          }
        }
      },
      legend: {
        show: false
      },
      tooltip: {
        shared: true,
        intersect: false,
        y: {
          formatter: (val) => {
            if (val === null || val === undefined) return 'N/A';
            return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val);
          }
        }
      }
    };
  }


  openConfigureDialog(): void {
    const dialogRef = this.dialog.open(EditBudgetDialogComponent, {
      width: '500px',
      data: {
        totalBudget: this.dashboardStats.totalBudget,
        safetyBuffer: this.dashboardStats.safetyBuffer,
        exists: !!this.dashboardStats.exists
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadData();
      }
    });
  }

  getMonthlySpend(): number[] {
    const burndown = this.dashboardStats.burndownData || [];
    let startBudget = this.dashboardStats.totalBudget || 0;

    if (this.selectedDepartmentId !== 0) {
      const selectedDept = this.departments.find(d => d.id === this.selectedDepartmentId);
      if (selectedDept) {
        startBudget = selectedDept.budget || 0;
      }
    }

    const monthlySpend: number[] = [];
    let previousRemaining = startBudget;

    for (let i = 0; i < 12; i++) {
      const remaining = burndown[i];
      if (remaining !== null && remaining !== undefined) {
        const spent = previousRemaining - remaining;
        monthlySpend.push(spent > 0 ? Math.round(spent * 100) / 100 : 0);
        previousRemaining = remaining;
      } else {
        monthlySpend.push(0);
      }
    }
    return monthlySpend;
  }

  generateTrendChartData(): void {
    const monthlySpend = this.getMonthlySpend();
    const categories = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

    let filteredCategories = [...categories];
    let filteredData = [...monthlySpend];

    if (this.selectedPeriod === 'q1') {
      filteredCategories = ['Jan', 'Feb', 'Mar'];
      filteredData = monthlySpend.slice(0, 3);
    } else if (this.selectedPeriod === 'q2') {
      filteredCategories = ['Apr', 'May', 'Jun'];
      filteredData = monthlySpend.slice(3, 6);
    } else if (this.selectedPeriod === 'q3') {
      filteredCategories = ['Jul', 'Aug', 'Sep'];
      filteredData = monthlySpend.slice(6, 9);
    } else if (this.selectedPeriod === 'q4') {
      filteredCategories = ['Oct', 'Nov', 'Dec'];
      filteredData = monthlySpend.slice(9, 12);
    }

    this.trendChartOptions = {
      series: [
        {
          name: 'Actual Spend',
          data: filteredData
        }
      ],
      chart: {
        type: 'bar',
        height: 220,
        toolbar: { show: false },
        fontFamily: 'Inter, Roboto, Helvetica, Arial, sans-serif'
      },
      plotOptions: {
        bar: {
          borderRadius: 4,
          columnWidth: '50%'
        }
      },
      colors: ['#10b981'], // Emerald-500
      dataLabels: {
        enabled: false
      },
      xaxis: {
        categories: filteredCategories,
        labels: {
          style: {
            colors: '#9ca3af',
            fontSize: '10px',
            fontWeight: 600
          }
        }
      },
      yaxis: {
        labels: {
          formatter: (val: number) => {
            return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val);
          },
          style: {
            colors: '#9ca3af',
            fontSize: '10px',
            fontWeight: 600
          }
        }
      },
      grid: {
        borderColor: '#f3f4f6',
        strokeDashArray: 4,
        yaxis: {
          lines: { show: true }
        }
      },
      tooltip: {
        y: {
          formatter: (val: number) => {
            return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val);
          }
        }
      }
    };
  }

  updateProjectChartData(): void {
    const filteredProjects = this.selectedProjectDeptId === 0
      ? this.allProjects
      : this.allProjects.filter(p => p.departmentId === this.selectedProjectDeptId);

    const projectSpends = filteredProjects.map(p => {
      const spent = (p.actualSpend || 0) + (p.committedSpend || 0);
      return {
        name: p.name || 'Unnamed Project',
        spend: spent
      };
    });

    this.projectDonutChartOptions = {
      series: projectSpends.map(p => p.spend),
      chart: {
        type: 'donut',
        height: 220,
        fontFamily: 'Inter, Roboto, Helvetica, Arial, sans-serif'
      },
      labels: projectSpends.map(p => p.name),
      colors: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#f43f5e', '#14b8a6'],
      legend: {
        position: 'bottom',
        fontSize: '11px',
        fontWeight: 500,
        labels: {
          colors: '#4b5563'
        }
      },
      dataLabels: {
        enabled: false
      },
      tooltip: {
        y: {
          formatter: (val: number) => {
            return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val);
          }
        }
      }
    };
  }

  hasProjectSpend(): boolean {
    return this.projectDonutChartOptions && this.projectDonutChartOptions.series && this.projectDonutChartOptions.series.some((val: number) => val > 0);
  }

  getSafetyBufferStatus(): { text: string; bgClass: string; textClass: string } {
    const spent = (this.dashboardStats.actualSpend || 0) + (this.dashboardStats.committedFunds || 0);
    const budget = this.dashboardStats.totalBudget || 0;
    const utilizationRate = budget > 0 ? (spent / budget) * 100 : 0;
    const safetyBufferPercent = this.dashboardStats.safetyBuffer || 0;

    if (utilizationRate >= 100) {
      return {
        text: 'Depleted',
        bgClass: 'bg-rose-50 border border-rose-100',
        textClass: 'text-rose-700'
      };
    } else if (utilizationRate > (100 - safetyBufferPercent)) {
      return {
        text: 'Warning',
        bgClass: 'bg-amber-50 border border-amber-100',
        textClass: 'text-amber-700'
      };
    } else {
      return {
        text: 'Healthy',
        bgClass: 'bg-green-50 border border-green-100',
        textClass: 'text-green-700'
      };
    }
  }
}
