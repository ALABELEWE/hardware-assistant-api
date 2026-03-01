package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.dto.request.*;
import com.hardwareassistant.hardware_assistant_api.exception.BusinessException;
import com.hardwareassistant.hardware_assistant_api.exception.ResourceNotFoundException;
import com.hardwareassistant.hardware_assistant_api.model.*;
import com.hardwareassistant.hardware_assistant_api.repository.*;
import com.hardwareassistant.hardware_assistant_api.service.DataCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataCollectionServiceImpl implements DataCollectionService {

    private final ProductRepository              productRepository;
    private final SalesTransactionRepository     salesRepository;
    private final ExpenseRecordRepository        expenseRepository;
    private final InventorySnapshotRepository    inventoryRepository;
    private final MerchantProfileRepository      merchantProfileRepository;


    private MerchantProfile getProfile(User user) {
        return merchantProfileRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Please complete your merchant profile first"));
    }

    @Override
    @Transactional
    public Product createProduct(User user, ProductRequest req) {
        MerchantProfile profile = getProfile(user);

        if (productRepository.existsByNameIgnoreCaseAndMerchantProfile(req.getName(), profile)) {
            throw new BusinessException("A product named '" + req.getName() + "' already exists");
        }

        Product product = Product.builder()
                .merchantProfile(profile)
                .name(req.getName().trim())
                .category(req.getCategory())
                .unit(req.getUnit() != null ? req.getUnit() : "piece")
                .costPrice(req.getCostPrice())
                .sellingPrice(req.getSellingPrice())
                .currentStock(req.getCurrentStock())
                .reorderLevel(req.getReorderLevel())
                .build();

        product = productRepository.save(product);
        log.info("Product created: '{}' for merchant: {}", product.getName(), user.getEmail());
        return product;
    }

    @Override
    @Transactional
    public Product updateProduct(User user, UUID productId, ProductRequest req) {
        MerchantProfile profile = getProfile(user);
        Product product = productRepository.findByIdAndMerchantProfile(productId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setName(req.getName().trim());
        product.setCategory(req.getCategory());
        if (req.getUnit() != null) product.setUnit(req.getUnit());
        if (req.getCostPrice() != null) product.setCostPrice(req.getCostPrice());
        if (req.getSellingPrice() != null) product.setSellingPrice(req.getSellingPrice());
        if (req.getCurrentStock() != null) product.setCurrentStock(req.getCurrentStock());
        if (req.getReorderLevel() != null) product.setReorderLevel(req.getReorderLevel());

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(User user, UUID productId) {
        MerchantProfile profile = getProfile(user);
        Product product = productRepository.findByIdAndMerchantProfile(productId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        // Soft delete — preserve historical transaction references
        product.setActive(false);
        productRepository.save(product);
        log.info("Product soft-deleted: '{}' for merchant: {}", product.getName(), user.getEmail());
    }

    @Override
    public List<Product> getProducts(User user) {
        return productRepository.findByMerchantProfileOrderByNameAsc(getProfile(user));
    }

    @Override
    public List<Product> getActiveProducts(User user) {
        return productRepository.findByMerchantProfileAndActiveOrderByNameAsc(getProfile(user), true);
    }

    // ── Sales ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SalesTransaction recordSale(User user, SalesTransactionRequest req) {
        MerchantProfile profile = getProfile(user);

        if (req.getProductId() == null && (req.getProductName() == null || req.getProductName().isBlank())) {
            throw new BusinessException("Either productId or productName must be provided");
        }

        Product product = null;
        String productName = req.getProductName();

        if (req.getProductId() != null) {
            product = productRepository.findByIdAndMerchantProfile(req.getProductId(), profile)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            productName = product.getName();
        }

        SalesTransaction sale = SalesTransaction.builder()
                .merchantProfile(profile)
                .product(product)
                .productName(productName)
                .quantity(req.getQuantity())
                .unitPrice(req.getUnitPrice())
                .costPrice(req.getCostPrice() != null ? req.getCostPrice()
                        : (product != null ? product.getCostPrice() : null))
                .transactionDate(req.getTransactionDate())
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "cash")
                .notes(req.getNotes())
                .build();

        // Deduct from product stock if linked
        if (product != null) {
            product.setCurrentStock(product.getCurrentStock().subtract(req.getQuantity()));
            productRepository.save(product);
        }

        sale = salesRepository.save(sale);
        log.info("Sale recorded: {} x {} for merchant: {}", req.getQuantity(), productName, user.getEmail());
        return sale;
    }

    @Override
    @Transactional
    public void deleteSale(User user, UUID saleId) {
        MerchantProfile profile = getProfile(user);
        SalesTransaction sale = salesRepository.findByIdAndMerchantProfile(saleId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
        salesRepository.delete(sale);
    }

    @Override
    public Page<SalesTransaction> getSales(User user, Pageable pageable) {
        return salesRepository.findByMerchantProfileOrderByTransactionDateDescCreatedAtDesc(
                getProfile(user), pageable);
    }


    @Override
    @Transactional
    public ExpenseRecord recordExpense(User user, ExpenseRequest req) {
        MerchantProfile profile = getProfile(user);

        ExpenseRecord expense = ExpenseRecord.builder()
                .merchantProfile(profile)
                .category(req.getCategory())
                .description(req.getDescription())
                .amount(req.getAmount())
                .expenseDate(req.getExpenseDate())
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "cash")
                .notes(req.getNotes())
                .build();

        expense = expenseRepository.save(expense);
        log.info("Expense recorded: {} {} for merchant: {}", req.getAmount(), req.getCategory(), user.getEmail());
        return expense;
    }

    @Override
    @Transactional
    public void deleteExpense(User user, UUID expenseId) {
        MerchantProfile profile = getProfile(user);
        ExpenseRecord expense = expenseRepository.findByIdAndMerchantProfile(expenseId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        expenseRepository.delete(expense);
    }

    @Override
    public Page<ExpenseRecord> getExpenses(User user, Pageable pageable) {
        return expenseRepository.findByMerchantProfileOrderByExpenseDateDescCreatedAtDesc(
                getProfile(user), pageable);
    }

    @Override
    @Transactional
    public InventorySnapshot recordInventory(User user, InventorySnapshotRequest req) {
        MerchantProfile profile = getProfile(user);

        if (req.getProductId() == null && (req.getProductName() == null || req.getProductName().isBlank())) {
            throw new BusinessException("Either productId or productName must be provided");
        }

        Product product = null;
        String productName = req.getProductName();

        if (req.getProductId() != null) {
            product = productRepository.findByIdAndMerchantProfile(req.getProductId(), profile)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            productName = product.getName();
            // Update current stock on product
            product.setCurrentStock(req.getQuantityCounted());
            productRepository.save(product);
        }

        InventorySnapshot snapshot = InventorySnapshot.builder()
                .merchantProfile(profile)
                .product(product)
                .productName(productName)
                .quantityCounted(req.getQuantityCounted())
                .unitCost(req.getUnitCost() != null ? req.getUnitCost()
                        : (product != null ? product.getCostPrice() : null))
                .snapshotDate(req.getSnapshotDate())
                .notes(req.getNotes())
                .build();

        snapshot = inventoryRepository.save(snapshot);
        log.info("Inventory snapshot recorded: {} x {} for merchant: {}", req.getQuantityCounted(), productName, user.getEmail());
        return snapshot;
    }

    @Override
    @Transactional
    public void deleteInventorySnapshot(User user, UUID snapshotId) {
        MerchantProfile profile = getProfile(user);
        InventorySnapshot snapshot = inventoryRepository.findByIdAndMerchantProfile(snapshotId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory snapshot not found"));
        inventoryRepository.delete(snapshot);
    }

    @Override
    public Page<InventorySnapshot> getInventorySnapshots(User user, Pageable pageable) {
        return inventoryRepository.findByMerchantProfileOrderBySnapshotDateDescCreatedAtDesc(
                getProfile(user), pageable);
    }

    @Override
    public List<InventorySnapshot> getCurrentInventory(User user) {
        return inventoryRepository.findLatestPerProduct(getProfile(user));
    }
}