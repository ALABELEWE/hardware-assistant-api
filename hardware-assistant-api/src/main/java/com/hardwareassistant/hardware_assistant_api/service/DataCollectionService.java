// ── DataCollectionService.java (interface) ────────────────────────────────────
package com.hardwareassistant.hardware_assistant_api.service;

import com.hardwareassistant.hardware_assistant_api.dto.request.*;
import com.hardwareassistant.hardware_assistant_api.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DataCollectionService {

    // Products
    Product createProduct(User user, ProductRequest request);
    Product updateProduct(User user, UUID productId, ProductRequest request);
    void deleteProduct(User user, UUID productId);
    List<Product> getProducts(User user);
    List<Product> getActiveProducts(User user);

    // Sales
    SalesTransaction recordSale(User user, SalesTransactionRequest request);
    void deleteSale(User user, UUID saleId);
    Page<SalesTransaction> getSales(User user, Pageable pageable);

    // Expenses
    ExpenseRecord recordExpense(User user, ExpenseRequest request);
    void deleteExpense(User user, UUID expenseId);
    Page<ExpenseRecord> getExpenses(User user, Pageable pageable);

    // Inventory
    InventorySnapshot recordInventory(User user, InventorySnapshotRequest request);
    void deleteInventorySnapshot(User user, UUID snapshotId);
    Page<InventorySnapshot> getInventorySnapshots(User user, Pageable pageable);
    List<InventorySnapshot> getCurrentInventory(User user);
}