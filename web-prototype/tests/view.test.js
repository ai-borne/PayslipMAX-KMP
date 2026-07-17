// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { PayslipView } from '../view.js';

function setUpDom() {
  document.body.innerHTML = `
    <select id="select-year"></select>
    <select id="select-month"></select>
    <div id="upload-modal"></div>
    <div class="upload-card"></div>
    <div class="modal-close"></div>
    <div id="modal-form-content"></div>
    <div id="parser-loader"></div>
    <div id="loader-status"></div>
    <canvas id="incomeTrendChart"></canvas>
    <canvas id="shareBreakdownChart"></canvas>
    <canvas id="dsopGrowthChart"></canvas>
    <canvas id="taxProjectionsChart"></canvas>
    <span id="stat-net-remittance"></span>
    <span id="stat-net-date"></span>
    <span id="stat-basic-pay"></span>
    <span id="stat-dsop-balance"></span>
    <span id="stat-tax-rate"></span>
    <span id="officer-name"></span>
    <span id="cda-account"></span>
    <span id="pan-number"></span>
    <span id="replica-date"></span>
    <span id="replica-name"></span>
    <span id="replica-cda"></span>
    <div id="replica-earnings-list"></div>
    <div id="replica-deductions-list"></div>
    <span id="replica-gross-pay"></span>
    <span id="replica-total-deductions"></span>
    <span id="replica-net-remittance"></span>
    <div id="insights-container"></div>
  `;
}

function buildRecord(dateStr) {
  return {
    date_str: dateStr,
    officer: { name: 'Test Officer', account_no: '00/000/0000', pan: 'AA****0A' },
    earnings: {},
    deductions: {},
    ledger_balances: {},
    summary: { gross_pay: 1000, total_deductions: 200, net_remittance: 800 },
  };
}

function makeFakeViewModel(records) {
  const subscribers = [];
  return {
    activeRecord: records[records.length - 1],
    selectedYear: 2026,
    selectedMonth: 'April',
    model: {
      getPayslips: () => records,
      getYears: () => [2026],
      getMonthsForYear: () => ['April'],
    },
    subscribe(cb) {
      subscribers.push(cb);
    },
    notify() {
      subscribers.forEach(cb => cb());
    },
    getOverviewStats: () => ({ netRemittance: '₹800', netDate: 'April 2026', basicPay: '₹0', dsopBalance: '₹0', taxRate: '0%' }),
    getInsights: () => [],
    formatCurrency: (v) => `₹${v}`,
    formatLabel: (k) => k,
  };
}

function makeFakeChartManager() {
  return {
    incomeChart: null,
    shareChart: null,
    dsopChart: null,
    taxChart: null,
    renderIncomeTrend: vi.fn(function () { this.incomeChart = { destroy: vi.fn() }; }),
    renderShareBreakdown: vi.fn(function () { this.shareChart = { destroy: vi.fn() }; }),
    renderDsopGrowth: vi.fn(function () { this.dsopChart = { destroy: vi.fn() }; }),
    renderTaxProjections: vi.fn(function () { this.taxChart = { destroy: vi.fn() }; }),
  };
}

describe('PayslipView chart refresh on data update', () => {
  beforeEach(() => {
    setUpDom();
  });

  it('re-renders income, DSOP, and tax charts (not just the share chart) when new data arrives', () => {
    const records = [buildRecord('03/2026')];
    const vm = makeFakeViewModel(records);
    const cm = makeFakeChartManager();

    new PayslipView(vm, cm);

    // initCharts() has now created all four charts via the constructor's initial pass.
    expect(cm.renderIncomeTrend).toHaveBeenCalledTimes(1);
    expect(cm.renderDsopGrowth).toHaveBeenCalledTimes(1);
    expect(cm.renderTaxProjections).toHaveBeenCalledTimes(1);

    // Simulate a new payslip being added -- viewModel notifies subscribers, triggering render().
    records.push(buildRecord('04/2026'));
    vm.activeRecord = records[records.length - 1];
    vm.notify();

    expect(cm.renderIncomeTrend).toHaveBeenCalledTimes(2);
    expect(cm.renderDsopGrowth).toHaveBeenCalledTimes(2);
    expect(cm.renderTaxProjections).toHaveBeenCalledTimes(2);
    expect(cm.renderIncomeTrend.mock.calls[1][1]).toEqual(records);
  });

  it('preserves the active income chart filter across a data-driven re-render', () => {
    const records = [buildRecord('03/2026')];
    const vm = makeFakeViewModel(records);
    const cm = makeFakeChartManager();

    const view = new PayslipView(vm, cm);
    view.handleIncomeChartFilter('2026');
    expect(cm.renderIncomeTrend).toHaveBeenLastCalledWith(null, records, '2026');

    records.push(buildRecord('04/2026'));
    vm.notify();

    // The re-render triggered by new data must keep using the user's active filter, not reset to 'all'.
    expect(cm.renderIncomeTrend).toHaveBeenLastCalledWith(null, records, '2026');
  });
});
