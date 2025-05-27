package com.example.backend.ta.controller;

import com.example.backend.ta.entity.TA;
import com.example.backend.ta.service.TaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ta")
@RequiredArgsConstructor
public class TaController {

    private final TaService taService;

    @GetMapping
    public List<TA> getAllTa() {
        return taService.findAll();
    }
}
