package com.openmarket.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "receipts")
@EqualsAndHashCode(callSuper = false)
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
    @Column(nullable = false)
    private String name;
    @Column(name = "request_number")
    private String requestNumber;
    @Column(name = "receipt_date")
    private LocalDate receiptDate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_contract_id")
    private CounterpartyContract counterpartyContract;
    /** Snapshot of contract at the time of receipt creation */
    @Column
    private String contract;
    @Column(name = "total_cost", precision = 18, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReceiptItem> items;
}

