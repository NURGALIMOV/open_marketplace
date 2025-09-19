package com.openmarket.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "counterparties_contracts")
@EqualsAndHashCode(callSuper = false)
public class CounterpartyContract {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
    @Column(nullable = false)
    private String counterparty;
    @Column(nullable = false)
    private String contract;
    @Column(name = "contract_date")
    private LocalDate contractDate;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @OneToMany(mappedBy = "counterpartyContract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Receipt> receipts;
}

