package com.veritas.backend.seeding;

import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.service.WorkflowService;
import com.veritas.backend.budget.entity.InternalBudget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepo;
    private final ProjectRepository projectRepo;
    private final WorkflowService workflowService;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final VendorRepository vendorRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting seed data initialization...");

        // Seed Essential Departments
        Department department = departmentRepository.findByName("IT").orElseGet(() -> {
            Department it = new Department();
            it.setName("IT");
            it.setInternalBudget(InternalBudget.builder()
                    .budgetName("Project: Enterprise Lifecycle Management")
                    .totalAmount(new BigDecimal("900000"))
                    .build());
            return departmentRepository.save(it);
        });

        Department department2 = departmentRepository.findByName("HR").orElseGet(() -> {
            Department hr = new Department();
            hr.setName("HR");
            hr.setInternalBudget(InternalBudget.builder()
                    .budgetName("Project: Enterprise Lifecycle Management")
                    .totalAmount(new BigDecimal("800000"))
                    .build());
            return departmentRepository.save(hr);
        });

        // Seed Essential Teams
        Team teamOne = teamRepo.findByName("Software Engineering").orElseGet(() -> {
            Team team = new Team();
            team.setName("Software Engineering");
            team.setDepartment(department);
            team.setDescription("Core software development and innovation team");
            log.info("Seeded team: {}", team.getName());
            return teamRepo.save(team);
        });

        Team teamTwo = teamRepo.findByName("Human Resources").orElseGet(() -> {
            Team team = new Team();
            team.setName("Human Resources");
            team.setDepartment(department2);
            team.setDescription("HR core team");
            log.info("Seeded team: {}", team.getName());
            return teamRepo.save(team);
        });

        // Seed Essential Users
        if (userRepo.findByEmail("admin@veritas.com").isEmpty()) {
            User admin = User.builder()
                    .name("Veritas Admin")
                    .email("admin@veritas.com")
                    .passwordHash(encoder.encode("password123"))
                    .role(UserRole.ADMINISTRATOR)
                    .isActive(true)
                    .requiresPasswordChange(false)
                    .build();
            userRepo.save(admin);
            log.info("Seeded user: {} (role={})", admin.getEmail(), admin.getRole());
        }

        if (userRepo.findByEmail("finance@veritas.com").isEmpty()) {
            User finance = User.builder()
                    .name("Veritas Finance")
                    .email("finance@veritas.com")
                    .passwordHash(encoder.encode("password123"))
                    .role(UserRole.FINANCE_OFFICER)
                    .isActive(true)
                    .requiresPasswordChange(false)
                    .build();
            userRepo.save(finance);
            log.info("Seeded user: {} (role={})", finance.getEmail(), finance.getRole());
        }

        if (userRepo.findByEmail("procurement@veritas.com").isEmpty()) {
            User procurement = User.builder()
                    .name("Veritas Procurement")
                    .email("procurement@veritas.com")
                    .passwordHash(encoder.encode("password123"))
                    .role(UserRole.PROCUREMENT_OFFICER)
                    .department(department)
                    .isActive(true)
                    .requiresPasswordChange(false)
                    .build();
            userRepo.save(procurement);
            log.info("Seeded user: {} (role={}, department={})", procurement.getEmail(), procurement.getRole(),
                    department.getName());
        }

        if (userRepo.findByEmail("requester@veritas.com").isEmpty()) {
            User requester = User.builder()
                    .name("Veritas Requester")
                    .email("requester@veritas.com")
                    .passwordHash(encoder.encode("password123"))
                    .role(UserRole.REQUESTER)
                    .isActive(true)
                    .team(teamOne)
                    .requiresPasswordChange(false)
                    .build();
            User savedRequester = userRepo.save(requester);
            log.info("Seeded user: {} (role={}, team={})", requester.getEmail(), requester.getRole(),
                    teamOne.getName());

            // Set requester as team leader
            teamOne.setLeader(savedRequester);
            teamRepo.save(teamOne);
            log.info("Set user {} as leader of team {}", savedRequester.getEmail(), teamOne.getName());
        }

        if (userRepo.findByEmail("hr_requester@veritas.com").isEmpty()) {
            User hrRequester = User.builder()
                    .name("Veritas HR Requester")
                    .email("hr_requester@veritas.com")
                    .passwordHash(encoder.encode("password123"))
                    .role(UserRole.REQUESTER)
                    .isActive(true)
                    .team(teamTwo)
                    .requiresPasswordChange(false)
                    .build();
            User savedHrRequester = userRepo.save(hrRequester);
            log.info("Seeded user: {} (role={}, team={})", hrRequester.getEmail(), hrRequester.getRole(),
                    teamTwo.getName());

            // Set requester as team leader
            teamTwo.setLeader(savedHrRequester);
            teamRepo.save(teamTwo);
            log.info("Set user {} as leader of team {}", savedHrRequester.getEmail(), teamTwo.getName());
        }

        // Seed Essential Projects
        if (projectRepo.count() == 0) {
            Project p1 = Project.builder()
                    .name("Apollo Architecture Audit")
                    .internalBudget(InternalBudget.builder()
                            .budgetName("Project: Apollo Architecture Audit")
                            .totalAmount(new BigDecimal("150000"))
                            .build())
                    .projectKey("apollo-architecture-audit")
                    .startDate(LocalDate.of(2026, 1, 1))
                    .endDate(LocalDate.of(2026, 12, 31))
                    .team(teamOne)
                    .build();

            Project p2 = Project.builder()
                    .name("Enterprise Lifecycle Management")
                    .projectKey("Second")
                    .internalBudget(InternalBudget.builder()
                            .budgetName("Project: Enterprise Lifecycle Management")
                            .totalAmount(new BigDecimal("275000"))
                            .build())
                    .startDate(LocalDate.of(2026, 3, 15))
                    .endDate(LocalDate.of(2027, 6, 1))
                    .team(teamOne)
                    .build();

            projectRepo.save(p1);
            projectRepo.save(p2);
            log.info("Seeded 2 basic projects");
        }

        // Seed Essential Workflow
        if (workflowDefinitionRepository.count() == 0) {
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
                    <bpmn:process id="PR_Tiered_Approval_001" name="Tiered Procurement Approval" isExecutable="false">
                    <bpmn:startEvent id="Event_1if1b7g" name="Start Procurement">
                    <bpmn:outgoing>Flow_1hlbu7w</bpmn:outgoing>
                    </bpmn:startEvent>
                    <bpmn:task id="Activity_1u1p6ue" name="Fill in Details and Upload Vendor Quotes">
                    <bpmn:incoming>Flow_1hlbu7w</bpmn:incoming>
                    <bpmn:incoming>Flow_1s24xzj</bpmn:incoming>
                    <bpmn:outgoing>Flow_0vsq5o5</bpmn:outgoing>
                    </bpmn:task>
                    <bpmn:sequenceFlow id="Flow_1hlbu7w" sourceRef="Event_1if1b7g" targetRef="Activity_1u1p6ue"/>
                    <bpmn:exclusiveGateway id="Gateway_0dumvhe" name="Budget Check">
                    <bpmn:incoming>Flow_0vsq5o5</bpmn:incoming>
                    <bpmn:outgoing>Flow_1kxm0s8</bpmn:outgoing>
                    <bpmn:outgoing>Flow_0z8w32s</bpmn:outgoing>
                    <bpmn:outgoing>Flow_04ni0t0</bpmn:outgoing>
                    </bpmn:exclusiveGateway>
                    <bpmn:sequenceFlow id="Flow_0vsq5o5" sourceRef="Activity_1u1p6ue" targetRef="Gateway_0dumvhe"/>
                    <bpmn:task id="Activity_0mib3l3" name="Automatic Validation">
                    <bpmn:incoming>Flow_04ni0t0</bpmn:incoming>
                    <bpmn:outgoing>Flow_0vl4es9</bpmn:outgoing>
                    </bpmn:task>
                    <bpmn:task id="Activity_124j12i" name="Normal Finance Review">
                    <bpmn:incoming>Flow_0z8w32s</bpmn:incoming>
                    <bpmn:outgoing>Flow_1pumtq5</bpmn:outgoing>
                    </bpmn:task>
                    <bpmn:task id="Activity_05zq0ij" name="Detailed Finance Review">
                    <bpmn:incoming>Flow_1kxm0s8</bpmn:incoming>
                    <bpmn:outgoing>Flow_0nupddp</bpmn:outgoing>
                    </bpmn:task>
                    <bpmn:sequenceFlow id="Flow_1kxm0s8" name="budget > $10.000" sourceRef="Gateway_0dumvhe" targetRef="Activity_05zq0ij">
                        <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
                            ${true}    \\s
                        </bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:sequenceFlow id="Flow_0z8w32s" name="$500 > budget > $10.000" sourceRef="Gateway_0dumvhe" targetRef="Activity_124j12i">
                        <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
                            ${false}   \\s
                        </bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:sequenceFlow id="Flow_04ni0t0" name="budget &lt; $500" sourceRef="Gateway_0dumvhe" targetRef="Activity_0mib3l3">
                    <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
                            ${false}  \\s
                        </bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:exclusiveGateway id="Gateway_0jludpd">
                    <bpmn:incoming>Flow_1pumtq5</bpmn:incoming>
                    <bpmn:incoming>Flow_0nupddp</bpmn:incoming>
                    <bpmn:outgoing>Flow_1s24xzj</bpmn:outgoing>
                    <bpmn:outgoing>Flow_0m5sv3t</bpmn:outgoing>
                    </bpmn:exclusiveGateway>
                    <bpmn:sequenceFlow id="Flow_1pumtq5" name="accept / reject" sourceRef="Activity_124j12i" targetRef="Gateway_0jludpd"/>
                    <bpmn:sequenceFlow id="Flow_0nupddp" name="accept / reject" sourceRef="Activity_05zq0ij" targetRef="Gateway_0jludpd"/>
                    <bpmn:sequenceFlow id="Flow_1s24xzj" name="Request rejected" sourceRef="Gateway_0jludpd" targetRef="Activity_1u1p6ue"/>
                    <bpmn:endEvent id="Event_0zjfdeg">
                    <bpmn:incoming>Flow_0m5sv3t</bpmn:incoming>
                    <bpmn:incoming>Flow_0vl4es9</bpmn:incoming>
                    </bpmn:endEvent>
                    <bpmn:sequenceFlow id="Flow_0m5sv3t" name="Request accepted" sourceRef="Gateway_0jludpd" targetRef="Event_0zjfdeg"/>
                    <bpmn:sequenceFlow id="Flow_0vl4es9" name="auto-accept" sourceRef="Activity_0mib3l3" targetRef="Event_0zjfdeg"/>
                    </bpmn:process>
                    <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="PR_Tiered_Approval_001">
                    <bpmndi:BPMNShape id="Event_1if1b7g_di" bpmnElement="Event_1if1b7g">
                    <dc:Bounds x="-78" y="122" width="36" height="36"/>
                    <bpmndi:BPMNLabel>
                    <dc:Bounds x="-105" y="165" width="90" height="14"/>
                    </bpmndi:BPMNLabel>
                    </bpmndi:BPMNShape>
                    <bpmndi:BPMNShape id="Activity_1u1p6ue_di" bpmnElement="Activity_1u1p6ue">
                    <dc:Bounds x="70" y="100" width="100" height="80"/>
                    <bpmndi:BPMNLabel/>
                    </bpmndi:BPMNShape>
                    <bpmndi:BPMNShape id="Gateway_0dumvhe_di" bpmnElement="Gateway_0dumvhe" isMarkerVisible="true">
                    <dc:Bounds x="245" y="115" width="50" height="50"/>
                    <bpmndi:BPMNLabel>
                    <dc:Bounds x="235" y="172" width="70" height="14"/>
                    </bpmndi:BPMNLabel>
                    </bpmndi:BPMNShape>
                    <bpmndi:BPMNShape id="Activity_0mib3l3_di" bpmnElement="Activity_0mib3l3">
                    <dc:Bounds x="370" y="-30" width="100" height="80"/>
                    <bpmndi:BPMNLabel/>
                    </bpmndi:BPMNShape>
                    <bpmndi:BPMNShape id="Activity_124j12i_di" bpmnElement="Activity_124j12i">
                    <dc:Bounds x="370" y="100" width="100" height="80"/>
                    <bpmndi:BPMNLabel/>
                    </bpmndi:BPMNShape>
                    <bpmndi:BPMNShape id="Activity_05zq0ij_di" bpmnElement="Activity_05zq0ij">
                    <dc:Bounds x="370" y="230" width="100" height="80"/>
                    <bpmndi:BPMNLabel/>
                    </bpmndi:BPMNShape>
                    <bpmndi:BPMNShape id="Gateway_0jludpd_di" bpmnElement="Gateway_0jludpd" isMarkerVisible="true">
                    <dc:Bounds x="555" y="175" width="50" height="50"/>
                    </bpmndi:BPMNShape>
                    <bpmndi:BPMNShape id="Event_0zjfdeg_di" bpmnElement="Event_0zjfdeg">
                    <dc:Bounds x="692" y="122" width="36" height="36"/>
                    </bpmndi:BPMNShape>
                    <bpmndi:BPMNEdge id="Flow_1hlbu7w_di" bpmnElement="Flow_1hlbu7w">
                    <di:waypoint x="-42" y="140"/>
                    <di:waypoint x="70" y="140"/>
                    </bpmndi:BPMNEdge>
                    <bpmndi:BPMNEdge id="Flow_0vsq5o5_di" bpmnElement="Flow_0vsq5o5">
                    <di:waypoint x="170" y="140"/>
                    <di:waypoint x="245" y="140"/>
                    </bpmndi:BPMNEdge>
                    <bpmndi:BPMNEdge id="Flow_1kxm0s8_di" bpmnElement="Flow_1kxm0s8">
                    <di:waypoint x="270" y="165"/>
                    <di:waypoint x="270" y="270"/>
                    <di:waypoint x="370" y="270"/>
                    <bpmndi:BPMNLabel>
                    <dc:Bounds x="242" y="215" width="86" height="14"/>
                    </bpmndi:BPMNLabel>
                    </bpmndi:BPMNEdge>
                    <bpmndi:BPMNEdge id="Flow_0z8w32s_di" bpmnElement="Flow_0z8w32s">
                    <di:waypoint x="295" y="140"/>
                    <di:waypoint x="370" y="140"/>
                    <bpmndi:BPMNLabel>
                    <dc:Bounds x="292" y="116" width="81" height="27"/>
                    </bpmndi:BPMNLabel>
                    </bpmndi:BPMNEdge>
                    <bpmndi:BPMNEdge id="Flow_04ni0t0_di" bpmnElement="Flow_04ni0t0">
                    <di:waypoint x="270" y="115"/>
                    <di:waypoint x="270" y="10"/>
                    <di:waypoint x="370" y="10"/>
                    <bpmndi:BPMNLabel>
                    <dc:Bounds x="250" y="60" width="71" height="14"/>
                    </bpmndi:BPMNLabel>
                    </bpmndi:BPMNEdge>
                    <bpmndi:BPMNEdge id="Flow_1pumtq5_di" bpmnElement="Flow_1pumtq5">
                    <di:waypoint x="470" y="140"/>
                    <di:waypoint x="580" y="140"/>
                    <di:waypoint x="580" y="175"/>
                    <bpmndi:BPMNLabel>
                    <dc:Bounds x="491" y="122" width="69" height="14"/>
                    </bpmndi:BPMNLabel>
                    </bpmndi:BPMNEdge>
                    <bpmndi:BPMNEdge id="Flow_0nupddp_di" bpmnElement="Flow_0nupddp">
                    <di:waypoint x="420" y="230"/>
                    <di:waypoint x="420" y="200"/>
                    <di:waypoint x="555" y="200"/>
                    <bpmndi:BPMNLabel>
                    <dc:Bounds x="401" y="212" width="69" height="14"/>
                    </bpmndi:BPMNLabel>
                    </bpmndi:BPMNEdge>
                    <bpmndi:BPMNEdge id="Flow_1s24xzj_di" bpmnElement="Flow_1s24xzj">
                    <di:waypoint x="580" y="225"/>
                    <di:waypoint x="580" y="340"/>
                    <di:waypoint x="120" y="340"/>
                    <di:waypoint x="120" y="180"/>
                    <bpmndi:BPMNLabel>
                    <dc:Bounds x="308" y="322" width="84" height="14"/>
                    </bpmndi:BPMNLabel>
                    </bpmndi:BPMNEdge>
                    <bpmndi:BPMNEdge id="Flow_0m5sv3t_di" bpmnElement="Flow_0m5sv3t">
                    <di:waypoint x="605" y="200"/>
                    <di:waypoint x="710" y="200"/>
                    <di:waypoint x="710" y="158"/>
                    <bpmndi:BPMNLabel>
                    <dc:Bounds x="613" y="182" width="89" height="14"/>
                    </bpmndi:BPMNLabel>
                    </bpmndi:BPMNEdge>
                    <bpmndi:BPMNEdge id="Flow_0vl4es9_di" bpmnElement="Flow_0vl4es9">
                    <di:waypoint x="470" y="10"/>
                    <di:waypoint x="710" y="10"/>
                    <di:waypoint x="710" y="122"/>
                    <bpmndi:BPMNLabel>
                    <dc:Bounds x="561" y="-8" width="58" height="14"/>
                    </bpmndi:BPMNLabel>
                    </bpmndi:BPMNEdge>
                    </bpmndi:BPMNPlane>
                    </bpmndi:BPMNDiagram>
                    </bpmn:definitions>
                    """;
            workflowService.createWorkflow(new WorkflowSaveDto(xml, null));
            log.info("Seeded basic workflow definition");
        }

        // Seed Essential Vendors
        if (vendorRepository.findByTaxId("US-123456789").isEmpty()) {
            Vendor vendor1 = new Vendor();
            vendor1.setVendorName("Global Tech Solutions");
            vendor1.setTaxId("US-123456789");
            vendor1.setDescription("Leading provider of enterprise hardware and software licenses.");
            vendor1.setPrimaryContactName("John Smith");
            vendor1.setPrimaryContactEmail("jsmith@globaltech.com");
            vendorRepository.save(vendor1);
            log.info("Seeded vendor: {}", vendor1.getVendorName());
        }

        if (vendorRepository.findByTaxId("EU-987654321").isEmpty()) {
            Vendor vendor2 = new Vendor();
            vendor2.setVendorName("Prime Logistics & Services");
            vendor2.setTaxId("EU-987654321");
            vendor2.setDescription("Global shipping, handling, and logistics partner.");
            vendor2.setPrimaryContactName("Elena Vance");
            vendor2.setPrimaryContactEmail("evance@primelogistics.com");
            vendorRepository.save(vendor2);
            log.info("Seeded vendor: {}", vendor2.getVendorName());
        }

        // Seed Dynamic Rich Test Data (Faker)
        // Check if rich seeding is already done (e.g. if we have more than the basic 5
        // users)
        if (userRepo.count() <= 5) {
            log.info("Initializing rich dynamic mock data seeding with Faker...");
            Random random = new Random(42);
            Faker faker = new Faker(random);

            // Extra Departments
            List<Department> allDepts = new ArrayList<>();
            allDepts.add(department);
            allDepts.add(department2);

            String[] extraDeptNames = { "Sales & Marketing", "Operations & Logistics" };
            for (String deptName : extraDeptNames) {
                if (departmentRepository.findByName(deptName).isEmpty()) {
                    Department d = new Department();
                    d.setName(deptName);
                    d.setInternalBudget(InternalBudget.builder()
                            .budgetName("Operational Budget: " + deptName)
                            .totalAmount(new BigDecimal(faker.number().numberBetween(300000, 800000)))
                            .build());
                    Department savedDept = departmentRepository.save(d);
                    allDepts.add(savedDept);
                    log.info("Seeded extra department: {}", savedDept.getName());
                }
            }

            // Extra Teams
            List<Team> allTeams = new ArrayList<>();
            allTeams.add(teamOne);
            allTeams.add(teamTwo);

            // We define dynamic teams matching different departments
            String[][] extraTeams = {
                    { "Digital Campaigns", "Sales & Marketing",
                            "Handles marketing campaigns and social media presence" },
                    { "Direct Sales & B2B", "Sales & Marketing", "Handles corporate accounts and B2B partnerships" },
                    { "Customer Support & Success", "Operations & Logistics",
                            "First line of customer care and engagement" },
                    { "Cloud Infrastructure Ops", "IT", "Manages servers, cloud instances, and DevOps pipelines" }
            };

            for (String[] teamData : extraTeams) {
                String teamName = teamData[0];
                String deptName = teamData[1];
                String desc = teamData[2];

                if (teamRepo.findByName(teamName).isEmpty()) {
                    Department teamDept = departmentRepository.findByName(deptName)
                            .orElse(allDepts.get(random.nextInt(allDepts.size())));
                    Team t = new Team();
                    t.setName(teamName);
                    t.setDepartment(teamDept);
                    t.setDescription(desc);
                    t.setIsActive(true);
                    Team savedTeam = teamRepo.save(t);
                    allTeams.add(savedTeam);
                    log.info("Seeded extra team: {}", savedTeam.getName());
                }
            }

            // Dynamic Users (25 users)
            // Distribute roles: 15 Requesters, 5 Procurement Officers, 3 Finance Officers,
            // 2 Administrators
            List<User> seededRequesters = new ArrayList<>();

            for (int i = 1; i <= 25; i++) {
                String name = faker.name().fullName();
                String cleanName = name.toLowerCase().replaceAll("[^a-z]", "");
                String email = cleanName + "." + i + "@veritas.com";

                if (userRepo.findByEmail(email).isEmpty()) {
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

                    User user = User.builder()
                            .name(name)
                            .email(email)
                            .passwordHash(encoder.encode("password123"))
                            .role(role)
                            .team(userTeam)
                            .department(userDept)
                            .isActive(true)
                            .requiresPasswordChange(false)
                            .build();

                    User savedUser = userRepo.save(user);
                    if (role == UserRole.REQUESTER) {
                        seededRequesters.add(savedUser);
                    }
                    log.info("Seeded extra user: {} (role={}, team/dept={})",
                            savedUser.getEmail(),
                            savedUser.getRole(),
                            savedUser.getTeam() != null ? savedUser.getTeam().getName()
                                    : (savedUser.getDepartment() != null ? savedUser.getDepartment().getName()
                                            : "None"));
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
                        User leader = teamMembers.get(0);
                        currentTeam.setLeader(leader);
                        teamRepo.save(currentTeam);
                        log.info("Assigned team leader: {} to team {}", leader.getEmail(), currentTeam.getName());
                    }
                }
            }

            // Extra Projects (5 projects)
            String[] extraProjectNames = { "Alpha Cloud Migration", "Project Oasis CRM",
                    "Titan Supply Chain Optimization", "Direct Sales Booster", "Omnichannel Care Suite" };
            for (int i = 0; i < extraProjectNames.length; i++) {
                String projName = extraProjectNames[i];
                String key = projName.toLowerCase().replaceAll("[^a-z]", "") + "-" + i;

                if (!projectRepo.existsByNameOrProjectKey(projName, key)) {
                    Team projectTeam = allTeams.get(random.nextInt(allTeams.size()));
                    Project p = Project.builder()
                            .name(projName)
                            .projectKey(key)
                            .team(projectTeam)
                            .startDate(LocalDate.now().minusMonths(random.nextInt(1, 6)))
                            .endDate(LocalDate.now().plusMonths(random.nextInt(6, 18)))
                            .internalBudget(InternalBudget.builder()
                                    .budgetName("Project Budget: " + projName)
                                    .totalAmount(new BigDecimal(faker.number().numberBetween(50000, 250000)))
                                    .build())
                            .build();

                    projectRepo.save(p);
                    log.info("Seeded extra project: {} (assigned to team {})", p.getName(), projectTeam.getName());
                }
            }

            // Extra Vendors (6 vendors)
            for (int i = 0; i < 6; i++) {
                String vendorName = faker.company().name();
                String taxId = "US-" + faker.number().digits(9);

                if (vendorRepository.findByTaxId(taxId).isEmpty()) {
                    Vendor v = new Vendor();
                    v.setVendorName(vendorName);
                    v.setTaxId(taxId);
                    v.setDescription(faker.company().catchPhrase());
                    v.setPrimaryContactName(faker.name().fullName());
                    v.setPrimaryContactEmail(faker.internet().emailAddress());
                    vendorRepository.save(v);
                    log.info("Seeded extra vendor: {}", v.getVendorName());
                }
            }

            log.info("Rich dynamic mock data seeding complete.");
        }

        log.info("Seed data initialization complete.");
    }
}
