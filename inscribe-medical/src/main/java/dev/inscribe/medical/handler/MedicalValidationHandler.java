package dev.inscribe.medical.handler;

import dev.inscribe.core.spi.StepHandler;
import dev.inscribe.core.spi.StepResult;
import dev.inscribe.core.spi.WorkflowContext;
import dev.inscribe.medical.model.MedicalExam;
import dev.inscribe.medical.repository.MedicalExamRepository;
import dev.inscribe.medical.repository.PrescriptionExamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Validates that the requested exam exists in the medical catalog
 * and has not already been inserted for this container (idempotency).
 */
@Component
public class MedicalValidationHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(MedicalValidationHandler.class);

    private final MedicalExamRepository examRepository;
    private final PrescriptionExamRepository prescriptionExamRepository;

    public MedicalValidationHandler(MedicalExamRepository examRepository,
                                     PrescriptionExamRepository prescriptionExamRepository) {
        this.examRepository = examRepository;
        this.prescriptionExamRepository = prescriptionExamRepository;
    }

    @Override
    public String getName() {
        return "MedicalValidationHandler";
    }

    @Override
    public StepResult handle(WorkflowContext context) {
        String examCode = context.catalogItemId();
        log.debug("[{}] Validating medical exam: {}", context.correlationId(), examCode);

        // Check if the exam exists in the catalog
        Optional<MedicalExam> exam = examRepository.findByCode(examCode);
        if (exam.isEmpty()) {
            return StepResult.reject("Unknown exam code: " + examCode);
        }

        // Idempotency: check if this exam was already inserted for this prescription
        boolean alreadyInserted = prescriptionExamRepository
                .findByPrescriptionIdOrderByInsertedAtAsc(context.containerId())
                .stream()
                .anyMatch(e -> e.getExamCode().equals(examCode));
        if (alreadyInserted) {
            return StepResult.reject("Exam " + examCode + " already present in this prescription");
        }

        return StepResult.success();
    }
}
