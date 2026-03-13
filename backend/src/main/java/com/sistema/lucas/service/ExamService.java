package com.sistema.lucas.service;

import com.sistema.lucas.model.Exam;
import com.sistema.lucas.repository.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ExamService {

    @Autowired
    private ExamRepository repository;

    public List<Exam> getMyExams(String email) {
        return repository.findByPatientEmail(email);
    }

    public Exam createExam(Exam exam) {
        return repository.save(exam);
    }
}