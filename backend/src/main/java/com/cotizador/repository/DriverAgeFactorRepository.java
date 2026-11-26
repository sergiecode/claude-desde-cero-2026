package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.DriverAgeFactor;

public interface DriverAgeFactorRepository extends JpaRepository<DriverAgeFactor, Long> {

}
