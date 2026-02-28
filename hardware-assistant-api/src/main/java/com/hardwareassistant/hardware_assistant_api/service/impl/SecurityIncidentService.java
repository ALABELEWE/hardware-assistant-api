package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.model.SecurityIncident;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.repository.SecurityIncidentRepository;
import com.hardwareassistant.hardware_assistant_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityIncidentService {

    private static final int BAN_THRESHOLD = 3; // Ban after 3 confirmed injection attempts

    private final SecurityIncidentRepository incidentRepository;
    private final UserRepository             userRepository;

    // ── Core: persist an incident ─────────────────────────────────────────

    @Transactional
    public void record(User user, String fieldName, String flaggedInput,
                       String matchedPattern, SecurityIncident.Severity severity,
                       int attemptCount) {
        SecurityIncident incident = SecurityIncident.builder()
                .user(user)
                .userEmail(user != null ? user.getEmail() : "anonymous")
                .fieldName(fieldName)
                .flaggedInput(flaggedInput)
                .matchedPattern(matchedPattern)
                .severity(severity)
                .userAttemptCount(attemptCount)
                .build();

        incidentRepository.save(incident);
        log.warn("SecurityIncident persisted — user={} severity={} field={} attempt={}",
                user != null ? user.getEmail() : "anonymous", severity, fieldName, attemptCount);
    }

    // ── Escalation: ban user if threshold reached ─────────────────────────

    @Transactional
    public void banIfThresholdReached(User user, long attemptCount) {
        if (user == null) return;
        if (user.getRole() == User.Role.ADMIN) {
            log.info("Ban skipped — user={} is ADMIN", user.getEmail());
            return;
        }
        if (attemptCount >= BAN_THRESHOLD && !user.isBanned()) {
            user.setBanned(true);
            user.setBanReason("Repeated prompt injection attempts (" + attemptCount + " incidents)");
            user.setBannedAt(LocalDateTime.now());
            userRepository.save(user);
            log.warn("USER BANNED — email={} reason='{}' totalAttempts={}",
                    user.getEmail(), user.getBanReason(), attemptCount);
        }
    }

    // ── Count attempts ────────────────────────────────────────────────────

    public long countAttempts(User user) {
        if (user == null) return 0;
        return incidentRepository.countByUser(user);
    }

    public long countAttemptsInWindow(User user, int hours) {
        if (user == null) return 0;
        return incidentRepository.countByUserAndCreatedAtAfter(
                user, LocalDateTime.now().minusHours(hours));
    }

    // ── Admin analytics ───────────────────────────────────────────────────

    public Page<SecurityIncident> getAllIncidents(int page, int size) {
        return incidentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public List<SecurityIncident> getIncidentsByUser(User user) {
        return incidentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long countIncidentsThisMonth() {
        return incidentRepository.countIncidentsSince(
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0));
    }

    public List<Object[]> getIncidentsPerDay(int days) {
        return incidentRepository.countIncidentsPerDaySince(
                LocalDateTime.now().minusDays(days));
    }

    public List<Object[]> getTopOffenders(int limit) {
        return incidentRepository.topOffenders(PageRequest.of(0, limit));
    }
}