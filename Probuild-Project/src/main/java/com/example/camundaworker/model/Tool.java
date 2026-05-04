package com.example.camundaworker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tools")
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private double purchasePrice;

    @Column(nullable = false)
    private double hireRatePerDay;

    @Column(nullable = false)
    private double depositAmount;

    @Column(nullable = false)
    private int availableQuantity;

    public Tool() {}

    public Tool(String name, String displayName, double purchasePrice, double hireRatePerDay, double depositAmount, int availableQuantity) {
        this.name = name;
        this.displayName = displayName;
        this.purchasePrice = purchasePrice;
        this.hireRatePerDay = hireRatePerDay;
        this.depositAmount = depositAmount;
        this.availableQuantity = availableQuantity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public double getPurchasePrice() { return purchasePrice; }
    public double getHireRatePerDay() { return hireRatePerDay; }
    public double getDepositAmount() { return depositAmount; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
}
