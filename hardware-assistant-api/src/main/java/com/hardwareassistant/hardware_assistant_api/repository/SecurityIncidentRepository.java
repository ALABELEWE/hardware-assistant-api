package com.hardwareassistant.hardware_assistant_api.repository;

import com.hardwareassistant.hardware_assistant_api.model.SecurityIncident;
import com.hardwareassistant.hardware_assistant_api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SecurityIncidentRepository extends JpaRepository<SecurityIncident, UUID> {

    // How many injection attempts has this user made total?
    long countByUser(User user);

    // Attempts in a rolling time window (used for escalation logic)
    long countByUserAndCreatedAtAfter(User user, LocalDateTime since);

    // All incidents for a specific user (admin view)
    List<SecurityIncident> findByUserOrderByCreatedAtDesc(User user);

    // Paginated incident log for admin dashboard
    Page<SecurityIncident> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Platform-wide stats — incidents per day for chart
    @Query("""
        SELECT DATE(s.createdAt), COUNT(s)
        FROM SecurityIncident s
        WHERE s.createdAt >= :since
        GROUP BY DATE(s.createdAt)
        ORDER BY DATE(s.createdAt)
    """)
    List<Object[]> countIncidentsPerDaySince(@Param("since") LocalDateTime since);

    // Top offenders (admin analytics)
    @Query("""
        SELECT s.userEmail, COUNT(s) as total
        FROM SecurityIncident s
        WHERE s.userEmail IS NOT NULL
        GROUP BY s.userEmail
        ORDER BY total DESC
    """)
    List<Object[]> topOffenders(Pageable pageable);

    // Total incident count for current month
    @Query("""
        SELECT COUNT(s) FROM SecurityIncident s
        WHERE s.createdAt >= :monthStart
    """)
    long countIncidentsSince(@Param("monthStart") LocalDateTime monthStart);
}