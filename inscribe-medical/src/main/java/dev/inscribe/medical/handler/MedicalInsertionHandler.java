package dev.inscribe.medical.handler;

import dev.inscribe.core.spi.StepHandler;
import dev.inscribe.core.spi.StepResult;
import dev.inscribe.core.spi.WorkflowContext;
import dev.inscribe.medical.model.PrescriptionExam;
import dev.inscribe.medical.repository.PrescriptionExamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Persists the exam into the plugin's own table (prescription_exams).
 * This handler is called AFTER validation has passed.
 * It participates in the Orchestrator's @Transactional boundary.
 */
@Component
public class MedicalInsertionHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(MedicalInsertionHandler.class);

    private final PrescriptionExamRepository repository;

    public MedicalInsertionHandler(PrescriptionExamRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getName() {
        return "MedicalInsertionHandler";
    }

    @Override
    public StepResult handle(WorkflowContext context) {
        PrescriptionExam exam = new PrescriptionExam(
                context.containerId(),
                context.catalogItemId(),
                context.catalogItemName());

        // Read optional metadata (e.g. notes from the request)
        Object notes = context.metadata().get("notes");
        if (notes instanceof String s) {
            exam.setNotes(s);
        }

        repository.save(exam);
        log.info("[{}] Exam {} inserted into prescription_exams for prescription {}",
                context.correlationId(), context.catalogItemId(), context.containerId());

        return StepResult.success();
    }
}
