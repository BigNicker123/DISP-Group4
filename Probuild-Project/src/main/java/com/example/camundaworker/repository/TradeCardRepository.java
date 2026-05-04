package com.example.camundaworker.repository;

import com.example.camundaworker.model.TradeCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradeCardRepository extends JpaRepository<TradeCard, Long> {
    Optional<TradeCard> findByCardNumberIgnoreCase(String cardNumber);
}
