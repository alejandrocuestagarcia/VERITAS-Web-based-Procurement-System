package com.veritas.backend.seeding;

import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.entity.ClosedReason;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.QuoteLineItem;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.vendor.repository.QuoteLineItemRepository;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.service.WorkflowService;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.entity.RequestItemUnit;
import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.entity.Attachment;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.vendor.entity.VendorEvaluation;
import com.veritas.backend.vendor.repository.VendorEvaluationRepository;
import com.veritas.backend.integrations.currency.entity.Currency;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

//AI-REFACTORED
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seeding.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepo;
    private final ProjectRepository projectRepo;
    private final WorkflowService workflowService;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final VendorRepository vendorRepository;
    private final RequestRepository requestRepository;
    private final InternalBudgetRepository internalBudgetRepository;
    private final InvoiceRepository invoiceRepository;
    private final QuoteRepository quoteRepository;
    private final QuoteLineItemRepository quoteLineItemRepository;
    private final VendorEvaluationRepository vendorEvaluationRepository;
    private final AttachmentRepository attachmentRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting seed data initialization...");

        // 0. Seed Global Budget
        InternalBudget globalBudget = internalBudgetRepository.findAll().stream()
                .filter(b -> "Global Budget".equals(b.getBudgetName()) && b.getBudgetType() == BudgetType.GLOBAL)
                .findFirst()
                .orElseGet(() -> {
                    InternalBudget gb = InternalBudget.builder()
                            .budgetName("Global Budget")
                            .totalAmount(new BigDecimal("500000"))
                            .budgetType(BudgetType.GLOBAL)
                            .safetyBuffer(new BigDecimal("5"))
                            .actualSpend(BigDecimal.ZERO)
                            .committedSpend(BigDecimal.ZERO)
                            .build();
                    InternalBudget saved = internalBudgetRepository.save(gb);
                    log.info("Seeded global budget: {}", saved.getBudgetName());
                    return saved;
                });

        // 1. Seed Essential Departments
        Department department = seedDepartment("Cloud Platform Engineering", new BigDecimal("130000"), globalBudget);
        Department department2 = seedDepartment("Talent & People Operations", new BigDecimal("95000"), globalBudget);

        // 2. Seed Essential Teams
        Team teamOne = seedTeam("Core Platform Engineering", department,
                "Core software platform development, devops, and reliability engineering team");
        Team teamTwo = seedTeam("Talent Acquisition & Ops", department2,
                "Enterprise talent acquisition and people operations core team");

        // 3. Seed Essential Users
        seedUser("Veritas Admin", "admin@veritas.com", "password123", UserRole.ADMINISTRATOR, null, null);
        seedUser("Veritas Finance", "finance@veritas.com", "password123", UserRole.FINANCE_OFFICER, null, null);
        seedUser("Veritas Procurement", "procurement@veritas.com", "password123", UserRole.PROCUREMENT_OFFICER,
                department, null);

        User requester = seedUser("Veritas Requester", "requester@veritas.com", "password123", UserRole.REQUESTER, null,
                teamOne);
        if (teamOne.getLeader() == null) {
            teamOne.setLeader(requester);
            teamRepo.save(teamOne);
            log.info("Set user {} as leader of team {}", requester.getEmail(), teamOne.getName());
        }

        User hrRequester = seedUser("Veritas HR Requester", "hr_requester@veritas.com", "password123",
                UserRole.REQUESTER, null, teamTwo);
        if (teamTwo.getLeader() == null) {
            teamTwo.setLeader(hrRequester);
            teamRepo.save(teamTwo);
            log.info("Set user {} as leader of team {}", hrRequester.getEmail(), teamTwo.getName());
        }

        // 4. Seed Essential Projects
        Project p1 = seedProject("Project Artemis: Cloud Architecture Audit", "artemis", new BigDecimal("60000"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), teamOne);
        Project p2 = seedProject("NextGen ERP Integration", "nextgen-erp", new BigDecimal("65000"),
                LocalDate.of(2026, 3, 15), LocalDate.of(2027, 6, 1), teamOne);

        // 5. Seed Essential Workflow
        if (workflowDefinitionRepository.count() == 0) {
            workflowService.createWorkflow(new WorkflowSaveDto(DatabaseSeederConstants.STANDARD_WORKFLOW, null));
            log.info("Seeded basic workflow definition");
        }

        // 6. Seed Essential Vendors
        Vendor globalTech = seedVendor("Global Tech Solutions", "US-123456789",
                "Leading provider of enterprise hardware and software licenses.", "John Smith",
                "jsmith@globaltech.com");
        Vendor primeLogistics = seedVendor("Prime Logistics & Services", "EU-987654321",
                "Global shipping, handling, and logistics partner.",
                "Elena Vance", "evance@primelogistics.com");

        // 7. Seed Initial Requests with proper quotes and budgets
        var workflowDef = workflowDefinitionRepository.findAll().stream().findFirst().orElse(null);
        Project primaryProject = p1 != null ? p1 : p2;

        // ---- 7a. Draft Request - no quotes yet ----
        RequestItem draftItem = new RequestItem();
        draftItem.setName("Core Edge Firewall Requisition Licenses");
        draftItem.setQuantity(1);
        draftItem.setUnit(RequestItemUnit.PIECES);
        draftItem.setDescription("High-throughput firewall license pack");

        Request draftRequest = seedMockRequest(
                "Core Edge Firewall Requisition",
                "Acquisition of high-throughput firewall licenses to secure core edge network endpoints and prevent unauthorized traffic.",
                Priority.MEDIUM,
                requester,
                primaryProject,
                workflowDef,
                WorkflowComponent.START_EVENT,
                null,
                RequestStatus.DRAFT,
                null,
                new BigDecimal("8000.00"),
                List.of(draftItem),
                null);
        log.info("Seeded draft request: {}", draftRequest.getRequestKey());

        if (workflowDef != null && requester != null && primaryProject != null) {
            // ---- 7b. Active Request - Team Leader Confirmation (no quotes yet) ----
            RequestItem requesterReviewItem = new RequestItem();
            requesterReviewItem.setName("AWS Dev Sandbox Credits");
            requesterReviewItem.setQuantity(1);
            requesterReviewItem.setUnit(RequestItemUnit.PIECES);
            requesterReviewItem.setDescription("Monthly dev allowance");

            Request teamLeaderRequest = seedMockRequest(
                    "AWS Multi-Region Infrastructure Sandbox",
                    "Resource credit requisition to facilitate high-availability staging and multi-region failover dry-runs for the microservices platform.",
                    Priority.MEDIUM,
                    requester,
                    primaryProject,
                    workflowDef,
                    WorkflowComponent.STEP,
                    "Team Leader Confirmation",
                    RequestStatus.ACTIVE,
                    requester,
                    new BigDecimal("2000.00"),
                    List.of(requesterReviewItem),
                    null);

            // ---- 7c. Active Request - Vendor Quote Selection (has quotes, none selected) ----
            RequestItem chairsItem = new RequestItem();
            chairsItem.setName("Ergonomic Chairs");
            chairsItem.setQuantity(5);
            chairsItem.setUnit(RequestItemUnit.PIECES);
            chairsItem.setDescription("Mesh backing, fully adjustable");

            RequestItem desksItem = new RequestItem();
            desksItem.setName("Standing Desk Converters");
            desksItem.setQuantity(3);
            desksItem.setUnit(RequestItemUnit.PIECES);
            desksItem.setDescription("Dual monitor support");

            Request vendorSelectRequest = seedMockRequest(
                    "Premium Ergonomic Office Furniture Upgrade",
                    "Procurement of fully adjustable ergonomic task chairs to support long-term physical wellness and platform engineering comfort.",
                    Priority.MEDIUM,
                    requester,
                    primaryProject,
                    workflowDef,
                    WorkflowComponent.STEP,
                    "Vendor Quote Selection",
                    RequestStatus.ACTIVE,
                    userRepo.findByEmail("procurement@veritas.com").orElse(null),
                    new BigDecimal("10000.00"),
                    List.of(chairsItem, desksItem),
                    null);
            // Create quotes but none selected (procurement officer will select later)
            List<Vendor> allVendors = vendorRepository.findAll();
            seedQuotesForRequest(vendorSelectRequest, allVendors, new Random(42), false);

            // ---- 7d. Active Request - Finance Review (quotes exist, one selected, committedSpend set) ----
            RequestItem licensesItem = new RequestItem();
            licensesItem.setName("IntelliJ IDEA Ultimate License");
            licensesItem.setQuantity(15);
            licensesItem.setUnit(RequestItemUnit.PIECES);
            licensesItem.setDescription("Annual corporate subscription");

            Request financeReviewRequest = seedMockRequest(
                    "IntelliJ IDEA Enterprise Renewal",
                    "Annual corporate license renewal of the IntelliJ IDEA developer IDE for the core engineering and applications platform team.",
                    Priority.HIGH,
                    requester,
                    primaryProject,
                    workflowDef,
                    WorkflowComponent.STEP,
                    "Standard Finance Review",
                    RequestStatus.ACTIVE,
                    userRepo.findByEmail("finance@veritas.com").orElse(null),
                    new BigDecimal("24000.00"),
                    List.of(licensesItem),
                    null);
            // Create quotes and select best one -> updates committedSpend
            seedQuotesForRequest(financeReviewRequest, allVendors, new Random(42), true);
            Quote financeSelectedQuote = financeReviewRequest.getSelectedQuote();
            if (financeSelectedQuote != null) {
                Invoice financeInv = seedUnpaidInvoice(financeReviewRequest, financeSelectedQuote.getVendorID(),
                        financeSelectedQuote.getTotalAmount(), LocalDate.now().minusDays(3));
                seedInvoiceAttachment(financeReviewRequest, financeInv);
            }

            // ---- 7e. Finished Request (quotes exist, one selected, invoice paid) ----
            RequestItem finishedItem = new RequestItem();
            finishedItem.setName("GitKraken Pro License Pack");
            finishedItem.setQuantity(1);
            finishedItem.setUnit(RequestItemUnit.BOXES);
            finishedItem.setDescription("10 licenses annual subscription");

            Request finishedRequest = seedMockRequest(
                    "GitKraken Pro Suite Requisition",
                    "Acquisition of collaborative git visual client licenses to optimize team git operations and enhance code review workflow transparency.",
                    Priority.MEDIUM,
                    requester,
                    primaryProject,
                    workflowDef,
                    WorkflowComponent.END_EVENT,
                    null,
                    RequestStatus.FINISHED,
                    null,
                    new BigDecimal("8000.00"),
                    List.of(finishedItem),
                    null);
            // Create quotes, select best, invoice unpaid (awaiting payment)
            seedQuotesForRequest(finishedRequest, allVendors, new Random(42), true);
            Quote selectedQuote = finishedRequest.getSelectedQuote();
            if (selectedQuote != null) {
                Invoice paidInv = seedUnpaidInvoice(finishedRequest, selectedQuote.getVendorID(),
                        selectedQuote.getTotalAmount(), LocalDate.now().minusDays(5));
                seedInvoiceAttachment(finishedRequest, paidInv);
            }

            log.info("Seeded manually crafted requests with quotes and budget tracking");
        }

        // AI-GENERATED
        // 8. Seed Dynamic Rich Test Data (Faker)
        if (userRepo.count() <= 5) {
            log.info("Initializing rich dynamic mock data seeding with Faker...");
            Random random = new Random(42);
            Faker faker = new Faker(random);

            // Extra Departments
            List<Department> allDepts = new ArrayList<>();
            allDepts.add(department);
            allDepts.add(department2);

            String[] extraDeptNames = { "Digital Growth & Marketing", "Global Logistics & Ops" };
            for (String deptName : extraDeptNames) {
                BigDecimal budgetAmt = new BigDecimal(faker.number().numberBetween(55000, 80000));
                Department d = seedDepartment(deptName, budgetAmt, globalBudget);
                if (d != null && !allDepts.contains(d)) {
                    allDepts.add(d);
                }
            }

            // Extra Teams
            List<Team> allTeams = new ArrayList<>();
            allTeams.add(teamOne);
            allTeams.add(teamTwo);

            String[][] extraTeams = {
                    { "Digital Growth Marketing", "Digital Growth & Marketing",
                            "Handles corporate marketing campaigns, branding, and social media presence" },
                    { "Strategic Enterprise Sales", "Digital Growth & Marketing",
                            "Handles high-value enterprise accounts and strategic partnerships" },
                    { "Global Customer Success", "Global Logistics & Ops",
                            "First line of customer care, strategic onboarding, and account retention" },
                    { "Cloud Platform Operations", "Cloud Platform Engineering",
                            "Manages staging environments, server scaling, and deployment pipelines" }
            };

            for (String[] teamData : extraTeams) {
                String teamName = teamData[0];
                String deptName = teamData[1];
                String desc = teamData[2];

                Department teamDept = departmentRepository.findByName(deptName)
                        .orElse(allDepts.get(random.nextInt(allDepts.size())));
                Team t = seedTeam(teamName, teamDept, desc);
                if (t != null && !allTeams.contains(t)) {
                    allTeams.add(t);
                }
            }

            // Dynamic Users (25 users)
            List<User> seededRequesters = new ArrayList<>();

            for (int i = 1; i <= 25; i++) {
                String name = faker.name().fullName();
                String cleanName = name.toLowerCase().replaceAll("[^a-z]", "");
                String email = cleanName + "." + i + "@veritas.com";

                UserRole role;
                Team userTeam = null;
                Department userDept = null;

                if (i <= 15) {
                    role = UserRole.REQUESTER;
                    userTeam = allTeams.get(random.nextInt(allTeams.size()));
                } else if (i <= 20) {
                    role = UserRole.PROCUREMENT_OFFICER;
                    userDept = allDepts.get(random.nextInt(allDepts.size()));
                } else if (i <= 23) {
                    role = UserRole.FINANCE_OFFICER;
                } else {
                    role = UserRole.ADMINISTRATOR;
                }

                User user = seedUser(name, email, "password123", role, userDept, userTeam);
                if (user != null && role == UserRole.REQUESTER) {
                    seededRequesters.add(user);
                }
            }

            // Assign Team Leaders for Teams that don't have one
            for (Team t : allTeams) {
                Team currentTeam = teamRepo.findById(t.getTeamId()).orElse(t);
                if (currentTeam.getLeader() == null) {
                    List<User> teamMembers = userRepo.findAllByTeamTeamId(currentTeam.getTeamId()).stream()
                            .filter(u -> u.getRole() == UserRole.REQUESTER)
                            .toList();
                    if (!teamMembers.isEmpty()) {
                        User leader = teamMembers.getFirst();
                        currentTeam.setLeader(leader);
                        teamRepo.save(currentTeam);
                        log.info("Assigned team leader: {} to team {}", leader.getEmail(), currentTeam.getName());
                    }
                }
            }

            // Seed Extra Projects
            String[] extraProjectNames = { "Aether Multi-Region Cloud Migration", "Project Oasis: AI-Driven CRM Portal",
                    "Titan: Real-Time Logistics Optimization", "Project Velocity: Strategic Sales Booster",
                    "Apex Omnichannel Support Hub" };
            for (int i = 0; i < extraProjectNames.length; i++) {
                String projName = extraProjectNames[i];
                String key = projName.toLowerCase().replaceAll("[^a-z]", "") + "-" + i;
                Team projectTeam = allTeams.get(i % allTeams.size());
                BigDecimal budgetAmt = new BigDecimal(faker.number().numberBetween(70000, 110000));
                LocalDate startDate = LocalDate.now().minusMonths(random.nextInt(1, 6));
                LocalDate endDate = LocalDate.now().plusMonths(random.nextInt(6, 18));
                seedProject(projName, key, budgetAmt, startDate, endDate, projectTeam);
            }

            // Extra Vendors (6 vendors)
            for (int i = 0; i < 6; i++) {
                String vendorName = faker.company().name();
                String taxId = "US-" + faker.number().digits(9);
                String desc = faker.company().catchPhrase();
                String contactName = faker.name().fullName();
                String contactEmail = faker.internet().emailAddress();
                seedVendor(vendorName, taxId, desc, contactName, contactEmail);
            }

            // Load all vendors for quotes
            List<Vendor> allDynamicVendors = vendorRepository.findAll();

            // Seed Mock Requests in Different States using Standard Workflow
            var standardWorkflowDef = workflowDefinitionRepository.findAll().stream().findFirst().orElse(null);
            User primaryRequester = userRepo.findByEmail("requester@veritas.com").orElse(null);
            User primaryProcurement = userRepo.findByEmail("procurement@veritas.com").orElse(null);
            User primaryFinance = userRepo.findByEmail("finance@veritas.com").orElse(null);

            // 8a. Seed a couple of ACTIVE requests at different workflow stages (without quotes yet)
            if (standardWorkflowDef != null && primaryRequester != null && primaryProject != null) {
                // 1. Another Draft
                RequestItem draftItem2 = new RequestItem();
                draftItem2.setName("Dell XPS 15 Laptop");
                draftItem2.setQuantity(1);
                draftItem2.setUnit(RequestItemUnit.PIECES);
                draftItem2.setDescription("16GB RAM, 512GB SSD");

                seedMockRequest(
                        "Developer Workstation Procurement",
                        "Procurement of high-performance development workstations for incoming senior software engineering and devops personnel.",
                        Priority.LOW,
                        primaryRequester,
                        primaryProject,
                        standardWorkflowDef,
                        WorkflowComponent.START_EVENT,
                        null,
                        RequestStatus.DRAFT,
                        null,
                        BigDecimal.ZERO,
                        List.of(draftItem2),
                        null);

                // 2. Standard Finance Review for a non-Cloud-Platform project
                Project otherProject = projectRepo.findAll().stream()
                        .filter(p -> !"artemis".equals(p.getProjectKey()) && !"nextgen-erp".equals(p.getProjectKey()))
                        .findFirst().orElse(p2);

                RequestItem financeItem2 = new RequestItem();
                financeItem2.setName("Docker Enterprise License");
                financeItem2.setQuantity(20);
                financeItem2.setUnit(RequestItemUnit.PIECES);
                financeItem2.setDescription("Annual container registry and orchestration license");

                Request financeReview2 = seedMockRequest(
                        "Docker Enterprise Container Platform Renewal",
                        "Renewal of Docker Enterprise licenses for containerized microservices deployment and CI/CD pipeline infrastructure.",
                        Priority.HIGH,
                        primaryRequester,
                        otherProject,
                        standardWorkflowDef,
                        WorkflowComponent.STEP,
                        "Standard Finance Review",
                        RequestStatus.ACTIVE,
                        primaryFinance,
                        new BigDecimal("18000.00"),
                        List.of(financeItem2),
                        null);
                seedQuotesForRequest(financeReview2, allDynamicVendors, new Random(42), true);
                Quote fr2Quote = financeReview2.getSelectedQuote();
                if (fr2Quote != null) {
                    Invoice fr2Inv = seedUnpaidInvoice(financeReview2, fr2Quote.getVendorID(),
                            fr2Quote.getTotalAmount(), LocalDate.now().minusDays(4));
                    seedInvoiceAttachment(financeReview2, fr2Inv);
                }

                // 3. Awaiting Payment for a non-Cloud-Platform project
                Project otherProject2 = projectRepo.findAll().stream()
                        .filter(p -> !"artemis".equals(p.getProjectKey()) && !"nextgen-erp".equals(p.getProjectKey())
                                && !p.getProjectKey().equals(otherProject.getProjectKey()))
                        .findFirst().orElse(otherProject);

                RequestItem awaitingItem2 = new RequestItem();
                awaitingItem2.setName("Kubernetes Production Support");
                awaitingItem2.setQuantity(1);
                awaitingItem2.setUnit(RequestItemUnit.PIECES);
                awaitingItem2.setDescription("Annual enterprise support subscription for 3 production clusters");

                Request awaitingPayment2 = seedMockRequest(
                        "Kubernetes Enterprise Support Subscription",
                        "Annual enterprise-grade support subscription for Kubernetes production clusters powering the platform and related microservices.",
                        Priority.HIGH,
                        primaryRequester,
                        otherProject2,
                        standardWorkflowDef,
                        WorkflowComponent.END_EVENT,
                        null,
                        RequestStatus.FINISHED,
                        null,
                        new BigDecimal("15000.00"),
                        List.of(awaitingItem2),
                        null);
                seedQuotesForRequest(awaitingPayment2, allDynamicVendors, new Random(42), true);
                Quote ap2Quote = awaitingPayment2.getSelectedQuote();
                if (ap2Quote != null) {
                    Invoice ap2Inv = seedUnpaidInvoice(awaitingPayment2, ap2Quote.getVendorID(),
                            ap2Quote.getTotalAmount(), LocalDate.now().minusDays(1));
                    seedInvoiceAttachment(awaitingPayment2, ap2Inv);
                }
            }

            List<Project> allProjects = projectRepo.findAll();
            // AI-GENERATED
            Random dynamicRandom = new Random(42);
            for (int i = 0; i < 100; i++) {
                Project userProject = allProjects.isEmpty() ? primaryProject : allProjects.get(dynamicRandom.nextInt(allProjects.size()));

                User randomRequester = null;
                if (userProject != null && userProject.getTeam() != null) {
                    randomRequester = userRepo.findAllByTeamTeamId(userProject.getTeam().getTeamId()).stream()
                            .filter(u -> u.getRole() == UserRole.REQUESTER)
                            .findFirst()
                            .orElse(null);
                }
                if (randomRequester == null) {
                    randomRequester = seededRequesters.isEmpty() ? primaryRequester : seededRequesters.get(dynamicRandom.nextInt(seededRequesters.size()));
                }

                int currentMonth = LocalDate.now().getMonthValue();
                int currentYear = LocalDate.now().getYear();
                int month = (i % currentMonth) + 1;
                int day = dynamicRandom.nextInt(1, 28);
                BigDecimal amount = new BigDecimal(dynamicRandom.nextInt(2000, 12000));

                // Budget check against project budget
                if (userProject != null && userProject.getInternalBudget() != null) {
                    InternalBudget projectBudget = internalBudgetRepository.findById(userProject.getInternalBudget().getId()).orElse(null);
                    if (projectBudget != null) {
                        BigDecimal totalAmount = projectBudget.getTotalAmount() != null ? projectBudget.getTotalAmount() : BigDecimal.ZERO;
                        BigDecimal currentActual = projectBudget.getActualSpend() != null ? projectBudget.getActualSpend() : BigDecimal.ZERO;
                        BigDecimal currentCommitted = projectBudget.getCommittedSpend() != null ? projectBudget.getCommittedSpend() : BigDecimal.ZERO;
                        BigDecimal totalSpendAfter = currentActual.add(currentCommitted).add(amount);
                        BigDecimal limit = totalAmount.multiply(new BigDecimal("0.60"));
                        if (totalSpendAfter.compareTo(limit) > 0) {
                            continue;
                        }

                        // Budget check against department budget (parent of project)
                        InternalBudget deptBudget = projectBudget.getParentBudget();
                        if (deptBudget != null) {
                            deptBudget = internalBudgetRepository.findById(deptBudget.getId()).orElse(null);
                            if (deptBudget != null) {
                                BigDecimal deptTotal = deptBudget.getTotalAmount() != null ? deptBudget.getTotalAmount() : BigDecimal.ZERO;
                                BigDecimal deptActual = deptBudget.getActualSpend() != null ? deptBudget.getActualSpend() : BigDecimal.ZERO;
                                BigDecimal deptCommitted = deptBudget.getCommittedSpend() != null ? deptBudget.getCommittedSpend() : BigDecimal.ZERO;
                                BigDecimal deptSpendAfter = deptActual.add(deptCommitted).add(amount);
                                BigDecimal deptLimit = deptTotal.multiply(new BigDecimal("0.90"));
                                if (deptSpendAfter.compareTo(deptLimit) > 0) {
                                    continue;
                                }
                            }
                        }
                    }
                }

                String reqName = faker.commerce().productName() + " Acquisition";
                String reqDesc = "Dynamic procurement request for " + reqName.toLowerCase() + " to support business operations.";

                // Create a couple of realistic line items
                List<RequestItem> dynamicItems = new ArrayList<>();
                int numItems = dynamicRandom.nextInt(1, 4);
                for (int j = 0; j < numItems; j++) {
                    RequestItem paidItem = new RequestItem();
                    paidItem.setName(faker.commerce().productName());
                    paidItem.setQuantity(dynamicRandom.nextInt(1, 10));
                    paidItem.setUnit(RequestItemUnit.values()[dynamicRandom.nextInt(RequestItemUnit.values().length)]);
                    paidItem.setDescription(faker.commerce().material() + " - " + faker.lorem().sentence());
                    dynamicItems.add(paidItem);
                }

                LocalDateTime createdAt = LocalDateTime.of(currentYear, month, day, 10, 0);

                Request mockReq = seedMockRequest(
                        reqName,
                        reqDesc,
                        Priority.values()[dynamicRandom.nextInt(Priority.values().length)],
                        randomRequester,
                        userProject,
                        standardWorkflowDef,
                        WorkflowComponent.END_EVENT,
                        null,
                        RequestStatus.FINISHED,
                        null,
                        amount,
                        dynamicItems,
                        createdAt);

                // Create quotes for each finished request
                if (!allDynamicVendors.isEmpty()) {
                    seedQuotesForRequest(mockReq, allDynamicVendors, dynamicRandom, true);
                }

                // Create paid invoice for each finished request
                Quote selectedQuote = mockReq.getSelectedQuote();
                Vendor invoiceVendor = !allDynamicVendors.isEmpty()
                        ? allDynamicVendors.get(dynamicRandom.nextInt(allDynamicVendors.size()))
                        : null;

                Invoice paidInvoice = null;
                if (selectedQuote != null) {
                    paidInvoice = seedPaidInvoice(mockReq, selectedQuote.getVendorID(),
                            selectedQuote.getTotalAmount(),
                            LocalDate.of(currentYear, month, day).plusDays(5));
                } else if (invoiceVendor != null) {
                    paidInvoice = seedPaidInvoice(mockReq, invoiceVendor,
                            amount,
                            LocalDate.of(currentYear, month, day).plusDays(5));
                }

                // Seed vendor evaluation for finished requests
                if (selectedQuote != null && !allDynamicVendors.isEmpty()) {
                    Vendor evaluatedVendor = selectedQuote.getVendorID();
                    User evaluator = userRepo.findAll().stream()
                            .filter(u -> u.getRole() == UserRole.PROCUREMENT_OFFICER)
                            .findAny()
                            .orElse(null);
                    if (evaluator != null && evaluatedVendor != null) {
                        seedVendorEvaluation(evaluatedVendor, mockReq, evaluator, dynamicRandom);
                    }
                }

                // Attach sample invoice PDF to finished requests
                if (paidInvoice != null) {
                    seedInvoiceAttachment(mockReq, paidInvoice);
                }
            }

            log.info("Rich dynamic mock data seeding complete.");
        }

        log.info("Seed data initialization complete.");
    }

    private Department seedDepartment(String name, BigDecimal budgetAmount, InternalBudget globalBudget) {
        return departmentRepository.findByName(name).map(dept -> {
            if (dept.getInternalBudget() != null && dept.getInternalBudget().getParentBudget() == null && globalBudget != null) {
                dept.getInternalBudget().setParentBudget(globalBudget);
                internalBudgetRepository.save(dept.getInternalBudget());
            }
            return dept;
        }).orElseGet(() -> {
            Department dept = new Department();
            dept.setName(name);
            dept.setInternalBudget(InternalBudget.builder()
                    .budgetName(name)
                    .totalAmount(budgetAmount)
                    .budgetType(BudgetType.DEPARTMENT)
                    .parentBudget(globalBudget)
                    .actualSpend(BigDecimal.ZERO)
                    .committedSpend(BigDecimal.ZERO)
                    .safetyBuffer(new BigDecimal("5"))
                    .build());
            Department saved = departmentRepository.save(dept);
            log.info("Seeded department: {}", saved.getName());
            return saved;
        });
    }

    private Team seedTeam(String name, Department department, String description) {
        return teamRepo.findByName(name).orElseGet(() -> {
            Team team = new Team();
            team.setName(name);
            team.setDepartment(department);
            team.setDescription(description);
            team.setIsActive(true);
            Team saved = teamRepo.save(team);
            log.info("Seeded team: {}", saved.getName());
            return saved;
        });
    }

    private User seedUser(String name, String email, String password, UserRole role, Department department, Team team) {
        return userRepo.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .passwordHash(encoder.encode(password))
                    .role(role)
                    .department(department)
                    .team(team)
                    .isActive(true)
                    .requiresPasswordChange(false)
                    .build();
            User saved = userRepo.save(user);
            log.info("Seeded user: {} (role={})", saved.getEmail(), saved.getRole());
            return saved;
        });
    }

    private Project seedProject(String name, String key, BigDecimal budgetAmount, LocalDate startDate,
            LocalDate endDate, Team team) {
        if (!projectRepo.existsByNameOrProjectKey(name, key)) {
            InternalBudget deptBudget = null;
            if (team != null && team.getDepartment() != null) {
                deptBudget = team.getDepartment().getInternalBudget();
            }

            Project p = Project.builder()
                    .name(name)
                    .projectKey(key)
                    .internalBudget(InternalBudget.builder()
                            .budgetName(name)
                            .totalAmount(budgetAmount)
                            .budgetType(BudgetType.PROJECT)
                            .parentBudget(deptBudget)
                            .actualSpend(BigDecimal.ZERO)
                            .committedSpend(BigDecimal.ZERO)
                            .safetyBuffer(new BigDecimal("5"))
                            .build())
                    .startDate(startDate)
                    .endDate(endDate)
                    .team(team)
                    .build();
            Project saved = projectRepo.save(p);
            log.info("Seeded project: {} (assigned to team {})", saved.getName(),
                    team != null ? team.getName() : "none");
            return saved;
        }
        return projectRepo.findByName(name).orElse(null);
    }

    private Vendor seedVendor(String name, String taxId, String description, String contactName, String contactEmail) {
        return vendorRepository.findByTaxId(taxId).orElseGet(() -> {
            Vendor v = new Vendor();
            v.setVendorName(name);
            v.setTaxId(taxId);
            v.setDescription(description);
            v.setPrimaryContactName(contactName);
            v.setPrimaryContactEmail(contactEmail);
            Vendor saved = vendorRepository.save(v);
            log.info("Seeded vendor: {}", saved.getVendorName());
            return saved;
        });
    }

    private Request seedMockRequest(
            String name,
            String description,
            Priority priority,
            User creator,
            Project project,
            WorkflowDefinition workflowDef,
            WorkflowComponent stepComponent,
            String stepName,
            RequestStatus state,
            User assignee,
            BigDecimal budgetAmount,
            List<RequestItem> items,
            LocalDateTime createdAt) {
        Request request = new Request();
        request.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
        request.setRequestName(name);
        request.setDescription(description);
        request.setPriority(priority);
        request.setUser(creator);
        request.setProject(project);
        request.setTeam(creator != null ? creator.getTeam() : null);
        request.setWorkflowDefinition(workflowDef);

        if (workflowDef != null) {
            WorkflowStep step = workflowStepRepository.findAllByWorkflowDefinition(workflowDef).stream()
                    .filter(s -> {
                        if (stepComponent != null && s.getWorkflowComponent() != stepComponent) {
                            return false;
                        }
                        if (stepName != null) {
                            return stepName.equalsIgnoreCase(s.getName());
                        }
                        return true;
                    })
                    .findFirst()
                    .orElse(null);
            request.setCurrentStep(step);
        }

        request.setState(state);
        if (state == RequestStatus.FINISHED) {
            request.setClosedReason(ClosedReason.COMPLETED);
        }
        request.setAssignee(assignee);

        // Increment project request counter and set request key
        if (project != null) {
            int currentCounter = project.getRequestCounter();
            project.setRequestCounter(currentCounter + 1);
            projectRepo.save(project);
            request.setRequestKey(project.getProjectKey() + "-" + project.getRequestCounter());
        }

        // Initialize and save budget
        InternalBudget budget = InternalBudget.builder()
                .budgetName(name)
                .budgetType(BudgetType.REQUEST)
                .totalAmount(budgetAmount != null ? budgetAmount : BigDecimal.ZERO)
                .actualSpend(BigDecimal.ZERO)
                .committedSpend(BigDecimal.ZERO)
                .safetyBuffer(BigDecimal.ZERO)
                .parentBudget(project != null ? project.getInternalBudget() : null)
                .build();
        InternalBudget savedBudget = internalBudgetRepository.save(budget);
        request.setBudget(savedBudget);

        // Add line items
        if (items != null) {
            for (RequestItem item : items) {
                item.setRequest(request);
                request.getItems().add(item);
            }
        }

        LocalDateTime desiredCreatedAt = request.getCreatedAt();
        Request saved = requestRepository.save(request);

        // @PrePersist overwrites createdAt — fix it to the intended timestamp
        if (desiredCreatedAt != null) {
            entityManager.createNativeQuery(
                    "UPDATE requests SET created_at = ? WHERE request_id = ?")
                    .setParameter(1, desiredCreatedAt)
                    .setParameter(2, saved.getRequestID())
                    .executeUpdate();
            entityManager.refresh(saved);
        }

        return saved;
    }

    // AI-GENERATED
    /**
     * Creates 3-5 vendor quotes for a request, each with line items matching the
     * request items.
     * If selectOne is true, the cheapest quote is marked as selected and the budget
     * committedSpend is cascaded up the budget chain.
     */
    private void seedQuotesForRequest(Request request, List<Vendor> vendors, Random random, boolean selectOne) {
        if (vendors.isEmpty()) {
            log.warn("Cannot create quotes for request {}: no vendors available", request.getRequestKey());
            return;
        }

        List<RequestItem> requestItems = request.getItems();
        if (requestItems.isEmpty()) {
            log.warn("Cannot create quotes for request {}: no line items", request.getRequestKey());
            return;
        }

        int numQuotes = random.nextInt(3, 6); // 3-5 quotes per request
        List<Quote> createdQuotes = new ArrayList<>();
        List<Vendor> shuffledVendors = new ArrayList<>(vendors);
        java.util.Collections.shuffle(shuffledVendors, random);

        for (int q = 0; q < numQuotes && q < shuffledVendors.size(); q++) {
            Vendor vendor = shuffledVendors.get(q);

            // Calculate quote amounts with some realistic variance per vendor
            BigDecimal baseMultiplier = new BigDecimal("0." + (70 + random.nextInt(60))); // 0.70 - 1.29
            BigDecimal shippingCosts = new BigDecimal(random.nextInt(0, 150));

            // Pre-compute line items and total amounts before saving the quote
            List<QuoteLineItem> lineItems = new ArrayList<>();
            BigDecimal totalQuoteBase = BigDecimal.ZERO;

            for (RequestItem reqItem : requestItems) {
                BigDecimal unitPrice = new BigDecimal(random.nextInt(100, 3000))
                        .multiply(baseMultiplier)
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(reqItem.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP);
                totalQuoteBase = totalQuoteBase.add(subtotal);

                QuoteLineItem qli = new QuoteLineItem();
                qli.setProductDescription(reqItem.getDescription() != null
                        ? reqItem.getDescription()
                        : reqItem.getName());
                qli.setQuantity(reqItem.getQuantity());
                qli.setUnitPrice(unitPrice);
                qli.setSubtotal(subtotal);
                qli.setRequestItem(reqItem);
                lineItems.add(qli);
            }

            Quote quote = new Quote();
            quote.setVendorID(vendor);
            quote.setRequest(request);
            quote.setCurrency(Currency.EUR);
            quote.setBaseAmount(totalQuoteBase);
            quote.setShippingCosts(shippingCosts);
            quote.setTotalAmount(totalQuoteBase.add(shippingCosts));
            quote.setShippingTime(random.nextInt(1, 15));
            quote.setSelected(false);

            Quote savedQuote = quoteRepository.save(quote);

            // Now save line items with the saved quote reference
            for (QuoteLineItem qli : lineItems) {
                qli.setQuote(savedQuote);
                quoteLineItemRepository.save(qli);
            }

            createdQuotes.add(savedQuote);
            request.getQuotes().add(savedQuote);
        }

        // Select the cheapest quote if requested
        if (selectOne && !createdQuotes.isEmpty()) {
            Quote selected = createdQuotes.stream()
                    .min(Comparator.comparing(Quote::getTotalAmount))
                    .orElse(createdQuotes.get(0));
            selected.setSelected(true);
            quoteRepository.save(selected);

            // Cascade committedSpend up the budget chain
            BigDecimal committedAmount = selected.getTotalAmount();
            InternalBudget budget = request.getBudget();
            while (budget != null) {
                BigDecimal currentCommitted = budget.getCommittedSpend() != null
                        ? budget.getCommittedSpend()
                        : BigDecimal.ZERO;
                budget.setCommittedSpend(currentCommitted.add(committedAmount));
                internalBudgetRepository.save(budget);
                budget = budget.getParentBudget();
            }
            log.debug("Selected quote {} for request {} (committed: {})",
                    selected.getQuoteID(), request.getRequestKey(), committedAmount);
        }
    }

    // AI-GENERATED
    private Invoice seedPaidInvoice(Request request, Vendor vendor, BigDecimal amount, LocalDate date) {
        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setVendor(vendor);
        invoice.setInvoiceNumber("INV-" + request.getRequestKey());
        invoice.setInvoiceDate(date);
        invoice.setTotalAmount(amount);
        invoice.setCurrency(Currency.EUR);
        invoice.setDueDate(date.plusDays(30));
        invoice.setIsPaid(true);
        invoice.setPaidAmountEur(amount);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        request.setInvoice(savedInvoice);
        requestRepository.save(request);

        // Move from committed to actual spend at every budget level
        InternalBudget budget = request.getBudget();
        while (budget != null) {
            BigDecimal currentCommitted = budget.getCommittedSpend() != null ? budget.getCommittedSpend() : BigDecimal.ZERO;
            BigDecimal currentActual = budget.getActualSpend() != null ? budget.getActualSpend() : BigDecimal.ZERO;
            budget.setCommittedSpend(currentCommitted.subtract(amount));
            budget.setActualSpend(currentActual.add(amount));
            internalBudgetRepository.save(budget);
            budget = budget.getParentBudget();
        }
        return savedInvoice;
    }

    // AI-GENERATED
    /**
     * Creates an unpaid invoice for a request awaiting payment.
     * Does NOT update actualSpend since payment hasn't happened yet.
     */
    private Invoice seedUnpaidInvoice(Request request, Vendor vendor, BigDecimal amount, LocalDate date) {
        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setVendor(vendor);
        invoice.setInvoiceNumber("INV-" + request.getRequestKey());
        invoice.setInvoiceDate(date);
        invoice.setTotalAmount(amount);
        invoice.setCurrency(Currency.EUR);
        invoice.setDueDate(date.plusDays(30));
        invoice.setIsPaid(false);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        request.setInvoice(savedInvoice);
        requestRepository.save(request);

        return savedInvoice;
    }

    // AI-GENERATED
    /**
     * Creates a vendor evaluation for a finished request with realistic random scores.
     */
    private void seedVendorEvaluation(Vendor vendor, Request request, User evaluator, Random random) {
        if (vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(vendor.getId(), request.getRequestID())) {
            return;
        }

        // Calculate gap score based on quote vs invoice
        BigDecimal quoteTotal = request.getSelectedQuoteTotalAmount();
        Invoice invoice = request.getInvoice();
        double gapScore = 10.0;
        if (quoteTotal != null && invoice != null && invoice.getTotalAmount() != null) {
            double quoteVal = quoteTotal.doubleValue();
            double invoiceVal = invoice.getTotalAmount().doubleValue();
            double delta = Math.abs(invoiceVal - quoteVal) / quoteVal;
            gapScore = Math.max(0.0, 10.0 - (delta * 10.0));
            gapScore = Math.round(gapScore * 10.0) / 10.0;
        }

        VendorEvaluation evaluation = new VendorEvaluation();
        evaluation.setVendor(vendor);
        evaluation.setRequest(request);
        evaluation.setEvaluator(evaluator);
        evaluation.setCommunicationScore(random.nextInt(5, 11)); // 5-10
        evaluation.setDeliveryScore(random.nextInt(5, 11));
        evaluation.setQualityScore(random.nextInt(5, 11));
        evaluation.setGapScore(gapScore);

        vendorEvaluationRepository.save(evaluation);
        log.debug("Seeded vendor evaluation for vendor {} on request {}",
                vendor.getVendorName(), request.getRequestKey());
    }

    // AI-GENERATED
    /**
     * Attaches a sample invoice PDF to a finished request's invoice.
     */
    private void seedInvoiceAttachment(Request request, Invoice invoice) {
        try {
            java.io.InputStream pdfStream = getClass().getResourceAsStream("/invoices/sample-invoice.pdf");
            if (pdfStream == null) {
                log.warn("Sample invoice PDF not found on classpath, skipping attachment for request {}",
                        request.getRequestKey());
                return;
            }

            java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads/requisitions");
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            String storedFilename = java.util.UUID.randomUUID().toString() + ".pdf";
            java.nio.file.Path targetLocation = uploadPath.resolve(storedFilename);
            java.nio.file.Files.copy(pdfStream, targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            pdfStream.close();

            long fileSize = java.nio.file.Files.size(targetLocation);

            Attachment attachment = new Attachment();
            attachment.setRequest(request);
            attachment.setInvoice(invoice);
            attachment.setFileName("sample-invoice.pdf");
            attachment.setFileType("application/pdf");
            attachment.setFileSize(fileSize);
            attachment.setStoragePath(targetLocation.toString());
            attachmentRepository.save(attachment);

            log.debug("Seeded invoice PDF attachment for request {}", request.getRequestKey());
        } catch (java.io.IOException e) {
            log.error("Failed to seed invoice attachment for request {}: {}",
                    request.getRequestKey(), e.getMessage());
        }
    }
}
