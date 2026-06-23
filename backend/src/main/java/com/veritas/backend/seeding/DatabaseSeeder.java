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
import com.veritas.backend.requisition.repository.InvoiceRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
                            .totalAmount(new BigDecimal("10000000"))
                            .budgetType(BudgetType.GLOBAL)
                            .safetyBuffer(new BigDecimal("5"))
                            .build();
                    InternalBudget saved = internalBudgetRepository.save(gb);
                    log.info("Seeded global budget: {}", saved.getBudgetName());
                    return saved;
                });

        // 1. Seed Essential Departments
        Department department = seedDepartment("Cloud Platform Engineering", new BigDecimal("900000"), globalBudget);
        Department department2 = seedDepartment("Talent & People Operations", new BigDecimal("800000"), globalBudget);

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
        Project p1 = seedProject("Project Artemis: Cloud Architecture Audit", "artemis", new BigDecimal("150000"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), teamOne);
        Project p2 = seedProject("NextGen ERP Integration", "nextgen-erp", new BigDecimal("275000"),
                LocalDate.of(2026, 3, 15), LocalDate.of(2027, 6, 1), teamOne);

        // 5. Seed Essential Workflow
        if (workflowDefinitionRepository.count() == 0) {
            workflowService.createWorkflow(new WorkflowSaveDto(DatabaseSeederConstants.STANDARD_WORKFLOW, null));
            log.info("Seeded basic workflow definition");
        }

        // 6. Seed Essential Vendors
        seedVendor("Global Tech Solutions", "US-123456789",
                "Leading provider of enterprise hardware and software licenses.", "John Smith",
                "jsmith@globaltech.com");
        seedVendor("Prime Logistics & Services", "EU-987654321", "Global shipping, handling, and logistics partner.",
                "Elena Vance", "evance@primelogistics.com");

        // 7. Seed Initial Finished Request
        if (requestRepository.count() == 0) {
            var workflowDef = workflowDefinitionRepository.findAll().stream().findFirst().orElse(null);

            RequestItem finishedItem = new RequestItem();
            finishedItem.setName("Core Edge Firewall Requisition Licenses");
            finishedItem.setQuantity(1);
            finishedItem.setUnit(RequestItemUnit.PIECES);
            finishedItem.setDescription("High-throughput firewall license pack");

            seedMockRequest(
                    "Core Edge Firewall Requisition",
                    "Acquisition of high-throughput firewall licenses to secure core edge network endpoints and prevent unauthorized traffic.",
                    Priority.MEDIUM,
                    requester,
                    p1 != null ? p1 : p2,
                    workflowDef,
                    WorkflowComponent.END_EVENT,
                    null,
                    RequestStatus.FINISHED,
                    null,
                    new BigDecimal("1500.00"),
                    List.of(finishedItem),
                    null
            );
            log.info("Seeded basic finished request");
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
                BigDecimal budgetAmt = new BigDecimal(faker.number().numberBetween(300000, 800000));
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
                BigDecimal budgetAmt = new BigDecimal(faker.number().numberBetween(50000, 250000));
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

            // Seed Mock Requests in Different States using Standard Workflow
            var standardWorkflowDef = workflowDefinitionRepository.findAll().stream().findFirst().orElse(null);
            User primaryRequester = userRepo.findByEmail("requester@veritas.com").orElse(null);
            User primaryProcurement = userRepo.findByEmail("procurement@veritas.com").orElse(null);
            User primaryFinance = userRepo.findByEmail("finance@veritas.com").orElse(null);
            Project primaryProject = projectRepo.findAll().stream().findFirst().orElse(null);

            if (standardWorkflowDef != null && primaryRequester != null && primaryProject != null) {
                // 1. Draft Request
                RequestItem draftItem = new RequestItem();
                draftItem.setName("Dell XPS 15 Laptop");
                draftItem.setQuantity(1);
                draftItem.setUnit(RequestItemUnit.PIECES);
                draftItem.setDescription("16GB RAM, 512GB SSD");

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
                        List.of(draftItem),
                        null);

                // 2. Active Request - Team Leader Confirmation
                RequestItem requesterReviewItem = new RequestItem();
                requesterReviewItem.setName("AWS Dev Sandbox Credits");
                requesterReviewItem.setQuantity(1);
                requesterReviewItem.setUnit(RequestItemUnit.PIECES);
                requesterReviewItem.setDescription("Monthly dev allowance");

                seedMockRequest(
                        "AWS Multi-Region Infrastructure Sandbox",
                        "Resource credit requisition to facilitate high-availability staging and multi-region failover dry-runs for the microservices platform.",
                        Priority.MEDIUM,
                        primaryRequester,
                        primaryProject,
                        standardWorkflowDef,
                        WorkflowComponent.STEP,
                        "Team Leader Confirmation",
                        RequestStatus.ACTIVE,
                        primaryRequester,
                        new BigDecimal("300.00"),
                        List.of(requesterReviewItem),
                        null);

                // 3. Active Request - Procurement Review
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

                seedMockRequest(
                        "Premium Ergonomic Office Furniture Upgrade",
                        "Procurement of fully adjustable ergonomic task chairs to support long-term physical wellness and platform engineering comfort.",
                        Priority.MEDIUM,
                        primaryRequester,
                        primaryProject,
                        standardWorkflowDef,
                        WorkflowComponent.STEP,
                        "Vendor Quote Selection",
                        RequestStatus.ACTIVE,
                        primaryProcurement,
                        new BigDecimal("1200.00"),
                        List.of(chairsItem, desksItem),
                        null);

                // 4. Active Request - Finance Review
                RequestItem licensesItem = new RequestItem();
                licensesItem.setName("IntelliJ IDEA Ultimate License");
                licensesItem.setQuantity(15);
                licensesItem.setUnit(RequestItemUnit.PIECES);
                licensesItem.setDescription("Annual corporate subscription");

                seedMockRequest(
                        "IntelliJ IDEA Enterprise Renewal",
                        "Annual corporate license renewal of the IntelliJ IDEA developer IDE for the core engineering and applications platform team.",
                        Priority.HIGH,
                        primaryRequester,
                        primaryProject,
                        standardWorkflowDef,
                        WorkflowComponent.STEP,
                        "Standard Finance Review",
                        RequestStatus.ACTIVE,
                        primaryFinance,
                        new BigDecimal("4500.00"),
                        List.of(licensesItem),
                        null);

                // 5. Finished Request
                RequestItem finishedItem = new RequestItem();
                finishedItem.setName("GitKraken Pro License Pack");
                finishedItem.setQuantity(1);
                finishedItem.setUnit(RequestItemUnit.BOXES);
                finishedItem.setDescription("10 licenses annual subscription");

                seedMockRequest(
                        "GitKraken Pro Suite Requisition",
                        "Acquisition of collaborative git visual client licenses to optimize team git operations and enhance code review workflow transparency.",
                        Priority.MEDIUM,
                        primaryRequester,
                        primaryProject,
                        standardWorkflowDef,
                        WorkflowComponent.END_EVENT,
                        null,
                        RequestStatus.FINISHED,
                        null,
                        new BigDecimal("990.00"),
                        List.of(finishedItem),
                        null
                );

                List<Project> allProjects = projectRepo.findAll();
                Vendor vendor = vendorRepository.findAll().stream().findFirst().orElse(null);
                // AI-GENERATED
                for (int i = 0; i < 100; i++) {
                    // Pick a random project to ensure even distribution across projects/departments
                    Project userProject = allProjects.isEmpty() ? primaryProject : allProjects.get(random.nextInt(allProjects.size()));

                    // Select a requester from the team associated with the project, falling back to a random requester
                    User randomRequester = null;
                    if (userProject != null && userProject.getTeam() != null) {
                        randomRequester = userRepo.findAllByTeamTeamId(userProject.getTeam().getTeamId()).stream()
                                .filter(u -> u.getRole() == UserRole.REQUESTER)
                                .findFirst()
                                .orElse(null);
                    }
                    if (randomRequester == null) {
                        randomRequester = seededRequesters.isEmpty() ? primaryRequester : seededRequesters.get(random.nextInt(seededRequesters.size()));
                    }

                    int currentMonth = LocalDate.now().getMonthValue();
                    int currentYear = LocalDate.now().getYear();
                    int month = (i % currentMonth) + 1;
                    int day = random.nextInt(1, 28);
                    BigDecimal amount = new BigDecimal(random.nextInt(5000, 20000));

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
                        }
                    }

                    String reqName = faker.commerce().productName() + " Acquisition";
                    String reqDesc = "Dynamic procurement request for " + reqName.toLowerCase() + " to support business operations.";

                    RequestItem paidItem = new RequestItem();
                    paidItem.setName(reqName);
                    paidItem.setQuantity(random.nextInt(1, 5));
                    paidItem.setUnit(RequestItemUnit.PIECES);
                    paidItem.setDescription("Rich seeded mock item");

                    Request mockReq = seedMockRequest(
                            reqName,
                            reqDesc,
                            Priority.values()[random.nextInt(Priority.values().length)],
                            randomRequester,
                            userProject,
                            standardWorkflowDef,
                            WorkflowComponent.END_EVENT,
                            null,
                            RequestStatus.FINISHED,
                            null,
                            amount,
                            List.of(paidItem),
                            LocalDateTime.of(currentYear, month, day, 10, 0)
                    );

                    if (vendor != null) {
                        seedPaidInvoice(mockReq, vendor, amount, LocalDate.of(currentYear, month, day).plusDays(5));
                    }
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
        InternalBudget budget = new InternalBudget();
        budget.setBudgetName(name);
        budget.setBudgetType(BudgetType.REQUEST);
        budget.setTotalAmount(budgetAmount != null ? budgetAmount : BigDecimal.ZERO);
        if (project != null && project.getInternalBudget() != null) {
            budget.setParentBudget(project.getInternalBudget());
        }
        InternalBudget savedBudget = internalBudgetRepository.save(budget);
        request.setBudget(savedBudget);

        // Add line items
        if (items != null) {
            for (RequestItem item : items) {
                item.setRequest(request);
                request.getItems().add(item);
            }
        }

        return requestRepository.save(request);
    }

    // AI-GENERATED
    private void seedPaidInvoice(Request request, Vendor vendor, BigDecimal amount, LocalDate date) {
        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setVendor(vendor);
        invoice.setInvoiceNumber("INV-" + request.getRequestKey());
        invoice.setInvoiceDate(date);
        invoice.setTotalAmount(amount);
        invoice.setCurrency(com.veritas.backend.integrations.currency.entity.Currency.EUR);
        invoice.setDueDate(date.plusDays(30));
        invoice.setIsPaid(true);
        invoice.setPaidAmountEur(amount);
        invoiceRepository.save(invoice);

        request.setInvoice(invoice);
        requestRepository.save(request);

        // Update budgets actual spend
        InternalBudget budget = request.getBudget();
        while (budget != null) {
            BigDecimal currentSpent = budget.getActualSpend() != null ? budget.getActualSpend() : BigDecimal.ZERO;
            budget.setActualSpend(currentSpent.add(amount));
            internalBudgetRepository.save(budget);
            budget = budget.getParentBudget();
        }
    }
}
