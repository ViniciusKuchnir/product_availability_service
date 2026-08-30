package com.example.product_availability_service;

import com.example.product_availability_service.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProductAvailabilityServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
