// Bootstrapper for Indian Army Payslip Dashboard

import { PayslipModel } from './model.js';
import { PayslipViewModel } from './viewModel.js';
import { ChartManager } from './chartManager.js';
import { PayslipView } from './view.js';

document.addEventListener('DOMContentLoaded', () => {
  const model = new PayslipModel();
  const viewModel = new PayslipViewModel(model);
  const chartManager = new ChartManager();
  new PayslipView(viewModel, chartManager);
});
