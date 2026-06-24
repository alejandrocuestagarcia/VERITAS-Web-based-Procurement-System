package com.veritas.backend.requisition.service;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.integrations.currency.entity.ExchangeRate;
import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import com.veritas.backend.integrations.currency.repository.ExchangeRateRepository;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.dto.QuoteCreateDto;
import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.dto.QuoteLineItemCreateDto;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.service.RequisitionQuoteService;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.QuoteLineItem;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.QuoteLineItemRepository;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.vendor.repository.VendorRepository;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.util.RequestFactory;
import com.veritas.backend.util.UserFactory;
import com.veritas.backend.requisition.entity.Attachment;
import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RequisitionQuoteServiceIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private RequisitionQuoteService quoteService;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private QuoteLineItemRepository quoteLineItemRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private InternalBudgetRepository internalBudgetRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private RequestFactory requestFactory;
    @Autowired
    private UserFactory userFactory;

    private Request request;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        cleanAllData();

        Department department = new Department();
        department.setName("IT");
        department.setInternalBudget(InternalBudget.builder()
                .budgetName("IT")
                .budgetType(BudgetType.DEPARTMENT)
                .totalAmount(BigDecimal.valueOf(1000000.0))
                .build());
        department = departmentRepository.save(department);

        Team team = new Team();
        team.setName("Testing Team");
        team.setDescription("We do tests");
        team.setDepartment(department);
        team = teamRepository.save(team);

        User requester = userFactory.createUser("requester@test.com", team, UserRole.REQUESTER);
        request = requestFactory.createValidRequest("Integration Request", requester);

        vendor = new Vendor();
        vendor.setVendorName("Integration Vendor");
        vendor.setTaxId("TAX-INT-999");
        vendor.setDescription("Integration description");
        vendor = vendorRepository.save(vendor);

        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.PROCUREMENT_OFFICER);
        user.setDepartment(department);
        user.setEmail("test@test.com");

        ExchangeRate rate = ExchangeRate.builder()
            .targetCurrency(Currency.USD)
            .rate(BigDecimal.valueOf(1.1))
            .fetchedAt(LocalDateTime.now())
            .source(ExchangeRateSource.FRANKFURTER)
            .build();

        exchangeRateRepository.save(rate);

        SecurityContextHolder.clearContext();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void cleanUp() {
        cleanAllData();
        SecurityContextHolder.clearContext();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void cleanAllData() {
        jdbcTemplate.update("UPDATE users SET team_id = NULL");
        jdbcTemplate.update("UPDATE teams SET leader_id = NULL");
        jdbcTemplate.update("UPDATE internal_budgets SET parent_budget_id = NULL");

        attachmentRepository.deleteAll();
        invoiceRepository.deleteAll();
        quoteLineItemRepository.deleteAll();
        quoteRepository.deleteAll();
        requestRepository.deleteAll();

        projectRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();
        departmentRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM internal_budgets");

        vendorRepository.deleteAll();
        exchangeRateRepository.deleteAll();
    }

    @Test
    void QuoteCreation_ValidInput_PersistsInDatabase() {
        QuoteCreateDto createDto = QuoteCreateDto.builder()
                .vendorId(vendor.getId())
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(200))
                .shippingCosts(BigDecimal.valueOf(15))
                .totalAmount(BigDecimal.valueOf(215))
                .shippingTime(5)
                .items(List.of(new QuoteLineItemCreateDto("Integration Item", 2, BigDecimal.valueOf(100), null)))
                .build();

        QuoteDto result = quoteService.createQuoteForRequest(request.getRequestID(), createDto);

        assertAll(
            () -> assertNotNull(result),
            () -> assertNotNull(result.quoteId()),
            () -> assertEquals(0, result.totalAmount().compareTo(BigDecimal.valueOf(215)))
        );

        Optional<Quote> persistedQuote = quoteRepository.findById(result.quoteId());
        assertTrue(persistedQuote.isPresent());
        assertEquals(Currency.EUR, persistedQuote.get().getCurrency());

        List<QuoteLineItem> persistedItems = quoteLineItemRepository.findByQuoteQuoteID(result.quoteId());
        assertAll(
            () -> assertEquals(1, persistedItems.size()),
            () -> assertEquals("Integration Item", persistedItems.getFirst().getProductDescription())
        );
    }

    @Test
    void FindQuotes_ForRequest_ReturnsAllQuotes() {
        Quote quote1 = saveTestQuote(Currency.EUR, BigDecimal.valueOf(100));
        Quote quote2 = saveTestQuote(Currency.USD, BigDecimal.valueOf(150));

        List<QuoteDto> result = quoteService.getQuotesForRequest(request.getRequestID());

        assertEquals(2, result.size());
        List<Long> ids = result.stream().map(QuoteDto::quoteId).toList();
        assertAll(
            () -> assertTrue(ids.contains(quote1.getQuoteID())),
            () -> assertTrue(ids.contains(quote2.getQuoteID()))
        );
    }

    @Test
    void UpdateQuote_ValidInput_PersistsChanges() {
        Quote quote = saveTestQuote(Currency.EUR, BigDecimal.valueOf(100));

        QuoteCreateDto updateDto = QuoteCreateDto.builder()
                .vendorId(vendor.getId())
                .currency(Currency.USD)
                .baseAmount(BigDecimal.valueOf(120))
                .shippingCosts(BigDecimal.valueOf(5))
                .totalAmount(BigDecimal.valueOf(125))
                .shippingTime(5)
                .items(List.of(new QuoteLineItemCreateDto("Updated Item", 1, BigDecimal.valueOf(120), null)))
                .build();

        QuoteDto result = quoteService.updateQuoteForRequest(request.getRequestID(), quote.getQuoteID(), updateDto);

        assertAll(
            () -> assertEquals(Currency.USD, result.currency()),
            () -> assertEquals(0, result.totalAmount().compareTo(BigDecimal.valueOf(125)))
        );

        Quote persisted = quoteRepository.findById(quote.getQuoteID()).orElseThrow();
        assertAll(
            () -> assertEquals(Currency.USD, persisted.getCurrency()),
            () -> assertEquals(0, persisted.getTotalAmount().compareTo(BigDecimal.valueOf(125)))
        );

        List<QuoteLineItem> persistedItems = quoteLineItemRepository.findByQuoteQuoteID(quote.getQuoteID());
        assertAll(
            () -> assertEquals(1, persistedItems.size()),
            () -> assertEquals("Updated Item", persistedItems.getFirst().getProductDescription())
        );
    }

    @Test
    void SelectQuote_ValidQuoteId_SetsPreferredAndClearsOthers() {
        Quote quote1 = saveTestQuote(Currency.EUR, BigDecimal.valueOf(100));
        Quote quote2 = saveTestQuote(Currency.USD, BigDecimal.valueOf(150));

        quoteService.selectQuoteForRequest(request.getRequestID(), quote1.getQuoteID());

        final Quote persisted1 = quoteRepository.findById(quote1.getQuoteID()).orElseThrow();
        final Quote persisted2 = quoteRepository.findById(quote2.getQuoteID()).orElseThrow();

        assertAll(
            () -> assertTrue(persisted1.isSelected()),
            () -> assertFalse(persisted2.isSelected())
        );

        // Select the second one, should clear the first
        quoteService.selectQuoteForRequest(request.getRequestID(), quote2.getQuoteID());

        final Quote persisted1After = quoteRepository.findById(quote1.getQuoteID()).orElseThrow();
        final Quote persisted2After = quoteRepository.findById(quote2.getQuoteID()).orElseThrow();

        assertAll(
            () -> assertFalse(persisted1After.isSelected()),
            () -> assertTrue(persisted2After.isSelected())
        );
    }

    @Test
    void DeleteQuote_ValidQuoteId_RemovesFromDatabase() {
        Quote quote = saveTestQuote(Currency.EUR, BigDecimal.valueOf(100));
        
        QuoteLineItem item = new QuoteLineItem();
        item.setQuote(quote);
        item.setProductDescription("Delete Me");
        item.setQuantity(1);
        item.setUnitPrice(BigDecimal.valueOf(100));
        item.setSubtotal(BigDecimal.valueOf(100));
        quoteLineItemRepository.save(item);

        quoteService.deleteQuoteForRequest(request.getRequestID(), quote.getQuoteID());

        assertAll(
            () -> assertTrue(quoteRepository.findById(quote.getQuoteID()).isEmpty()),
            () -> assertTrue(quoteLineItemRepository.findByQuoteQuoteID(quote.getQuoteID()).isEmpty())
        );
    }

    private Quote saveTestQuote(Currency currency, BigDecimal totalAmount) {
        Quote quote = new Quote();
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setCurrency(currency);
        quote.setBaseAmount(totalAmount);
        quote.setShippingCosts(BigDecimal.ZERO);
        quote.setTotalAmount(totalAmount);
        quote.setShippingTime(5);
        quote.setSelected(false);
        return quoteRepository.save(quote);
    }

    //AI-Generated
    @Test
    void SelectQuote_WithBudgetHierarchy_UpdatesCommittedSpendAcrossHierarchy() {
        InternalBudget globalBudget = InternalBudget.builder()
                .budgetName("Global Budget")
                .budgetType(BudgetType.GLOBAL)
                .totalAmount(BigDecimal.valueOf(10000))
                .committedSpend(BigDecimal.valueOf(200))
                .build();
        globalBudget = internalBudgetRepository.save(globalBudget);

        InternalBudget deptBudget = InternalBudget.builder()
                .budgetName("Department Budget")
                .budgetType(BudgetType.DEPARTMENT)
                .totalAmount(BigDecimal.valueOf(5000))
                .committedSpend(BigDecimal.valueOf(200))
                .parentBudget(globalBudget)
                .build();
        deptBudget = internalBudgetRepository.save(deptBudget);

        InternalBudget projBudget = InternalBudget.builder()
                .budgetName("Project Budget")
                .budgetType(BudgetType.PROJECT)
                .totalAmount(BigDecimal.valueOf(2000))
                .committedSpend(BigDecimal.valueOf(200))
                .parentBudget(deptBudget)
                .build();
        projBudget = internalBudgetRepository.save(projBudget);

        InternalBudget reqBudget = InternalBudget.builder()
                .budgetName("Request Budget")
                .budgetType(BudgetType.REQUEST)
                .totalAmount(BigDecimal.ZERO)
                .committedSpend(BigDecimal.ZERO)
                .parentBudget(projBudget)
                .build();
        reqBudget = internalBudgetRepository.save(reqBudget);

        // Assign budget to request
        request.setBudget(reqBudget);
        request = requestRepository.save(request);

        Quote quote1 = saveTestQuote(Currency.EUR, BigDecimal.valueOf(100));
        Quote quote2 = saveTestQuote(Currency.EUR, BigDecimal.valueOf(150));

        // Select quote1 (100)
        quoteService.selectQuoteForRequest(request.getRequestID(), quote1.getQuoteID());

        // Refresh budgets from repository
        InternalBudget updatedReqBudget = internalBudgetRepository.findById(reqBudget.getId()).orElseThrow();
        InternalBudget updatedProjBudget = internalBudgetRepository.findById(projBudget.getId()).orElseThrow();
        InternalBudget updatedDeptBudget = internalBudgetRepository.findById(deptBudget.getId()).orElseThrow();
        InternalBudget updatedGlobalBudget = internalBudgetRepository.findById(globalBudget.getId()).orElseThrow();

        // Check committedSpend has increased by 100
        assertAll("Committed spend increased by 100 across hierarchy",
                () -> assertEquals(0, updatedReqBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(100))),
                () -> assertEquals(0, updatedProjBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(300))),
                () -> assertEquals(0, updatedDeptBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(300))),
                () -> assertEquals(0, updatedGlobalBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(300)))
        );

        // Select quote2 (150), should subtract quote1 (100) and add quote2 (150), net change +50
        quoteService.selectQuoteForRequest(request.getRequestID(), quote2.getQuoteID());

        InternalBudget updatedReqBudget2 = internalBudgetRepository.findById(reqBudget.getId()).orElseThrow();
        InternalBudget updatedProjBudget2 = internalBudgetRepository.findById(projBudget.getId()).orElseThrow();
        InternalBudget updatedDeptBudget2 = internalBudgetRepository.findById(deptBudget.getId()).orElseThrow();
        InternalBudget updatedGlobalBudget2 = internalBudgetRepository.findById(globalBudget.getId()).orElseThrow();

        assertAll("Committed spend updated after selecting another quote",
                () -> assertEquals(0, updatedReqBudget2.getCommittedSpend().compareTo(BigDecimal.valueOf(150))),
                () -> assertEquals(0, updatedProjBudget2.getCommittedSpend().compareTo(BigDecimal.valueOf(350))),
                () -> assertEquals(0, updatedDeptBudget2.getCommittedSpend().compareTo(BigDecimal.valueOf(350))),
                () -> assertEquals(0, updatedGlobalBudget2.getCommittedSpend().compareTo(BigDecimal.valueOf(350)))
        );
    }

    //AI-Generated
    @Test
    void UpdateSelectedQuote_UpdatesCommittedSpendAcrossHierarchy() {
        // Create budget hierarchy: Project Budget -> Department Budget
        InternalBudget deptBudget = InternalBudget.builder()
                .budgetName("Department Budget")
                .budgetType(BudgetType.DEPARTMENT)
                .totalAmount(BigDecimal.valueOf(5000))
                .committedSpend(BigDecimal.valueOf(200))
                .build();
        deptBudget = internalBudgetRepository.save(deptBudget);

        InternalBudget projBudget = InternalBudget.builder()
                .budgetName("Project Budget")
                .budgetType(BudgetType.PROJECT)
                .totalAmount(BigDecimal.valueOf(2000))
                .committedSpend(BigDecimal.valueOf(200))
                .parentBudget(deptBudget)
                .build();
        projBudget = internalBudgetRepository.save(projBudget);

        InternalBudget reqBudget = InternalBudget.builder()
                .budgetName("Request Budget")
                .budgetType(BudgetType.REQUEST)
                .totalAmount(BigDecimal.ZERO)
                .committedSpend(BigDecimal.ZERO)
                .parentBudget(projBudget)
                .build();
        reqBudget = internalBudgetRepository.save(reqBudget);

        // Assign budget to request
        request.setBudget(reqBudget);
        request = requestRepository.save(request);

        Quote quote = saveTestQuote(Currency.EUR, BigDecimal.valueOf(100));

        // Select the quote first (so committedSpend increases by 100)
        quoteService.selectQuoteForRequest(request.getRequestID(), quote.getQuoteID());

        // Verify initial committedSpend
        InternalBudget updatedReqBudget = internalBudgetRepository.findById(reqBudget.getId()).orElseThrow();
        assertEquals(0, updatedReqBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(100)));

        // Update the selected quote to totalAmount 150
        QuoteCreateDto updateDto = new QuoteCreateDto(
                vendor.getId(),
                Currency.EUR,
                BigDecimal.valueOf(140),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(150),
                7,
                List.of(new QuoteLineItemCreateDto("Some Item", 1, BigDecimal.valueOf(140), null))
        );

        quoteService.updateQuoteForRequest(request.getRequestID(), quote.getQuoteID(), updateDto);

        // Verify committed spend reflects the updated quote amount (+50 net change)
        InternalBudget updatedReqBudget2 = internalBudgetRepository.findById(reqBudget.getId()).orElseThrow();
        InternalBudget updatedProjBudget = internalBudgetRepository.findById(projBudget.getId()).orElseThrow();
        InternalBudget updatedDeptBudget = internalBudgetRepository.findById(deptBudget.getId()).orElseThrow();

        assertAll("Committed spend updated after editing selected quote",
                () -> assertEquals(0, updatedReqBudget2.getCommittedSpend().compareTo(BigDecimal.valueOf(150))),
                () -> assertEquals(0, updatedProjBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(350))),
                () -> assertEquals(0, updatedDeptBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(350)))
        );
    }

    //AI-Generated
    @Test
    void DeleteSelectedQuote_SubtractsCommittedSpendAcrossHierarchy() {
        // Create budget hierarchy: Project Budget -> Department Budget
        InternalBudget deptBudget = InternalBudget.builder()
                .budgetName("Department Budget")
                .budgetType(BudgetType.DEPARTMENT)
                .totalAmount(BigDecimal.valueOf(5000))
                .committedSpend(BigDecimal.valueOf(200))
                .build();
        deptBudget = internalBudgetRepository.save(deptBudget);

        InternalBudget projBudget = InternalBudget.builder()
                .budgetName("Project Budget")
                .budgetType(BudgetType.PROJECT)
                .totalAmount(BigDecimal.valueOf(2000))
                .committedSpend(BigDecimal.valueOf(200))
                .parentBudget(deptBudget)
                .build();
        projBudget = internalBudgetRepository.save(projBudget);

        InternalBudget reqBudget = InternalBudget.builder()
                .budgetName("Request Budget")
                .budgetType(BudgetType.REQUEST)
                .totalAmount(BigDecimal.ZERO)
                .committedSpend(BigDecimal.ZERO)
                .parentBudget(projBudget)
                .build();
        reqBudget = internalBudgetRepository.save(reqBudget);

        // Assign budget to request
        request.setBudget(reqBudget);
        request = requestRepository.save(request);

        Quote quote = saveTestQuote(Currency.EUR, BigDecimal.valueOf(100));

        // Select the quote (committedSpend increases by 100)
        quoteService.selectQuoteForRequest(request.getRequestID(), quote.getQuoteID());

        // Verify initial committedSpend
        InternalBudget updatedReqBudget = internalBudgetRepository.findById(reqBudget.getId()).orElseThrow();
        assertEquals(0, updatedReqBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(100)));

        // Delete the quote
        quoteService.deleteQuoteForRequest(request.getRequestID(), quote.getQuoteID());

        // Verify committed spend is decremented by the quote's amount (100)
        InternalBudget updatedReqBudget2 = internalBudgetRepository.findById(reqBudget.getId()).orElseThrow();
        InternalBudget updatedProjBudget = internalBudgetRepository.findById(projBudget.getId()).orElseThrow();
        InternalBudget updatedDeptBudget = internalBudgetRepository.findById(deptBudget.getId()).orElseThrow();

        assertAll("Committed spend decremented after deleting selected quote",
                () -> assertEquals(0, updatedReqBudget2.getCommittedSpend().compareTo(BigDecimal.valueOf(0))),
                () -> assertEquals(0, updatedProjBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(200))),
                () -> assertEquals(0, updatedDeptBudget.getCommittedSpend().compareTo(BigDecimal.valueOf(200)))
        );
    }

    //AI-Generated
    @Test
    void SelectQuote_WithExistingInvoice_DeletesInvoiceAndAttachments() {
        Quote quote1 = saveTestQuote(Currency.EUR, BigDecimal.valueOf(100));
        Quote quote2 = saveTestQuote(Currency.EUR, BigDecimal.valueOf(150));

        // Select first quote initially
        quoteService.selectQuoteForRequest(request.getRequestID(), quote1.getQuoteID());

        // Create and save an invoice associated with the request
        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setVendor(vendor);
        invoice.setInvoiceDate(java.time.LocalDate.now());
        invoice.setCreatedAt(java.time.LocalDateTime.now());
        invoice.setInvoiceNumber("INV-001");
        invoice.setTotalAmount(BigDecimal.valueOf(100));
        invoice.setCurrency(Currency.EUR);
        invoice.setIsPaid(false);
        invoice = invoiceRepository.save(invoice);

        // Update request reference
        request.setInvoice(invoice);
        request = requestRepository.save(request);

        // Add an attachment to the invoice
        Attachment attachment = new Attachment();
        attachment.setFileName("invoice.pdf");
        attachment.setFileType("application/pdf");
        attachment.setFileSize(1234L);
        attachment.setUploadedAt(java.time.LocalDateTime.now());
        attachment.setStoragePath("/tmp/nonexistent-test-invoice.pdf");
        attachment.setRequest(request);
        attachment.setInvoice(invoice);
        attachment = attachmentRepository.save(attachment);

        invoice.setAttachments(List.of(attachment));
        invoice = invoiceRepository.save(invoice);

        // Verify pre-conditions
        assertTrue(invoiceRepository.findById(invoice.getInvoiceId()).isPresent());
        assertTrue(attachmentRepository.findById(attachment.getAttachmentId()).isPresent());

        // Select quote2, which should trigger invoice and attachment deletion
        quoteService.selectQuoteForRequest(request.getRequestID(), quote2.getQuoteID());

        // Verify post-conditions
        Request updatedRequest = requestRepository.findById(request.getRequestID()).orElseThrow();
        assertNull(updatedRequest.getInvoice());
        assertTrue(invoiceRepository.findById(invoice.getInvoiceId()).isEmpty());
        assertTrue(attachmentRepository.findById(attachment.getAttachmentId()).isEmpty());
    }

    //AI-Generated
    @Test
    void SelectQuote_BudgetValidationFails_DoesNotDeleteInvoiceOrAttachments() {
        InternalBudget globalBudget = InternalBudget.builder()
                .budgetName("Global Budget")
                .budgetType(BudgetType.GLOBAL)
                .totalAmount(BigDecimal.valueOf(100))
                .committedSpend(BigDecimal.ZERO)
                .safetyBuffer(BigDecimal.ZERO)
                .build();
        globalBudget = internalBudgetRepository.save(globalBudget);

        InternalBudget deptBudget = InternalBudget.builder()
                .budgetName("Department Budget")
                .budgetType(BudgetType.DEPARTMENT)
                .totalAmount(BigDecimal.valueOf(100))
                .committedSpend(BigDecimal.ZERO)
                .safetyBuffer(BigDecimal.ZERO)
                .parentBudget(globalBudget)
                .build();
        deptBudget = internalBudgetRepository.save(deptBudget);

        InternalBudget projBudget = InternalBudget.builder()
                .budgetName("Project Budget")
                .budgetType(BudgetType.PROJECT)
                .totalAmount(BigDecimal.valueOf(100))
                .committedSpend(BigDecimal.ZERO)
                .safetyBuffer(BigDecimal.ZERO)
                .parentBudget(deptBudget)
                .build();
        projBudget = internalBudgetRepository.save(projBudget);

        InternalBudget reqBudget = InternalBudget.builder()
                .budgetName("Request Budget")
                .budgetType(BudgetType.REQUEST)
                .totalAmount(BigDecimal.ZERO)
                .committedSpend(BigDecimal.ZERO)
                .safetyBuffer(BigDecimal.ZERO)
                .parentBudget(projBudget)
                .build();
        reqBudget = internalBudgetRepository.save(reqBudget);

        request.setBudget(reqBudget);
        request = requestRepository.save(request);

        Quote quote1 = saveTestQuote(Currency.EUR, BigDecimal.valueOf(50));
        Quote quote2 = saveTestQuote(Currency.EUR, BigDecimal.valueOf(150));

        // Select first quote initially (50 EUR)
        quoteService.selectQuoteForRequest(request.getRequestID(), quote1.getQuoteID());

        // Create and save an invoice associated with the request
        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setVendor(vendor);
        invoice.setInvoiceDate(java.time.LocalDate.now());
        invoice.setCreatedAt(java.time.LocalDateTime.now());
        invoice.setInvoiceNumber("INV-001");
        invoice.setTotalAmount(BigDecimal.valueOf(50));
        invoice.setCurrency(Currency.EUR);
        invoice.setIsPaid(false);
        invoice = invoiceRepository.save(invoice);

        request.setInvoice(invoice);
        request = requestRepository.save(request);

        Attachment attachment = new Attachment();
        attachment.setFileName("invoice.pdf");
        attachment.setFileType("application/pdf");
        attachment.setFileSize(1234L);
        attachment.setUploadedAt(java.time.LocalDateTime.now());
        attachment.setStoragePath("/tmp/nonexistent-test-invoice.pdf");
        attachment.setRequest(request);
        attachment.setInvoice(invoice);
        attachment = attachmentRepository.save(attachment);

        invoice.setAttachments(List.of(attachment));
        invoice = invoiceRepository.save(invoice);

        // Verify pre-conditions
        assertTrue(invoiceRepository.findById(invoice.getInvoiceId()).isPresent());
        assertTrue(attachmentRepository.findById(attachment.getAttachmentId()).isPresent());

        // Selecting quote2 (150 EUR) exceeds budget limit (100 EUR), should throw WorkflowStateException
        final Long reqId = request.getRequestID();
        final Long q2Id = quote2.getQuoteID();
        assertThrows(com.veritas.backend.common.exception.WorkflowStateException.class, () -> {
            quoteService.selectQuoteForRequest(reqId, q2Id);
        });

        // Verify that the invoice and attachment still exist (transaction rolled back, file not deleted)
        Request updatedRequest = requestRepository.findById(request.getRequestID()).orElseThrow();
        assertNotNull(updatedRequest.getInvoice());
        assertTrue(invoiceRepository.findById(invoice.getInvoiceId()).isPresent());
        assertTrue(attachmentRepository.findById(attachment.getAttachmentId()).isPresent());
    }
}
