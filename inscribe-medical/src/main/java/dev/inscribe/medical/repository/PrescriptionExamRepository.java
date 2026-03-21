package dev.inscribe.medical.repository;

import dev.inscribe.medical.model.PrescriptionExam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PrescriptionExamRepository extends JpaRepository<PrescriptionExam, UUID> {
    List<PrescriptionExam> findByPrescriptionIdOrderByInsertedAtAsc(UUID prescriptionId);
}
