package com.veritas.backend.requisition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.veritas.backend.requisition.dto.*;
import com.veritas.backend.requisition.service.RequisitionService;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.integrations.currency.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RequisitionControllerUnitTest {

    @Mock
    private RequisitionService requisitionService;

    @InjectMocks
    private RequisitionController requisitionController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@veritas.com");
    }

    @Test
    void createRequest_ValidInput_ReturnsCreated() {
        RequisitionCreateDto createDto = mock(RequisitionCreateDto.class);
        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionService.createRequest(createDto, testUser)).thenReturn(expectedDto);

        ResponseEntity<RequisitionDto> response = requisitionController.createRequest(createDto, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).createRequest(createDto, testUser);
    }

    @Test
    void getRequests_ReturnsPageOfRequisitions() {
        Page<RequisitionDto> expectedPage = Page.empty();
        PageRequest expectedPageRequest = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt").nullsLast()));
        when(requisitionService.getRequests("ACTIVE", "search", 1L, null, null, null, testUser, expectedPageRequest))
                .thenReturn(expectedPage);

        Page<RequisitionDto> result = requisitionController.getRequests("ACTIVE", "search", 1L, null, null, null, 0, 10, testUser);

        assertNotNull(result);
        verify(requisitionService).getRequests("ACTIVE", "search", 1L, null, null, null, testUser, expectedPageRequest);
    }

    @Test
    void getRequestById_ValidId_ReturnsRequisitionDto() {
        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionService.getRequestById(1L, testUser)).thenReturn(expectedDto);

        RequisitionDto result = requisitionController.getRequestById(1L, testUser);

        assertNotNull(result);
        assertEquals(expectedDto, result);
        verify(requisitionService).getRequestById(1L, testUser);
    }

    @Test
    void updateRequest_ValidInput_ReturnsOk() {
        RequisitionUpdateDto updateDto = mock(RequisitionUpdateDto.class);
        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionService.updateRequest(1L, updateDto, testUser)).thenReturn(expectedDto);

        ResponseEntity<RequisitionDto> response = requisitionController.updateRequest(1L, updateDto, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).updateRequest(1L, updateDto, testUser);
    }

    @Test
    void changeRequester_ValidInput_ReturnsOk() {
        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionService.changeRequester(1L, 2L, testUser)).thenReturn(expectedDto);

        ResponseEntity<RequisitionDto> response = requisitionController.changeRequester(1L, 2L, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).changeRequester(1L, 2L, testUser);
    }

    @Test
    void submitRequest_ValidInput_ReturnsOk() {
        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionService.submitRequest(1L, testUser, 2L)).thenReturn(expectedDto);

        ResponseEntity<RequisitionDto> response = requisitionController.submitRequest(1L, testUser, 2L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).submitRequest(1L, testUser, 2L);
    }

    @Test
    void approveRequest_ValidInput_ReturnsOk() {
        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionService.approveRequest(1L, testUser, 2L)).thenReturn(expectedDto);

        ResponseEntity<RequisitionDto> response = requisitionController.approveRequest(1L, testUser, 2L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).approveRequest(1L, testUser, 2L);
    }

    @Test
    void revertRequest_ValidInput_ReturnsOk() {
        RequisitionRejectDto rejectDto = mock(RequisitionRejectDto.class);
        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionService.revertRequest(1L, testUser, rejectDto)).thenReturn(expectedDto);

        ResponseEntity<RequisitionDto> response = requisitionController.revertRequest(1L, testUser, rejectDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).revertRequest(1L, testUser, rejectDto);
    }

    @Test
    void rejectRequest_ValidInput_ReturnsOk() {
        RequisitionRejectDto rejectDto = mock(RequisitionRejectDto.class);
        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionService.rejectRequest(1L, testUser, rejectDto)).thenReturn(expectedDto);

        ResponseEntity<RequisitionDto> response = requisitionController.rejectRequest(1L, testUser, rejectDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).rejectRequest(1L, testUser, rejectDto);
    }

    @Test
    void cancelRequest_ValidId_ReturnsOk() {
        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionService.cancelRequest(1L, testUser)).thenReturn(expectedDto);

        ResponseEntity<RequisitionDto> response = requisitionController.cancelRequest(1L, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).cancelRequest(1L, testUser);
    }

    @Test
    void getNextStepRole_RoleNotNull_ReturnsQuotedRole() {
        when(requisitionService.getNextStepRole(1L, testUser)).thenReturn("ROLE_APPROVER");

        ResponseEntity<String> response = requisitionController.getNextStepRole(1L, testUser);

        assertNotNull(response);
        assertEquals("\"ROLE_APPROVER\"", response.getBody());
        verify(requisitionService).getNextStepRole(1L, testUser);
    }

    @Test
    void getNextStepRole_RoleNull_ReturnsEmptyQuotes() {
        when(requisitionService.getNextStepRole(1L, testUser)).thenReturn(null);

        ResponseEntity<String> response = requisitionController.getNextStepRole(1L, testUser);

        assertNotNull(response);
        assertEquals("\"\"", response.getBody());
        verify(requisitionService).getNextStepRole(1L, testUser);
    }

    @Test
    void getEligibleAssignees_ValidRequest_ReturnsList() {
        UserDto mockUserDto = mock(UserDto.class);
        List<UserDto> expectedList = List.of(mockUserDto);
        when(requisitionService.getEligibleAssignees(1L, "ROLE_APPROVER", testUser)).thenReturn(expectedList);

        ResponseEntity<List<UserDto>> response = requisitionController.getEligibleAssignees(1L, "ROLE_APPROVER", testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedList, response.getBody());
        verify(requisitionService).getEligibleAssignees(1L, "ROLE_APPROVER", testUser);
    }

    @Test
    void canAct_ValidRequest_ReturnsBoolean() {
        when(requisitionService.canAct(1L, testUser)).thenReturn(true);

        ResponseEntity<Boolean> response = requisitionController.canAct(1L, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody());
        verify(requisitionService).canAct(1L, testUser);
    }

    @Test
    void uploadAttachment_ValidFile_ReturnsOk() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");

        ResponseEntity<String> response = requisitionController.uploadAttachment(1L, mockFile, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("\"File test.txt uploaded for request 1\"", response.getBody());
        verify(requisitionService).saveAttachment(1L, mockFile, testUser);
    }

    @Test
    void downloadAttachment_ValidId_ReturnsResourceResponse() {
        ResponseEntity<Resource> expectedResponse = ResponseEntity.ok(mock(Resource.class));
        when(requisitionService.downloadAttachment(1L, testUser)).thenReturn(expectedResponse);

        ResponseEntity<Resource> response = requisitionController.downloadAttachment(1L, testUser);

        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(requisitionService).downloadAttachment(1L, testUser);
    }

    @Test
    void deleteAttachment_ValidId_ReturnsNoContent() {
        ResponseEntity<Void> response = requisitionController.deleteAttachment(1L, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(requisitionService).deleteAttachment(1L, testUser);
    }

    @Test
    void processPayment_ValidId_ReturnsNoContent() {
        ResponseEntity<Void> response = requisitionController.processPayment(1L, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(requisitionService).processPayment(1L, testUser);
    }

    @Test
    void createInvoice_ValidInput_ReturnsCreated() {
        InvoiceCreateDto invoiceData = new InvoiceCreateDto();
        invoiceData.setInvoiceNumber("INV-001");
        invoiceData.setTotalAmount(BigDecimal.TEN);
        invoiceData.setCurrency(Currency.EUR);
        invoiceData.setInvoiceDate(LocalDate.now());
        invoiceData.setDueDate(LocalDate.now().plusDays(30));

        MultipartFile mockFile = mock(MultipartFile.class);
        InvoiceDto expectedDto = InvoiceDto.builder()
                .invoiceId(1L)
                .invoiceNumber("INV-001")
                .totalAmount(BigDecimal.TEN)
                .currency(Currency.EUR)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .build();
        when(requisitionService.createInvoice(1L, invoiceData, mockFile, testUser)).thenReturn(expectedDto);

        ResponseEntity<InvoiceDto> response = requisitionController.createInvoice(1L, invoiceData, mockFile, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).createInvoice(1L, invoiceData, mockFile, testUser);
    }

    @Test
    void updateInvoice_ValidInput_ReturnsOk() {
        InvoiceCreateDto invoiceData = new InvoiceCreateDto();
        invoiceData.setInvoiceNumber("INV-002");
        invoiceData.setTotalAmount(BigDecimal.TEN);
        invoiceData.setCurrency(Currency.EUR);
        invoiceData.setInvoiceDate(LocalDate.now());
        invoiceData.setDueDate(LocalDate.now().plusDays(30));

        MultipartFile mockFile = mock(MultipartFile.class);
        InvoiceDto expectedDto = InvoiceDto.builder()
                .invoiceId(1L)
                .invoiceNumber("INV-002")
                .totalAmount(BigDecimal.TEN)
                .currency(Currency.EUR)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .build();
        when(requisitionService.updateInvoice(1L, invoiceData, mockFile, testUser)).thenReturn(expectedDto);

        ResponseEntity<InvoiceDto> response = requisitionController.updateInvoice(1L, invoiceData, mockFile, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).updateInvoice(1L, invoiceData, mockFile, testUser);
    }

    @Test
    void getInvoice_ValidId_ReturnsOk() {
        InvoiceDto expectedDto = InvoiceDto.builder().invoiceId(1L).build();
        when(requisitionService.getInvoice(1L, testUser)).thenReturn(expectedDto);

        ResponseEntity<InvoiceDto> response = requisitionController.getInvoice(1L, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDto, response.getBody());
        verify(requisitionService).getInvoice(1L, testUser);
    }

    @Test
    void deleteInvoice_ValidId_ReturnsNoContent() {
        ResponseEntity<Void> response = requisitionController.deleteInvoice(1L, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(requisitionService).deleteInvoice(1L, testUser);
    }
}

