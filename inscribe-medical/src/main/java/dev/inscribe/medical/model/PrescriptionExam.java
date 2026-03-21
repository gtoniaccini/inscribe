package dev.inscribe.medical.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * A medical exam line-item within a prescription.
 * This is the plugin's own table — the core never touches it.
 */
@Entity
@Table(name = "prescription_exams")
public class PrescriptionExam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID prescriptionId;

    @Column(nullable = false)
    private String examCode;

    @Column(nullable = false)
    private String examName;

    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant insertedAt = Instant.now();

    protected PrescriptionExam() {}

    public PrescriptionExam(UUID prescriptionId, String examCode, String examName) {
        this.prescriptionId = prescriptionId;
        this.examCode = examCode;
        this.examName = examName;
    }

    public UUID getId() { return id; }
    public UUID getPrescriptionId() { return prescriptionId; }
    public String getExamCode() { return examCode; }
    public String getExamName() { return examName; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getInsertedAt() { return insertedAt; }
}
