package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.AllowedUsage;
import com.cotizador.entity.AllowedUsageId;

public interface AllowedUsageRepository extends JpaRepository<AllowedUsage, AllowedUsageId> {

}
