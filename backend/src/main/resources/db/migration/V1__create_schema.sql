-- Schema from docs/spec/data-model.md

-- 3. Security and people

CREATE TABLE producers (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    license_number VARCHAR(20)  NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_producers_email UNIQUE (email),
    CONSTRAINT uk_producers_license_number UNIQUE (license_number),
    CONSTRAINT ck_producers_email_lowercase CHECK (email = LOWER(email))
);

-- 4. Location and zones

CREATE TABLE tariff_zones (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code   VARCHAR(10)  NOT NULL,
    name   VARCHAR(100) NOT NULL,
    factor NUMERIC(6,4) NOT NULL,
    CONSTRAINT uk_tariff_zones_code UNIQUE (code),
    CONSTRAINT ck_tariff_zones_factor CHECK (factor > 0)
);

CREATE TABLE provinces (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_provinces_name UNIQUE (name)
);

CREATE TABLE cities (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    province_id BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    zone_id     BIGINT       NOT NULL,
    CONSTRAINT fk_cities_province FOREIGN KEY (province_id) REFERENCES provinces (id),
    CONSTRAINT fk_cities_zone FOREIGN KEY (zone_id) REFERENCES tariff_zones (id),
    CONSTRAINT uk_cities_province_name UNIQUE (province_id, name)
);

CREATE TABLE clients (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_type   VARCHAR(10)  NOT NULL,
    document_number VARCHAR(11)  NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    business_name   VARCHAR(200),
    birth_date      DATE,
    email           VARCHAR(150) NOT NULL,
    phone           VARCHAR(30)  NOT NULL,
    address         VARCHAR(200) NOT NULL,
    postal_code     VARCHAR(10)  NOT NULL,
    city_id         BIGINT       NOT NULL,
    CONSTRAINT fk_clients_city FOREIGN KEY (city_id) REFERENCES cities (id),
    CONSTRAINT uk_clients_document UNIQUE (document_type, document_number),
    CONSTRAINT ck_clients_document_type CHECK (document_type IN ('DNI', 'CUIT')),
    CONSTRAINT ck_clients_document_number_digits CHECK (document_number ~ '^[0-9]+$'),
    CONSTRAINT ck_clients_person_or_company CHECK (
        (document_type = 'DNI'
            AND first_name IS NOT NULL AND last_name IS NOT NULL AND birth_date IS NOT NULL
            AND business_name IS NULL)
        OR
        (document_type = 'CUIT'
            AND first_name IS NULL AND last_name IS NULL AND birth_date IS NULL
            AND business_name IS NOT NULL)
    )
);

-- 5. Vehicle catalog

CREATE TABLE brands (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vehicle_type VARCHAR(20)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    CONSTRAINT uk_brands_vehicle_type_name UNIQUE (vehicle_type, name),
    CONSTRAINT ck_brands_vehicle_type CHECK (vehicle_type IN ('AUTO', 'MOTO', 'CAMIONETA', 'CAMION'))
);

CREATE TABLE models (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    brand_id            BIGINT       NOT NULL,
    name                VARCHAR(100) NOT NULL,
    available_from_year INTEGER      NOT NULL,
    CONSTRAINT fk_models_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT uk_models_brand_name UNIQUE (brand_id, name),
    CONSTRAINT ck_models_available_from_year CHECK (available_from_year >= 2010)
);

CREATE TABLE versions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    model_id        BIGINT        NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    reference_value NUMERIC(15,2),
    CONSTRAINT fk_versions_model FOREIGN KEY (model_id) REFERENCES models (id),
    CONSTRAINT uk_versions_model_name UNIQUE (model_id, name)
);

-- 6. Plans and tariffs

CREATE TABLE insurance_plans (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code          VARCHAR(30)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(300) NOT NULL,
    monthly_rate  NUMERIC(6,4) NOT NULL,
    display_order INTEGER      NOT NULL,
    CONSTRAINT uk_insurance_plans_code UNIQUE (code),
    CONSTRAINT ck_insurance_plans_monthly_rate CHECK (monthly_rate > 0)
);

CREATE TABLE usage_factors (
    usage_type VARCHAR(30)  PRIMARY KEY,
    factor     NUMERIC(6,4) NOT NULL,
    CONSTRAINT ck_usage_factors_factor CHECK (factor > 0),
    CONSTRAINT ck_usage_factors_usage_type CHECK (usage_type IN (
        'PARTICULAR', 'COMERCIAL', 'TAXI_REMIS', 'TRANSPORTE_CARGA',
        'TRANSPORTE_PASAJEROS', 'AGRICOLA', 'DELIVERY'))
);

CREATE TABLE allowed_usages (
    vehicle_type VARCHAR(20) NOT NULL,
    usage_type   VARCHAR(30) NOT NULL,
    CONSTRAINT pk_allowed_usages PRIMARY KEY (vehicle_type, usage_type),
    CONSTRAINT fk_allowed_usages_usage_type FOREIGN KEY (usage_type) REFERENCES usage_factors (usage_type),
    CONSTRAINT ck_allowed_usages_vehicle_type CHECK (vehicle_type IN ('AUTO', 'MOTO', 'CAMIONETA', 'CAMION'))
);

CREATE TABLE vehicle_age_factors (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    min_years INTEGER      NOT NULL,
    max_years INTEGER,
    factor    NUMERIC(6,4) NOT NULL,
    CONSTRAINT ck_vehicle_age_factors_min_years CHECK (min_years >= 0),
    CONSTRAINT ck_vehicle_age_factors_range CHECK (max_years IS NULL OR max_years >= min_years),
    CONSTRAINT ck_vehicle_age_factors_factor CHECK (factor > 0)
);

CREATE TABLE driver_age_factors (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    min_age INTEGER      NOT NULL,
    max_age INTEGER,
    factor  NUMERIC(6,4) NOT NULL,
    CONSTRAINT ck_driver_age_factors_min_age CHECK (min_age >= 18),
    CONSTRAINT ck_driver_age_factors_range CHECK (max_age IS NULL OR max_age >= min_age),
    CONSTRAINT ck_driver_age_factors_factor CHECK (factor > 0)
);

-- 7. Quotes and policies

CREATE TABLE quotes (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quote_number       VARCHAR(20)   NOT NULL,
    producer_id        BIGINT        NOT NULL,
    client_id          BIGINT        NOT NULL,
    vehicle_type       VARCHAR(20)   NOT NULL,
    manufacturing_year INTEGER       NOT NULL,
    brand_id           BIGINT        NOT NULL,
    model_id           BIGINT        NOT NULL,
    version_id         BIGINT        NOT NULL,
    brand_name         VARCHAR(100)  NOT NULL,
    model_name         VARCHAR(100)  NOT NULL,
    version_name       VARCHAR(100)  NOT NULL,
    patent             VARCHAR(6)    NOT NULL,
    usage_type         VARCHAR(30)   NOT NULL,
    plan_id            BIGINT        NOT NULL,
    plan_name          VARCHAR(100)  NOT NULL,
    plan_rate          NUMERIC(6,4)  NOT NULL,
    insured_amount     NUMERIC(15,2) NOT NULL,
    zone_code          VARCHAR(10)   NOT NULL,
    zone_factor        NUMERIC(6,4)  NOT NULL,
    usage_factor       NUMERIC(6,4)  NOT NULL,
    vehicle_age_years  INTEGER       NOT NULL,
    vehicle_age_factor NUMERIC(6,4)  NOT NULL,
    driver_age         INTEGER,
    driver_age_factor  NUMERIC(6,4)  NOT NULL,
    base_premium       NUMERIC(15,2) NOT NULL,
    premium            NUMERIC(15,2) NOT NULL,
    tax_rate           NUMERIC(6,4)  NOT NULL,
    taxes              NUMERIC(15,2) NOT NULL,
    total_price        NUMERIC(15,2) NOT NULL,
    currency           VARCHAR(3)    NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL,
    valid_until        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_quotes_quote_number UNIQUE (quote_number),
    CONSTRAINT fk_quotes_producer FOREIGN KEY (producer_id) REFERENCES producers (id),
    CONSTRAINT fk_quotes_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_quotes_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT fk_quotes_model FOREIGN KEY (model_id) REFERENCES models (id),
    CONSTRAINT fk_quotes_version FOREIGN KEY (version_id) REFERENCES versions (id),
    CONSTRAINT fk_quotes_plan FOREIGN KEY (plan_id) REFERENCES insurance_plans (id),
    CONSTRAINT ck_quotes_patent_uppercase CHECK (patent = UPPER(patent)),
    CONSTRAINT ck_quotes_currency CHECK (currency = 'ARS'),
    CONSTRAINT ck_quotes_status CHECK (status IN ('CALCULATED', 'EMITTED', 'EXPIRED')),
    CONSTRAINT ck_quotes_vehicle_type CHECK (vehicle_type IN ('AUTO', 'MOTO', 'CAMIONETA', 'CAMION')),
    CONSTRAINT ck_quotes_usage_type CHECK (usage_type IN (
        'PARTICULAR', 'COMERCIAL', 'TAXI_REMIS', 'TRANSPORTE_CARGA',
        'TRANSPORTE_PASAJEROS', 'AGRICOLA', 'DELIVERY'))
);

CREATE INDEX ix_quotes_producer_created_at ON quotes (producer_id, created_at DESC);

CREATE TABLE policies (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    policy_number  VARCHAR(20)   NOT NULL,
    quote_id       BIGINT        NOT NULL,
    producer_id    BIGINT        NOT NULL,
    client_id      BIGINT        NOT NULL,
    start_date     DATE          NOT NULL,
    end_date       DATE          NOT NULL,
    insured_amount NUMERIC(15,2) NOT NULL,
    premium        NUMERIC(15,2) NOT NULL,
    taxes          NUMERIC(15,2) NOT NULL,
    total_price    NUMERIC(15,2) NOT NULL,
    currency       VARCHAR(3)    NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    issued_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_policies_policy_number UNIQUE (policy_number),
    CONSTRAINT uk_policies_quote UNIQUE (quote_id),
    CONSTRAINT fk_policies_quote FOREIGN KEY (quote_id) REFERENCES quotes (id),
    CONSTRAINT fk_policies_producer FOREIGN KEY (producer_id) REFERENCES producers (id),
    CONSTRAINT fk_policies_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT ck_policies_currency CHECK (currency = 'ARS'),
    CONSTRAINT ck_policies_status CHECK (status IN ('ACTIVE', 'EXPIRED'))
);

CREATE TABLE number_sequences (
    prefix     VARCHAR(5) NOT NULL,
    year       INTEGER    NOT NULL,
    last_value INTEGER    NOT NULL,
    CONSTRAINT pk_number_sequences PRIMARY KEY (prefix, year)
);
