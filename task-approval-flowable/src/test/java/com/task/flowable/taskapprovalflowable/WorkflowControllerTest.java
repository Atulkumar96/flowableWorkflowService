package com.task.flowable.taskapprovalflowable;


import com.task.flowable.taskapprovalflowable.controller.WorkflowController;
import com.task.flowable.taskapprovalflowable.dto.WorkflowDTO;
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
    //@Deployment(resources = { "processes/task-approval-process.bpmn20.xml" })
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

}