package com.gostavdev.commercify.paymentservice.services;

import com.gostavdev.commercify.paymentservice.OrderClient;
import com.gostavdev.commercify.paymentservice.PaymentProvider;
import com.gostavdev.commercify.paymentservice.dto.*;
import com.gostavdev.commercify.paymentservice.entities.PaymentEntity;
import com.gostavdev.commercify.paymentservice.entities.PaymentStatus;
import com.gostavdev.commercify.paymentservice.exceptions.InvalidEventDataException;
import com.gostavdev.commercify.paymentservice.repositories.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class StripeService {
    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final PaymentService paymentService;

    public PaymentResponse checkoutSession(PaymentRequest paymentRequest) {
        OrderDTO order = orderClient.getOrderById(paymentRequest.orderId());
        if (order == null) {
            throw new InvalidEventDataException("Order not found");
        }

        List<OrderLineDTO> orderLineDTOS = order.orderLines();

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(paymentRequest.successUrl())
                        .setCancelUrl(paymentRequest.cancelUrl())
                        .setCurrency(paymentRequest.currency())
                        .addAllLineItem(orderLineDTOS.stream().map(ol -> {
                            Long quantity = Long.valueOf(ol.quantity());
                            if (quantity <= 0)
                                throw new InvalidEventDataException("Invalid quantity for order line");

                            SessionCreateParams.LineItem.Builder lineItem = SessionCreateParams.LineItem.builder()
                                    .setQuantity(quantity);

                            try {
                                Product product = Product.retrieve(ol.stripeProductId());
                                lineItem.setPrice(product.getDefaultPrice());
                            } catch (StripeException e) {
                                throw new RuntimeException(e);
                            }

                            return lineItem.build();
                        }).toList())
                        .putMetadata("orderId", paymentRequest.orderId().toString())
                        .build();
        try {
            // Create the PaymentIntent
            Session session = Session.create(params);

            // Save the payment in our local database with status "PENDING"
            PaymentEntity payment = PaymentEntity.builder()
                    .orderId(paymentRequest.orderId())
                    .stripePaymentIntent(session.getPaymentIntent())
                    .paymentProvider(PaymentProvider.STRIPE)
                    .status(PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(payment);

            System.out.println("Payment session created: " + session.getUrl());

            orderClient.updateOrderStatus(paymentRequest.orderId(), "CONFIRMED");

            // Return the payment intent's client secret for client-side confirmation
            return new PaymentResponse(payment.getPaymentId(), payment.getStatus(), session.getUrl());
        } catch (StripeException e) {
            System.out.println("Error processing payment: " + e.getMessage());
            return PaymentResponse.FailedPayment();
        }
    }

    public CancelPaymentResponse cancelPayment(Long paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(payment.getStripePaymentIntent());
            paymentIntent.cancel();
        } catch (StripeException e) {
            return new CancelPaymentResponse(false, e.getMessage());
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        return new CancelPaymentResponse(true, "Payment cancelled successfully");
    }

    public void handlePaymentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
        PaymentEntity payment = paymentRepository.findByStripePaymentIntent(paymentIntent.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentIntent.getId()));
        paymentService.updatePaymentStatus(payment.getOrderId(), PaymentStatus.PAID);
    }

    public void handlePaymentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
        PaymentEntity payment = paymentRepository.findByStripePaymentIntent(paymentIntent.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentIntent.getId()));
        paymentService.updatePaymentStatus(payment.getOrderId(), PaymentStatus.FAILED);
    }
}

