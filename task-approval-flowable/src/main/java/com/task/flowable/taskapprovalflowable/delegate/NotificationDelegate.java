package com.task.flowable.taskapprovalflowable.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationDelegate implements JavaDelegate  {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {

        String workflowState = String.valueOf(execution.getVariable("workflowState"));
        Long recordId = (Long) execution.getVariable("recordId");

        /**
         * an email will be triggered from here on the basis of workflow state

         * if workflow state = documentreadyforreview, trigger mail to Reviewer
         * if workflow state = reviewrejected, trigger mail to Document Owner
         * if workflow state = reviewaccepted, trigger mail to Approver
         * if workflow state = approvalrejected, trigger mail to Document Owner
         * if workflow state = approvalaccepted, trigger mail to Document Owner
         */
        logger.info("Sending email to notify user that record {} status updated to {}", recordId, workflowState);
    }
}
