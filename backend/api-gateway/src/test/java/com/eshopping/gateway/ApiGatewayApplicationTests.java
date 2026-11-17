package com.eshopping.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {

    @Test
    @DisplayName("Verify API Gateway context loads successfully")
    void contextLoads() {
        // Verifies Spring context initialization for API Gateway
    }
}

