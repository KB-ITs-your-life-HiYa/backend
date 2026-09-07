package com.fledge.region.repository;

import com.fledge.region.domain.Sido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SidoRepository extends JpaRepository<Sido, String> {
    Optional<Sido> findByName(String name);
}