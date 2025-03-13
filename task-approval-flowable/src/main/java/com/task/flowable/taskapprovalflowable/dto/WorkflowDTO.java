package com.task.flowable.taskapprovalflowable.dto;

import lombok.*;

import java.util.Optional;

@Getter
@Setter
@ToString
@Data
@Builder
public class WorkflowDTO {

    public enum WorkflowState {
        DRAFTED,
        DOCUMENT_READY_FOR_REVIEW,
        REVIEW_REJECTED,
        REVIEW_ACCEPTED,
        APPROVAL_REJECTED,
        APPROVAL_ACCEPTED
    }

    public enum State {
        DRAFTED,
        REVIEWED,
        SIGNED
    }

    private WorkflowState workflowState;
    private String recordType; // i.e. nerc_policy_procedure
    private String reviewRecordtype; // i.e. nerc_policy_procedure_review
    private String standard; // i.e. BAL-001-0
    private State state;
    private String clientId; // i.e. rhg
}
