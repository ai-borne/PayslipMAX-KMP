// ChartManager - Handles all Chart.js creation, updates, and styling

// Shared configuration helpers to uphold DRY
function getScales(yCallback, showXGrid = true) {
  return {
    x: {
      grid: { color: 'var(--border-light)', display: showXGrid },
      ticks: { color: 'var(--text-secondary)', font: { family: 'Plus Jakarta Sans', size: 9 } }
    },
    y: {
      grid: { color: 'var(--border-light)' },
      ticks: { 
        color: 'var(--text-secondary)', 
        font: { family: 'Plus Jakarta Sans', size: 9 },
        callback: yCallback
      }
    }
  };
}

function getLegendLabel(size = 10) {
  return {
    color: 'var(--text-primary)',
    font: { family: 'Plus Jakarta Sans', size }
  };
}

export class ChartManager {
  constructor() {
    this.incomeChart = null;
    this.shareChart = null;
    this.dsopChart = null;
    this.taxChart = null;
  }

  // Chart 1: Income Trend
  renderIncomeTrend(ctx, dataset, filterYear = 'all') {
    let filteredData = dataset;
    if (filterYear !== 'all') {
      filteredData = dataset.filter(d => d.year == parseInt(filterYear));
    }
    
    const labels = filteredData.map(d => d.date_str);
    const grossData = filteredData.map(d => d.summary.gross_pay);
    const netData = filteredData.map(d => d.summary.net_remittance);
    
    if (this.incomeChart) {
      this.incomeChart.destroy();
    }
    
    const grossGrad = ctx.createLinearGradient(0, 0, 0, 400);
    grossGrad.addColorStop(0, 'rgba(59, 130, 246, 0.4)');
    grossGrad.addColorStop(1, 'rgba(59, 130, 246, 0.0)');
    
    const netGrad = ctx.createLinearGradient(0, 0, 0, 400);
    netGrad.addColorStop(0, 'rgba(16, 185, 129, 0.4)');
    netGrad.addColorStop(1, 'rgba(16, 185, 129, 0.0)');
    
    this.incomeChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Gross Pay',
            data: grossData,
            borderColor: '#3b82f6',
            backgroundColor: grossGrad,
            fill: true,
            tension: 0.35,
            borderWidth: 2,
            pointRadius: 2,
            pointHoverRadius: 6
          },
          {
            label: 'Net Remittance (Take-Home)',
            data: netData,
            borderColor: '#10b981',
            backgroundColor: netGrad,
            fill: true,
            tension: 0.35,
            borderWidth: 2,
            pointRadius: 2,
            pointHoverRadius: 6
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { labels: getLegendLabel() },
          tooltip: {
            mode: 'index',
            intersect: false,
            callbacks: {
              label: function(context) {
                let label = context.dataset.label || '';
                if (label) { label += ': '; }
                if (context.parsed.y !== null) {
                  label += new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(context.parsed.y);
                }
                return label;
              }
            }
          }
        },
        scales: getScales(value => {
          if (value === 0) return '₹0';
          const lakhs = value / 100000;
          return '₹' + parseFloat(lakhs.toFixed(2)) + 'L';
        })
      }
    });
  }

  // Chart 2: Earnings vs Deductions Share
  renderShareBreakdown(ctx, record) {
    const gross = record ? (record.summary.gross_pay || 1) : 1;
    const net = record ? record.summary.net_remittance : 70;
    const dsop = record ? (record.deductions.dsop_subscription || 0) : 15;
    const tax = record ? ((record.deductions.income_tax || 0) + (record.deductions.education_cess || 0)) : 12;
    const other = record ? (record.summary.total_deductions - dsop - tax) : 3;
    
    const netPerc = record ? (net / gross) * 100 : 70;
    const dsopPerc = record ? (dsop / gross) * 100 : 15;
    const taxPerc = record ? (tax / gross) * 100 : 12;
    const otherPerc = record ? Math.max(0, (other / gross) * 100) : 3;

    if (this.shareChart) {
      this.shareChart.data.datasets[0].data = [netPerc, dsopPerc, taxPerc, otherPerc];
      this.shareChart.update();
      return;
    }

    this.shareChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Net Take-Home', 'Provident Fund (DSOP)', 'Taxes & Cess', 'Other Deductions'],
        datasets: [{
          data: [netPerc, dsopPerc, taxPerc, otherPerc],
          backgroundColor: ['#10b981', '#8b5cf6', '#ef4444', '#f59e0b'],
          borderWidth: 0,
          hoverOffset: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: getLegendLabel(10)
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                let label = context.label || '';
                if (context.parsed !== null) {
                  label += ': ' + context.parsed.toFixed(1) + '%';
                }
                return label;
              }
            }
          }
        },
        cutout: '65%'
      }
    });
  }

  // Chart 3: DSOP Growth
  renderDsopGrowth(ctx, dataset) {
    const dsopRecords = dataset.filter(d => d.tax_and_savings && d.tax_and_savings.dsop_fund && d.tax_and_savings.dsop_fund.closing_balance > 0);
    const labels = dsopRecords.map(d => d.date_str);
    const balances = dsopRecords.map(d => d.tax_and_savings.dsop_fund.closing_balance);
    
    if (this.dsopChart) {
      this.dsopChart.destroy();
    }

    const dsopGrad = ctx.createLinearGradient(0, 0, 0, 300);
    dsopGrad.addColorStop(0, 'rgba(139, 92, 246, 0.4)');
    dsopGrad.addColorStop(1, 'rgba(139, 92, 246, 0.0)');
    
    this.dsopChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'DSOP Balance',
          data: balances,
          borderColor: '#8b5cf6',
          backgroundColor: dsopGrad,
          fill: true,
          tension: 0.1,
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: function(context) {
                return 'DSOP: ' + new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(context.parsed.y);
              }
            }
          }
        },
        scales: getScales(value => '₹' + (value/100000).toFixed(1) + 'L')
      }
    });
  }

  // Chart 4: Tax Projections
  renderTaxProjections(ctx, dataset) {
    const years = [...new Set(dataset.map(d => d.year))];
    const yearlyTaxData = [];
    
    years.forEach(yr => {
      const records = dataset.filter(d => d.year === yr && d.tax_and_savings && d.tax_and_savings.gross_salary_ytd > 0);
      if (records.length > 0) {
        const latest = records[records.length - 1];
        yearlyTaxData.push({
          year: yr,
          tax: latest.tax_and_savings.total_tax_payable,
          paid: latest.tax_and_savings.tax_deducted_ytd + latest.tax_and_savings.cess_deducted_ytd
        });
      }
    });
    
    const labels = yearlyTaxData.map(d => d.year);
    const taxPayable = yearlyTaxData.map(d => d.tax);
    const taxPaid = yearlyTaxData.map(d => d.paid);
    
    if (this.taxChart) {
      this.taxChart.destroy();
    }

    this.taxChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Estimated Annual Tax',
            data: taxPayable,
            backgroundColor: 'rgba(239, 68, 68, 0.5)',
            borderColor: '#ef4444',
            borderWidth: 1,
            borderRadius: 6
          },
          {
            label: 'Tax Deducted to Date',
            data: taxPaid,
            backgroundColor: 'rgba(16, 185, 129, 0.5)',
            borderColor: '#10b981',
            borderWidth: 1,
            borderRadius: 6
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { labels: getLegendLabel() },
          tooltip: {
            callbacks: {
              label: function(context) {
                let label = context.dataset.label || '';
                if (label) { label += ': '; }
                if (context.parsed.y !== null) {
                  label += new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(context.parsed.y);
                }
                return label;
              }
            }
          }
        },
        scales: getScales(value => '₹' + (value/1000) + 'k', false)
      }
    });
  }
}
