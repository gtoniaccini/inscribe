package dev.inscribe.medical.catalog;

import dev.inscribe.core.spi.ItemCatalog;
import dev.inscribe.medical.repository.MedicalExamRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MedicalExamCatalog implements ItemCatalog {

    private final MedicalExamRepository repository;

    public MedicalExamCatalog(MedicalExamRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getWorkflowName() {
        return "medical";
    }

    @Override
    public List<CatalogItem> findAll() {
        return repository.findAll().stream()
                .map(exam -> new CatalogItem(
                        exam.getCode(),
                        exam.getName(),
                        exam.getDescription()))
                .toList();
    }
}
