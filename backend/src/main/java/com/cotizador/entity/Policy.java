package com.cotizador.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * An issued policy. At most one per quote (rule E2, enforced by the UNIQUE on {@code quote_id}).
 * Vehicle and plan data are read from its quote, which is never modified.
 */
@Entity
@Table(name = "policies")
public class Policy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "policy_number", nullable = false, unique = true, length = 20)
	private String policyNumber;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quote_id", nullable = false, unique = true)
	private Quote quote;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "producer_id", nullable = false)
	private Producer producer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "insured_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal insuredAmount;

	@Column(name = "premium", nullable = false, precision = 15, scale = 2)
	private BigDecimal premium;

	@Column(name = "taxes", nullable = false, precision = 15, scale = 2)
	private BigDecimal taxes;

	@Column(name = "total_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalPrice;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PolicyStatus status;

	@Column(name = "issued_at", nullable = false)
	private OffsetDateTime issuedAt;

	public Long getId() {
		return id;
	}

	public String getPolicyNumber() {
		return policyNumber;
	}

	public void setPolicyNumber(String policyNumber) {
		this.policyNumber = policyNumber;
	}

	public Quote getQuote() {
		return quote;
	}

	public void setQuote(Quote quote) {
		this.quote = quote;
	}

	public Producer getProducer() {
		return producer;
	}

	public void setProducer(Producer producer) {
		this.producer = producer;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public BigDecimal getInsuredAmount() {
		return insuredAmount;
	}

	public void setInsuredAmount(BigDecimal insuredAmount) {
		this.insuredAmount = insuredAmount;
	}

	public BigDecimal getPremium() {
		return premium;
	}

	public void setPremium(BigDecimal premium) {
		this.premium = premium;
	}

	public BigDecimal getTaxes() {
		return taxes;
	}

	public void setTaxes(BigDecimal taxes) {
		this.taxes = taxes;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public PolicyStatus getStatus() {
		return status;
	}

	public void setStatus(PolicyStatus status) {
		this.status = status;
	}

	public OffsetDateTime getIssuedAt() {
		return issuedAt;
	}

	public void setIssuedAt(OffsetDateTime issuedAt) {
		this.issuedAt = issuedAt;
	}

	/**
	 * Expiration on read (backend.md §7.2, manual §8.2): an ACTIVE policy becomes EXPIRED when {@code today} is
	 * after its end date. A policy is still ACTIVE on its end date. Returns true when the status changed.
	 */
	public boolean expireIfOverdue(LocalDate today) {
		if (status == PolicyStatus.ACTIVE && today.isAfter(endDate)) {
			status = PolicyStatus.EXPIRED;
			return true;
		}
		return false;
	}

}
