import { payslipHistory } from './ai_insights_data.js';
import { AIInsightsEngine } from './ai_insights_engine.js';

const engine = new AIInsightsEngine(payslipHistory);

// Helpers
const formatINR = (val) => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 0
}).format(val);

// Update Overview Metrics
function renderMetrics(summary, current) {
  const dsop = current.tax_and_savings.dsop_fund.closing_balance;
  const it = current.deductions.income_tax + current.deductions.education_cess;
  const gross = current.summary.gross_pay || 1;
  const taxRate = ((it / gross) * 100).toFixed(1);

  const container = document.getElementById('metrics-grid');
  container.innerHTML = `
    <div class="metric-card">
      <div class="label">Net Remittance</div>
      <div class="value" style="color: var(--color-success);">${formatINR(summary.netRemittance)}</div>
      <div class="subtext">Credited to bank A/c</div>
    </div>
    <div class="metric-card">
      <div class="label">Gross Pay</div>
      <div class="value">${formatINR(summary.grossPay)}</div>
      <div class="subtext">Before deductions</div>
    </div>
    <div class="metric-card">
      <div class="label">DSOP Fund Balance</div>
      <div class="value" style="color: var(--color-info);">${formatINR(dsop)}</div>
      <div class="subtext">Compounding tax-free</div>
    </div>
    <div class="metric-card">
      <div class="label">Effective Tax Rate</div>
      <div class="value" style="color: var(--color-warning);">${taxRate}%</div>
      <div class="subtext">Monthly TDS deduction</div>
    </div>
  `;
}

// Generate email copy dialog
function openEmailModal(action) {
  const modal = document.getElementById('action-modal');
  const title = document.getElementById('modal-title');
  const body = document.getElementById('modal-body');

  title.innerText = action.label;
  body.innerHTML = `
    <p style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 12px;">
      Copy the pre-drafted PCDA(O) request letter below to email or print:
    </p>
    <textarea class="textarea-copy" readonly>${action.emailBody}</textarea>
    <button class="btn btn-primary" id="copy-btn">Copy to Clipboard</button>
  `;

  document.getElementById('copy-btn').onclick = () => {
    navigator.clipboard.writeText(action.emailBody);
    alert('Copied to clipboard!');
  };
  modal.style.display = 'flex';
}

// Interactive DSOP simulator
function openDSOPModal(basic, currentBal) {
  const modal = document.getElementById('action-modal');
  const title = document.getElementById('modal-title');
  const body = document.getElementById('modal-body');

  title.innerText = "Provident Fund Retirement Simulator";
  body.innerHTML = `
    <p style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 16px;">
      Project compounding retirement wealth based on your Basic Pay: <strong>${formatINR(basic)}</strong>
    </p>
    <div style="margin-bottom: 20px;">
      <label style="display:flex; justify-content:space-between; margin-bottom: 6px; font-size:0.9rem;">
        <span>Monthly DSOP Contribution</span>
        <span id="slider-val" style="color: var(--color-accent); font-weight:600;">₹40,000</span>
      </label>
      <input type="range" id="dsop-slider" min="${Math.round(basic*0.06)}" max="${Math.round(basic*0.5)}" value="40000" style="width:100%;">
      <div style="display:flex; justify-content:space-between; font-size:0.75rem; color: var(--text-muted); margin-top:4px;">
        <span>6% (Min): ${formatINR(basic*0.06)}</span>
        <span>50% (Max): ${formatINR(basic*0.5)}</span>
      </div>
    </div>
    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:12px;" id="sim-results">
      <!-- Dynamic compounding output -->
    </div>
  `;

  const slider = document.getElementById('dsop-slider');
  const updateSim = () => {
    const monthly = parseInt(slider.value);
    document.getElementById('slider-val').innerText = formatINR(monthly);
    
    // Simple compound projection: principal * (1+r/n)^(nt) + monthly * [((1+r/n)^(nt) - 1)/(r/n)]
    const r = 0.071; // DSOP interest rate
    let proj5 = currentBal;
    let proj10 = currentBal;
    for(let i=0; i<60; i++) proj5 = proj5 * (1 + r/12) + monthly;
    for(let i=0; i<120; i++) proj10 = proj10 * (1 + r/12) + monthly;

    document.getElementById('sim-results').innerHTML = `
      <div class="metric-card" style="padding:12px;">
        <div class="label" style="font-size:0.7rem;">Value in 5 Years</div>
        <div class="value" style="font-size:1.3rem; color: var(--color-info);">${formatINR(proj5)}</div>
      </div>
      <div class="metric-card" style="padding:12px;">
        <div class="label" style="font-size:0.7rem;">Value in 10 Years</div>
        <div class="value" style="font-size:1.3rem; color: var(--color-success);">${formatINR(proj10)}</div>
      </div>
    `;
  };

  slider.oninput = updateSim;
  updateSim();
  modal.style.display = 'flex';
}

// Render dynamic insight cards
function renderInsights(report, current) {
  const container = document.getElementById('insights-container');
  document.getElementById('insights-count').innerText = `${report.insights.length} Insights Generated`;
  
  if (report.insights.length === 0) {
    container.innerHTML = `<p style="color: var(--text-secondary); text-align:center; padding:20px;">No critical anomalies found this month.</p>`;
    return;
  }

  container.innerHTML = report.insights.map(ins => `
    <div class="insight-card ${ins.type}">
      <div class="insight-title-row">
        <h4 class="insight-title">${ins.title}</h4>
        <div class="score-pills">
          <span class="pill imp">IMP: ${ins.scores.importance}/10</span>
          <span class="pill conf">CONF: ${ins.scores.confidence}/10</span>
          <span class="pill val">VAL: ${ins.scores.value}/10</span>
        </div>
      </div>
      <p class="insight-desc">${ins.desc}</p>
      ${ins.action ? `
        <div class="insight-actions">
          <button class="btn btn-primary" data-action-id="${ins.id}">
            ${ins.action.label}
          </button>
        </div>
      ` : ''}
    </div>
  `).join('');

  // Attach dynamic actions
  report.insights.forEach(ins => {
    if (!ins.action) return;
    const btn = container.querySelector(`[data-action-id="${ins.id}"]`);
    if (ins.action.modalType === "dsop_simulator") {
      btn.onclick = () => openDSOPModal(current.earnings.basic_pay, current.tax_and_savings.dsop_fund.closing_balance);
    } else {
      btn.onclick = () => openEmailModal(ins.action);
    }
  });
}

// Render historical timeline context (Accrued Intelligence)
function renderTimeline(activeYear, activeMonthNum) {
  const container = document.getElementById('timeline-container');
  // Generate chronologically up to selected month
  const items = payslipHistory.filter(d => d.year < activeYear || (d.year === activeYear && d.month_num <= activeMonthNum));
  
  const textMapping = {
    11: "Parsed November salary records. Standardized baseline: Net remittance ₹1.75L. Saved ₹40,000 in DSOP (27.6% of Basic Pay).",
    12: "Debit recovery anomaly registered. Recovered ₹15,742 directly from net pay. System recorded baseline variance.",
    1: "Annual Increment Applied. Basic pay permanently updated to ₹1.49L (rises from ₹1.44L). Gross pay rises to ₹2.86L.",
    2: "No debt anomalies. Net remittance recovered to ₹1.90L (best month). Tax adjustments processed correctly.",
    3: "Compounding value hit. Annual DSOP tax-free interest credit of ₹1,46,341 logged. Total fund balance jumps to ₹24.27L.",
    4: "PCDA(O) DA revision (58% to 60%) verified. Back-arrears of ₹9.8k DA and ₹216 TPTA DA audited and confirmed correct."
  };

  container.innerHTML = items.map(d => `
    <div class="timeline-item">
      <div class="timeline-dot" style="${d.year === activeYear && d.month_num === activeMonthNum ? 'background: var(--color-success); box-shadow: 0 0 12px var(--color-success);' : ''}"></div>
      <div class="timeline-content">
        <span class="timeline-date">${d.month_name} ${d.year}</span>
        <p class="timeline-text">${textMapping[d.month_num] || "Monthly statement processed."}</p>
      </div>
    </div>
  `).reverse().join('');
}

// Orchestrator
function updateUI(dateStr) {
  const [year, monthNum] = dateStr.split('-').map(Number);
  const current = payslipHistory.find(d => d.year === year && d.month_num === monthNum);
  const report = engine.generateReport(year, monthNum);
  
  renderMetrics(report.summary, current);
  renderInsights(report, current);
  renderTimeline(year, monthNum);
}

// Event Listeners
document.getElementById('month-select').onchange = (e) => {
  updateUI(e.target.value);
};

document.getElementById('modal-close').onclick = () => {
  document.getElementById('action-modal').style.display = 'none';
};

window.onclick = (e) => {
  const modal = document.getElementById('action-modal');
  if (e.target === modal) modal.style.display = 'none';
};

// Initial Render (April 2026)
updateUI("2026-4");
