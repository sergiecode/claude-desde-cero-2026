package com.cotizador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.Client;
import com.cotizador.entity.DocumentType;

public interface ClientRepository extends JpaRepository<Client, Long> {

	/** Loads the client with its city and province, which are part of the search response. */
	@EntityGraph(attributePaths = { "city", "city.province" })
	Optional<Client> findByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);

}
