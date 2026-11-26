package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.Producer;

public interface ProducerRepository extends JpaRepository<Producer, Long> {

}
