package dev.inscribe.medical.repository;

import dev.inscribe.medical.model.MedicalExam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MedicalExamRepository extends JpaRepository<MedicalExam, UUID> {
    Optional<MedicalExam> findByCode(String code);
}
