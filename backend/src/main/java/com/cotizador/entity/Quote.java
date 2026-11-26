package com.cotizador.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.Table;

/**
 * A saved quote. It stores every value used in the calculation (rule Q2), so later changes to tariffs or
 * catalog do not alter it. The vehicle is not a separate entity: its data lives here.
 */
@Entity
@Table(name = "quotes")
public class Quote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "quote_number", nullable = false, unique = true, length = 20)
	private String quoteNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "producer_id", nullable = false)
	private Producer producer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@Enumerated(EnumType.STRING)
	@Column(name = "vehicle_type", nullable = false, length = 20)
	private VehicleType vehicleType;

	@Column(name = "manufacturing_year", nullable = false)
	private Integer manufacturingYear;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "brand_id", nullable = false)
	private Brand brand;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "model_id", nullable = false)
	private VehicleModel model;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "version_id", nullable = false)
	private VehicleVersion version;

	@Column(name = "brand_name", nullable = false, length = 100)
	private String brandName;

	@Column(name = "model_name", nullable = false, length = 100)
	private String modelName;

	@Column(name = "version_name", nullable = false, length = 100)
	private String versionName;

	/** Uppercase. */
	@Column(name = "patent", nullable = false, length = 6)
	private String patent;

	@Enumerated(EnumType.STRING)
	@Column(name = "usage_type", nullable = false, length = 30)
	private VehicleUsageType usageType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_id", nullable = false)
	private InsurancePlan plan;

	@Column(name = "plan_name", nullable = false, length = 100)
	private String planName;

	@Column(name = "plan_rate", nullable = false, precision = 6, scale = 4)
	private BigDecimal planRate;

	@Column(name = "insured_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal insuredAmount;

	@Column(name = "zone_code", nullable = false, length = 10)
	private String zoneCode;

	@Column(name = "zone_factor", nullable = false, precision = 6, scale = 4)
	private BigDecimal zoneFactor;

	@Column(name = "usage_factor", nullable = false, precision = 6, scale = 4)
	private BigDecimal usageFactor;

	@Column(name = "vehicle_age_years", nullable = false)
	private Integer vehicleAgeYears;

	@Column(name = "vehicle_age_factor", nullable = false, precision = 6, scale = 4)
	private BigDecimal vehicleAgeFactor;

	/** Null when the client is a company (CUIT). */
	@Column(name = "driver_age")
	private Integer driverAge;

	@Column(name = "driver_age_factor", nullable = false, precision = 6, scale = 4)
	private BigDecimal driverAgeFactor;

	@Column(name = "base_premium", nullable = false, precision = 15, scale = 2)
	private BigDecimal basePremium;

	@Column(name = "premium", nullable = false, precision = 15, scale = 2)
	private BigDecimal premium;

	@Column(name = "tax_rate", nullable = false, precision = 6, scale = 4)
	private BigDecimal taxRate;

	@Column(name = "taxes", nullable = false, precision = 15, scale = 2)
	private BigDecimal taxes;

	@Column(name = "total_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalPrice;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private QuoteStatus status;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "valid_until", nullable = false)
	private OffsetDateTime validUntil;

	public Long getId() {
		return id;
	}

	public String getQuoteNumber() {
		return quoteNumber;
	}

	public void setQuoteNumber(String quoteNumber) {
		this.quoteNumber = quoteNumber;
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

	public VehicleType getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	public Integer getManufacturingYear() {
		return manufacturingYear;
	}

	public void setManufacturingYear(Integer manufacturingYear) {
		this.manufacturingYear = manufacturingYear;
	}

	public Brand getBrand() {
		return brand;
	}

	public void setBrand(Brand brand) {
		this.brand = brand;
	}

	public VehicleModel getModel() {
		return model;
	}

	public void setModel(VehicleModel model) {
		this.model = model;
	}

	public VehicleVersion getVersion() {
		return version;
	}

	public void setVersion(VehicleVersion version) {
		this.version = version;
	}

	public String getBrandName() {
		return brandName;
	}

	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public String getPatent() {
		return patent;
	}

	public void setPatent(String patent) {
		this.patent = patent;
	}

	public VehicleUsageType getUsageType() {
		return usageType;
	}

	public void setUsageType(VehicleUsageType usageType) {
		this.usageType = usageType;
	}

	public InsurancePlan getPlan() {
		return plan;
	}

	public void setPlan(InsurancePlan plan) {
		this.plan = plan;
	}

	public String getPlanName() {
		return planName;
	}

	public void setPlanName(String planName) {
		this.planName = planName;
	}

	public BigDecimal getPlanRate() {
		return planRate;
	}

	public void setPlanRate(BigDecimal planRate) {
		this.planRate = planRate;
	}

	public BigDecimal getInsuredAmount() {
		return insuredAmount;
	}

	public void setInsuredAmount(BigDecimal insuredAmount) {
		this.insuredAmount = insuredAmount;
	}

	public String getZoneCode() {
		return zoneCode;
	}

	public void setZoneCode(String zoneCode) {
		this.zoneCode = zoneCode;
	}

	public BigDecimal getZoneFactor() {
		return zoneFactor;
	}

	public void setZoneFactor(BigDecimal zoneFactor) {
		this.zoneFactor = zoneFactor;
	}

	public BigDecimal getUsageFactor() {
		return usageFactor;
	}

	public void setUsageFactor(BigDecimal usageFactor) {
		this.usageFactor = usageFactor;
	}

	public Integer getVehicleAgeYears() {
		return vehicleAgeYears;
	}

	public void setVehicleAgeYears(Integer vehicleAgeYears) {
		this.vehicleAgeYears = vehicleAgeYears;
	}

	public BigDecimal getVehicleAgeFactor() {
		return vehicleAgeFactor;
	}

	public void setVehicleAgeFactor(BigDecimal vehicleAgeFactor) {
		this.vehicleAgeFactor = vehicleAgeFactor;
	}

	public Integer getDriverAge() {
		return driverAge;
	}

	public void setDriverAge(Integer driverAge) {
		this.driverAge = driverAge;
	}

	public BigDecimal getDriverAgeFactor() {
		return driverAgeFactor;
	}

	public void setDriverAgeFactor(BigDecimal driverAgeFactor) {
		this.driverAgeFactor = driverAgeFactor;
	}

	public BigDecimal getBasePremium() {
		return basePremium;
	}

	public void setBasePremium(BigDecimal basePremium) {
		this.basePremium = basePremium;
	}

	public BigDecimal getPremium() {
		return premium;
	}

	public void setPremium(BigDecimal premium) {
		this.premium = premium;
	}

	public BigDecimal getTaxRate() {
		return taxRate;
	}

	public void setTaxRate(BigDecimal taxRate) {
		this.taxRate = taxRate;
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

	public QuoteStatus getStatus() {
		return status;
	}

	public void setStatus(QuoteStatus status) {
		this.status = status;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getValidUntil() {
		return validUntil;
	}

	public void setValidUntil(OffsetDateTime validUntil) {
		this.validUntil = validUntil;
	}

	/**
	 * Expiration on read (backend.md §7.2, rule Q3): a CALCULATED quote whose validity ended before {@code now}
	 * becomes EXPIRED. An EMITTED quote never expires (manual §8.1). Returns true when the status changed.
	 */
	public boolean expireIfOverdue(OffsetDateTime now) {
		if (status == QuoteStatus.CALCULATED && now.isAfter(validUntil)) {
			status = QuoteStatus.EXPIRED;
			return true;
		}
		return false;
	}

}
