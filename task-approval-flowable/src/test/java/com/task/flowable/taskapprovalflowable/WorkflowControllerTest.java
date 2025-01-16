package com.task.flowable.taskapprovalflowable;


import com.task.flowable.taskapprovalflowable.controller.WorkflowController;
import com.task.flowable.taskapprovalflowable.dto.WorkflowDTO;
import com.task.flowable.taskapprovalflowable.exception.InvalidStatusException;
import com.task.flowable.taskapprovalflowable.exception.ProcessingException;
import com.task.flowable.taskapprovalflowable.exception.RecordNotFoundException;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest
class WorkflowControllerTest {

    @Autowired
    private WorkflowController workflowController;

    @Test
    @Deployment(resources = { "processes/task-approval-process.bpmn20.xml" })
    void testProcessFlowInWorkflowController() {

        // 1. Start the process with the given record id and no variables
        var responseStatus = workflowController.startOrUpdateRecordState(8982L,null);

        //Check if the response status is 201 CREATED
        assertEquals(201, responseStatus.getStatusCode().value(), "The response status must be 201 CREATED");

        // Get the current state of the record
        var response = workflowController.getRecordState(8982L);

        // The expected workflow state and the document state must be DRAFTED
        assertEquals(String.valueOf("DRAFTED"), String.valueOf(response.getBody().get("workflowState")), "The workflow state must be DRAFTED");
        assertEquals(String.valueOf("DRAFTED"), String.valueOf(response.getBody().get("state")), "The document state must be DRAFTED");

        // 2. Update the record state to DOCUMENT_READY_FOR_REVIEW
        WorkflowDTO workflowDTO = WorkflowDTO.builder()
                .workflowState(WorkflowDTO.WorkflowState.DOCUMENT_READY_FOR_REVIEW)
                .state(WorkflowDTO.State.DRAFTED)
                .build();

        responseStatus = workflowController.startOrUpdateRecordState(8982L,workflowDTO);

        // Check if the response status is 200 OK
        assertEquals(200, responseStatus.getStatusCode().value(), "The response status must be 200 OK");

        // Get the current state of the record
        response = workflowController.getRecordState(8982L);

        // The expected workflow state and the document state must be DOCUMENT_READY_FOR_REVIEW and DRAFTED
        assertEquals(String.valueOf("DOCUMENT_READY_FOR_REVIEW"), String.valueOf(response.getBody().get("workflowState")), "The workflow state must be DOCUMENT_READY_FOR_REVIEW");
        assertEquals(String.valueOf("DRAFTED"), String.valueOf(response.getBody().get("state")), "The document state must be DRAFTED");

        // 3. Update the record state to REVIEW_ACCEPTED
        workflowDTO = WorkflowDTO.builder()
                .workflowState(WorkflowDTO.WorkflowState.REVIEW_ACCEPTED)
                .state(WorkflowDTO.State.DRAFTED)
                .build();

        responseStatus = workflowController.startOrUpdateRecordState(8982L,workflowDTO);

        // Check if the response status is 200 OK
        assertEquals(200, responseStatus.getStatusCode().value(), "The response status must be 200 OK");

        // Get the current state of the record
        response = workflowController.getRecordState(8982L);

        // The expected workflow state and the document state must be REVIEW_ACCEPTED and REVIEWED
        assertEquals(String.valueOf("REVIEW_ACCEPTED"), String.valueOf(response.getBody().get("workflowState")));
        assertEquals(String.valueOf("REVIEWED"), String.valueOf(response.getBody().get("state")));

        // 4. Update the record state to APPROVAL_ACCEPTED
        workflowDTO = WorkflowDTO.builder()
                .workflowState(WorkflowDTO.WorkflowState.APPROVAL_ACCEPTED)
                .state(WorkflowDTO.State.REVIEWED)
                .build();

        responseStatus = workflowController.startOrUpdateRecordState(8982L,workflowDTO);

        // Check if the response status is 200 OK
        assertEquals(200, responseStatus.getStatusCode().value(), "The response status must be 200 OK");

        // Get the current state of the record
        response = workflowController.getRecordState(8982L);

        // The expected workflow state and the document state must be APPROVAL_ACCEPTED and SIGNED
        assertEquals(String.valueOf("APPROVAL_ACCEPTED"), String.valueOf(response.getBody().get("workflowState")), "The workflow state must be APPROVAL_ACCEPTED");
        assertEquals(String.valueOf("SIGNED"), String.valueOf(response.getBody().get("state")), "The document state must be SIGNED");
    }

    //Negative Test Case 1: After REVIEW_REJECTED - The workflow state should again move to Draft
    @Test
    @Deployment(resources = { "processes/task-approval-process.bpmn20.xml" })
    void reviewRejectedFlow() {

        Long recordId = 8977L;

        var responseStatus = workflowController.startOrUpdateRecordState(recordId,null);
        assertEquals(201, responseStatus.getStatusCode().value(), "The response status must be 201 CREATED");

        var response = workflowController.getRecordState(recordId);

        assertEquals(String.valueOf("DRAFTED"), String.valueOf(response.getBody().get("workflowState")));

        WorkflowDTO workflowDTO = WorkflowDTO.builder()
                .workflowState(WorkflowDTO.WorkflowState.DOCUMENT_READY_FOR_REVIEW)
                .state(WorkflowDTO.State.DRAFTED)
                .build();

        responseStatus = workflowController.startOrUpdateRecordState(recordId,workflowDTO);
        assertEquals(200, responseStatus.getStatusCode().value(), "The response status must be 200 OK");

        response = workflowController.getRecordState(recordId);

        assertEquals(String.valueOf("DOCUMENT_READY_FOR_REVIEW"), String.valueOf(response.getBody().get("workflowState")), "The workflow state must be DOCUMENT_READY_FOR_REVIEW");
        assertEquals(String.valueOf("DRAFTED"), String.valueOf(response.getBody().get("state")), "The document state must be DRAFTED");

        workflowDTO = WorkflowDTO.builder()
                .workflowState(WorkflowDTO.WorkflowState.REVIEW_REJECTED)
                .state(WorkflowDTO.State.DRAFTED)
                .build();

        responseStatus = workflowController.startOrUpdateRecordState(recordId,workflowDTO);
        assertEquals(200, responseStatus.getStatusCode().value(), "The response status must be 200 OK");

        response = workflowController.getRecordState(recordId);

        assertEquals(String.valueOf("REVIEW_REJECTED"), String.valueOf(response.getBody().get("workflowState")));
        assertEquals(String.valueOf("DRAFTED"), String.valueOf(response.getBody().get("state")));

    }

    //After APPROVAL_REJECTED - The workflow state should again move to Draft
    @Test
    @Deployment(resources = { "processes/task-approval-process.bpmn20.xml" })
    void approvalRejectedFlow() {Long recordId = 8976L;

        workflowController.startOrUpdateRecordState(recordId,null);

        var response = workflowController.getRecordState(recordId);

        assertEquals(String.valueOf("DRAFTED"), String.valueOf(response.getBody().get("workflowState")));

        WorkflowDTO workflowDTO = WorkflowDTO.builder()
            .workflowState(WorkflowDTO.WorkflowState.DOCUMENT_READY_FOR_REVIEW)
            .state(WorkflowDTO.State.DRAFTED)
            .build();

        workflowController.startOrUpdateRecordState(recordId,workflowDTO);

        response = workflowController.getRecordState(recordId);

        assertEquals(String.valueOf("DOCUMENT_READY_FOR_REVIEW"), String.valueOf(response.getBody().get("workflowState")));
        assertEquals(String.valueOf("DRAFTED"), String.valueOf(response.getBody().get("state")));

        workflowDTO = WorkflowDTO.builder()
            .workflowState(WorkflowDTO.WorkflowState.REVIEW_ACCEPTED)
            .state(WorkflowDTO.State.DRAFTED)
            .build();

        workflowController.startOrUpdateRecordState(recordId,workflowDTO);

        response = workflowController.getRecordState(recordId);

        assertEquals(String.valueOf("REVIEW_ACCEPTED"), String.valueOf(response.getBody().get("workflowState")));
        assertEquals(String.valueOf("REVIEWED"), String.valueOf(response.getBody().get("state")));


        workflowDTO = WorkflowDTO.builder()
            .workflowState(WorkflowDTO.WorkflowState.APPROVAL_REJECTED)
            .state(WorkflowDTO.State.REVIEWED)
            .build();

        workflowController.startOrUpdateRecordState(recordId,workflowDTO);

        response = workflowController.getRecordState(recordId);

        assertEquals(String.valueOf("APPROVAL_REJECTED"), String.valueOf(response.getBody().get("workflowState")));
        assertEquals(String.valueOf("DRAFTED"), String.valueOf(response.getBody().get("state")));

    }

    @Test
    @Deployment(resources = { "processes/task-approval-process.bpmn20.xml" })
    void duplicateProcessWithSameRecordId() {

        Long recordId = 9000L;

        workflowController.startOrUpdateRecordState(recordId,null);

        var response = workflowController.getRecordState(recordId);

        ProcessingException exception = assertThrows(ProcessingException.class, () -> {
            // Call your method that should throw the exception
            workflowController.startOrUpdateRecordState(recordId,null);
        });

        // Verify the exception message
        assertEquals("A 'Review and Approval cycle process' with record ID: 9000 is already in an active intermediate state. Please complete the process before starting a new one.",
                exception.getMessage());

    }

    @Test
    @Deployment(resources = { "processes/task-approval-process.bpmn20.xml" })
    void recordNotFountWithProcessId() {

        Long recordId = 9000L;

        workflowController.startOrUpdateRecordState(recordId,null);

        workflowController.getRecordState(recordId);

        RecordNotFoundException exception = assertThrows(RecordNotFoundException.class, () -> {

            Long unknownRecordId = 7000L;

            // Call your method that should throw the exception
            WorkflowDTO workflowDTO = WorkflowDTO.builder()
                    .workflowState(WorkflowDTO.WorkflowState.DOCUMENT_READY_FOR_REVIEW)
                    .state(WorkflowDTO.State.DRAFTED)
                    .build();

            workflowController.startOrUpdateRecordState(unknownRecordId,workflowDTO);
        });

        // Verify the exception message
        assertEquals("Record not found: 7000",
                exception.getMessage());

    }

    @Test
    @Deployment(resources = { "processes/task-approval-process.bpmn20.xml" })
    void invalidStatus() {

        Long recordId = 9000L;

        workflowController.startOrUpdateRecordState(recordId,null);

        workflowController.getRecordState(recordId);

        InvalidStatusException exception = assertThrows(InvalidStatusException.class, () -> {

            // Call your method that should throw the exception
            WorkflowDTO workflowDTO = WorkflowDTO.builder()
                .workflowState(WorkflowDTO.WorkflowState.DOCUMENT_READY_FOR_REVIEW)
                .state(WorkflowDTO.State.SIGNED)
                .build();

            workflowController.startOrUpdateRecordState(recordId,workflowDTO);
        });

        // Verify the exception message
        assertEquals("Invalid state: Please provide a valid state",
            exception.getMessage());

    }

}