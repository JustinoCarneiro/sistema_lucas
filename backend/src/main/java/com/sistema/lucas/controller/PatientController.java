package com.sistema.lucas.controller;

import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.PatientCreateDTO;
import com.sistema.lucas.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {

    @Autowired
    private PatientService service;

    @GetMapping
    public ResponseEntity<List<Patient>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody @Valid PatientCreateDTO dto) {
        service.create(dto);
        return ResponseEntity.status(201).body("Paciente cadastrado com sucesso!");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}