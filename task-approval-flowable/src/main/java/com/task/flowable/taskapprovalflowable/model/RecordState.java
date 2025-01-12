package com.task.flowable.taskapprovalflowable.model;

public enum RecordState {
    DRAFT,
    DOCUMENT_READY_FOR_REVIEW,
    REVIEW_ACCEPTED,
    REVIEW_REJECTED,
    APPROVAL_ACCEPTED,
    APPROVAL_REJECTED,
    SIGNED
}
