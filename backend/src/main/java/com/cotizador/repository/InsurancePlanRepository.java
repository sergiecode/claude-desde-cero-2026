package com.cotizador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.InsurancePlan;

public interface InsurancePlanRepository extends JpaRepository<InsurancePlan, Long> {

	List<InsurancePlan> findAllByOrderByDisplayOrderAsc();

}
