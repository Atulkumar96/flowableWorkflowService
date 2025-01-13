package com.task.flowable.taskapprovalflowable.controller;

import com.task.flowable.taskapprovalflowable.dto.ProcessInstanceDTO;
import com.task.flowable.taskapprovalflowable.model.Record;
import com.task.flowable.taskapprovalflowable.service.FlowableTaskService;
import com.task.flowable.taskapprovalflowable.service.ProcessService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final FlowableTaskService flowableTaskService;
    private final ProcessService processService;
    private final RuntimeService runtimeService;
    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    @PostMapping("/{recordId}")
    public ResponseEntity<Map<String, Object>> startProcess(@PathVariable Long recordId) {
        logger.info("Starting process");

        Map<String, Object> variables = new HashMap<>();
        variables.put("recordId", recordId);

        // recordId is used as the business key in act_hi_procinst table
        String businessKey = recordId.toString();

        // Check for existing process instance with the same business key
        ProcessInstance existingProcessInstance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("taskApprovalProcess")
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (existingProcessInstance != null) {
            logger.info("Process already exists for recordId {}", recordId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403
        }

        // No existing process found, proceed with starting a new one
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            "taskApprovalProcess",
                businessKey,
                variables
        );

        logger.info("Process started with process id {} associated with a record id {}", processInstance.getId(), recordId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/process/{processInstanceId}/state")
    public ResponseEntity<String> getProcessState(@PathVariable String processInstanceId) {
        logger.info("Fetching state for process {}", processInstanceId);
        String state = processService.getProcessState(processInstanceId);
        return ResponseEntity.ok(state);
    }

    @GetMapping("/process/{processInstanceId}/details")
    public ResponseEntity<ProcessInstanceDTO> getProcessDetailsById(@PathVariable String processInstanceId) {
        logger.info("Fetching state for process {}", processInstanceId);
        ProcessInstanceDTO processInstanceDTO = processService.getProcessInstanceDetails(processInstanceId);
        return ResponseEntity.ok(processInstanceDTO);
    }

    /*
    @PostMapping("/{recordId}")
    public ResponseEntity<Void> updateRecordStatus(
        @PathVariable Long recordId,
        @RequestBody Record recordModel) {

        flowableTaskService.updateRecordStatus(recordId, recordModel);
        return ResponseEntity.ok().build();
    }

     */

    /**
     * Get all completed process instance IDs
     * @return List of completed process instance IDs
     */
    @GetMapping("/process/all")
    public ResponseEntity<List<ProcessInstanceDTO>> getCompletedProcessInstanceIds() {
        return ResponseEntity.ok(processService.getAllProcessInstancesWithActivities());
    }

}
