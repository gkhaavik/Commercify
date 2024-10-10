package com.gostavdev.commercify.paymentservice.services;

import com.gostavdev.commercify.paymentservice.PaymentProvider;
import com.gostavdev.commercify.paymentservice.dto.CancelPaymentResponse;
import com.gostavdev.commercify.paymentservice.dto.PaymentRequest;
import com.gostavdev.commercify.paymentservice.dto.PaymentResponse;
import com.gostavdev.commercify.paymentservice.entities.PaymentEntity;
import com.gostavdev.commercify.paymentservice.entities.PaymentStatus;
import com.gostavdev.commercify.paymentservice.repositories.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final StripeService stripeService;

    // Get payment status by orderId
    public PaymentStatus getPaymentStatus(Long orderId) {
        Optional<PaymentEntity> payment = paymentRepository.findByOrderId(orderId);
        return payment.map(PaymentEntity::getStatus).orElse(PaymentStatus.NOT_FOUND);
    }

    public CancelPaymentResponse cancelPayment(Long orderId) {
        PaymentEntity payment = paymentRepository.findByOrderId(orderId).orElse(null);

        if (payment == null) {
            return CancelPaymentResponse.PaymentNotFound();
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            return CancelPaymentResponse.PaymentAlreadyPaid();
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return CancelPaymentResponse.PaymentAlreadyCanceled();
        }

        if (payment.getPaymentProvider() == PaymentProvider.STRIPE) {
            return stripeService.cancelPayment(payment.getPaymentId());
        }

        return CancelPaymentResponse.InvalidPaymentProvider();
    }

    public PaymentResponse makePayment(PaymentProvider provider, PaymentRequest paymentRequest) {
        if (provider == PaymentProvider.STRIPE) {
            return stripeService.checkoutSession(paymentRequest);
        }

        return PaymentResponse.FailedPayment();
    }

    public void updatePaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        Optional<PaymentEntity> payment = paymentRepository.findByOrderId(orderId);
        payment.ifPresent(p -> {
            p.setStatus(paymentStatus);
            paymentRepository.save(p);
        });
    }
}