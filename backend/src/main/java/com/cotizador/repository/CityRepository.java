package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.City;

public interface CityRepository extends JpaRepository<City, Long> {

}
