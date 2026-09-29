# VERITAS User Guide

Welcome to **VERITAS**, your standardized procurement and audit management system. This guide will walk you through the essential features and workflows available in the platform based on your assigned role.

---

## 1. Getting Started

When you first open VERITAS (e.g., at `http://localhost:4200`), you will be greeted by the Login Screen. Use one of the designated accounts provided to you by your Administrator.

### Default Accounts for Local Development
- **Administrator:** `admin@veritas.com`
- **Finance Officer:** `finance@veritas.com`
- **Procurement Officer:** `procurement@veritas.com`
- **Requester:** `requester@veritas.com`

*(Default password for all local accounts is `password123`)*

---

## 2. Roles and Responsibilities

VERITAS is built around Role-Based Access Control. What you can see and do depends on your role:

- **Requesters:** Can draft and submit purchase requisitions, track their progress, and view their department's budget allocation.
- **Team Leaders:** (Usually a Requester with elevated team permissions) Responsible for the first line of approval ("Team Leader Confirmation") for requests coming from their team.
- **Procurement Officers:** Responsible for sourcing vendors, uploading vendor quotes, and selecting the best quote for a request.
- **Finance Officers:** Handle the final financial approval ("Standard Finance Review"), budget management, and invoice tracking.
- **Administrators:** Manage system settings, user accounts, departments, and custom workflow definitions (using the BPMN-like Workflow Editor).

---

## 3. The Procurement Workflow

The core of VERITAS is the Request Workflow. A typical procurement request follows these steps:

### Step 1: Creating a Requisition (Requester)
1. Navigate to the **Requests** tab and click **New Request**.
2. Fill in the required details: Title, Justification, Project, and Priority.
3. Add the items you need to purchase (e.g., "15 IntelliJ IDEA Ultimate Licenses").
4. Submit the request. It enters the workflow as **Active**.

### Step 2: Team Leader Confirmation
1. The request routes to the Team Leader of the requester's department.
2. The Team Leader reviews the request and either **Approves** or **Rejects** it.

### Step 3: Vendor Quote Selection (Procurement Officer)
1. Once approved by the team leader, the request moves to the Procurement team.
2. A Procurement Officer opens the request and adds quotes from different Vendors.
3. The officer selects the preferred quote and submits their choice.

### Step 4: Finance Review (Finance Officer)
1. The Finance Officer reviews the selected quote against the project/department budget.
2. They can approve the purchase, triggering the finalization of the request.
3. Unpaid invoices can be attached and evaluated here.

### Step 5: Completion
The request is marked as **Completed**, and the budget is officially deducted.

---

## 4. Key Features

### Budget Dashboard
Finance Officers and Admins can access the Budget Dashboard to visualize:
- Global and departmental budgets.
- Committed spend (funds allocated to active requests) vs. Actual spend (completed purchases).
- Remaining safety buffers.

### Vendor Management
Procurement Officers can manage a directory of Vendors, track their reliability scores, and evaluate their performance on past requests.

### Audit Logs
Administrators have access to a comprehensive **Audit Log**. Every critical action (approvals, role changes, budget modifications, deletions) is recorded immutably for compliance and security auditing.

### Jira Integration
VERITAS can synchronize request statuses directly with Jira, automatically adding comments and updating issue states when requests move through the procurement pipeline.

---

## 5. Settings and Profile

Click on your profile picture in the top right to access:
- **Profile Settings:** Update your name and email.
- **Password Reset:** Securely change your password.
- **Theme:** Toggle between Light and Dark mode.
