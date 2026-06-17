// View Layer - Coordinates DOM and user interactions

import { strings } from './strings.js';
import { glossary } from './glossary.js';

export class PayslipView {
  constructor(viewModel, chartManager) {
    this.vm = viewModel;
    this.cm = chartManager;
    
    this.selectYear = document.getElementById('select-year');
    this.selectMonth = document.getElementById('select-month');
    this.modal = document.getElementById('upload-modal');
    
    // Subscribe to VM updates
    this.vm.subscribe(() => this.render());
    
    this.initEvents();
    this.translateUI();
    this.render();
    this.initCharts();
  }

  translateUI() {
    document.querySelectorAll('[data-i18n]').forEach(el => {
      const key = el.getAttribute('data-i18n');
      if (strings.en[key]) {
        if (el.tagName === 'INPUT' && el.type === 'placeholder') {
          el.placeholder = strings.en[key];
        } else if (el.tagName === 'INPUT' && el.type === 'button') {
          el.value = strings.en[key];
        } else {
          el.textContent = strings.en[key];
        }
      }
    });
  }

  initEvents() {
    this.selectYear.addEventListener('change', (e) => {
      this.vm.setYear(e.target.value);
      this.updateMonthSelector();
    });

    this.selectMonth.addEventListener('change', (e) => {
      this.vm.setMonth(e.target.value);
    });

    // Upload & Modal
    document.querySelector('.upload-card').addEventListener('click', () => {
      this.modal.classList.add('active');
    });

    document.querySelector('.modal-close').addEventListener('click', () => {
      this.closeModal();
    });

    window.closeUploadModal = () => this.closeModal();
    window.startParsingAnimation = () => this.handleDecrypt();
    window.updateIncomeChart = (filter) => this.handleIncomeChartFilter(filter);
  }

  closeModal() {
    this.modal.classList.remove('active');
    document.getElementById('modal-form-content').style.display = 'block';
    document.getElementById('parser-loader').style.display = 'none';
  }

  handleDecrypt() {
    const fileInput = document.getElementById('payslip-file');
    if (fileInput.files.length === 0) {
      alert(strings.en.alertSelectPdf);
      return;
    }
    
    const password = document.getElementById('pdf-password').value;
    const loader = document.getElementById('parser-loader');
    const statusText = document.getElementById('loader-status');
    
    document.getElementById('modal-form-content').style.display = 'none';
    loader.style.display = 'flex';

    this.vm.parseUploadedPayslip(
      password,
      (status) => {
        statusText.textContent = status;
      },
      () => {
        alert(strings.en.alertSuccess);
        this.closeModal();
        this.updateSelectors();
      },
      (errorMsg) => {
        alert(errorMsg);
        this.closeModal();
      }
    );
  }

  handleIncomeChartFilter(filter) {
    const buttons = document.querySelectorAll('.filter-btn');
    buttons.forEach(btn => btn.classList.remove('active'));
    
    const activeBtn = Array.from(buttons).find(btn => 
      btn.textContent.toLowerCase() === filter.toLowerCase() || 
      (filter === 'all' && btn.textContent === 'All')
    );
    if (activeBtn) activeBtn.classList.add('active');
    
    const ctx = document.getElementById('incomeTrendChart').getContext('2d');
    this.cm.renderIncomeTrend(ctx, this.vm.model.getPayslips(), filter);
  }

  updateSelectors() {
    this.selectYear.innerHTML = '';
    this.vm.model.getYears().forEach(yr => {
      const opt = document.createElement('option');
      opt.value = yr;
      opt.textContent = yr;
      this.selectYear.appendChild(opt);
    });
    this.selectYear.value = this.vm.selectedYear;
    this.updateMonthSelector();
  }

  updateMonthSelector() {
    this.selectMonth.innerHTML = '';
    this.vm.model.getMonthsForYear(this.vm.selectedYear).forEach(month => {
      const opt = document.createElement('option');
      opt.value = month;
      opt.textContent = month;
      this.selectMonth.appendChild(opt);
    });
    this.selectMonth.value = this.vm.selectedMonth;
  }

  initCharts() {
    const incomeCtx = document.getElementById('incomeTrendChart').getContext('2d');
    const shareCtx = document.getElementById('shareBreakdownChart').getContext('2d');
    const dsopCtx = document.getElementById('dsopGrowthChart').getContext('2d');
    const taxCtx = document.getElementById('taxProjectionsChart').getContext('2d');

    this.cm.renderIncomeTrend(incomeCtx, this.vm.model.getPayslips(), 'all');
    this.cm.renderShareBreakdown(shareCtx, this.vm.activeRecord);
    this.cm.renderDsopGrowth(dsopCtx, this.vm.model.getPayslips());
    this.cm.renderTaxProjections(taxCtx, this.vm.model.getPayslips());
  }

  render() {
    // 1. Selector update (if options count mismatch)
    if (this.selectYear.options.length === 0) {
      this.updateSelectors();
    }

    // 2. Overview Stats
    const stats = this.vm.getOverviewStats();
    document.getElementById('stat-net-remittance').textContent = stats.netRemittance;
    document.getElementById('stat-net-date').textContent = stats.netDate;
    document.getElementById('stat-basic-pay').textContent = stats.basicPay;
    document.getElementById('stat-dsop-balance').textContent = stats.dsopBalance;
    document.getElementById('stat-tax-rate').textContent = stats.taxRate;

    // 3. Officer Replica Header
    const record = this.vm.activeRecord;
    if (record) {
      document.getElementById('officer-name').textContent = record.officer.name;
      document.getElementById('cda-account').textContent = record.officer.account_no;
      document.getElementById('pan-number').textContent = record.officer.pan;
      document.getElementById('replica-date').textContent = `FOR ${record.date_str}`;
      document.getElementById('replica-name').textContent = `Name: ${record.officer.name}`;
      document.getElementById('replica-cda').textContent = `CDA: ${record.officer.account_no}`;
      
      this.renderTableList(document.getElementById('replica-earnings-list'), record.earnings, 'earnings', record);
      this.renderTableList(document.getElementById('replica-deductions-list'), record.deductions, 'deductions', record);
      
      // Totals
      document.getElementById('replica-gross-pay').textContent = this.vm.formatCurrency(record.summary.gross_pay);
      document.getElementById('replica-total-deductions').textContent = this.vm.formatCurrency(record.summary.total_deductions);
      document.getElementById('replica-net-remittance').textContent = this.vm.formatCurrency(record.summary.net_remittance);
    }

    // 4. Insights panel
    this.renderInsights();

    // 5. Update Share Chart
    if (this.cm.shareChart && record) {
      const shareCtx = document.getElementById('shareBreakdownChart').getContext('2d');
      this.cm.renderShareBreakdown(shareCtx, record);
    }
  }

  renderTableList(container, data, type, record) {
    container.innerHTML = '';
    let itemAdded = false;

    for (const [key, val] of Object.entries(data || {})) {
      if (val !== 0) {
        const info = glossary[key] || { title: this.vm.formatLabel(key), desc: 'Component' };
        const row = document.createElement('div');
        row.className = 'data-row';
        row.innerHTML = `
          <span class="data-name">${info.title}</span>
          <span class="data-value">${this.vm.formatCurrency(val)}</span>
          <div class="glossary-tooltip">
            <h5>${info.title}</h5>
            <p>${info.desc}</p>
          </div>
        `;
        container.appendChild(row);
        itemAdded = true;
      }
    }

    // Render corresponding ledger balances in appropriate column
    const ledger = record.ledger_balances || {};
    for (const [key, val] of Object.entries(ledger)) {
      if (val > 0) {
        const isDebit = key.includes('debit') || (key.includes('credit_balance') && key.includes('closing'));
        if ((type === 'deductions' && isDebit) || (type === 'earnings' && !isDebit)) {
          const info = glossary[key] || { title: this.vm.formatLabel(key), desc: 'Ledger status' };
          const row = document.createElement('div');
          row.className = 'data-row';
          row.style.borderTop = '1px solid rgba(255, 255, 255, 0.05)';
          row.innerHTML = `
            <span class="data-name" style="color:var(--color-warning)">${info.title}</span>
            <span class="data-value">${this.vm.formatCurrency(val)}</span>
            <div class="glossary-tooltip">
              <h5>${info.title}</h5>
              <p>${info.desc}</p>
            </div>
          `;
          container.appendChild(row);
          itemAdded = true;
        }
      }
    }

    if (!itemAdded) {
      container.innerHTML = `<div class="data-row" style="color:var(--text-muted)">No ${type === 'earnings' ? 'credits' : 'deductions'} processed</div>`;
    }
  }

  renderInsights() {
    const container = document.getElementById('insights-container');
    container.innerHTML = '';
    
    const insights = this.vm.getInsights();
    insights.forEach(ins => {
      const div = document.createElement('div');
      div.className = 'insight-item';
      div.innerHTML = `
        <div class="insight-icon ${ins.type}">${ins.icon}</div>
        <div class="insight-content">
          <h4>${ins.title}</h4>
          <p>${ins.desc}</p>
        </div>
      `;
      container.appendChild(div);
    });
  }
}
