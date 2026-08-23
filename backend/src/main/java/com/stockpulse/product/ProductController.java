package com.stockpulse.product;

import com.stockpulse.product.dto.CreateProductRequest;
import com.stockpulse.product.dto.OrderRequest;
import com.stockpulse.product.dto.ProductResponse;
import com.stockpulse.product.dto.StockUpdateRequest;
import com.stockpulse.recommendation.dto.PricingSuggestionResponse;
import com.stockpulse.recommendation.dto.ReorderSuggestionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest req) {
        return ProductResponse.from(productService.create(req));
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam Optional<ProductStatus> status,
                                       @RequestParam Optional<Category> category) {
        return productService.list(status, category).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable String id) {
        return ProductResponse.from(productService.get(id));
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse updateStock(@PathVariable String id, @Valid @RequestBody StockUpdateRequest req) {
        return ProductResponse.from(productService.updateStock(id, req.getStockLevel()));
    }

    @PostMapping("/{id}/orders")
    public ProductResponse placeOrder(@PathVariable String id, @RequestBody(required = false) OrderRequest req) {
        int quantity = req == null ? 1 : req.getQuantity();
        return ProductResponse.from(productService.placeOrder(id, quantity));
    }

    @PostMapping("/{id}/suggest-pricing")
    public PricingSuggestionResponse suggestPricing(@PathVariable String id) {
        return PricingSuggestionResponse.from(productService.suggestPricingOnDemand(id));
    }

    @PostMapping("/{id}/suggest-reorder")
    public ReorderSuggestionResponse suggestReorder(@PathVariable String id) {
        return ReorderSuggestionResponse.from(productService.suggestReorderOnDemand(id));
    }
}