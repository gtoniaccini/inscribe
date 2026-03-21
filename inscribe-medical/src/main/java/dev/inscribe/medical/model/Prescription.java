package dev.inscribe.medical.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * The medical domain entity — a prescription.
 * Its id is used as containerId by the core orchestration engine.
 */
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String patientName;

    private String patientFiscalCode;

    private String doctorName;

    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Prescription() {}

    public Prescription(String patientName) {
        this.patientName = patientName;
    }

    public UUID getId() { return id; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientFiscalCode() { return patientFiscalCode; }
    public void setPatientFiscalCode(String patientFiscalCode) { this.patientFiscalCode = patientFiscalCode; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
}
