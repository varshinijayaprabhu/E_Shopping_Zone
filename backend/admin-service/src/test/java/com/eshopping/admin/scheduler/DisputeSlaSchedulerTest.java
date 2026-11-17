package com.eshopping.admin.scheduler;

import com.eshopping.admin.service.AdminGovernanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeSlaSchedulerTest {

    @Mock
    private AdminGovernanceService adminGovernanceService;

    @InjectMocks
    private DisputeSlaScheduler disputeSlaScheduler;

    @Test
    @DisplayName("monitorDisputeSla triggers auto-escalation of expired disputes")
    void testMonitorDisputeSla() {
        when(adminGovernanceService.escalateExpiredDisputes()).thenReturn(3);

        disputeSlaScheduler.monitorDisputeSla();

        verify(adminGovernanceService, times(1)).escalateExpiredDisputes();
    }
}
