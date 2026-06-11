package com.veritas.backend.requisition;

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

    private Request request;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        cleanAllData();

        Department department = new Department();
        department.setName("IT");
        department = departmentRepository.save(department);

        Team team = new Team();
        team.setName("Testing Team");
        team.setDescription("We do tests");
        team.setDepartment(department);
        team = teamRepository.save(team);

        request = new Request();
        request.setRequestName("Integration Request");
        request.setTeam(team);
        request = requestRepository.save(request);

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
        jdbcTemplate.update("UPDATE departments SET budget_id = NULL");
        jdbcTemplate.update("UPDATE projects SET budget_id = NULL");

        quoteLineItemRepository.deleteAll();
        quoteRepository.deleteAll();
        requestRepository.deleteAll();

        projectRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM internal_budgets");

        teamRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        vendorRepository.deleteAll();
        exchangeRateRepository.deleteAll();
    }

    @Test
    void QuoteCreation_ValidInput_PersistsInDatabase() {
        QuoteCreateDto createDto = new QuoteCreateDto(
                vendor.getId(),
                Currency.EUR,
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(215),
                5,
                List.of(new QuoteLineItemCreateDto("Integration Item", 2, BigDecimal.valueOf(100), null))
        );

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

        QuoteCreateDto updateDto = new QuoteCreateDto(
                vendor.getId(),
                Currency.USD,
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(125),
                5,
                List.of(new QuoteLineItemCreateDto("Updated Item", 1, BigDecimal.valueOf(120), null))
        );

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
}
