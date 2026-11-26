package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {

}
