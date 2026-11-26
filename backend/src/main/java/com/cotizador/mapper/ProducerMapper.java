package com.cotizador.mapper;

import org.springframework.stereotype.Component;

import com.cotizador.dto.ProducerResponse;
import com.cotizador.entity.Producer;

@Component
public class ProducerMapper {

	public ProducerResponse toResponse(Producer producer) {
		return new ProducerResponse(producer.getId(), producer.getFirstName(), producer.getLastName(),
				producer.getEmail());
	}

}
