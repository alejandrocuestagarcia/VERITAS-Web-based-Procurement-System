package com.veritas.backend.requisition.service;

import com.veritas.backend.common.exception.WorkflowStateException;
import com.veritas.backend.requisition.dto.InvoiceCreateDto;
import com.veritas.backend.requisition.dto.InvoiceDto;
import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.dto.RequisitionRejectDto;
import com.veritas.backend.requisition.dto.RequisitionUpdateDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.entity.User;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RequisitionService {

    /**
     * Creates a new requisition in DRAFT state with an initialized budget, assigned to the authenticated user.
     *
     * @param createDto the creation payload
     * @param user the authenticated user creating the request
     * @return the created {@link RequisitionDto}
     * @throws IllegalArgumentException if the user, project, or workflow is not found, or the workflow has no START_EVENT step
     */
    RequisitionDto createRequest(RequisitionCreateDto createDto, User user);

    /**
     * Stores an uploaded file as an attachment on the given request.
     *
     * @param requestId the ID of the request to attach the file to
     * @param file the uploaded file
     * @throws IllegalArgumentException if the request is not found
     */
    void saveAttachment(Long requestId, MultipartFile file, User actor);

    /**
     * Stores a file from an input stream as an attachment on the given request.
     * Used when importing attachments from external sources such as Jira.
     *
     * @param requestId the ID of the request to attach the file to
     * @param originalFilename the original filename
     * @param contentType the MIME type of the file
     * @param size the file size in bytes
     * @param inputStream the file content
     * @throws IllegalArgumentException if the request is not found
     */
    void saveAttachmentFromInputStream(Long requestId, String originalFilename, String contentType, long size, java.io.InputStream inputStream);

    /**
     * Returns a paginated, filtered list of requests visible to the authenticated user.
     * Requesters see only their own or their team's requests; procurement officers see their department's requests;
     * all other roles see everything.
     *
     * @param status optional status filter
     * @param search optional search string
     * @param projectId optional project filter
     * @param authUser the authenticated user
     * @param pageable pagination and sorting parameters
     * @return a page of {@link RequisitionDto}
     */
    Page<RequisitionDto> getRequests(String status, String search, Long projectId, User authUser, Pageable pageable);

    /**
     * Returns a single request by ID.
     *
     * @param id the request ID
     * @return the matching {@link RequisitionDto}
     * @throws EntityNotFoundException if no request exists with the given ID
     */
    RequisitionDto getRequestById(Long id, User actor);

    /**
     * Returns an attachment file as a downloadable HTTP response.
     *
     * @param attachmentId the ID of the attachment
     * @return a {@link ResponseEntity} containing the file resource
     * @throws EntityNotFoundException if the attachment is not found
     * @throws RuntimeException if the file cannot be read from disk
     */
    ResponseEntity<Resource> downloadAttachment(Long attachmentId, User actor);

    /**
     * Deletes an attachment and its file from disk.
     * Access is restricted based on the authenticated user's role and request ownership.
     *
     * @param attachmentId the ID of the attachment to delete
     * @throws EntityNotFoundException if the attachment is not found
     * @throws AccessDeniedException if the user is not permitted to access the request
     */
    void deleteAttachment(Long attachmentId, User actor);

    /**
     * Advances a request to its next workflow step and notifies relevant users.
     * If the request reaches FINISHED state, finance officers are also notified.
     *
     * @param id the request ID
     * @param actor the user performing the approval
     * @param nextAssigneeId the ID of the user to assign at the next step, if applicable
     * @return the updated {@link RequisitionDto}
     * @throws WorkflowStateException if the request is already finished, in draft, or already paid
     */
    RequisitionDto approveRequest(Long id, User actor, Long nextAssigneeId);

    /**
     * Sends a request back to the previous workflow step with an optional reason and revision flag.
     *
     * @param id the request ID
     * @param actor the user performing the revert
     * @param rejectionData the revert payload containing the reason and revision flag
     * @return the updated {@link RequisitionDto}
     * @throws WorkflowStateException if the request is finished, in draft, or already paid
     */
    RequisitionDto revertRequest(Long id, User actor, RequisitionRejectDto rejectionData);

    /**
     * Permanently rejects a request, marking it as FINISHED with a rejection reason and deletion timestamp.
     *
     * @param id the request ID
     * @param actor the user performing the rejection
     * @param rejectionData the rejection payload containing the reason
     * @return the updated {@link RequisitionDto}
     * @throws WorkflowStateException if the request is already finished or paid
     * @throws AccessDeniedException if the actor is not authorized to act on the current step
     */
    RequisitionDto rejectRequest(Long id, User actor, RequisitionRejectDto rejectionData);

    /**
     * Cancels a request. Only the original requester may cancel their own request, and only if it is in DRAFT
     * or the current step is assigned to them.
     *
     * @param id the request ID
     * @param actor the user performing the cancellation
     * @return the updated {@link RequisitionDto}
     * @throws WorkflowStateException if the request is finished or the actor is not permitted to cancel
     * @throws AccessDeniedException if the actor is not the original requester
     */
    RequisitionDto cancelRequest(Long id, User actor);

    /**
     * Submits a DRAFT request, transitioning it to ACTIVE and starting the workflow.
     *
     * @param id the request ID
     * @param actor the user submitting the request
     * @param nextAssigneeId the ID of the initial assignee
     * @return the updated {@link RequisitionDto}
     * @throws WorkflowStateException if the request is not in DRAFT state
     */
    RequisitionDto submitRequest(Long id, User actor, Long nextAssigneeId);

    /**
     * Reassigns a request to a different requester from the same team.
     * Notifies both the old and new requester and enqueues a Jira sync if applicable.
     *
     * @param id the request ID
     * @param newRequesterId the ID of the new requester
     * @return the updated {@link RequisitionDto}
     * @throws WorkflowStateException if the request is finished
     * @throws IllegalArgumentException if the new requester is not a REQUESTER from the same team
     */
    RequisitionDto changeRequester(Long id, Long newRequesterId);

    /**
     * Returns the role required to act at the next workflow step, or {@code null} if the next step is automated.
     *
     * @param id the request ID
     * @return the role name, or {@code null} if no manual assignee is needed
     */
    String getNextStepRole(Long id, User actor);

    /**
     * Returns the list of active users eligible to be assigned at the next workflow step,
     * filtered by role and department or team scope where applicable.
     *
     * @param id the request ID
     * @param roleName the name of the role required at the next step
     * @return a list of eligible {@link UserDto}
     */
    List<UserDto> getEligibleAssignees(Long id, String roleName, User actor);

    /**
     * Returns whether the given actor is authorized to act on the request's current workflow step.
     *
     * @param id the request ID
     * @param actor the user to check
     * @return {@code true} if the actor may act, {@code false} otherwise
     */
    boolean canAct(Long id, User actor);

    /**
     * Updates an editable request. Only allowed for requests in DRAFT state or flagged for revision,
     * and only by the original requester. If line items change, all associated quotes are also deleted.
     *
     * @param id the request ID
     * @param updates the update payload
     * @param actor the authenticated user performing the update
     * @return the updated {@link RequisitionDto}
     * @throws WorkflowStateException if the request is not editable
     * @throws AccessDeniedException if the actor is not the original requester
     */
    RequisitionDto updateRequest(Long id, RequisitionUpdateDto updates, User actor);

    /**
     * Processes payment for a request by committing the invoice amount to the budget hierarchy,
     * marking the invoice as paid, and transitioning the request to FINISHED.
     * Triggers a Jira sync if the request is linked to a Jira issue.
     *
     * @param requestId the request ID
     * @param actor the user processing the payment
     * @throws EntityNotFoundException if the request or invoice is not found
     */
    void processPayment(Long requestId, User actor);

    /**
     * Creates an invoice for a request, requiring a selected vendor quote.
     * Optionally attaches an uploaded file to the invoice.
     *
     * @param requestId the request ID
     * @param createDto the invoice creation payload
     * @param file an optional invoice file to attach
     * @return the created {@link InvoiceDto}
     * @throws EntityExistsException if an invoice already exists for the request
     * @throws IllegalStateException if no vendor quote has been selected
     */
    InvoiceDto createInvoice(Long requestId, InvoiceCreateDto createDto, MultipartFile file, User actor);

    /**
     * Returns the invoice for a given request, including EUR-converted total where available.
     *
     * @param requestId the request ID
     * @return the {@link InvoiceDto}
     * @throws EntityNotFoundException if the request or invoice is not found
     */
    InvoiceDto getInvoice(Long requestId, User actor);

    /**
     * Deletes an unpaid invoice and its associated attachments from disk and database.
     * Access is restricted to procurement officers within the request's department.
     *
     * @param requestId the request ID
     * @param user the authenticated user performing the deletion
     * @throws EntityNotFoundException if the request or invoice is not found
     * @throws IllegalStateException if the invoice has already been paid
     * @throws AccessDeniedException if the user is not permitted to access the request
     */
    void deleteInvoice(Long requestId, User user);
    void checkRequestAccess(Long requestId, User user);
}
