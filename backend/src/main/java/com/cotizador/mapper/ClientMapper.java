package com.cotizador.mapper;

import org.springframework.stereotype.Component;

import com.cotizador.dto.ClientResponse;
import com.cotizador.entity.Client;

@Component
public class ClientMapper {

	/** {@code age} is computed by the service with the business clock; null for companies. */
	public ClientResponse toResponse(Client client, Integer age) {
		return new ClientResponse(
				client.getId(),
				client.getDocumentType(),
				client.getDocumentNumber(),
				client.getFirstName(),
				client.getLastName(),
				client.getBusinessName(),
				client.getBirthDate(),
				age,
				client.getEmail(),
				client.getPhone(),
				client.getAddress(),
				client.getPostalCode(),
				client.getCity().getName(),
				client.getCity().getProvince().getName());
	}

}
