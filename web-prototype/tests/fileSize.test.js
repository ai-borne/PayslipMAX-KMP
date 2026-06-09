import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

function getFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const name = path.join(dir, file);
    if (fs.statSync(name).isDirectory()) {
      if (file !== 'node_modules' && file !== '.venv' && file !== '.git' && file !== 'dist' && file !== 'coverage') {
        getFiles(name, fileList);
      }
    } else {
      const ext = path.extname(file);
      if (['.js', '.css', '.html'].includes(ext)) {
        fileList.push(name);
      }
    }
  }
  return fileList;
}

describe('File Size Rules', () => {
  it('should verify that all code files are under 300 lines', () => {
    const files = getFiles(path.resolve('.'));
    expect(files.length).toBeGreaterThan(0);
    
    const violations = [];
    for (const file of files) {
      const content = fs.readFileSync(file, 'utf8');
      const lines = content.split(/\r?\n/).length;
      if (lines > 300) {
        violations.push(`${path.relative(path.resolve('.'), file)}: ${lines} lines`);
      }
    }
    
    expect(violations, `Files exceeding 300 lines limit:\n${violations.join('\n')}`).toEqual([]);
  });
});
