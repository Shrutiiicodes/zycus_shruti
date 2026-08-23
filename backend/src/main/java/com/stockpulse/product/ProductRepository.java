package com.stockpulse.product;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategory(Category category);

    List<Product> findByStatusAndCategory(ProductStatus status, Category category);

    /** Category-average demand velocity — used by strategies for spike comparison. */
    default double averageVelocityForCategory(Category category, List<Product> allInCategory) {
        return allInCategory.stream().mapToInt(Product::getDemandVelocity).average().orElse(0);
    }
}