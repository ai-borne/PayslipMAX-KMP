---
description: How to make an Implementation Plan
---

Role: You are an expert Software Architect and Developer.
Context: The current plan you generated is a great foundation. We will now build upon it by breaking it down into a strict, phase-wise execution strategy.
Task: Create a detailed execution plan.
Strict Development Rules:
As we execute this plan, you must adhere to the following constraints at all times:
1. Phase Independence & Success: Every single phase must end with a fully successful build. Do not proceed to the next phase if the current build is failing.
2. Test-Driven Development (TDD): Write tests (both unit and integration) before or alongside the implementation. 100% of tests must pass before a phase is marked complete.
3. Architecture & Clean Code: * Strictly uphold MVVM, SOLID, DRY, and SSOT (Single Source of Truth) principles.
    * Hard Limit: No file may exceed 300 lines of code. Refactor immediately if a file approaches this limit.
4. Resource Management: Never hardcode strings or colors. Strictly use string resources and follow the established universal color scheme to prevent UI/UX tech debt.
5. Cybersecurity: The codebase must remain 100% secure. Actively prevent and check for security vulnerabilities in every addition or update.
Phase Handoff Protocol:
At the end of every phase, before moving to the next, you must pause and provide a Phase Summary containing:
1. What tech debt was incurred during the phase.
2. The exact steps you took to immediately and fully resolve it (No skipping or delaying tech debt).
3. Confirmation that the build succeeds and all tests pass. Only after resolving all debt and verifying tests will we move to the next phase.