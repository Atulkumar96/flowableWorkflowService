package com.task.flowable.taskapprovalflowable.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationDelegate implements JavaDelegate  {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDelegate.class);
    // Ideally, the access token would be externalized to configuration
    //private static final String ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJZNXM3RENPWnEwd0lnZ2YzMHQzdkFScm1qMEh0Q19LN0k0SGJDLXk1TlowIn0.eyJleHAiOjE3NDE4NzYyODMsImlhdCI6MTc0MTg3MjY4MywianRpIjoiZTVkZGY2MmMtMjAwOC00MzM2LWE2YzgtYWU5NzJjNzFjODMxIiwiaXNzIjoiaHR0cHM6Ly9zcDNjcm10ZXN0LmNsYXJpdHlzeXN0ZW1zaW5jLmNvbS9hdXRoL3JlYWxtcy9jbGFyaXR5IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImNlM2IxZmM5LTNlYTctNGFkZi04MGU2LTg5MjhhYzkxNTk5NSIsInR5cCI6IkJlYXJlciIsImF6cCI6ImNsYXJpdHktb3BlbmlkIiwic2Vzc2lvbl9zdGF0ZSI6ImEyM2I1NTY2LWY0OGMtNDYxMi1iMjZmLWY0ZjY3YTZiNWVjNyIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cDovLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImRlZmF1bHQtcm9sZXMtY2xhcml0eSIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiIgYXR1bCAgYXR1bCIsInByZWZlcnJlZF91c2VybmFtZSI6ImF0dWxAY2xhcml0eXN5c3RlbXNpbmMuY29tIiwiZ2l2ZW5fbmFtZSI6IiBhdHVsIiwiZmFtaWx5X25hbWUiOiIgYXR1bCIsImVtYWlsIjoiYXR1bEBjbGFyaXR5c3lzdGVtc2luYy5jb20ifQ.EuXHsFctvsHD0PQ44wOMbyfjpc-2MfhbUEcTq79LrY49jMAm3F6_Qawc8pxff4k4YecD2_AfPQf1CVTwXUdXuRosKG9fbHSgP4cVMtUcn7CdpU5AgaCdTHPc83kWmtRlf44GB29vlyIAh3dHdgrSCh--dX8LQGImqNLmmhwCuPf-JIdlBySWvXvBqhWdTK4rtIU2ww3amKxtY0bKMPqHkpDr_zzqY7VZAepBxChPKn_YiLtV2L7swUkfLYBLLygeunpjUALWQC127GXjF3yqm_ctM6mbdIvV-LdHcKFLQLGyYgL4K2KWd9VSpc36FX9S0EWnoi_D2JTbxQiOgiJfeg";
    private static final String EMAIL_SERVICE_URL = "https://sp3crmtest.claritysystemsinc.com/send/file";

    @Override
    public void execute(DelegateExecution execution) {

        String workflowState = String.valueOf(execution.getVariable("workflowState"));
        Long recordId = (Long) execution.getVariable("recordId");

        /**
         * an email will be triggered from here on the basis of workflow state

         * if workflow state = documentreadyforreview, trigger mail to Reviewer
         * if workflow state = reviewrejected, trigger mail to Document Owner - atul@clarityauth.com
         * if workflow state = reviewaccepted, trigger mail to Approver
         * if workflow state = approvalrejected, trigger mail to Document Owner
         * if workflow state = approvalaccepted, trigger mail to Document Owner
         *
         *
         *         String recipient = null;
         *         String subject = "Workflow Notification for Record " + recordId + " is now in state " + workflowState;
         *         String body = "Record " + recordId + " is now in state " + workflowState;
         *
         *         // Determine recipient based on workflow state.
         *         if ("documentreadyforreview".equalsIgnoreCase(workflowState)) {
         *             recipient = (String) execution.getVariable("reviewer");
         *         }
         *         else if ("reviewaccepted".equalsIgnoreCase(workflowState)) {
         *             recipient = (String) execution.getVariable("approver");
         *         }
         *         else if ("reviewrejected".equalsIgnoreCase(workflowState)
         *                 || "approvalrejected".equalsIgnoreCase(workflowState)
         *                 || "approvalaccepted".equalsIgnoreCase(workflowState)) {
         *             recipient = (String) execution.getVariable("documentOwner");
         *         }
         *         else {
         *             logger.info("No email recipients configured for workflow state: {}", workflowState);
         *             return;
         *         }
         *
         *         if (recipient == null || recipient.trim().isEmpty()) {
         *             logger.info("No recipient found for workflow state {} and record {}", workflowState, recordId);
         *             return;
         *         }
         */

        logger.info("Sending email notification for record {} with workflowState {}", recordId, workflowState);
        //sendEmail(recipient, subject, body);
    }

    /**
     * Sends an email using the email service endpoint.
     *
     * @param recipient the email address of the recipient
     * @param subject   the subject of the email
     * @param body      the body content of the email


    private void sendEmail(String recipient, String subject, String body) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            //headers.add("Access-Token", ACCESS_TOKEN);

            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("recipients", recipient);
            formData.add("subject", subject);
            formData.add("body", body);
            // If we need to send a file, we will add it here:
            // formData.add("files", new FileSystemResource(new File("path/to/file")));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(EMAIL_SERVICE_URL, requestEntity, String.class);
            logger.info("Email sent to {}. Response: {}", recipient, response.getBody());
        }
        catch (Exception e) {
            logger.error("Error sending email to {}: {}", recipient, e.getMessage(), e);
        }
    }
     */
}
