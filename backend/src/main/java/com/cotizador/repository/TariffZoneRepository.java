package com.cotizador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.TariffZone;

public interface TariffZoneRepository extends JpaRepository<TariffZone, Long> {

	Optional<TariffZone> findByCode(String code);

}
