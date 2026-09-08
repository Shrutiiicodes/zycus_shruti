package com.stockpulse.forecasting;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DemandForecastService {

    private final ProductRepository productRepository;

    public DemandForecastService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public double calculateCategoryAverageVelocity(Product product) {
        List<Product> peers = productRepository.findByCategoryAndIdNot(product.getCategory(), product.getId());
        if (peers.isEmpty()) {
            return product.getDemandVelocity() > 0 ? (double) product.getDemandVelocity() : 1.0;
        }
        return peers.stream().mapToInt(Product::getDemandVelocity).average().orElse(1.0);
    }

    public double calculateMovingAverageVelocity(Product product) {
        // Uses baseline velocity smoothing
        return Math.max(0.5, (double) product.getDemandVelocity());
    }

    public int calculateLeadTimeDemand(Product product) {
        double dailyVelocity = calculateMovingAverageVelocity(product);
        int leadTimeDays = product.getLeadTimeDays() > 0 ? product.getLeadTimeDays() : 7;
        return (int) Math.ceil(dailyVelocity * leadTimeDays);
    }

    public double calculateVelocityRatio(Product product) {
        double categoryAvg = calculateCategoryAverageVelocity(product);
        if (categoryAvg <= 0) return 1.0;
        return (double) product.getDemandVelocity() / categoryAvg;
    }
}
