package com.eshopping.admin.scheduler;

import com.eshopping.admin.service.AdminGovernanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DisputeSlaScheduler {

    private static final Logger log = LoggerFactory.getLogger(DisputeSlaScheduler.class);

    private final AdminGovernanceService adminGovernanceService;

    public DisputeSlaScheduler(AdminGovernanceService adminGovernanceService) {
        this.adminGovernanceService = adminGovernanceService;
    }

    /**
     * Polls every 15 seconds to check if any dispute ticket pending merchant response
     * has exceeded the SLA deadline (default 2 minutes). Escalates them to Admin queue.
     */
    @Scheduled(fixedRate = 15000, initialDelay = 10000)
    public void monitorDisputeSla() {
        try {
            int escalated = adminGovernanceService.escalateExpiredDisputes();
            if (escalated > 0) {
                log.info("DisputeSlaScheduler: Escalated {} expired dispute(s) to Admin review queue", escalated);
            }
        } catch (Exception ex) {
            log.error("Error running DisputeSlaScheduler: {}", ex.getMessage(), ex);
        }
    }
}
