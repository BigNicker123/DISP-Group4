package com.example.camundaworker.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String bookingReference;

    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String customerPhone;
    private String toolNames;
    private int quantity;
    private LocalDate hireStartDate;
    private LocalDate hireEndDate;
    private int hireDuration;
    private double totalHireCost;
    private double depositAmount;
    private double totalAmount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Booking() {}

    public Booking(String bookingReference, String customerFirstName, String customerLastName,
                   String customerEmail, String customerPhone, String toolNames, int quantity,
                   LocalDate hireStartDate, LocalDate hireEndDate, int hireDuration,
                   double totalHireCost, double depositAmount, double totalAmount) {
        this.bookingReference = bookingReference;
        this.customerFirstName = customerFirstName;
        this.customerLastName = customerLastName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.toolNames = toolNames;
        this.quantity = quantity;
        this.hireStartDate = hireStartDate;
        this.hireEndDate = hireEndDate;
        this.hireDuration = hireDuration;
        this.totalHireCost = totalHireCost;
        this.depositAmount = depositAmount;
        this.totalAmount = totalAmount;
        this.status = "CONFIRMED";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getBookingReference() { return bookingReference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCustomerFirstName() { return customerFirstName; }
    public String getCustomerLastName() { return customerLastName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public String getToolNames() { return toolNames; }
    public int getQuantity() { return quantity; }
    public LocalDate getHireStartDate() { return hireStartDate; }
    public LocalDate getHireEndDate() { return hireEndDate; }
    public int getHireDuration() { return hireDuration; }
    public double getTotalHireCost() { return totalHireCost; }
    public double getDepositAmount() { return depositAmount; }
    public double getTotalAmount() { return totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
