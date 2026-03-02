package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.dto.response.InventoryAlertResult;
import com.hardwareassistant.hardware_assistant_api.dto.response.InventoryAlertResult.InventoryAlert;
import com.hardwareassistant.hardware_assistant_api.exception.BusinessException;
import com.hardwareassistant.hardware_assistant_api.model.MerchantProfile;
import com.hardwareassistant.hardware_assistant_api.model.Product;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.MerchantProfileRepository;
import com.hardwareassistant.hardware_assistant_api.repository.ProductRepository;
import com.hardwareassistant.hardware_assistant_api.repository.SalesTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryIntelligenceService {

    private final ProductRepository         productRepository;
    private final SalesTransactionRepository salesRepo;
    private final MerchantProfileRepository  merchantProfileRepository;

    // ── Thresholds ────────────────────────────────────────────
    private static final BigDecimal OVERSTOCK_MULTIPLIER = BigDecimal.valueOf(3);
    private static final int        DEAD_STOCK_DAYS      = 30;

    @Transactional(readOnly = true)
    public InventoryAlertResult analyze(User user) {
        MerchantProfile profile = merchantProfileRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Please complete your merchant profile first"));

        List<Product> products = productRepository
                .findByMerchantProfileAndActiveOrderByNameAsc(profile, true);

        LocalDate today        = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(DEAD_STOCK_DAYS);

        // 3-month window for avg monthly sales
        LocalDate threeMonthsAgo = today.minusMonths(3);

        List<InventoryAlert> alerts = new ArrayList<>();

        for (Product product : products) {
            BigDecimal stock        = product.getCurrentStock()  != null ? product.getCurrentStock()  : BigDecimal.ZERO;
            BigDecimal reorderLevel = product.getReorderLevel()  != null ? product.getReorderLevel()  : BigDecimal.ZERO;

            // ── 1. LOW STOCK ──────────────────────────────────
            if (stock.compareTo(BigDecimal.ZERO) > 0 && stock.compareTo(reorderLevel) <= 0) {
                alerts.add(InventoryAlert.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .category(product.getCategory())
                        .type("LOW_STOCK")
                        .severity("CRITICAL")
                        .message(String.format(
                                "%s is running low — only %.0f %s remaining",
                                product.getName(), stock, unitLabel(product.getUnit())))
                        .action(String.format(
                                "Reorder now. Your reorder level is %.0f %s",
                                reorderLevel, unitLabel(product.getUnit())))
                        .currentStock(stock)
                        .reorderLevel(reorderLevel)
                        .build());
                continue; // no need to check overstock/dead stock if critically low
            }

            // ── 2. OUT OF STOCK ───────────────────────────────
            if (stock.compareTo(BigDecimal.ZERO) == 0) {
                alerts.add(InventoryAlert.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .category(product.getCategory())
                        .type("LOW_STOCK")
                        .severity("CRITICAL")
                        .message(product.getName() + " is completely out of stock")
                        .action("Restock immediately to avoid lost sales")
                        .currentStock(stock)
                        .reorderLevel(reorderLevel)
                        .build());
                continue;
            }

            // ── 3. OVERSTOCK ──────────────────────────────────
            BigDecimal qtySoldLast3Months = salesRepo.sumQuantitySoldByProduct(
                    profile, product.getId(), threeMonthsAgo, today);

            BigDecimal avgMonthlySales = qtySoldLast3Months
                    .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

            if (avgMonthlySales.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal overstockThreshold = avgMonthlySales.multiply(OVERSTOCK_MULTIPLIER);
                if (stock.compareTo(overstockThreshold) > 0) {
                    BigDecimal monthsOfStock = stock.divide(avgMonthlySales, 1, RoundingMode.HALF_UP);
                    alerts.add(InventoryAlert.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .category(product.getCategory())
                            .type("OVERSTOCK")
                            .severity("WARNING")
                            .message(String.format(
                                    "%s is overstocked — %.1f months of supply on hand",
                                    product.getName(), monthsOfStock))
                            .action("Consider running a promotion or reducing reorder quantities")
                            .currentStock(stock)
                            .reorderLevel(reorderLevel)
                            .avgMonthlySales(avgMonthlySales)
                            .build());
                }
            }

            // ── 4. DEAD STOCK ─────────────────────────────────
            if (stock.compareTo(BigDecimal.ZERO) > 0) {
                long recentSales = salesRepo.countSalesByProduct(
                        profile, product.getId(), thirtyDaysAgo, today);

                if (recentSales == 0) {
                    alerts.add(InventoryAlert.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .category(product.getCategory())
                            .type("DEAD_STOCK")
                            .severity("INFO")
                            .message(String.format(
                                    "%s has had no sales in the last 30 days",
                                    product.getName()))
                            .action("Consider discounting, bundling, or returning to supplier")
                            .currentStock(stock)
                            .reorderLevel(reorderLevel)
                            .avgMonthlySales(avgMonthlySales)
                            .build());
                }
            }
        }

        // Sort: CRITICAL first, then WARNING, then INFO
        alerts.sort(Comparator.comparingInt(a -> severityOrder(a.getSeverity())));

        long critical = alerts.stream().filter(a -> "CRITICAL".equals(a.getSeverity())).count();
        long warning  = alerts.stream().filter(a -> "WARNING".equals(a.getSeverity())).count();
        long info     = alerts.stream().filter(a -> "INFO".equals(a.getSeverity())).count();

        log.info("Inventory analysis for {}: {} alerts ({} critical, {} warning, {} info)",
                user.getEmail(), alerts.size(), critical, warning, info);

        return InventoryAlertResult.builder()
                .totalAlerts(alerts.size())
                .criticalCount((int) critical)
                .warningCount((int) warning)
                .infoCount((int) info)
                .alerts(alerts)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────

    private int severityOrder(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 0;
            case "WARNING"  -> 1;
            default         -> 2;
        };
    }

    private String unitLabel(String unit) {
        if (unit == null) return "units";
        return switch (unit.toLowerCase()) {
            case "piece", "pieces" -> "pcs";
            case "bag", "bags"     -> "bags";
            case "kg"              -> "kg";
            case "litre", "litres" -> "L";
            case "carton"          -> "cartons";
            case "dozen"           -> "dozens";
            default                -> unit;
        };
    }
}