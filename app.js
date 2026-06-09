// Indian Army Payslip Dashboard Controller

// Glossary dictionary for educating the user
const glossary = {
  basic_pay: {
    title: "Basic Pay (BPAY)",
    desc: "The core salary component determined by your rank and pay level under the 7th Pay Commission. It forms the base for calculating allowances like DA."
  },
  dearness_allowance: {
    title: "Dearness Allowance (DA)",
    desc: "A cost-of-living adjustment allowance paid to military personnel to mitigate inflation. Revised twice a year (January and July) based on the Consumer Price Index."
  },
  military_service_pay: {
    title: "Military Service Pay (MSP)",
    desc: "A unique monthly allowance of ₹15,500 paid to Indian military officers to compensate for the hazards, hardships, and unique constraints of military life."
  },
  transport_allowance: {
    title: "Transport Allowance (TPTA)",
    desc: "A fixed allowance paid to cover commuting costs between residence and headquarters. Varies based on pay level and cities classified by location."
  },
  transport_allowance_da: {
    title: "DA on Transport Allowance (TPTADA)",
    desc: "The inflation adjustment (Dearness Allowance percentage) applied specifically on top of your Transport Allowance rate."
  },
  dress_allowance: {
    title: "Dress Allowance (DRESALW)",
    desc: "An annual uniform maintenance allowance (typically ₹20,000, paid in July) to assist officers with uniform procurement and tailoring."
  },
  ration_money: {
    title: "Ration Money Allowance (RSHNA)",
    desc: "A cash allowance paid to officers to cover dietary expenses when free messing or dry rations are not provided at their station."
  },
  special_forces_pay: {
    title: "Special Forces / Command Pay (SPCDO)",
    desc: "A hazard and proficiency allowance paid to officers serving in Special Forces, airborne units, or specialized operational commands."
  },
  field_allowance: {
    title: "Field Area Allowance (FD)",
    desc: "A monthly allowance compensating for postings in active field areas, modified field areas, or high-altitude sectors."
  },
  children_education_allowance: {
    title: "Children Education Allowance (CEA)",
    desc: "A reimbursement allowance to assist with the schooling and hostel expenses of up to two children."
  },
  dsop_subscription: {
    title: "Defence Services Officers Provident Fund (DSOP)",
    desc: "A mandatory retirement savings scheme. Contributed monthly (minimum 6% of basic pay). It earns high, compound tax-free interest."
  },
  agif: {
    title: "Army Group Insurance Fund (AGIF)",
    desc: "A mandatory group insurance scheme providing high life cover and terminal benefits. Premium is deducted directly from monthly salary."
  },
  income_tax: {
    title: "Income Tax Deducted (ITAX)",
    desc: "Tax Deducted at Source (TDS) calculated based on your estimated taxable annual income under the selected tax regime."
  },
  education_cess: {
    title: "Health & Education Cess (EHCESS)",
    desc: "An additional 4% surcharge levied on your Income Tax amount to fund national education and health programs."
  },
  license_fee: {
    title: "License Fee (LF)",
    desc: "A highly subsidized monthly rent deducted for officers occupying government-provided married or single accommodation."
  },
  furniture_rent: {
    title: "Furniture Rent (FUR)",
    desc: "A nominal charge deducted for using government-provided furniture, fans, geysers, or electrical appliances in military quarters."
  },
  water_charges: {
    title: "Water Charges (WATER)",
    desc: "A nominal recovery fee for water supply provided to your government accommodation."
  },
  electricity_charges: {
    title: "Electricity Charges (Elec)",
    desc: "Deduction for electric power consumed in your quarter, based on reading units or flat-rate quarters classification."
  },
  barrack_damage: {
    title: "Barrack Damage Recovery",
    desc: "Deductions made to recover the cost of repairs for any damage done to government quarters or property during occupancy."
  },
  ticket_recovery: {
    title: "Air Ticket Recovery (ETKT)",
    desc: "Recovery of advances generated when booking official air travel through the Defense Travel System (DTS) portal."
  },
  opening_credit_balance: {
    title: "Opening Credit Balance (Op Cr Bal)",
    desc: "A positive balance brought forward from your previous month's ledger. Indicates PCDA owed you money."
  },
  opening_debit_balance: {
    title: "Opening Debit Balance (Op Dr Bal)",
    desc: "A negative balance brought forward from the previous month. Indicates you owed money to the PCDA ledger."
  },
  closing_credit_balance: {
    title: "Closing Credit Balance (Cl. Cr. Bal.)",
    desc: "The net positive balance at the month's end. Carries forward to next month's ledger instead of being paid out as cash."
  },
  closing_debit_balance: {
    title: "Closing Debit Balance (Cl. Dr. Bal.)",
    desc: "The net negative balance at the month's end. Represents your outstanding dues to PCDA for the next month."
  },
  recovery_of_debits: {
    title: "Recovery of Debits",
    desc: "Ledger adjustments where past outstanding dues are reconciled and recovered from your current credits."
  }
};

// Global variables to hold chart instances
let incomeChartInstance = null;
let shareChartInstance = null;
let dsopChartInstance = null;
let taxChartInstance = null;

// Initializer
document.addEventListener("DOMContentLoaded", () => {
  if (!window.payslipData || window.payslipData.length === 0) {
    console.error("No payslip data found!");
    return;
  }
  
  initializeSelectors();
  initializeOverviewStats();
  renderIncomeTrendChart('all');
  renderShareBreakdownChart();
  renderDsopGrowthChart();
  renderTaxProjectionsChart();
  
  // Set initial payslip replica
  const latestRecord = window.payslipData[window.payslipData.length - 1];
  updatePayslipReplica(latestRecord);
});

// Setup selector drop-downs
function initializeSelectors() {
  const selectYear = document.getElementById("select-year");
  const selectMonth = document.getElementById("select-month");
  
  // Extract unique years
  const years = [...new Set(window.payslipData.map(d => d.year))].sort((a, b) => b - a);
  
  selectYear.innerHTML = "";
  years.forEach(yr => {
    const opt = document.createElement("option");
    opt.value = yr;
    opt.textContent = yr;
    selectYear.appendChild(opt);
  });
  
  updateMonthSelector(years[0]);
}

function updateMonthSelector(year) {
  const selectMonth = document.getElementById("select-month");
  selectMonth.innerHTML = "";
  
  const monthsForYear = window.payslipData
    .filter(d => d.year == year)
    .sort((a, b) => b.month_num - a.month_num); // latest month first
    
  monthsForYear.forEach(m => {
    const opt = document.createElement("option");
    opt.value = m.month_name;
    opt.textContent = m.month_name;
    selectMonth.appendChild(opt);
  });
}

function onPayslipSelectChange() {
  const year = document.getElementById("select-year").value;
  const month = document.getElementById("select-month").value;
  
  const record = window.payslipData.find(d => d.year == year && d.month_name === month);
  if (record) {
    updatePayslipReplica(record);
  }
}

// When year changes, we must reload months
document.getElementById("select-year").addEventListener("change", (e) => {
  updateMonthSelector(e.target.value);
  onPayslipSelectChange();
});

// Overview Statistics Setup
function initializeOverviewStats() {
  // Find latest records
  const latestRecord = window.payslipData[window.payslipData.length - 1];
  
  // Find record with valid basic pay
  const latestWithBasic = [...window.payslipData]
    .reverse()
    .find(d => d.earnings && d.earnings.basic_pay > 0);
    
  // Find latest DSOP closing balance
  const latestWithTax = [...window.payslipData]
    .reverse()
    .find(d => d.tax_and_savings && d.tax_and_savings.dsop_fund && d.tax_and_savings.dsop_fund.closing_balance > 0);
    
  if (latestRecord) {
    document.getElementById("stat-net-remittance").textContent = `₹${latestRecord.summary.net_remittance.toLocaleString("en-IN")}`;
    document.getElementById("stat-net-date").textContent = `As of ${latestRecord.month_name} ${latestRecord.year}`;
  }
  
  if (latestWithBasic) {
    const basic = latestWithBasic.earnings.basic_pay;
    document.getElementById("stat-basic-pay").textContent = `₹${basic.toLocaleString("en-IN")}`;
  }
  
  if (latestWithTax) {
    const dsop = latestWithTax.tax_and_savings.dsop_fund.closing_balance;
    document.getElementById("stat-dsop-balance").textContent = `₹${dsop.toLocaleString("en-IN")}`;
  }
  
  // Calculate average tax rate
  if (latestWithTax && latestWithTax.tax_and_savings.total_taxable_income > 0) {
    const tax = latestWithTax.tax_and_savings.total_tax_payable;
    const income = latestWithTax.tax_and_savings.total_taxable_income;
    const rate = (tax / income) * 100;
    document.getElementById("stat-tax-rate").textContent = `${rate.toFixed(1)}%`;
  }
}

// Helper to format key names into readable text
function formatLabel(key) {
  return key
    .split("_")
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

// Update the interactive visual replica and insights
function updatePayslipReplica(record) {
  document.getElementById("replica-date").textContent = `FOR ${record.date_str}`;
  document.getElementById("replica-name").textContent = `Name: ${record.officer.name || 'OFFICER OFFICER OFFICER'}`;
  document.getElementById("replica-cda").textContent = `CDA: ${record.officer.account_no || '16/000/000000X'}`;
  
  // Render Earnings List
  const earnList = document.getElementById("replica-earnings-list");
  earnList.innerHTML = "";
  
  const earnings = record.earnings || {};
  if (Object.keys(earnings).length === 0) {
    earnList.innerHTML = `<div class="data-row" style="color:var(--text-muted)">No credits processed</div>`;
  } else {
    for (const [key, val] of Object.entries(earnings)) {
      if (val > 0) {
        const item = glossary[key] || { title: formatLabel(key), desc: "Allowance component" };
        const row = document.createElement("div");
        row.className = "data-row";
        row.innerHTML = `
          <span class="data-name">${item.title}</span>
          <span class="data-value">₹${val.toLocaleString("en-IN")}</span>
          <div class="glossary-tooltip">
            <h5>${item.title}</h5>
            <p>${item.desc}</p>
          </div>
        `;
        earnList.appendChild(row);
      }
    }
  }
  
  // Render Deductions List
  const dedList = document.getElementById("replica-deductions-list");
  dedList.innerHTML = "";
  
  const deductions = record.deductions || {};
  if (Object.keys(deductions).length === 0) {
    dedList.innerHTML = `<div class="data-row" style="color:var(--text-muted)">No deductions processed</div>`;
  } else {
    for (const [key, val] of Object.entries(deductions)) {
      if (val > 0) {
        const item = glossary[key] || { title: formatLabel(key), desc: "Deduction component" };
        const row = document.createElement("div");
        row.className = "data-row";
        row.innerHTML = `
          <span class="data-name">${item.title}</span>
          <span class="data-value">₹${val.toLocaleString("en-IN")}</span>
          <div class="glossary-tooltip">
            <h5>${item.title}</h5>
            <p>${item.desc}</p>
          </div>
        `;
        dedList.appendChild(row);
      }
    }
  }
  
  // Render ledger balances if any
  const ledger = record.ledger_balances || {};
  for (const [key, val] of Object.entries(ledger)) {
    if (val > 0) {
      const item = glossary[key] || { title: formatLabel(key), desc: "Ledger status" };
      const row = document.createElement("div");
      row.className = "data-row";
      row.style.borderTop = "1px solid rgba(255, 255, 255, 0.05)";
      row.innerHTML = `
        <span class="data-name" style="color:var(--color-warning)">${item.title}</span>
        <span class="data-value">₹${val.toLocaleString("en-IN")}</span>
        <div class="glossary-tooltip">
          <h5>${item.title}</h5>
          <p>${item.desc}</p>
        </div>
      `;
      if (key.includes("debit") || key.includes("credit_balance") && key.includes("closing")) {
        dedList.appendChild(row);
      } else {
        earnList.appendChild(row);
      }
    }
  }
  
  // Set totals
  document.getElementById("replica-gross-pay").textContent = `₹${record.summary.gross_pay.toLocaleString("en-IN")}`;
  document.getElementById("replica-total-deductions").textContent = `₹${record.summary.total_deductions.toLocaleString("en-IN")}`;
  document.getElementById("replica-net-remittance").textContent = `₹${record.summary.net_remittance.toLocaleString("en-IN")}`;
  
  // Generate Insights
  generateInsights(record);
  
  // Update Share chart
  updateShareChart(record);
}

// Generate Educative Financial Insights based on selected payslip
function generateInsights(record) {
  const container = document.getElementById("insights-container");
  container.innerHTML = "";
  
  const insights = [];
  
  // 1. Savings Rate Insight
  const basicPay = record.earnings.basic_pay || 0;
  const dsop = record.deductions.dsop_subscription || 0;
  const gross = record.summary.gross_pay || 1;
  
  if (dsop > 0) {
    const dsopRate = (dsop / gross) * 100;
    insights.push({
      title: `Excellent Savings Rate (${dsopRate.toFixed(1)}%)`,
      desc: `You saved ₹${dsop.toLocaleString("en-IN")} in your DSOP Fund this month, which is ${dsopRate.toFixed(1)}% of your gross earnings. DSOP is compounding tax-free interest, making it a powerful wealth builder.`,
      icon: "📈",
      type: "success"
    });
  } else if (basicPay > 0) {
    insights.push({
      title: "Action Needed: Zero DSOP Contribution",
      desc: "No DSOP subscription was deducted this month. Army rules mandate a minimum contribution of 6% of your Basic Pay to your provident fund for retirement security.",
      icon: "⚠️",
      type: "warning"
    });
  }
  
  // 2. Tax Burden Insight
  const tax = (record.deductions.income_tax || 0) + (record.deductions.education_cess || 0);
  if (tax > 0) {
    const taxRate = (tax / gross) * 100;
    insights.push({
      title: `Income Tax Burden (${taxRate.toFixed(1)}%)`,
      desc: `A total of ₹${tax.toLocaleString("en-IN")} was deducted for Income Tax (including Cess) this month, consuming ${taxRate.toFixed(1)}% of your gross pay.`,
      icon: "🏛️",
      type: "accent"
    });
  }
  
  // 3. Accommodation Subsidy Insight
  const lf = record.deductions.license_fee || 0;
  const fur = record.deductions.furniture_rent || 0;
  if (lf > 0) {
    const totalAcc = lf + fur;
    insights.push({
      title: "Government Housing Value",
      desc: `You were charged a License Fee of ₹${lf.toLocaleString("en-IN")} and Furniture Rent of ₹${fur.toLocaleString("en-IN")} (Total: ₹${totalAcc.toLocaleString("en-IN")}). Compared to open-market rentals, this represents an immense cost subsidy of approximately ₹20,000+ per month.`,
      icon: "🏠",
      type: "success"
    });
  }
  
  // 4. Special Allowances Insight
  const spcf = record.earnings.special_forces_pay || 0;
  if (spcf > 0) {
    insights.push({
      title: "Special Forces Operational Pay Active",
      desc: `Your payslip shows ₹${spcf.toLocaleString("en-IN")} credited as Special Forces/Command Pay. This is an elite hazard allowance paid for specialized service conditions.`,
      icon: "🦅",
      type: "success"
    });
  }
  
  // 5. Arrears or Large Adjustments
  const arrearsList = Object.keys(record.earnings).filter(k => k.startsWith("arrears_") || k.startsWith("adj_"));
  if (arrearsList.length > 0) {
    const totalArr = arrearsList.reduce((sum, k) => sum + record.earnings[k], 0);
    insights.push({
      title: `Arrears & Adjustments Credited: ₹${totalArr.toLocaleString("en-IN")}`,
      desc: `Your account received arrears credits this month (e.g. ${arrearsList.map(a => formatLabel(a)).join(", ")}). Consider routing these lump-sums directly into additional savings or mutual funds rather than discretionary spending.`,
      icon: "💰",
      type: "success"
    });
  }
  
  // 6. General Education
  const msp = record.earnings.military_service_pay || 0;
  if (msp > 0) {
    insights.push({
      title: `Military Service Pay (₹${msp.toLocaleString("en-IN")})`,
      desc: `MSP is a compensation model paid only to military officers. Unlike civil services, it compensates for the constant hazard, relocation, and physical challenges of your military career.`,
      icon: "🎖️",
      type: "accent"
    });
  }

  // Render insights
  insights.forEach(ins => {
    const div = document.createElement("div");
    div.className = "insight-item";
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

// ---------------- CHARTS RENDERING ----------------

// Chart 1: Income Trend Chart (Line Chart)
function renderIncomeTrendChart(filter) {
  const ctx = document.getElementById('incomeTrendChart').getContext('2d');
  
  let filteredData = window.payslipData;
  if (filter !== 'all') {
    filteredData = window.payslipData.filter(d => d.year == parseInt(filter));
  }
  
  const labels = filteredData.map(d => d.date_str);
  const grossData = filteredData.map(d => d.summary.gross_pay);
  const netData = filteredData.map(d => d.summary.net_remittance);
  
  if (incomeChartInstance) {
    incomeChartInstance.destroy();
  }
  
  // Create gradient glows for lines
  const grossGrad = ctx.createLinearGradient(0, 0, 0, 400);
  grossGrad.addColorStop(0, 'rgba(59, 130, 246, 0.4)');
  grossGrad.addColorStop(1, 'rgba(59, 130, 246, 0.0)');
  
  const netGrad = ctx.createLinearGradient(0, 0, 0, 400);
  netGrad.addColorStop(0, 'rgba(16, 185, 129, 0.4)');
  netGrad.addColorStop(1, 'rgba(16, 185, 129, 0.0)');
  
  incomeChartInstance = new Chart(ctx, {
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
        legend: {
          labels: { color: '#f3f4f6', font: { family: 'Plus Jakarta Sans' } }
        },
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
      scales: {
        x: {
          grid: { color: 'rgba(255, 255, 255, 0.03)' },
          ticks: { color: '#9ca3af', font: { family: 'Plus Jakarta Sans', size: 9 } }
        },
        y: {
          grid: { color: 'rgba(255, 255, 255, 0.03)' },
          ticks: { 
            color: '#9ca3af', 
            font: { family: 'Plus Jakarta Sans', size: 9 },
            callback: function(value) { return '₹' + (value/1000) + 'k'; }
          }
        }
      }
    }
  });
}

function updateIncomeChart(filter) {
  // Update button active state
  const buttons = document.querySelectorAll('.filter-btn');
  buttons.forEach(btn => btn.classList.remove('active'));
  
  const activeBtn = Array.from(buttons).find(btn => btn.textContent.toLowerCase() === filter.toLowerCase() || (filter === 'all' && btn.textContent === 'All'));
  if (activeBtn) activeBtn.classList.add('active');
  
  renderIncomeTrendChart(filter);
}

// Chart 2: Earnings vs Deductions Share (Doughnut Chart)
function renderShareBreakdownChart() {
  const ctx = document.getElementById('shareBreakdownChart').getContext('2d');
  
  shareChartInstance = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Net Take-Home', 'Provident Fund (DSOP)', 'Taxes & Cess', 'Other Deductions'],
      datasets: [{
        data: [70, 15, 12, 3],
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
          labels: { color: '#f3f4f6', font: { family: 'Plus Jakarta Sans', size: 10 } }
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

function updateShareChart(record) {
  if (!shareChartInstance) return;
  
  const gross = record.summary.gross_pay || 1;
  const net = record.summary.net_remittance;
  const dsop = record.deductions.dsop_subscription || 0;
  const tax = (record.deductions.income_tax || 0) + (record.deductions.education_cess || 0);
  const other = record.summary.total_deductions - dsop - tax;
  
  const netPerc = (net / gross) * 100;
  const dsopPerc = (dsop / gross) * 100;
  const taxPerc = (tax / gross) * 100;
  const otherPerc = Math.max(0, (other / gross) * 100);
  
  shareChartInstance.data.datasets[0].data = [netPerc, dsopPerc, taxPerc, otherPerc];
  shareChartInstance.update();
}

// Chart 3: DSOP Wealth Accumulation Chart (Area Chart)
function renderDsopGrowthChart() {
  const ctx = document.getElementById('dsopGrowthChart').getContext('2d');
  
  // Filter records that have DSOP closing balance available
  const dsopRecords = window.payslipData.filter(d => d.tax_and_savings && d.tax_and_savings.dsop_fund && d.tax_and_savings.dsop_fund.closing_balance > 0);
  
  const labels = dsopRecords.map(d => d.date_str);
  const balances = dsopRecords.map(d => d.tax_and_savings.dsop_fund.closing_balance);
  
  const dsopGrad = ctx.createLinearGradient(0, 0, 0, 300);
  dsopGrad.addColorStop(0, 'rgba(139, 92, 246, 0.4)');
  dsopGrad.addColorStop(1, 'rgba(139, 92, 246, 0.0)');
  
  dsopChartInstance = new Chart(ctx, {
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
      scales: {
        x: {
          grid: { color: 'rgba(255, 255, 255, 0.02)' },
          ticks: { color: '#9ca3af', font: { family: 'Plus Jakarta Sans', size: 9 } }
        },
        y: {
          grid: { color: 'rgba(255, 255, 255, 0.02)' },
          ticks: { 
            color: '#9ca3af', 
            font: { family: 'Plus Jakarta Sans', size: 9 },
            callback: function(value) { return '₹' + (value/100000).toFixed(1) + 'L'; }
          }
        }
      }
    }
  });
}

// Chart 4: YTD Income Tax Projections (Bar Chart)
function renderTaxProjectionsChart() {
  const ctx = document.getElementById('taxProjectionsChart').getContext('2d');
  
  // Extract yearly summaries of tax
  // We look for December/Jan records of each year or the latest available record of each year
  const years = [...new Set(window.payslipData.map(d => d.year))];
  const yearlyTaxData = [];
  
  years.forEach(yr => {
    // Find the latest record of this year that has tax_and_savings details
    const records = window.payslipData.filter(d => d.year === yr && d.tax_and_savings && d.tax_and_savings.gross_salary_ytd > 0);
    if (records.length > 0) {
      // get the latest one in the year
      const latest = records[records.length - 1];
      yearlyTaxData.push({
        year: yr,
        gross: latest.tax_and_savings.gross_salary_ytd,
        tax: latest.tax_and_savings.total_tax_payable,
        paid: latest.tax_and_savings.tax_deducted_ytd + latest.tax_and_savings.cess_deducted_ytd
      });
    }
  });
  
  const labels = yearlyTaxData.map(d => d.year);
  const taxPayable = yearlyTaxData.map(d => d.tax);
  const taxPaid = yearlyTaxData.map(d => d.paid);
  
  taxChartInstance = new Chart(ctx, {
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
        legend: {
          labels: { color: '#f3f4f6', font: { family: 'Plus Jakarta Sans', size: 10 } }
        },
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
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: '#9ca3af', font: { family: 'Plus Jakarta Sans' } }
        },
        y: {
          grid: { color: 'rgba(255, 255, 255, 0.02)' },
          ticks: { 
            color: '#9ca3af', 
            font: { family: 'Plus Jakarta Sans', size: 9 },
            callback: function(value) { return '₹' + (value/1000) + 'k'; }
          }
        }
      }
    }
  });
}

// ---------------- MOCK PARSER ACTIONS ----------------

function openUploadModal() {
  document.getElementById("upload-modal").classList.add("active");
}

function closeUploadModal() {
  document.getElementById("upload-modal").classList.remove("active");
  // Reset loader views
  document.getElementById("modal-form-content").style.display = "block";
  document.getElementById("parser-loader").style.display = "none";
}

function handleFileSelect() {
  const fileInput = document.getElementById("payslip-file");
  if (fileInput.files.length > 0) {
    const filename = fileInput.files[0].name;
    console.log("Selected file:", filename);
  }
}

function startParsingAnimation() {
  const fileInput = document.getElementById("payslip-file");
  if (fileInput.files.length === 0) {
    alert("Please select a payslip PDF file to parse.");
    return;
  }
  
  const passwordInput = document.getElementById("pdf-password").value;
  if (!passwordInput) {
    alert("Please enter the PDF decryption password.");
    return;
  }
  
  // Transition to loading view
  document.getElementById("modal-form-content").style.display = "none";
  document.getElementById("parser-loader").style.display = "flex";
  
  const statusEl = document.getElementById("loader-status");
  
  // Run steps
  setTimeout(() => {
    statusEl.innerHTML = "🔓 Decrypting PDF with password: <strong>" + passwordInput + "</strong>...";
    setTimeout(() => {
      statusEl.innerHTML = "🔍 Running OCR & Text layout alignment...";
      setTimeout(() => {
        statusEl.innerHTML = "📊 Extracting Earnings & Deductions Tables...";
        setTimeout(() => {
          statusEl.innerHTML = "🗂️ Mapping custom army pay codes (BPAY, MSP, DSOP)...";
          setTimeout(() => {
            statusEl.innerHTML = "📈 Standardizing database records...";
            setTimeout(() => {
              // Successfully parsed!
              alert("Successfully decrypted and parsed payslip! Added new data records to dashboard.");
              closeUploadModal();
              
              // We simulate updating the dashboard with the latest month (August 2025)
              const record = window.payslipData.find(d => d.year == 2025 && d.month_num == 8);
              if (record) {
                // Populate selectors to August 2025
                document.getElementById("select-year").value = "2025";
                updateMonthSelector("2025");
                document.getElementById("select-month").value = record.month_name;
                updatePayslipReplica(record);
              }
            }, 800);
          }, 800);
        }, 800);
      }, 800);
    }, 800);
  }, 500);
}
