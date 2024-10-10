package com.gostavdev.commercify.paymentservice.controllers;

import com.gostavdev.commercify.paymentservice.services.StripeService;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/stripe")
public class StripeController {
    private final StripeService stripeService;
    private final String stripeWebhookSecret;

    @PostMapping("/webhooks")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) {
        String payload;
        String sigHeader = request.getHeader("Stripe-Signature");

        // Read the payload (body) of the webhook request
        try (BufferedReader reader = request.getReader()) {
            payload = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error reading payload");
        }

        Event event;
        try {
            // Verify and construct the event
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Webhook signature verification failed");
        }

        // Handle the event
        switch (event.getType()) {
            case "payment_intent.succeeded":
                // Handle successful payment
                stripeService.handlePaymentSucceeded(event);
                break;
            case "payment_intent.payment_failed":
                // Handle failed payment
                stripeService.handlePaymentFailed(event);
                break;
            // Add more cases as needed for other events like refunds, disputes, etc.
            default:
                return ResponseEntity.ok("Unhandled event type");
        }

        return ResponseEntity.ok("Event processed");
    }
}
