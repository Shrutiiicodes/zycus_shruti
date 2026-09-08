package com.stockpulse.fulfillment;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

    private final FulfillmentService fulfillmentService;

    public PurchaseOrderController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping
    public List<PurchaseOrder> list(@RequestParam(required = false) String productId) {
        return fulfillmentService.listPurchaseOrders(productId);
    }

    @PatchMapping("/{id}/receive")
    public PurchaseOrder receiveShipment(@PathVariable Long id) {
        return fulfillmentService.receiveShipment(id);
    }

    @PatchMapping("/{id}/cancel")
    public PurchaseOrder cancelPurchaseOrder(@PathVariable Long id) {
        return fulfillmentService.cancelPurchaseOrder(id);
    }
}
