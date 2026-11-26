package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.NumberSequence;
import com.cotizador.entity.NumberSequenceId;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, NumberSequenceId> {

}
