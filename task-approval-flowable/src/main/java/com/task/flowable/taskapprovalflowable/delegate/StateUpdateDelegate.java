package com.task.flowable.taskapprovalflowable.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

//TODO : Can be deleted - as all logics moved to xml, Just using for Logging
@Component
public class StateUpdateDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(StateUpdateDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {

        // Retrieve the variables from the execution context
        String currentState = String.valueOf(execution.getVariable("state"));
        Long recordId = (Long) execution.getVariable("recordId");
        String workflowState = String.valueOf(execution.getVariable("workflowState")); // Assuming 'workflowState' is set

        // Log the current state and record ID for debugging
        logger.info("Updating record state from '{}' to '{}' for record {}", currentState, workflowState, recordId);



    }

}
