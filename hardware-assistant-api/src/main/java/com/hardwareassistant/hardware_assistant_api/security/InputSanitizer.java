package com.hardwareassistant.hardware_assistant_api.security;

import com.hardwareassistant.hardware_assistant_api.model.SecurityIncident;
import com.hardwareassistant.hardware_assistant_api.model.User;
import com.hardwareassistant.hardware_assistant_api.service.impl.SecurityIncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class InputSanitizer {

    private static final int MAX_FIELD_LENGTH        = 500;
    private static final int MAX_DESCRIPTION_LENGTH  = 1000;

    // Thresholds for escalation
    static final int WARN_THRESHOLD  = 1; // 1st attempt  → WARNING
    static final int BLOCK_THRESHOLD = 3; // 3rd+ attempt → BLOCKED + ban trigger

    private static final List<PatternEntry> INJECTION_PATTERNS = List.of(
            // ── Existing patterns ─────────────────────────────────────────────
            new PatternEntry("ignore_instructions",  Pattern.compile("ignore (all |previous )?instructions?",               Pattern.CASE_INSENSITIVE)),
            new PatternEntry("system_override",      Pattern.compile("system (prompt|override|message)",                    Pattern.CASE_INSENSITIVE)),
            new PatternEntry("persona_hijack",       Pattern.compile("you are now",                                         Pattern.CASE_INSENSITIVE)),
            new PatternEntry("data_extraction",      Pattern.compile("reveal|disclose|expose|extract|output (your|the|all)",Pattern.CASE_INSENSITIVE)),
            new PatternEntry("control_tokens",       Pattern.compile("\\<\\|im_start\\|\\>|\\<\\|im_end\\|\\>")),
            new PatternEntry("jailbreak",            Pattern.compile("DAN|jailbreak|developer mode|god mode",               Pattern.CASE_INSENSITIVE)),
            new PatternEntry("credential_fishing",   Pattern.compile("api.?key|secret|password|token|credential",          Pattern.CASE_INSENSITIVE)),
            new PatternEntry("delimiter_injection",  Pattern.compile("---+|===+|###+")),
            new PatternEntry("newline_injection",    Pattern.compile("\\\\n\\\\n|\\n{3,}")),
            new PatternEntry("system_boundary",      Pattern.compile("END OF INPUT|BEGIN SYSTEM|SYSTEM OVERRIDE",          Pattern.CASE_INSENSITIVE)),
            new PatternEntry("role_play",            Pattern.compile("act as|pretend (you are|to be)|roleplay",            Pattern.CASE_INSENSITIVE)),
            new PatternEntry("prompt_leak",          Pattern.compile("repeat (everything|your instructions|the above)",    Pattern.CASE_INSENSITIVE)),

            // ── Authority impersonation (Attack 1) ────────────────────────────
            new PatternEntry("authority_claim",      Pattern.compile("(this request is |i am |coming from ).{0,30}(admin|administrator|system|security|developer|support|staff|operator)", Pattern.CASE_INSENSITIVE)),
            new PatternEntry("security_audit",       Pattern.compile("security (audit|review|test|scan|check|assessment)", Pattern.CASE_INSENSITIVE)),
            new PatternEntry("override_restrictions",Pattern.compile("override (your|all|the|my|any).{0,20}(restriction|limit|rule|policy|constraint|filter|safety|guideline)", Pattern.CASE_INSENSITIVE)),
            new PatternEntry("display_config",       Pattern.compile("display (your|the|all|hidden|internal).{0,20}(config|configuration|rule|policy|prompt|instruction|setting)", Pattern.CASE_INSENSITIVE)),
            new PatternEntry("hidden_config",        Pattern.compile("hidden (config|configuration|message|instruction|prompt|rule|policy|setting)", Pattern.CASE_INSENSITIVE)),
            new PatternEntry("internal_policy",      Pattern.compile("internal (policy|policies|rule|setting|config|prompt|instruction|guideline)", Pattern.CASE_INSENSITIVE)),
            new PatternEntry("safety_rules",         Pattern.compile("safety (rule|rules|policy|policies|filter|restriction|guideline)", Pattern.CASE_INSENSITIVE)),

            // ── Function call injection (Attack 2) ────────────────────────────
            new PatternEntry("function_call",        Pattern.compile("call (the |a )?(\\w+) ?(function|method|api|endpoint|command)", Pattern.CASE_INSENSITIVE)),
            new PatternEntry("function_with_params", Pattern.compile("(function|method|command)\\s*\\(|\\w+\\s*=\\s*\\d+",           Pattern.CASE_INSENSITIVE)),
            new PatternEntry("delete_operation",     Pattern.compile("delete(User|Account|Record|Data|All|Every)",                    Pattern.CASE_INSENSITIVE)),
            new PatternEntry("execute_command",      Pattern.compile("execute|run (the |a )?(command|script|function|query|code)",    Pattern.CASE_INSENSITIVE)),

            // ── System prompt extraction (Attack 3) ───────────────────────────
            new PatternEntry("print_conversation",   Pattern.compile("print (the |your |all |complete |full )?(conversation|message|chat|history|log|thread)", Pattern.CASE_INSENSITIVE)),
            new PatternEntry("hidden_messages",      Pattern.compile("hidden (message|messages|prompt|instruction|system)",           Pattern.CASE_INSENSITIVE)),
            new PatternEntry("developer_messages",   Pattern.compile("developer (message|messages|prompt|instruction|note)",         Pattern.CASE_INSENSITIVE)),
            new PatternEntry("system_level",         Pattern.compile("system.level (config|configuration|message|instruction|prompt|setting)", Pattern.CASE_INSENSITIVE)),
            new PatternEntry("output_verbatim",      Pattern.compile("output (everything|verbatim|all|the above|complete|full)",     Pattern.CASE_INSENSITIVE)),
            new PatternEntry("do_not_summarize",     Pattern.compile("do not summarize|don'?t summarize|no summary|output raw|verbatim output", Pattern.CASE_INSENSITIVE)),
            new PatternEntry("transparency_dump",    Pattern.compile("for (transparency|full disclosure|completeness).{0,30}(print|output|show|display|reveal|list)", Pattern.CASE_INSENSITIVE))
    );

    // Named patterns for analytics
    public record PatternEntry(String name, Pattern pattern) {}

    private final SecurityIncidentService incidentService;

    /**
     * Sanitise a single field and return a typed result.
     * Never throws — callers decide what to do with WARNING/BLOCKED.
     */
    public SanitizationResult sanitize(String input, String fieldName, User currentUser) {
        if (input == null) return SanitizationResult.clean("");

        // ── Length limit ──────────────────────────────────────────────────
        int maxLength = fieldName.equals("description") || fieldName.equals("products")
                ? MAX_DESCRIPTION_LENGTH : MAX_FIELD_LENGTH;
        if (input.length() > maxLength) {
            log.warn("Input truncated for field: {} - original length: {}", fieldName, input.length());
            input = input.substring(0, maxLength);
        }

        // ── Injection pattern check ───────────────────────────────────────
        for (PatternEntry entry : INJECTION_PATTERNS) {
            if (entry.pattern().matcher(input).find()) {

                String preview     = input.substring(0, Math.min(100, input.length()));
                long   totalAttempts = incidentService.countAttempts(currentUser);

                log.warn("SECURITY ALERT - pattern='{}' field='{}' user='{}' totalAttempts={}",
                        entry.name(), fieldName,
                        currentUser != null ? currentUser.getEmail() : "anonymous",
                        totalAttempts + 1);

                if (totalAttempts + 1 >= BLOCK_THRESHOLD) {
                    // Persist as BLOCKED incident
                    incidentService.record(currentUser, fieldName, preview,
                            entry.name(), SecurityIncident.Severity.BLOCKED, (int)(totalAttempts + 1));
                    // Trigger ban if threshold crossed
                    incidentService.banIfThresholdReached(currentUser, totalAttempts + 1);
                    return SanitizationResult.blocked(fieldName, entry.name());
                } else {
                    // Persist as WARNING incident
                    incidentService.record(currentUser, fieldName, preview,
                            entry.name(), SecurityIncident.Severity.WARNING, (int)(totalAttempts + 1));
                    return SanitizationResult.warning(fieldName, entry.name(), preview);
                }
            }
        }

        // ── Strip control characters ──────────────────────────────────────
        input = input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        input = input.replaceAll("<\\|.*?\\|>", "");

        return SanitizationResult.clean(input.trim());
    }

    /**
     * Sanitise all merchant profile fields at once.
     * Returns the first non-clean result found, or CLEAN if all pass.
     */
    public SanitizationResult sanitizeMerchantProfile(
            String businessName, String location,
            String products, String customerType,
            User currentUser) {

        String[] values = { businessName, location, products, customerType };
        String[] fields = { "businessName", "location", "products", "customerType" };

        for (int i = 0; i < values.length; i++) {
            SanitizationResult result = sanitize(values[i], fields[i], currentUser);
            if (!result.isClean()) return result;
        }
        return SanitizationResult.clean("");
    }
}