package dev.inscribe.medical.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Domain entity representing a medical examination available in the catalog.
 */
@Entity
@Table(name = "medical_exams")
public class MedicalExam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String category;

    private boolean requiresFasting;

    protected MedicalExam() {}

    public MedicalExam(String code, String name, String description, String category, boolean requiresFasting) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.category = category;
        this.requiresFasting = requiresFasting;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public boolean isRequiresFasting() { return requiresFasting; }
}
