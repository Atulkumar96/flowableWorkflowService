package com.task.flowable.taskapprovalflowable.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
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

            // Proceed with process creation if no duplicate active process found
            // Set process variables
            Map<String, Object> variables = new HashMap<>();
            variables.put(KEY_RECORD_ID, recordId);
            variables.put(KEY_WORKFLOW_STATE, WorkflowDTO.WorkflowState.DRAFTED);
            variables.put(KEY_STATE, WorkflowDTO.State.DRAFTED);
            variables.put(KEY_RECORD_TYPE, workflowDTO.getRecordType());

            if (workflowDTO != null && workflowDTO.getReviewRecordtype() != null) {
                variables.put(KEY_REVIEW_RECORD_TYPE, workflowDTO.getReviewRecordtype());
            }

            if (workflowDTO != null && workflowDTO.getStandard() != null) {
                variables.put(KEY_STANDARD, workflowDTO.getStandard());
            }

            if (workflowDTO != null && workflowDTO.getClientId() != null) {
                variables.put(KEY_CLIENT_ID, workflowDTO.getClientId());
            }

            // NEW: Extract notification details and populate process variables.
            addNotificationVariables(variables, workflowDTO);

            // Default process definition key
            String processDefinitionKey = defaultProcessDefinitionKey;

            // If workflowDTO has recordType then fetch the processDefinitionKey from the matrix based on the recordType
            // fetch process definition from workflow matrix based on the record type ((e.g. nerc_policy_procedure_rsaw-> review_and_approval_cycle))
            if (!isNullOrEmpty(workflowDTO.getRecordType())) {
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

    /**
     * Stub method that simulates calling the external POST endpoint
     * https://sp3crmtest.claritysystemsinc.com/records and returning the JSON response.
     */
    private String fetchNotificationResponse(String clientId) {
        // In a real implementation we might use RestTemplate or WebClient or jar to fetch the response.
        // For now, we return the stubbed JSON as provided.
        return "{\n" +
                "    \"totalCount\": 1,\n" +
                "    \"records\": [\n" +
                "        {\n" +
                "            \"recordId\": \"a5877f:JeR87J\",\n" +
                "            \"recordType\": \"nerc_emailsettings\",\n" +
                "            \"properties\": [\n" +
                "                {\n" +
                "                    \"name\": \"clientId\",\n" +
                "                    \"type\": \"STRING\",\n" +
                "                    \"value\": \"rhg\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"createdDate\",\n" +
                "                    \"type\": \"NUMBER\",\n" +
                "                    \"value\": 1741780619651\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"createdBy\",\n" +
                "                    \"type\": \"STRING\",\n" +
                "                    \"value\": \" atul  atul\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"txnState\",\n" +
                "                    \"type\": \"STRING\",\n" +
                "                    \"value\": \"Committed\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"matrix\",\n" +
                "                    \"type\": \"ARRAY\",\n" +
                "                    \"value\": [\n" +
                "                        {\n" +
                "                            \"standard\": \"BAL-001-2\",\n" +
                "                            \"emailReceipentsForReview\": [\n" +
                "                                {\n" +
                "                                    \"reviewer\": \"baskar@claritysystemsinc.com\"\n" +
                "                                }\n" +
                "                            ],\n" +
                "                            \"emailReceipentsForApprove\": [\n" +
                "                                {\n" +
                "                                    \"approver\": \"lsowner@gmail.com\"\n" +
                "                                }\n" +
                "                            ],\n" +
                "                            \"emailReceipentsForEscalation1\": [\n" +
                "                                {\n" +
                "                                    \"escalation\": \"\"\n" +
                "                                }\n" +
                "                            ],\n" +
                "                            \"emailReceipentsForEscalation2\": [\n" +
                "                                {\n" +
                "                                    \"escalation\": \"\"\n" +
                "                                }\n" +
                "                            ]\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }

    /**
     * Helper method to fetch and parse notification details.
     * It fetches the notification JSON (stubbed), looks for a record with recordType "nerc_emailsettings"
     * matching the given clientId, then extracts reviewer and approver based on the provided standard.
     * The extracted values are added to the provided variables map.
     */
    private void addNotificationVariables(Map<String, Object> variables, WorkflowDTO workflowDTO) throws IOException {
        String clientId = workflowDTO.getClientId();
        String standard = workflowDTO.getStandard();
        String jsonResponse = fetchNotificationResponse(clientId);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);

        if (root.has("records")) {
            for (JsonNode record : root.get("records")) {
                if (record.has("recordType") && "nerc_emailsettings".equals(record.get("recordType").asText())) {
                    // Check if the record's clientId matches the provided clientId.
                    boolean clientIdMatch = false;
                    JsonNode properties = record.get("properties");
                    for (JsonNode property : properties) {
                        if ("clientId".equals(property.get("name").asText()) &&
                                clientId.equals(property.get("value").asText().trim())) {
                            clientIdMatch = true;
                            break;
                        }
                    }

                    if (clientIdMatch) {
                        // Find the 'matrix' property in the record and extract reviewer/approver.
                        for (JsonNode property : properties) {
                            if ("matrix".equals(property.get("name").asText())) {
                                JsonNode matrixArray = property.get("value");
                                if (matrixArray.isArray()) {
                                    for (JsonNode matrixItem : matrixArray) {
                                        if (matrixItem.has("standard") && standard.equals(matrixItem.get("standard").asText())) {
                                            String reviewer = "";
                                            String approver = "";

                                            JsonNode reviewers = matrixItem.get("emailReceipentsForReview");
                                            if (reviewers != null && reviewers.isArray() && reviewers.size() > 0) {
                                                reviewer = reviewers.get(0).get("reviewer").asText();
                                            }

                                            JsonNode approvers = matrixItem.get("emailReceipentsForApprove");
                                            if (approvers != null && approvers.isArray() && approvers.size() > 0) {
                                                approver = approvers.get(0).get("approver").asText();
                                            }

                                            variables.put("reviewer", reviewer);
                                            variables.put("approver", approver);
                                            variables.put("documentOwner", "atul@claritysystemsinc.com");
                                            //variables.put("notificationRecordType", "nerc_emailsettings");
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}
