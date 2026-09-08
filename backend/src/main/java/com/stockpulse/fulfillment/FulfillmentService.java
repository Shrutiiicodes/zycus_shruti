package com.stockpulse.fulfillment;

import com.stockpulse.audit.AuditService;
import com.stockpulse.audit.TransactionType;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FulfillmentService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    public FulfillmentService(PurchaseOrderRepository purchaseOrderRepository,
                               ProductRepository productRepository,
                               AuditService auditService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(String productId, int quantity, Long suggestionId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));

        // Increment incoming stock (physical stock level is ONLY incremented when shipment is received)
        product.setIncomingStock(product.getIncomingStock() + quantity);
        productRepository.save(product);

        PurchaseOrder po = PurchaseOrder.builder()
                .productId(productId)
                .quantityOrdered(quantity)
                .supplierId(product.getSupplierId() != null ? product.getSupplierId() : "SUPPLIER-DEFAULT")
                .suggestionId(suggestionId)
                .status(POStatus.CREATED)
                .expectedArrivalDate(Instant.now().plus(product.getLeadTimeDays(), ChronoUnit.DAYS))
                .build();

        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        auditService.recordInventoryTransaction(
                productId,
                quantity,
                product.getStockLevel(),
                TransactionType.REORDER_ACCEPTED,
                "PO-" + savedPo.getId()
        );

        return savedPo;
    }

    @Transactional
    public PurchaseOrder receiveShipment(Long poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new NoSuchElementException("Purchase Order not found: " + poId));

        if (po.getStatus() == POStatus.RECEIVED) {
            throw new IllegalStateException("Purchase Order " + poId + " has already been received.");
        }

        Product product = productRepository.findById(po.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + po.getProductId()));

        // Receive physical stock and reduce incoming stock
        product.receiveStock(po.getQuantityOrdered());
        product.setIncomingStock(Math.max(0, product.getIncomingStock() - po.getQuantityOrdered()));
        productRepository.save(product);

        po.setStatus(POStatus.RECEIVED);
        po.setReceivedAt(Instant.now());
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        auditService.recordInventoryTransaction(
                product.getId(),
                po.getQuantityOrdered(),
                product.getStockLevel(),
                TransactionType.SHIPMENT_RECEIVED,
                "PO-" + savedPo.getId()
        );

        return savedPo;
    }

    public List<PurchaseOrder> listPurchaseOrders(String productId) {
        return productId == null || productId.isBlank()
                ? purchaseOrderRepository.findAllByOrderByCreatedAtDesc()
                : purchaseOrderRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
}
