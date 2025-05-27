package com.example.backend.ta.service;


import com.example.backend.ta.entity.TA;
import com.example.backend.ta.repository.TaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaService {

    private final TaRepository taRepository;

    public List<TA> findAll() {
        return taRepository.findAll();
    }
}
