package com.hardwareassistant.hardware_assistant_api.service.impl;

import com.hardwareassistant.hardware_assistant_api.service.EmailService;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${app.resend.api-key}")
    private String apiKey;

    @Value("${app.resend.from-email}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            Resend resend = new Resend(apiKey);

            String html = buildResetEmailHtml(resetLink);

            CreateEmailOptions options = CreateEmailOptions.builder()
                .from("HardwareAI <" + fromEmail + ">")
                .to(toEmail)
                .subject("Reset your HardwareAI password")
                .html(html)
                .build();

            resend.emails().send(options);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (ResendException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send reset email");
        }
    }

    private String buildResetEmailHtml(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background:#0a0a0a;font-family:Inter,sans-serif;">
              <div style="max-width:520px;margin:40px auto;background:#111;border:1px solid #222;border-radius:16px;overflow:hidden;">
                
                <!-- Header -->
                <div style="background:#f97316;padding:32px;text-align:center;">
                  <div style="display:inline-flex;align-items:center;gap:10px;">
                    <div style="background:rgba(255,255,255,0.2);border-radius:10px;padding:8px;">
                      <span style="font-size:20px;">🔧</span>
                    </div>
                    <span style="color:white;font-size:22px;font-weight:700;">HardwareAI</span>
                  </div>
                </div>

                <!-- Body -->
                <div style="padding:40px 32px;">
                  <h1 style="color:white;font-size:24px;font-weight:700;margin:0 0 8px;">
                    Reset your password
                  </h1>
                  <p style="color:#888;font-size:15px;line-height:1.6;margin:0 0 32px;">
                    We received a request to reset your HardwareAI password. 
                    Click the button below to create a new password.
                  </p>

                  <a href="%s"
                    style="display:block;background:#f97316;color:white;text-decoration:none;
                      text-align:center;padding:16px 24px;border-radius:12px;
                      font-weight:600;font-size:15px;margin-bottom:24px;">
                    Reset Password →
                  </a>

                  <p style="color:#555;font-size:13px;line-height:1.6;margin:0 0 8px;">
                    This link expires in <strong style="color:#888;">1 hour</strong>.
                  </p>
                  <p style="color:#555;font-size:13px;line-height:1.6;margin:0;">
                    If you didn't request this, you can safely ignore this email. 
                    Your password will not be changed.
                  </p>
                </div>

                <!-- Footer -->
                <div style="padding:24px 32px;border-top:1px solid #222;text-align:center;">
                  <p style="color:#444;font-size:12px;margin:0;">
                    © 2026 HardwareAI · Built for Lagos merchants
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(resetLink);
    }
}