package com.ion.llm.repository;

import com.ion.llm.domain.LlmEndpointConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LlmEndpointConfigRepository extends JpaRepository<LlmEndpointConfig, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    long countByEnabledTrue();

    Optional<LlmEndpointConfig> findByIsDefaultTrueAndEnabledTrue();

    List<LlmEndpointConfig> findAllByOrderByIsDefaultDescEnabledDescNameAsc();
}
