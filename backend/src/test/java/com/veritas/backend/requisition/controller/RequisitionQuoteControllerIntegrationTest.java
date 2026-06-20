package com.veritas.backend.requisition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import com.veritas.backend.requisition.dto.QuoteCreateDto;
import com.veritas.backend.requisition.dto.QuoteDto;
import com.veritas.backend.requisition.service.RequisitionQuoteService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RequisitionQuoteControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequisitionQuoteService requisitionQuoteService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void GetQuotesForRequest_AsProcurementOfficer_ReturnsQuotesList() throws Exception {
        QuoteDto quoteDto = QuoteDto.builder()
                .quoteId(10L)
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100))
                .totalAmountEuro(BigDecimal.valueOf(100))
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(requisitionQuoteService.getQuotesForRequest(1L)).thenReturn(List.of(quoteDto));

        mockMvc.perform(get("/api/v1/requisitions/1/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quoteId").value(10L))
                .andExpect(jsonPath("$[0].currency").value("EUR"));
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void GetQuotesForRequest_AsRequester_ReturnsQuotesList() throws Exception {
        QuoteDto quoteDto = QuoteDto.builder()
                .quoteId(10L)
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100))
                .totalAmountEuro(BigDecimal.valueOf(100))
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(requisitionQuoteService.getQuotesForRequest(1L)).thenReturn(List.of(quoteDto));

        mockMvc.perform(get("/api/v1/requisitions/1/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quoteId").value(10L));
    }

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void GetQuote_AsProcurementOfficer_ReturnsQuote() throws Exception {
        QuoteDto quoteDto = QuoteDto.builder()
                .quoteId(10L)
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100))
                .totalAmountEuro(BigDecimal.valueOf(100))
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(requisitionQuoteService.getQuoteById(1L, 10L)).thenReturn(quoteDto);

        mockMvc.perform(get("/api/v1/requisitions/1/quotes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId").value(10L));
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void GetQuote_AsRequester_ReturnsQuote() throws Exception {
        QuoteDto quoteDto = QuoteDto.builder()
                .quoteId(10L)
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100))
                .totalAmountEuro(BigDecimal.valueOf(100))
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(requisitionQuoteService.getQuoteById(1L, 10L)).thenReturn(quoteDto);

        mockMvc.perform(get("/api/v1/requisitions/1/quotes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId").value(10L));
    }

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void CreateQuote_AsProcurementOfficer_ReturnsCreated() throws Exception {
        QuoteCreateDto createDto = QuoteCreateDto.builder()
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100))
                .shippingTime(5)
                .items(List.of())
                .build();

        QuoteDto quoteDto = QuoteDto.builder()
                .quoteId(10L)
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100))
                .totalAmountEuro(BigDecimal.valueOf(100))
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(requisitionQuoteService.createQuoteForRequest(eq(1L), any(QuoteCreateDto.class))).thenReturn(quoteDto);

        mockMvc.perform(post("/api/v1/requisitions/1/quotes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quoteId").value(10L));
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void CreateQuote_AsRequester_ReturnsForbidden() throws Exception {
        QuoteCreateDto createDto = QuoteCreateDto.builder()
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(100))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(100))
                .shippingTime(5)
                .items(List.of())
                .build();

        mockMvc.perform(post("/api/v1/requisitions/1/quotes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void UpdateQuote_AsProcurementOfficer_ReturnsUpdated() throws Exception {
        QuoteCreateDto updateDto = QuoteCreateDto.builder()
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(120))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(120))
                .shippingTime(5)
                .items(List.of())
                .build();

        QuoteDto quoteDto = QuoteDto.builder()
                .quoteId(10L)
                .vendorId(2L)
                .currency(Currency.EUR)
                .baseAmount(BigDecimal.valueOf(120))
                .shippingCosts(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(120))
                .totalAmountEuro(BigDecimal.valueOf(100))
                .shippingTime(5)
                .isSelected(false)
                .items(List.of())
                .build();

        when(requisitionQuoteService.updateQuoteForRequest(eq(1L), eq(10L), any(QuoteCreateDto.class))).thenReturn(quoteDto);

        mockMvc.perform(put("/api/v1/requisitions/1/quotes/10")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseAmount").value(120.0));
    }

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void DeleteQuote_AsProcurementOfficer_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/requisitions/1/quotes/10")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(requisitionQuoteService).deleteQuoteForRequest(1L, 10L);
    }

    @Test
    @WithMockUser(roles = "PROCUREMENT_OFFICER")
    void SelectQuote_AsProcurementOfficer_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/requisitions/1/quotes/10/select")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(requisitionQuoteService).selectQuoteForRequest(1L, 10L);
    }
}
