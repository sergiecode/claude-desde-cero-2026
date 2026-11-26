package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {

}
