package com.cotizador.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.config.PricingProperties;
import com.cotizador.dto.ClientResponse;
import com.cotizador.entity.Client;
import com.cotizador.entity.DocumentType;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.mapper.ClientMapper;
import com.cotizador.repository.ClientRepository;
import com.cotizador.validation.DocumentValidator;

/** Client search and client rules C1 to C5 (manual §6.1, api.md §4). */
@Service
public class ClientService {

	private final ClientRepository clientRepository;
	private final DocumentValidator documentValidator;
	private final ClientMapper clientMapper;
	private final PricingProperties pricingProperties;
	private final Clock clock;

	public ClientService(ClientRepository clientRepository, DocumentValidator documentValidator,
			ClientMapper clientMapper, PricingProperties pricingProperties, Clock clock) {
		this.clientRepository = clientRepository;
		this.documentValidator = documentValidator;
		this.clientMapper = clientMapper;
		this.pricingProperties = pricingProperties;
		this.clock = clock;
	}

	/**
	 * Finds a client by document. Validates the document format (INVALID_DOCUMENT), its existence
	 * (CLIENT_NOT_FOUND) and, for a DNI, the minimum age (CLIENT_UNDERAGE).
	 */
	@Transactional(readOnly = true)
	public ClientResponse search(DocumentType documentType, String documentNumber) {
		String normalized = documentValidator.normalize(documentType, documentNumber);
		Client client = clientRepository.findByDocumentTypeAndDocumentNumber(documentType, normalized)
				.orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));
		ensureMinimumAge(client);
		return clientMapper.toResponse(client, ageOf(client));
	}

	/**
	 * Age of the client today according to the business clock (backend.md §4.1). Only for DNI clients;
	 * null for companies.
	 */
	public Integer ageOf(Client client) {
		if (client.getDocumentType() != DocumentType.DNI) {
			return null;
		}
		return Period.between(client.getBirthDate(), LocalDate.now(clock)).getYears();
	}

	/**
	 * Rule C5: a DNI client must be at least {@code app.pricing.min-client-age} years old today.
	 * Companies (CUIT) are not checked. Reused by quote calculation (backend.md §5.3, step 2).
	 *
	 * @throws BusinessException with {@link ErrorCode#CLIENT_UNDERAGE}
	 */
	public void ensureMinimumAge(Client client) {
		Integer age = ageOf(client);
		if (age != null && age < pricingProperties.minClientAge()) {
			throw new BusinessException(ErrorCode.CLIENT_UNDERAGE);
		}
	}

}
