package com.veritas.backend;

import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	ApplicationRunner seedData(UserRepository userRepo, PasswordEncoder encoder,
							   DepartmentRepository departmentRepository, TeamRepository teamRepo,
							   ProjectRepository projectRepo, WorkflowService workflowService,
							   WorkflowDefinitionRepository workflowDefinitionRepository,
							   VendorRepository vendorRepository,
							   RequestRepository requestRepository,
							   QuoteRepository quoteRepository, WorkflowStepRepository workflowStepRepository,
							   Environment env) {
		return args -> {
			log.info("Starting seed data initialization...");

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
				log.info("Seeded user: {} (role={}, department={})", procurement.getEmail(), procurement.getRole(), department.getName());
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
				log.info("Seeded user: {} (role={}, team={})", requester.getEmail(), requester.getRole(), teamOne.getName());

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
				log.info("Seeded user: {} (role={}, team={})", hrRequester.getEmail(), hrRequester.getRole(), teamTwo.getName());

				// Set requester as team leader
				teamTwo.setLeader(savedHrRequester);
				teamRepo.save(teamTwo);
				log.info("Set user {} as leader of team {}", savedHrRequester.getEmail(), teamTwo.getName());
			}

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
				log.info("Seeded {} projects", 2);
			}

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
						<bpmn:sequenceFlow id="Flow_1kxm0s8" name="budget &gt; $10.000" sourceRef="Gateway_0dumvhe" targetRef="Activity_05zq0ij">
						    <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
						        ${true}    \s
						    </bpmn:conditionExpression>
						</bpmn:sequenceFlow>
						<bpmn:sequenceFlow id="Flow_0z8w32s" name="$500 &gt; budget &gt; $10.000" sourceRef="Gateway_0dumvhe" targetRef="Activity_124j12i">
							<bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
						        ${false}   \s
						    </bpmn:conditionExpression>
						</bpmn:sequenceFlow>
						<bpmn:sequenceFlow id="Flow_04ni0t0" name="budget &lt; $500" sourceRef="Gateway_0dumvhe" targetRef="Activity_0mib3l3">
						<bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
						        ${false}  \s
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
						<bpmn:sequenceFlow id="Flow_1s24xzj" name="Request rejected" sourceRef="Gateway_0jludpd" targetRef="Activity_1u1p6ue">
						    <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
						        ${false}
						    </bpmn:conditionExpression>
						</bpmn:sequenceFlow>
						<bpmn:endEvent id="Event_0zjfdeg">
						<bpmn:incoming>Flow_0m5sv3t</bpmn:incoming>
						<bpmn:incoming>Flow_0vl4es9</bpmn:incoming>
						</bpmn:endEvent>
						<bpmn:sequenceFlow id="Flow_0m5sv3t" name="Request accepted" sourceRef="Gateway_0jludpd" targetRef="Event_0zjfdeg">
						    <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
						        ${true}
						    </bpmn:conditionExpression>
						</bpmn:sequenceFlow>
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
				log.info("Seeded 1 workflow");
			}

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

			log.info("Seed data initialization complete.");

			// AI-REFACTORED
			if (env.acceptsProfiles(org.springframework.core.env.Profiles.of("test"))) {
				log.info("Skipping mock vendor and request seeding in test profile.");
				return;
			}

			if (vendorRepository.findByTaxId("MOCK-1234").isEmpty()) {
				Vendor vendor = new Vendor();
				vendor.setVendorName("Mock Vendor Inc");
				vendor.setTaxId("MOCK-1234");
				vendor.setDescription("A vendor for testing evaluations.");
				vendor.setPrimaryContactEmail("contact@mockvendor.com");
				vendor.setPrimaryContactName("John Doe");
				vendor = vendorRepository.save(vendor);

				Request request = new Request();
				request.setRequestName("Mock Finished Requisition");
				request.setDescription("This is a mock request created for testing vendor evaluations.");
				request.setPriority(Priority.MEDIUM);
				request.setUserID(userRepo.findByEmail("requester@veritas.com").orElse(null));
				request.setProjectID(projectRepo.findAll().stream().findFirst().orElse(null));
				request.setTeamID(request.getUserID() != null ? request.getUserID().getTeam() : null);

				var workflowDef = workflowDefinitionRepository.findAll().stream().findFirst().orElse(null);
				if (workflowDef != null) {
					request.setWorkflowDefinitionID(workflowDef);
					WorkflowStep endStep = workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(workflowDef, WorkflowComponent.END_EVENT).orElse(null);
					request.setCurrentStepID(endStep);
				}

				request.setState(RequestStatus.FINISHED);
				request = requestRepository.save(request);

				Quote quote = new Quote();
				quote.setVendorID(vendor);
				quote.setCurrency(com.veritas.backend.vendor.entity.Currency.USD);
				quote.setBaseAmount(new BigDecimal("1000.00"));
				quote.setShippingCosts(new BigDecimal("0.00"));
				quote.setTotalAmount(new BigDecimal("1000.00"));
				quote.setRequest(request);
				quote.setSelected(true);
				quoteRepository.save(quote);

				log.info("Successfully initialized additional mock evaluation seed data with finished requisition, selected quote and test vendor '{}'", vendor.getVendorName());
			}
		};
	}

}
