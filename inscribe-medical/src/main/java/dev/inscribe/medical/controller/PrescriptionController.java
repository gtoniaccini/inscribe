package dev.inscribe.medical.controller;

import dev.inscribe.core.ai.AiResolver;
import dev.inscribe.core.command.InsertItemCommand;
import dev.inscribe.core.engine.Orchestrator;
import dev.inscribe.core.spi.ItemCatalog.CatalogItem;
import dev.inscribe.medical.model.Prescription;
import dev.inscribe.medical.model.PrescriptionExam;
import dev.inscribe.medical.repository.PrescriptionExamRepository;
import dev.inscribe.medical.repository.PrescriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private static final String WORKFLOW = "medical";

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionExamRepository examRepository;
    private final Orchestrator orchestrator;
    private final Optional<AiResolver> aiResolver;

    public PrescriptionController(
            PrescriptionRepository prescriptionRepository,
            PrescriptionExamRepository examRepository,
            Orchestrator orchestrator,
            Optional<AiResolver> aiResolver) {
        this.prescriptionRepository = prescriptionRepository;
        this.examRepository = examRepository;
        this.orchestrator = orchestrator;
        this.aiResolver = aiResolver;
    }

    // --- CRUD ---

    @PostMapping
    public ResponseEntity<PrescriptionResponse> create(@RequestBody CreatePrescriptionRequest request) {
        Prescription prescription = new Prescription(request.patientName());
        prescription.setPatientFiscalCode(request.patientFiscalCode());
        prescription.setDoctorName(request.doctorName());
        prescription.setNotes(request.notes());
        prescriptionRepository.save(prescription);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(prescription));
    }

    @GetMapping("/{id}")
    public PrescriptionDetailResponse get(@PathVariable UUID id) {
        Prescription prescription = findPrescription(id);
        List<PrescriptionExam> exams = examRepository.findByPrescriptionIdOrderByInsertedAtAsc(id);
        return toDetailResponse(prescription, exams);
    }

    @GetMapping
    public List<PrescriptionResponse> list() {
        return prescriptionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // --- Add exams ---

    @PostMapping("/{id}/exams")
    public ResponseEntity<Void> addExam(
            @PathVariable UUID id,
            @RequestBody AddExamRequest request) {
        findPrescription(id); // verify exists
        
        InsertItemCommand command = new InsertItemCommand(
                id, WORKFLOW, request.examCode(), request.examName(), request.metadata());
                
        orchestrator.submit(command);
        
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/exams/ai")
    public ResponseEntity<List<String>> addExamsViaAi(
            @PathVariable UUID id,
            @RequestBody AiPromptRequest request) {
        findPrescription(id); // verify exists

        AiResolver resolver = aiResolver.orElseThrow(() -> new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "AI is not enabled. Set inscribe.ai.enabled=true and provide an OpenAI API key."));

        List<CatalogItem> resolved = resolver.resolve(WORKFLOW, request.prompt());

        for (CatalogItem item : resolved) {
            orchestrator.submit(new InsertItemCommand(id, WORKFLOW, item.id(), item.name()));
        }

        return ResponseEntity.accepted()
                .body(resolved.stream().map(CatalogItem::name).toList());
    }

    // --- Helpers ---

    private Prescription findPrescription(UUID id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Prescription not found: " + id));
    }

    private PrescriptionResponse toResponse(Prescription p) {
        return new PrescriptionResponse(p.getId(), p.getPatientName(),
                p.getDoctorName(), p.getCreatedAt());
    }

    private PrescriptionDetailResponse toDetailResponse(Prescription p, List<PrescriptionExam> exams) {
        List<ExamResponse> examResponses = exams.stream()
                .map(e -> new ExamResponse(e.getExamCode(), e.getExamName(), e.getNotes(), e.getInsertedAt()))
                .toList();
        return new PrescriptionDetailResponse(p.getId(), p.getPatientName(),
                p.getPatientFiscalCode(), p.getDoctorName(), p.getNotes(),
                p.getCreatedAt(), examResponses);
    }

    // --- DTOs ---

    public record CreatePrescriptionRequest(String patientName, String patientFiscalCode,
                                             String doctorName, String notes) {}

    public record AddExamRequest(String examCode, String examName, Map<String, Object> metadata) {}

    public record AiPromptRequest(String prompt) {}

    public record PrescriptionResponse(UUID id, String patientName, String doctorName,
                                        java.time.Instant createdAt) {}

    public record PrescriptionDetailResponse(UUID id, String patientName, String patientFiscalCode,
                                              String doctorName, String notes, java.time.Instant createdAt,
                                              List<ExamResponse> exams) {}

    public record ExamResponse(String examCode, String examName, String notes,
                                java.time.Instant insertedAt) {}
}
