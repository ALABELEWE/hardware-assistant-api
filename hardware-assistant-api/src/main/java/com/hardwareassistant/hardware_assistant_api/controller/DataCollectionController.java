package com.hardwareassistant.hardware_assistant_api.controller;

import com.hardwareassistant.hardware_assistant_api.dto.request.*;
import com.hardwareassistant.hardware_assistant_api.dto.response.ApiResponse;
import com.hardwareassistant.hardware_assistant_api.model.*;
import com.hardwareassistant.hardware_assistant_api.service.DataCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataCollectionController {

    private final DataCollectionService dataService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<Product>>> getProducts(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<Product> products = includeInactive
                ? dataService.getProducts(user)
                : dataService.getActiveProducts(user);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProductRequest request) {
        Product product = dataService.createProduct(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        Product product = dataService.updateProduct(user, id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated", product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        dataService.deleteProduct(user, id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<Page<SalesTransaction>>> getSales(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SalesTransaction> sales = dataService.getSales(user, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(sales));
    }

    @PostMapping("/sales")
    public ResponseEntity<ApiResponse<SalesTransaction>> recordSale(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SalesTransactionRequest request) {
        SalesTransaction sale = dataService.recordSale(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sale recorded", sale));
    }

    @DeleteMapping("/sales/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSale(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        dataService.deleteSale(user, id);
        return ResponseEntity.ok(ApiResponse.success("Sale deleted", null));
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<Page<ExpenseRecord>>> getExpenses(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ExpenseRecord> expenses = dataService.getExpenses(user, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<ExpenseRecord>> recordExpense(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseRecord expense = dataService.recordExpense(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense recorded", expense));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        dataService.deleteExpense(user, id);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted", null));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<Page<InventorySnapshot>>> getInventoryHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InventorySnapshot> snapshots = dataService.getInventorySnapshots(
                user, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(snapshots));
    }

    @GetMapping("/inventory/current")
    public ResponseEntity<ApiResponse<List<InventorySnapshot>>> getCurrentInventory(
            @AuthenticationPrincipal User user) {
        List<InventorySnapshot> current = dataService.getCurrentInventory(user);
        return ResponseEntity.ok(ApiResponse.success(current));
    }

    @PostMapping("/inventory")
    public ResponseEntity<ApiResponse<InventorySnapshot>> recordInventory(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody InventorySnapshotRequest request) {
        InventorySnapshot snapshot = dataService.recordInventory(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventory recorded", snapshot));
    }

    @DeleteMapping("/inventory/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInventorySnapshot(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        dataService.deleteInventorySnapshot(user, id);
        return ResponseEntity.ok(ApiResponse.success("Snapshot deleted", null));
    }
}