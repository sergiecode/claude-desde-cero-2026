package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.InsurancePlan;

public interface InsurancePlanRepository extends JpaRepository<InsurancePlan, Long> {

}
