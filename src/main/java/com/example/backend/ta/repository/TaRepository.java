package com.example.backend.ta.repository;

import com.example.backend.ta.entity.TA;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaRepository extends JpaRepository<TA, Long> {
}
