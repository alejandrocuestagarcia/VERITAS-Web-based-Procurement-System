package com.veritas.backend.seeding;

import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.entity.*;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.QuoteLineItem;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.entity.VendorEvaluation;
import com.veritas.backend.vendor.repository.QuoteLineItemRepository;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.vendor.repository.VendorEvaluationRepository;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.service.WorkflowService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

// AI-GENERATED
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

    // ── shared context bag ──────────────────────
    private record SeedContext(
            InternalBudget globalBudget,
            Department cloudPlatformEng, Department talentPeopleOps,
            Team corePlatformTeam, Team talentAcquisitionTeam,
            User requester, User hrRequester, User financeOfficer, User procurementOfficer,
            Project artemis, Project nextgenErp,
            WorkflowDefinition workflowDef,
            List<Vendor> allVendors,
            List<Department> allDepartments, List<Team> allTeams,
            List<User> requesters, List<Project> allProjects,
            Random rng
    ) {}

    // ══════════════════════════════════════════════
    // Orchestrator
    // ══════════════════════════════════════════════
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting seed data initialization...");

        InternalBudget globalBudget = seedGlobalBudget();

        Department cloudPlatformEng = seedDepartment("Cloud Platform Engineering", "130000", globalBudget);
        Department talentPeopleOps  = seedDepartment("Talent & People Operations",   "95000",  globalBudget);

        Team corePlatformTeam      = seedTeam("Core Platform Engineering", cloudPlatformEng,
                "Core software platform development, devops, and reliability engineering team");
        Team talentAcquisitionTeam = seedTeam("Talent Acquisition & Ops", talentPeopleOps,
                "Enterprise talent acquisition and people operations core team");

        seedEssentialUsers(corePlatformTeam, talentAcquisitionTeam, cloudPlatformEng);

        User requester       = userRepo.findByEmail("requester@veritas.com").orElseThrow();
        User hrRequester     = userRepo.findByEmail("hr_requester@veritas.com").orElseThrow();
        User financeOfficer  = userRepo.findByEmail("finance@veritas.com").orElseThrow();
        User procurementOfficer = userRepo.findByEmail("procurement@veritas.com").orElseThrow();

        Project artemis   = seedProject("Project Artemis: Cloud Architecture Audit", "artemis",
                "60000", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), corePlatformTeam);
        Project nextgenErp = seedProject("NextGen ERP Integration", "nextgen-erp",
                "65000", LocalDate.of(2026, 3, 15), LocalDate.of(2027, 6, 1), corePlatformTeam);

        seedWorkflows();

        seedVendor("Global Tech Solutions",      "US-123456789",
                "Leading provider of enterprise hardware and software licenses.",
                "John Smith",  "jsmith@globaltech.com");
        seedVendor("Prime Logistics & Services", "EU-987654321",
                "Global shipping, handling, and logistics partner.",
                "Elena Vance", "evance@primelogistics.com");

        // Extra vendors so crafted requests have more than 2 to pick from
        String[][] extraVendors = {
                {"CloudNative Systems Inc.",  "US-234567890", "Cloud infrastructure and platform solutions.",       "Alice Chen",   "achen@cloudnative.com"},
                {"Atlassian Platinum Partner", "US-345678901", "Official Atlassian enterprise reseller and consultancy.", "Bob Marley",  "bmarley@atlassian-partner.com"},
                {"CyberSec Solutions LLC",     "US-456789012", "Enterprise cybersecurity and compliance solutions.",   "Carol Davis",  "cdavis@cybersec.com"},
                {"DataVault Storage Co.",      "US-567890123", "Enterprise data storage and backup infrastructure.",   "Dan Evans",    "devans@datavault.com"},
                {"NetOps Communications",      "US-678901234", "Enterprise networking and telecommunications provider.", "Eve Foster", "efoster@netops.com"},
                {"SoftServe Development Ltd.", "US-789012345", "Custom software development and IT consulting services.", "Frank Green", "fgreen@softserve.com"},
        };
        for (String[] v : extraVendors) seedVendor(v[0], v[1], v[2], v[3], v[4]);

        WorkflowDefinition workflowDef = workflowDefinitionRepository.findAll().stream().findFirst().orElse(null);
        List<Vendor> allVendors = vendorRepository.findAll();
        Random craftedRng = new Random(42);

        seedCraftedRequests(requester, financeOfficer, procurementOfficer,
                artemis, workflowDef, allVendors, craftedRng);

        if (userRepo.count() <= 5) {
            var ctx = buildSeedContext(globalBudget, cloudPlatformEng, talentPeopleOps,
                    corePlatformTeam, talentAcquisitionTeam,
                    requester, hrRequester, financeOfficer, procurementOfficer,
                    artemis, nextgenErp, workflowDef, allVendors, craftedRng);
            seedDynamicData(ctx);
        }

        log.info("Seed data initialization complete.");
    }

    // ══════════════════════════════════════════════
    // 0. Global budget
    // ══════════════════════════════════════════════
    private InternalBudget seedGlobalBudget() {
        return internalBudgetRepository.findAll().stream()
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
                    log.info("Seeded global budget: {}", internalBudgetRepository.save(gb).getBudgetName());
                    return gb;
                });
    }

    // ══════════════════════════════════════════════
    // 1–3  Departments, teams, users
    // ══════════════════════════════════════════════
    private void seedEssentialUsers(Team corePlatformTeam, Team talentAcquisitionTeam, Department cloudPlatformEng) {
        seedUser("Veritas Admin",       "admin@veritas.com",       UserRole.ADMINISTRATOR,       null, null);
        seedUser("Veritas Finance",     "finance@veritas.com",     UserRole.FINANCE_OFFICER,     null, null);
        seedUser("Veritas Procurement", "procurement@veritas.com", UserRole.PROCUREMENT_OFFICER, cloudPlatformEng, null);

        User requester = seedUser("Veritas Requester", "requester@veritas.com",
                UserRole.REQUESTER, null, corePlatformTeam);
        if (corePlatformTeam.getLeader() == null) { corePlatformTeam.setLeader(requester); teamRepo.save(corePlatformTeam); }

        User hrRequester = seedUser("Veritas HR Requester", "hr_requester@veritas.com",
                UserRole.REQUESTER, null, talentAcquisitionTeam);
        if (talentAcquisitionTeam.getLeader() == null) { talentAcquisitionTeam.setLeader(hrRequester); teamRepo.save(talentAcquisitionTeam); }
    }

    // ══════════════════════════════════════════════
    // 5  Workflows (standard + simple)
    // ══════════════════════════════════════════════
    private void seedWorkflows() {
        if (workflowDefinitionRepository.count() == 0) {
            workflowService.createWorkflow(new WorkflowSaveDto(DatabaseSeederConstants.STANDARD_WORKFLOW, null));
            workflowService.createWorkflow(new WorkflowSaveDto(DatabaseSeederConstants.SIMPLE_WORKFLOW, null));
            log.info("Seeded standard and simple workflow definitions");
        }
    }

    // ══════════════════════════════════════════════
    // 7  Crafted requests
    // ══════════════════════════════════════════════
    private void seedCraftedRequests(User requester, User financeOfficer, User procurementOfficer,
                                      Project project, WorkflowDefinition workflowDef, List<Vendor> vendors,
                                      Random rng) {
        if (workflowDef == null || requester == null || project == null) return;

        // 7a  Draft
        item("Core Edge Firewall Requisition Licenses", 1, RequestItemUnit.PIECES,
                "High-throughput firewall license pack");
        seedMockRequest("Core Edge Firewall Requisition",
                "Acquisition of high-throughput firewall licenses to secure core edge network endpoints.",
                Priority.MEDIUM, requester, project, workflowDef,
                WorkflowComponent.START_EVENT, null, RequestStatus.DRAFT, null, "8000.00", null);

        // 7b  Team Leader Confirmation
        item("AWS Dev Sandbox Credits", 1, RequestItemUnit.PIECES, "Monthly dev allowance");
        seedMockRequest("AWS Multi-Region Infrastructure Sandbox",
                "Resource credit requisition for high-availability staging and multi-region failover dry-runs.",
                Priority.MEDIUM, requester, project, workflowDef,
                WorkflowComponent.STEP, "Team Leader Confirmation", RequestStatus.ACTIVE, requester, "2000.00", null);

        // 7c  Vendor Quote Selection (quotes, none selected)
        item("Ergonomic Chairs", 5, RequestItemUnit.PIECES, "Mesh backing, fully adjustable");
        item("Standing Desk Converters", 3, RequestItemUnit.PIECES, "Dual monitor support");
        Request vendorSelect = seedMockRequest("Premium Ergonomic Office Furniture Upgrade",
                "Procurement of fully adjustable ergonomic task chairs for platform engineering comfort.",
                Priority.MEDIUM, requester, project, workflowDef,
                WorkflowComponent.STEP, "Vendor Quote Selection", RequestStatus.ACTIVE, procurementOfficer,
                "10000.00", null);
        seedQuotesForRequest(vendorSelect, vendors, rng, false);

        // 7d  Standard Finance Review (selected quote + unpaid invoice + evaluation)
        item("IntelliJ IDEA Ultimate License", 15, RequestItemUnit.PIECES, "Annual corporate subscription");
        Request financeReview = seedMockRequest("IntelliJ IDEA Enterprise Renewal",
                "Annual corporate license renewal of IntelliJ IDEA for the core engineering team.",
                Priority.HIGH, requester, project, workflowDef,
                WorkflowComponent.STEP, "Standard Finance Review", RequestStatus.ACTIVE, financeOfficer,
                "24000.00", null);
        seedQuotesForRequest(financeReview, vendors, rng, true);
        attachUnpaidInvoice(financeReview);
        evaluateVendor(financeReview, procurementOfficer, rng);

        // 7e  Awaiting payment (finished workflow, unpaid invoice + evaluation)
        item("GitKraken Pro License Pack", 1, RequestItemUnit.BOXES, "10 licenses annual subscription");
        Request awaiting = seedMockRequest("GitKraken Pro Suite Requisition",
                "Acquisition of collaborative git visual client licenses to optimize team git operations.",
                Priority.MEDIUM, requester, project, workflowDef,
                WorkflowComponent.END_EVENT, null, RequestStatus.FINISHED, null, "8000.00", null);
        seedQuotesForRequest(awaiting, vendors, rng, true);
        attachUnpaidInvoice(awaiting);
        evaluateVendor(awaiting, procurementOfficer, rng);
    }

    // ══════════════════════════════════════════════
    // 8  Dynamic Faker data
    // ══════════════════════════════════════════════
    private SeedContext buildSeedContext(InternalBudget globalBudget,
                                          Department d1, Department d2,
                                          Team t1, Team t2,
                                          User u1, User u2, User u3, User u4,
                                          Project p1, Project p2,
                                          WorkflowDefinition wf, List<Vendor> vendors, Random rng) {
        return new SeedContext(globalBudget, d1, d2, t1, t2, u1, u2, u3, u4, p1, p2, wf, vendors,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), rng);
    }

    private void seedDynamicData(SeedContext ctx) {
        log.info("Initializing rich dynamic mock data seeding with Faker...");
        Random rng = new Random(42);
        Faker faker = new Faker(rng);

        seedExtraDepartments(ctx, faker);
        seedExtraTeams(ctx, rng);
        seedDynamicUsers(ctx, faker, rng);
        assignTeamLeaders(ctx);
        seedExtraProjects(ctx, faker, rng);

        seedCraftedRequestsForOtherProjects(ctx);
        seedBulkDynamicRequests(ctx, faker, new Random(42));

        log.info("Rich dynamic mock data seeding complete.");
    }

    private void seedExtraDepartments(SeedContext ctx, Faker faker) {
        for (String name : new String[]{"Digital Growth & Marketing", "Global Logistics & Ops"}) {
            BigDecimal amt = new BigDecimal(faker.number().numberBetween(55000, 80000));
            Department d = seedDepartment(name, amt.toString(), ctx.globalBudget());
            if (d != null) ctx.allDepartments().add(d);
        }
    }

    private void seedExtraTeams(SeedContext ctx, Random rng) {
        String[][] data = {
                {"Digital Growth Marketing",  "Digital Growth & Marketing",
                        "Handles corporate marketing campaigns, branding, and social media presence"},
                {"Strategic Enterprise Sales","Digital Growth & Marketing",
                        "Handles high-value enterprise accounts and strategic partnerships"},
                {"Global Customer Success",   "Global Logistics & Ops",
                        "First line of customer care, strategic onboarding, and account retention"},
                {"Cloud Platform Operations", "Cloud Platform Engineering",
                        "Manages staging environments, server scaling, and deployment pipelines"}
        };
        for (String[] td : data) {
            Department dept = departmentRepository.findByName(td[1])
                    .orElse(ctx.allDepartments().get(rng.nextInt(ctx.allDepartments().size())));
            Team t = seedTeam(td[0], dept, td[2]);
            if (t != null) ctx.allTeams().add(t);
        }
    }

    private void seedDynamicUsers(SeedContext ctx, Faker faker, Random rng) {
        for (int i = 1; i <= 25; i++) {
            String name  = faker.name().fullName();
            String email = name.toLowerCase().replaceAll("[^a-z]", "") + "." + i + "@veritas.com";
            UserRole role;
            Team team = null;
            Department dept = null;
            if (i <= 15)      { role = UserRole.REQUESTER;          team = ctx.allTeams().get(rng.nextInt(ctx.allTeams().size())); }
            else if (i <= 20) { role = UserRole.PROCUREMENT_OFFICER; dept = ctx.allDepartments().get(rng.nextInt(ctx.allDepartments().size())); }
            else if (i <= 23) { role = UserRole.FINANCE_OFFICER; }
            else              { role = UserRole.ADMINISTRATOR; }

            User u = seedUser(name, email, role, dept, team);
            if (u != null && role == UserRole.REQUESTER) ctx.requesters().add(u);
        }
    }

    private void assignTeamLeaders(SeedContext ctx) {
        for (Team t : ctx.allTeams()) {
            Team current = teamRepo.findById(t.getTeamId()).orElse(t);
            if (current.getLeader() == null) {
                List<User> members = userRepo.findAllByTeamTeamId(current.getTeamId()).stream()
                        .filter(u -> u.getRole() == UserRole.REQUESTER).toList();
                if (!members.isEmpty()) { current.setLeader(members.getFirst()); teamRepo.save(current); }
            }
        }
    }

    private void seedExtraProjects(SeedContext ctx, Faker faker, Random rng) {
        String[] names = {"Aether Multi-Region Cloud Migration", "Project Oasis: AI-Driven CRM Portal",
                "Titan: Real-Time Logistics Optimization", "Project Velocity: Strategic Sales Booster",
                "Apex Omnichannel Support Hub"};
        for (int i = 0; i < names.length; i++) {
            String key = names[i].toLowerCase().replaceAll("[^a-z]", "") + "-" + i;
            Team team = ctx.allTeams().get(i % ctx.allTeams().size());
            BigDecimal budget = new BigDecimal(faker.number().numberBetween(70000, 110000));
            seedProject(names[i], key, budget.toString(),
                    LocalDate.now().minusMonths(rng.nextInt(1, 6)),
                    LocalDate.now().plusMonths(rng.nextInt(6, 18)), team);
        }
    }

    private void seedExtraVendors(Faker faker) {
        for (int i = 0; i < 6; i++)
            seedVendor(faker.company().name(), "US-" + faker.number().digits(9),
                    faker.company().catchPhrase(), faker.name().fullName(), faker.internet().emailAddress());
    }

    private void seedCraftedRequestsForOtherProjects(SeedContext ctx) {
        if (ctx.workflowDef() == null || ctx.requester() == null) return;

        Project other = projectRepo.findAll().stream()
                .filter(p -> !"artemis".equals(p.getProjectKey()) && !"nextgen-erp".equals(p.getProjectKey()))
                .findFirst().orElse(ctx.nextgenErp());

        // Extra draft
        item("Dell XPS 15 Laptop", 1, RequestItemUnit.PIECES, "16GB RAM, 512GB SSD");
        seedMockRequest("Developer Workstation Procurement",
                "High-performance development workstations for incoming senior engineering personnel.",
                Priority.LOW, ctx.requester(), other, ctx.workflowDef(),
                WorkflowComponent.START_EVENT, null, RequestStatus.DRAFT, null, "0", null);

        // ── Talent & People Operations requests ──
        // Find a project under Talent & People Operations, or create one if none exists
        Project hrProject = projectRepo.findAll().stream()
                .filter(p -> p.getTeam() != null && p.getTeam().getDepartment() != null
                        && "Talent & People Operations".equals(p.getTeam().getDepartment().getName()))
                .findFirst().orElseGet(() -> seedProject("HR Digital Transformation", "hr-digital",
                        "50000", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(10),
                        ctx.talentAcquisitionTeam()));

        User hrRequester = userRepo.findByEmail("hr_requester@veritas.com").orElse(ctx.requester());

        // Standard Finance Review for HR project
        item("LinkedIn Recruiter Corporate License", 5, RequestItemUnit.PIECES,
                "Annual corporate recruiting platform license");
        Request hrFinance = seedMockRequest("LinkedIn Recruiter Enterprise Renewal",
                "Annual renewal of LinkedIn Recruiter corporate licenses for the talent acquisition team.",
                Priority.MEDIUM, hrRequester, hrProject, ctx.workflowDef(),
                WorkflowComponent.STEP, "Standard Finance Review", RequestStatus.ACTIVE, ctx.financeOfficer(),
                "12000.00", null);
        seedQuotesForRequest(hrFinance, ctx.allVendors(), ctx.rng(), true);
        attachUnpaidInvoice(hrFinance);
        evaluateVendor(hrFinance, ctx.procurementOfficer(), ctx.rng());

        // Awaiting payment for HR project
        item("BambooHR Enterprise Plan", 1, RequestItemUnit.PIECES,
                "Annual HRIS platform subscription for 200 employees");
        Request hrAwaiting = seedMockRequest("BambooHR Platform Subscription Renewal",
                "Annual renewal of BambooHR enterprise plan covering HR, payroll, and performance management.",
                Priority.HIGH, hrRequester, hrProject, ctx.workflowDef(),
                WorkflowComponent.END_EVENT, null, RequestStatus.FINISHED, null, "10000.00", null);
        seedQuotesForRequest(hrAwaiting, ctx.allVendors(), ctx.rng(), true);
        attachUnpaidInvoice(hrAwaiting);
        evaluateVendor(hrAwaiting, ctx.procurementOfficer(), ctx.rng());
    }

    private void seedBulkDynamicRequests(SeedContext ctx, Faker faker, Random rng) {
        List<Project> projects = projectRepo.findAll();

        for (int i = 0; i < 100; i++) {
            Project project = projects.get(rng.nextInt(projects.size()));
            User requester = findRequester(project, ctx.requesters(), ctx.requester(), rng);

            int month = (i % LocalDate.now().getMonthValue()) + 1;
            int year  = LocalDate.now().getYear();
            int day   = rng.nextInt(1, 28);
            BigDecimal amount = new BigDecimal(rng.nextInt(2000, 12000));

            if (!budgetAllows(project, amount)) continue;

            Request req = seedMockRequest(
                    faker.commerce().productName() + " Acquisition",
                    "Dynamic procurement request for business operations.",
                    Priority.values()[rng.nextInt(Priority.values().length)],
                    requester, project, ctx.workflowDef(),
                    WorkflowComponent.END_EVENT, null, RequestStatus.FINISHED, null,
                    amount.toString(),
                    generateItems(faker, rng),
                    LocalDateTime.of(year, month, day, 10, 0));

            if (!ctx.allVendors().isEmpty())
                seedQuotesForRequest(req, ctx.allVendors(), rng, true);

            Quote selected = req.getSelectedQuote();
            if (selected != null) {
                seedPaidInvoice(req, selected.getVendorID(),
                        selected.getTotalAmount(), LocalDate.of(year, month, day).plusDays(5));
                seedInvoiceAttachment(req, req.getInvoice());

                User evaluator = userRepo.findAll().stream()
                        .filter(u -> u.getRole() == UserRole.PROCUREMENT_OFFICER).findAny().orElse(null);
                if (evaluator != null) seedVendorEvaluation(selected.getVendorID(), req, evaluator, rng);
            }
        }
    }

    private User findRequester(Project project, List<User> requesters, User fallback, Random rng) {
        if (project.getTeam() != null) {
            return userRepo.findAllByTeamTeamId(project.getTeam().getTeamId()).stream()
                    .filter(u -> u.getRole() == UserRole.REQUESTER).findFirst().orElse(null);
        }
        return requesters.isEmpty() ? fallback : requesters.get(rng.nextInt(requesters.size()));
    }

    private List<RequestItem> generateItems(Faker faker, Random rng) {
        List<RequestItem> items = new ArrayList<>();
        for (int j = 0, n = rng.nextInt(1, 4); j < n; j++) {
            RequestItem item = new RequestItem();
            item.setName(faker.commerce().productName());
            item.setQuantity(rng.nextInt(1, 10));
            item.setUnit(RequestItemUnit.values()[rng.nextInt(RequestItemUnit.values().length)]);
            item.setDescription(faker.commerce().material() + " - " + faker.lorem().sentence());
            items.add(item);
        }
        return items;
    }

    private boolean budgetAllows(Project project, BigDecimal amount) {
        if (project == null || project.getInternalBudget() == null) return true;
        InternalBudget pb = internalBudgetRepository.findById(project.getInternalBudget().getId()).orElse(null);
        if (pb == null) return true;

        BigDecimal total = nvl(pb.getTotalAmount());
        if (nvl(pb.getActualSpend()).add(nvl(pb.getCommittedSpend())).add(amount)
                .compareTo(total.multiply(new BigDecimal("0.60"))) > 0) return false;

        InternalBudget db = pb.getParentBudget();
        if (db != null) {
            db = internalBudgetRepository.findById(db.getId()).orElse(null);
            if (db != null && nvl(db.getActualSpend()).add(nvl(db.getCommittedSpend())).add(amount)
                    .compareTo(nvl(db.getTotalAmount()).multiply(new BigDecimal("0.90"))) > 0) return false;
        }
        return true;
    }

    // ──────────────────────────────────────────────
    // Entity seeders
    // ──────────────────────────────────────────────
    private Department seedDepartment(String name, String budgetAmount, InternalBudget globalBudget) {
        BigDecimal amt = new BigDecimal(budgetAmount);
        return departmentRepository.findByName(name).map(dept -> {
            if (dept.getInternalBudget() != null && dept.getInternalBudget().getParentBudget() == null && globalBudget != null) {
                dept.getInternalBudget().setParentBudget(globalBudget);
                internalBudgetRepository.save(dept.getInternalBudget());
            }
            return dept;
        }).orElseGet(() -> {
            Department dept = new Department();
            dept.setName(name);
            dept.setInternalBudget(budget(name, amt, BudgetType.DEPARTMENT, globalBudget));
            log.info("Seeded department: {}", departmentRepository.save(dept).getName());
            return dept;
        });
    }

    private Team seedTeam(String name, Department department, String description) {
        return teamRepo.findByName(name).orElseGet(() -> {
            Team t = new Team();
            t.setName(name); t.setDepartment(department); t.setDescription(description); t.setIsActive(true);
            log.info("Seeded team: {}", teamRepo.save(t).getName());
            return t;
        });
    }

    private User seedUser(String name, String email, UserRole role, Department dept, Team team) {
        return userRepo.findByEmail(email).orElseGet(() -> {
            User u = User.builder().name(name).email(email).passwordHash(encoder.encode("password123"))
                    .role(role).department(dept).team(team).isActive(true).requiresPasswordChange(false).build();
            User saved = userRepo.save(u);
            log.info("Seeded user: {} (role={}, dept={})", saved.getEmail(), saved.getRole(),
                    saved.getDepartment() != null ? saved.getDepartment().getName() : "none");
            return saved;
        });
    }

    private Project seedProject(String name, String key, String budgetAmount,
                                 LocalDate start, LocalDate end, Team team) {
        if (projectRepo.existsByNameOrProjectKey(name, key)) return projectRepo.findByName(name).orElse(null);
        InternalBudget parent = (team != null && team.getDepartment() != null) ? team.getDepartment().getInternalBudget() : null;
        Project p = Project.builder().name(name).projectKey(key)
                .internalBudget(budget(name, new BigDecimal(budgetAmount), BudgetType.PROJECT, parent))
                .startDate(start).endDate(end).team(team).build();
        log.info("Seeded project: {} (team {})", projectRepo.save(p).getName(), team != null ? team.getName() : "none");
        return p;
    }

    private Vendor seedVendor(String name, String taxId, String desc, String contact, String email) {
        return vendorRepository.findByTaxId(taxId).orElseGet(() -> {
            Vendor v = new Vendor();
            v.setVendorName(name); v.setTaxId(taxId); v.setDescription(desc);
            v.setPrimaryContactName(contact); v.setPrimaryContactEmail(email);
            log.info("Seeded vendor: {}", vendorRepository.save(v).getVendorName());
            return v;
        });
    }

    // ──────────────────────────────────────────────
    // Request seeder
    // ──────────────────────────────────────────────
    private final List<RequestItem> pendingItems = new ArrayList<>();

    private void item(String name, int qty, RequestItemUnit unit, String desc) {
        RequestItem ri = new RequestItem();
        ri.setName(name); ri.setQuantity(qty); ri.setUnit(unit); ri.setDescription(desc);
        pendingItems.add(ri);
    }

    private Request seedMockRequest(String name, String desc, Priority priority,
                                     User creator, Project project, WorkflowDefinition workflowDef,
                                     WorkflowComponent stepComponent, String stepName,
                                     RequestStatus state, User assignee,
                                     String budgetAmount, LocalDateTime createdAt) {
        return seedMockRequest(name, desc, priority, creator, project, workflowDef,
                stepComponent, stepName, state, assignee, budgetAmount, null, createdAt);
    }

    private Request seedMockRequest(String name, String desc, Priority priority,
                                     User creator, Project project, WorkflowDefinition workflowDef,
                                     WorkflowComponent stepComponent, String stepName,
                                     RequestStatus state, User assignee,
                                     String budgetAmount, List<RequestItem> extraItems,
                                     LocalDateTime createdAt) {

        Request request = new Request();
        request.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
        request.setRequestName(name);
        request.setDescription(desc);
        request.setPriority(priority);
        request.setUser(creator);
        request.setProject(project);
        request.setTeam(creator != null ? creator.getTeam() : null);
        request.setWorkflowDefinition(workflowDef);
        request.setCurrentStep(findStep(workflowDef, stepComponent, stepName));
        request.setState(state);
        if (state == RequestStatus.FINISHED) request.setClosedReason(ClosedReason.COMPLETED);
        request.setAssignee(assignee);

        if (project != null) {
            project.setRequestCounter(project.getRequestCounter() + 1);
            projectRepo.save(project);
            request.setRequestKey(project.getProjectKey() + "-" + project.getRequestCounter());
        }

        request.setBudget(internalBudgetRepository.save(
                budget(name, new BigDecimal(budgetAmount), BudgetType.REQUEST,
                        project != null ? project.getInternalBudget() : null)));

        for (RequestItem ri : pendingItems) { ri.setRequest(request); request.getItems().add(ri); }
        pendingItems.clear();
        if (extraItems != null) for (RequestItem ri : extraItems) { ri.setRequest(request); request.getItems().add(ri); }

        LocalDateTime desired = request.getCreatedAt();
        Request saved = requestRepository.save(request);
        if (desired != null) {
            entityManager.createNativeQuery("UPDATE requests SET created_at = ? WHERE request_id = ?")
                    .setParameter(1, desired).setParameter(2, saved.getRequestID()).executeUpdate();
            entityManager.refresh(saved);
        }
        return saved;
    }

    private WorkflowStep findStep(WorkflowDefinition wf, WorkflowComponent comp, String stepName) {
        if (wf == null) return null;
        return workflowStepRepository.findAllByWorkflowDefinition(wf).stream()
                .filter(s -> (comp == null || s.getWorkflowComponent() == comp)
                        && (stepName == null || stepName.equalsIgnoreCase(s.getName())))
                .findFirst().orElse(null);
    }

    // ──────────────────────────────────────────────
    // Quotes
    // ──────────────────────────────────────────────
    private void seedQuotesForRequest(Request request, List<Vendor> vendors, Random rng, boolean selectOne) {
        if (vendors.isEmpty() || request.getItems().isEmpty()) return;

        int n = rng.nextInt(3, 6);
        List<Quote> created = new ArrayList<>();
        List<Vendor> shuffled = new ArrayList<>(vendors);
        Collections.shuffle(shuffled, rng);

        for (int q = 0; q < n && q < shuffled.size(); q++) {
            Quote quote = buildQuote(request, shuffled.get(q), rng);
            created.add(quote);
            request.getQuotes().add(quote);
        }

        if (selectOne && !created.isEmpty()) {
            // Pick from top half to spread vendor evaluations instead of always cheapest
            created.sort(Comparator.comparing(Quote::getTotalAmount));
            int topN = Math.max(1, created.size() / 2);
            Quote selected = created.get(rng.nextInt(Math.min(topN, created.size())));
            selected.setSelected(true);
            quoteRepository.save(selected);
            cascadeCommitted(request.getBudget(), selected.getTotalAmount());
        }
    }

    private Quote buildQuote(Request request, Vendor vendor, Random rng) {
        BigDecimal mult = new BigDecimal("0." + (70 + rng.nextInt(60)));
        BigDecimal ship = new BigDecimal(rng.nextInt(0, 150));
        BigDecimal base = BigDecimal.ZERO;
        List<QuoteLineItem> lines = new ArrayList<>();

        for (RequestItem ri : request.getItems()) {
            BigDecimal unit = new BigDecimal(rng.nextInt(100, 3000)).multiply(mult).setScale(2, RoundingMode.HALF_UP);
            BigDecimal sub  = unit.multiply(BigDecimal.valueOf(ri.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            base = base.add(sub);
            QuoteLineItem qli = new QuoteLineItem();
            qli.setProductDescription(ri.getDescription() != null ? ri.getDescription() : ri.getName());
            qli.setQuantity(ri.getQuantity()); qli.setUnitPrice(unit); qli.setSubtotal(sub); qli.setRequestItem(ri);
            lines.add(qli);
        }

        Quote quote = new Quote();
        quote.setVendorID(vendor); quote.setRequest(request); quote.setCurrency(Currency.EUR);
        quote.setBaseAmount(base); quote.setShippingCosts(ship);
        quote.setTotalAmount(base.add(ship)); quote.setShippingTime(rng.nextInt(1, 15));
        Quote saved = quoteRepository.save(quote);
        for (QuoteLineItem qli : lines) { qli.setQuote(saved); quoteLineItemRepository.save(qli); }
        return saved;
    }

    private void cascadeCommitted(InternalBudget budget, BigDecimal amount) {
        while (budget != null) {
            budget.setCommittedSpend(nvl(budget.getCommittedSpend()).add(amount));
            internalBudgetRepository.save(budget);
            budget = budget.getParentBudget();
        }
    }

    // ──────────────────────────────────────────────
    // Invoices
    // ──────────────────────────────────────────────
    private Invoice seedPaidInvoice(Request request, Vendor vendor, BigDecimal amount, LocalDate date) {
        Invoice inv = buildInvoice(request, vendor, amount, date, true);
        request.setInvoice(inv);
        requestRepository.save(request);
        convertCommittedToActual(request.getBudget(), amount);
        return inv;
    }

    private Invoice seedUnpaidInvoice(Request request, Vendor vendor, BigDecimal amount, LocalDate date) {
        Invoice inv = buildInvoice(request, vendor, amount, date, false);
        request.setInvoice(inv);
        requestRepository.save(request);
        return inv;
    }

    private Invoice buildInvoice(Request request, Vendor vendor, BigDecimal amount, LocalDate date, boolean paid) {
        Invoice inv = new Invoice();
        inv.setRequest(request); inv.setVendor(vendor);
        inv.setInvoiceNumber("INV-" + request.getRequestKey());
        inv.setInvoiceDate(date); inv.setTotalAmount(amount);
        inv.setCurrency(Currency.EUR); inv.setDueDate(date.plusDays(30));
        inv.setIsPaid(paid);
        if (paid) inv.setPaidAmountEur(amount);
        return invoiceRepository.save(inv);
    }

    private void convertCommittedToActual(InternalBudget budget, BigDecimal amount) {
        while (budget != null) {
            budget.setCommittedSpend(nvl(budget.getCommittedSpend()).subtract(amount));
            budget.setActualSpend(nvl(budget.getActualSpend()).add(amount));
            internalBudgetRepository.save(budget);
            budget = budget.getParentBudget();
        }
    }

    private void attachUnpaidInvoice(Request request) {
        Quote selected = request.getSelectedQuote();
        if (selected != null) {
            seedUnpaidInvoice(request, selected.getVendorID(),
                    selected.getTotalAmount(), LocalDate.now().minusDays(3));
            seedInvoiceAttachment(request, request.getInvoice());
        }
    }

    // ──────────────────────────────────────────────
    // Vendor evaluation
    // ──────────────────────────────────────────────
    private void evaluateVendor(Request request, User evaluator, Random rng) {
        Quote selected = request.getSelectedQuote();
        if (selected != null) seedVendorEvaluation(selected.getVendorID(), request, evaluator, rng);
    }

    private void seedVendorEvaluation(Vendor vendor, Request request, User evaluator, Random rng) {
        if (vendorEvaluationRepository.existsByVendorIdAndRequestRequestID(vendor.getId(), request.getRequestID()))
            return;

        double gap = Math.round((1.0 + rng.nextDouble() * 9.0) * 10.0) / 10.0;

        VendorEvaluation eval = new VendorEvaluation();
        eval.setVendor(vendor); eval.setRequest(request); eval.setEvaluator(evaluator);
        eval.setCommunicationScore(rng.nextInt(2, 11));
        eval.setDeliveryScore(rng.nextInt(2, 11));
        eval.setQualityScore(rng.nextInt(2, 11));
        eval.setGapScore(gap);
        vendorEvaluationRepository.save(eval);
    }

    // ──────────────────────────────────────────────
    // Invoice PDF attachment
    // ──────────────────────────────────────────────
    private void seedInvoiceAttachment(Request request, Invoice invoice) {
        try {
            var pdf = getClass().getResourceAsStream("/invoices/sample-invoice.pdf");
            if (pdf == null) { log.warn("sample-invoice.pdf not found, skipping attachment for {}", request.getRequestKey()); return; }

            var dir = java.nio.file.Paths.get("uploads/requisitions");
            if (!java.nio.file.Files.exists(dir)) java.nio.file.Files.createDirectories(dir);

            String name = UUID.randomUUID() + ".pdf";
            var target = dir.resolve(name);
            java.nio.file.Files.copy(pdf, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            pdf.close();

            Attachment att = new Attachment();
            att.setRequest(request); att.setInvoice(invoice);
            att.setFileName("sample-invoice.pdf"); att.setFileType("application/pdf");
            att.setFileSize(java.nio.file.Files.size(target)); att.setStoragePath(target.toString());
            attachmentRepository.save(att);
        } catch (java.io.IOException e) {
            log.error("Failed attachment for {}: {}", request.getRequestKey(), e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────
    private static InternalBudget budget(String name, BigDecimal amount, BudgetType type, InternalBudget parent) {
        return InternalBudget.builder().budgetName(name)
                .totalAmount(amount).budgetType(type).parentBudget(parent)
                .actualSpend(BigDecimal.ZERO).committedSpend(BigDecimal.ZERO)
                .safetyBuffer(type == BudgetType.REQUEST ? BigDecimal.ZERO : new BigDecimal("5"))
                .build();
    }

    private static BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
