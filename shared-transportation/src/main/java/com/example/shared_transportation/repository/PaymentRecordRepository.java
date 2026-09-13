package com.example.shared_transportation.repository;

import com.example.shared_transportation.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    Optional<PaymentRecord> findByOutTradeNo(String outTradeNo);
}
