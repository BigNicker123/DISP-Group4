package com.example.camundaworker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "trade_cards")
public class TradeCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String cardNumber;

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false)
    private boolean active;

    public TradeCard() {}

    public TradeCard(String cardNumber, String holderName, boolean active) {
        this.cardNumber = cardNumber;
        this.holderName = holderName;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getCardNumber() { return cardNumber; }
    public String getHolderName() { return holderName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
