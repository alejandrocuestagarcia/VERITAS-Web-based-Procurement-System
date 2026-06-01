package com.veritas.backend.requisition;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
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
import com.veritas.backend.vendor.entity.Currency;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
        request.setTeamID(team);
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
    }

    @Test
    void QuoteCreation_ValidInput_PersistsInDatabase() {
        QuoteCreateDto createDto = new QuoteCreateDto(
                vendor.getId(),
                Currency.EUR,
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(215),
                List.of(new QuoteLineItemCreateDto("Integration Item", 2, BigDecimal.valueOf(100), null))
        );

        QuoteDto result = quoteService.createQuoteForRequest(request.getRequestID(), createDto);

        assertThat(result).isNotNull();
        assertThat(result.quoteId()).isNotNull();
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(215));

        Optional<Quote> persistedQuote = quoteRepository.findById(result.quoteId());
        assertThat(persistedQuote).isPresent();
        assertThat(persistedQuote.get().getCurrency()).isEqualTo(Currency.EUR);

        List<QuoteLineItem> persistedItems = quoteLineItemRepository.findByQuoteQuoteID(result.quoteId());
        assertThat(persistedItems).hasSize(1);
        assertThat(persistedItems.getFirst().getProductDescription()).isEqualTo("Integration Item");
    }

    @Test
    void FindQuotes_ForRequest_ReturnsAllQuotes() {
        Quote quote1 = saveTestQuote(Currency.EUR, BigDecimal.valueOf(100));
        Quote quote2 = saveTestQuote(Currency.USD, BigDecimal.valueOf(150));

        List<QuoteDto> result = quoteService.getQuotesForRequest(request.getRequestID());

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(QuoteDto::quoteId)).containsExactlyInAnyOrder(quote1.getQuoteID(), quote2.getQuoteID());
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
                List.of(new QuoteLineItemCreateDto("Updated Item", 1, BigDecimal.valueOf(120), null))
        );

        QuoteDto result = quoteService.updateQuoteForRequest(request.getRequestID(), quote.getQuoteID(), updateDto);

        assertThat(result.currency()).isEqualTo(Currency.USD);
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(125));

        Quote persisted = quoteRepository.findById(quote.getQuoteID()).orElseThrow();
        assertThat(persisted.getCurrency()).isEqualTo(Currency.USD);
        assertThat(persisted.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(125));

        List<QuoteLineItem> persistedItems = quoteLineItemRepository.findByQuoteQuoteID(quote.getQuoteID());
        assertThat(persistedItems).hasSize(1);
        assertThat(persistedItems.getFirst().getProductDescription()).isEqualTo("Updated Item");
    }

    @Test
    void SelectQuote_ValidQuoteId_SetsPreferredAndClearsOthers() {
        Quote quote1 = saveTestQuote(Currency.EUR, BigDecimal.valueOf(100));
        Quote quote2 = saveTestQuote(Currency.USD, BigDecimal.valueOf(150));

        quoteService.selectQuoteForRequest(request.getRequestID(), quote1.getQuoteID());

        Quote persisted1 = quoteRepository.findById(quote1.getQuoteID()).orElseThrow();
        Quote persisted2 = quoteRepository.findById(quote2.getQuoteID()).orElseThrow();

        assertThat(persisted1.isSelected()).isTrue();
        assertThat(persisted2.isSelected()).isFalse();

        // Select the second one, should clear the first
        quoteService.selectQuoteForRequest(request.getRequestID(), quote2.getQuoteID());

        persisted1 = quoteRepository.findById(quote1.getQuoteID()).orElseThrow();
        persisted2 = quoteRepository.findById(quote2.getQuoteID()).orElseThrow();

        assertThat(persisted1.isSelected()).isFalse();
        assertThat(persisted2.isSelected()).isTrue();
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

        assertThat(quoteRepository.findById(quote.getQuoteID())).isEmpty();
        assertThat(quoteLineItemRepository.findByQuoteQuoteID(quote.getQuoteID())).isEmpty();
    }

    private Quote saveTestQuote(Currency currency, BigDecimal totalAmount) {
        Quote quote = new Quote();
        quote.setRequest(request);
        quote.setVendorID(vendor);
        quote.setCurrency(currency);
        quote.setBaseAmount(totalAmount);
        quote.setShippingCosts(BigDecimal.ZERO);
        quote.setTotalAmount(totalAmount);
        quote.setSelected(false);
        return quoteRepository.save(quote);
    }
}
