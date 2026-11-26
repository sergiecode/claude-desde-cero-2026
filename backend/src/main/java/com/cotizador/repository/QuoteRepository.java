package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.Quote;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

}
