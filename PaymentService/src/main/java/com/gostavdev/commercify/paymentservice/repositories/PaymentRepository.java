package com.gostavdev.commercify.paymentservice.repositories;

import com.gostavdev.commercify.paymentservice.entities.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByOrderId(Long orderId);

    Optional<PaymentEntity> findByStripePaymentIntent(String stripePaymentIntent);
}
