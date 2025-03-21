package com.task.flowable.taskapprovalflowable.controller;

import com.task.flowable.taskapprovalflowable.dto.WorkflowDTO;
import com.task.flowable.taskapprovalflowable.exception.DataCorruptionException;
import com.task.flowable.taskapprovalflowable.exception.DuplicateRecordException;
import com.task.flowable.taskapprovalflowable.exception.InvalidStatusException;
import com.task.flowable.taskapprovalflowable.exception.ProcessingException;
import com.task.flowable.taskapprovalflowable.exception.RecordNotFoundException;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    @Value("${default-process-definition-key}")
    private String defaultProcessDefinitionKey;

    private final static HashMap<String,String> WORKFLOW_MATRIX = new HashMap<>();
    private final static String KEY_RECORD_ID = "recordId";
    private final static String KEY_WORKFLOW_STATE = "workflowState";
    private final static String KEY_RECORD_TYPE = "recordType";
    private final static String KEY_REVIEW_RECORD_TYPE = "reviewRecordtype";
    private final static String KEY_STANDARD = "standard";
    private final static String KEY_STATE = "state";
    private final static String KEY_CLIENT_ID = "clientId";
    private final static String KEY_PROCESS_INSTANCE_ID = "processInstanceId";

    static {
        //for recordType r1
        WORKFLOW_MATRIX.put("r1","review_and_approval_cycle_R1");
    }

    /**
     * Start or update the state of a record

     * @param recordId Long
     * @param workflowDTO WorkflowDTO: workflowState, recordType, reviewRecordtype, standard, state, clientId
     * @return ResponseEntity<Map<String, Object>>
     *     * @throws RecordNotFoundException
     *     * @throws DuplicateRecordException
     *     * @throws ProcessingException
     *     * @throws InvalidStatusException
     *     * @throws DataCorruptionException
     */

    @PostMapping("/{recordId}")
    public ResponseEntity<Map<String, Object>> startOrUpdateRecordState(
        @PathVariable Long recordId,
        @RequestBody(required = false) WorkflowDTO workflowDTO) {

        // When starting a new process, mandatory fields must be provided.
        if (workflowDTO == null) {
            logger.error("Workflow body/details are required to start the workflow for provided RecordId: {}", recordId);
            throw new InvalidStatusException("Workflow details are required to start the workflow.");
        }

        // When starting a new process in DRAFTED state, mandatory fields must be provided.
        if (workflowDTO.getWorkflowState() == WorkflowDTO.WorkflowState.DRAFTED) {
            // Validate that mandatory fields are not null or empty
            if (isNullOrEmpty(workflowDTO.getRecordType()) ||
                    isNullOrEmpty(workflowDTO.getReviewRecordtype()) ||
                    isNullOrEmpty(workflowDTO.getStandard()) ||
                    isNullOrEmpty(workflowDTO.getClientId())) {
                logger.error("Mandatory fields recordType, reviewRecordtype, standard, and clientId must not be null or empty for provided RecordId: {}", recordId);
                throw new InvalidStatusException("Mandatory fields recordType, reviewRecordtype, standard, and clientId must not be null or empty.");
            }
            Map<String, Object> response = startProcess(recordId, workflowDTO);
            logger.info("Process started with process id {} and record id {}", response.get(KEY_PROCESS_INSTANCE_ID), response.get(KEY_RECORD_ID));
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        else {
            // For non-DRAFTED states, we update the workflow state
            updateWorkflowState(recordId, workflowDTO);
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Get the state of a record

     * @param recordId Long
     * @return ResponseEntity<Map<String, Object>>
     * @throws RecordNotFoundException
     */

    @GetMapping("/{recordId}")
    public ResponseEntity<Map<String, Object>> getRecordState(
        @PathVariable Long recordId) {

        String businessKey = String.valueOf(recordId);
        Map<String, Object> response = new HashMap<>();

        try {
            // First check active processes
            ProcessInstance activeProcess = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();

            if (activeProcess != null) {

                // Get process variables
                Map<String, Object> variables = runtimeService.getVariables(activeProcess.getId());

                if (variables.containsKey(KEY_WORKFLOW_STATE)) {
                    response.put(KEY_WORKFLOW_STATE, variables.get(KEY_WORKFLOW_STATE));
                }

                //check if variables contains state and is not equal to null
                if(variables.containsKey(KEY_STATE) && variables.get(KEY_STATE) != null){
                    response.put(KEY_STATE, variables.get(KEY_STATE));
                }

                return ResponseEntity.ok(response);
            }

            // If no active process, check historic processes
            // Order by end time to get the latest process
            // This is a business rule to prevent reprocessing of the same record
            /*
             HistoricProcessInstance historicProcess = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .orderByProcessInstanceEndTime()
                .desc()
                .singleResult();

             */

            List<HistoricProcessInstance> historicProcess = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .orderByProcessInstanceEndTime()
                    .desc()
                    .list();

            if (historicProcess != null && !historicProcess.isEmpty()) {
                //response.put("processInstanceId", historicProcess.getId());
                //response.put("status", "COMPLETED");
                //response.put("endTime", historicProcess.getEndTime());

                // Get historic variables
                List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(historicProcess.get(0).getId())
                    .list();

                Map<String, Object> variables = historicVariables.stream()
                    .collect(Collectors.toMap(
                        HistoricVariableInstance::getVariableName,
                        HistoricVariableInstance::getValue
                    ));

                if (variables.containsKey(KEY_STATE)) {
                    response.put(KEY_STATE, variables.get(KEY_STATE));
                }
                if (variables.containsKey(KEY_WORKFLOW_STATE)) {
                    response.put(KEY_WORKFLOW_STATE, variables.get(KEY_WORKFLOW_STATE));
                }

                return ResponseEntity.ok(response);
            }

            // No process found
            return ResponseEntity.notFound().build();

        } catch (FlowableException e) {
            logger.error("Error retrieving process state for record {} : {}", recordId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("error",
                    "Error retrieving process state. Please contact administrator."));
        }
    }


    // Start a new process with a Record
    private Map<String, Object> startProcess(Long recordId, WorkflowDTO workflowDTO) {

        String businessKey = String.valueOf(recordId);

        try {
            // Check active process instances: must be zero
            List<ProcessInstance> activeProcesses = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .list();

            // Handle multiple active processes (data corruption scenario)
            if (activeProcesses.size() > 1) {
                String processIds = activeProcesses.stream()
                    .map(ProcessInstance::getProcessInstanceId)
                    .collect(Collectors.joining(", "));

                logger.error("Data corruption detected: Multiple active processes found for business key {}: {}",
                    businessKey, processIds);
                throw new DataCorruptionException("Multiple active processes found for record ID: " + recordId +
                    ". Please contact system administrator. Reference: " + processIds);
            }

            // Handle for single active process
            if (!activeProcesses.isEmpty()) {
                ProcessInstance existingProcess = activeProcesses.get(0);
                throw new DuplicateRecordException("Duplicate record with ID: " + recordId +
                    " is already associated to active process " + existingProcess.getProcessInstanceId());
            }

            // Proceed with process creation if no duplicates found
            Map<String, Object> variables = new HashMap<>();
            variables.put(KEY_RECORD_ID, recordId);
            variables.put(KEY_WORKFLOW_STATE, WorkflowDTO.WorkflowState.DRAFTED);
            variables.put(KEY_STATE, WorkflowDTO.State.DRAFTED);

            if (workflowDTO != null && workflowDTO.getReviewRecordtype() != null) {
                variables.put(KEY_REVIEW_RECORD_TYPE, workflowDTO.getReviewRecordtype());
            }

            if (workflowDTO != null && workflowDTO.getStandard() != null) {
                variables.put(KEY_STANDARD, workflowDTO.getStandard());
            }

            if (workflowDTO != null && workflowDTO.getClientId() != null) {
                variables.put(KEY_CLIENT_ID, workflowDTO.getClientId());
            }

            // Default process definition key
            String processDefinitionKey = defaultProcessDefinitionKey;

            // If workflowDTO is not null & workflowDTO has recordType, set it as a process variable
            // According to the recordType, set the processDefinitionKey
            // fetch process definition from workflow matrix based on the record type ((e.g. nerc_policy_procedure_rsaw-> review_and_approval_cycle))
            if (workflowDTO != null && workflowDTO.getRecordType() != null) {
                variables.put(KEY_RECORD_TYPE, workflowDTO.getRecordType());
                processDefinitionKey = WORKFLOW_MATRIX.getOrDefault(workflowDTO.getRecordType(), defaultProcessDefinitionKey);
            }

            // Start the process instance
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                processDefinitionKey,
                businessKey,
                variables
            );

            Map<String, Object> response = new HashMap<>();
            response.put(KEY_PROCESS_INSTANCE_ID, processInstance.getId());
            response.put(KEY_RECORD_ID, recordId);

            return response;

        } catch (FlowableException e) {
            logger.error("Flowable error while processing business key {}: {}", businessKey, e.getMessage(), e);
            throw new ProcessingException("Error processing record ID: " + recordId +
                ". Please contact system administrator.", e);
        } catch (Exception e) {
            logger.error("Unexpected error while processing business key {}: {}", businessKey, e.getMessage(), e);
            throw new ProcessingException("A 'Review and Approval cycle process' with record ID: " + recordId +
                " is already in an active intermediate state. Please complete the process before starting a new one.", e);
        }
    }

    // Update the task status and complete the associated process task
    private void updateWorkflowState(Long recordId, WorkflowDTO workflowDTO) {

        String businessKey = String.valueOf(recordId);

        ProcessInstance existingProcess = runtimeService.createProcessInstanceQuery()
            .processInstanceBusinessKey(businessKey)
            .singleResult();

        Optional.ofNullable(existingProcess)
            .orElseThrow(() -> new RecordNotFoundException("Record not found: " + recordId));

        // Get current task
        org.flowable.task.api.Task currentTask = taskService.createTaskQuery()
            .processInstanceId(existingProcess.getId())
            .singleResult();

        if (currentTask == null) {
            throw new InvalidStatusException("No active task found for record: " + recordId);
        }

        // Prepare variables for the workflow
        Map<String, Object> variables = new HashMap<>();
        variables.put(KEY_WORKFLOW_STATE, workflowDTO.getWorkflowState());
        variables.put(KEY_RECORD_ID, recordId);
        if (workflowDTO.getRecordType() != null) {
            variables.put(KEY_RECORD_TYPE, workflowDTO.getRecordType());
        }

        if (workflowDTO.getReviewRecordtype() != null) {
            variables.put(KEY_REVIEW_RECORD_TYPE, workflowDTO.getReviewRecordtype());
        }

        if (workflowDTO.getStandard() != null) {
            variables.put(KEY_STANDARD, workflowDTO.getStandard());
        }

        if (workflowDTO.getClientId() != null) {
            variables.put(KEY_CLIENT_ID, workflowDTO.getClientId());
        }

        // Complete the current task with variables
        taskService.complete(currentTask.getId(), variables);

    }

    // Helper method to check if a string is null or empty after trimming.
    private boolean isNullOrEmpty(String str) {
        return (str == null || str.trim().isEmpty());
    }

}
