package com.stockpulse.commerce;

import com.stockpulse.forecasting.DemandForecastService;
import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.recommendation.ChangeDirection;
import com.stockpulse.recommendation.TriggerReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeterministicCommerceCalculatorTest {

    @Mock
    private DemandForecastService forecastService;

    @InjectMocks
    private DeterministicCommerceCalculator calculator;

    @Test
    void shouldCalculateReorderQuantityWithMoqAndLeadTimeDemand() {
        Product product = Product.builder()
                .id("PRD-001")
                .name("Calculator Product")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("100.00"))
                .stockLevel(5)
                .reorderThreshold(15)
                .leadTimeDays(7)
                .safetyStock(10)
                .incomingStock(0)
                .minimumOrderQuantity(25)
                .build();

        ProductContext ctx = ProductContext.builder()
                .productId("PRD-001")
                .name("Calculator Product")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("100.00"))
                .stockLevel(5)
                .reorderThreshold(15)
                .demandVelocity(2)
                .categoryAverageVelocity(2.0)
                .build();

        TriggerContext trigger = TriggerContext.builder().reason(TriggerReason.INVENTORY_LOW).build();

        when(forecastService.calculateLeadTimeDemand(any())).thenReturn(14); // 2/day * 7 days = 14

        CommerceRecommendation rec = calculator.calculate(ctx, trigger, product);

        // Expected reorder = (14 leadTimeDemand + 10 safetyStock) - (5 stock + 0 incoming) = 19 -> floored at MOQ 25
        assertEquals(25, rec.getReorder().getRecommendedQuantity());
        assertEquals(new BigDecimal("110.00"), rec.getPricing().getRecommendedPrice());
        assertEquals(ChangeDirection.INCREASE, rec.getPricing().getDirection());
    }
}
