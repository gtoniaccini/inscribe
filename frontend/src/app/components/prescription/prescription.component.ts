import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PrescriptionService } from '../../services/prescription.service';
import {
  ExamRow,
  ExamStatus,
  PrescriptionDetail,
  SseItemInsertedEvent,
  SseItemRejectedEvent,
  SseItemRequestedEvent,
  SseItemValidatedEvent,
} from '../../models/prescription.model';

@Component({
  selector: 'app-prescription',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prescription.component.html',
  styleUrl: './prescription.component.scss',
})
export class PrescriptionComponent implements OnDestroy {

  prescriptionIdInput = '';
  prescription: PrescriptionDetail | null = null;
  exams: ExamRow[] = [];

  examCode = '';
  examName = '';
  aiPrompt = '';
  insertMode: 'manual' | 'ai' = 'manual';
  sendingAi = false;
  aiError = '';

  loadError = '';
  loadingPrescription = false;

  // New prescription form
  showCreateForm = false;
  newPatientName = '';
  newPatientFiscalCode = '';
  newDoctorName = '';
  newNotes = '';
  creating = false;
  createError = '';

  private eventSource: EventSource | null = null;

  constructor(private service: PrescriptionService) {}

  toggleCreateForm(): void {
    this.showCreateForm = !this.showCreateForm;
    this.createError = '';
  }

  createPrescription(): void {
    if (!this.newPatientName.trim() || !this.newDoctorName.trim()) return;

    this.creating = true;
    this.createError = '';

    this.service.createPrescription(
      this.newPatientName.trim(),
      this.newPatientFiscalCode.trim(),
      this.newDoctorName.trim(),
      this.newNotes.trim()
    ).subscribe({
      next: (p) => {
        this.creating = false;
        this.showCreateForm = false;
        this.newPatientName = '';
        this.newPatientFiscalCode = '';
        this.newDoctorName = '';
        this.newNotes = '';
        // Auto-load the newly created prescription
        this.prescriptionIdInput = p.id;
        this.loadPrescription();
      },
      error: () => {
        this.creating = false;
        this.createError = 'Errore durante la creazione.';
      },
    });
  }

  loadPrescription(): void {    const id = this.prescriptionIdInput.trim();
    if (!id) return;

    this.loadError = '';
    this.loadingPrescription = true;
    this.prescription = null;
    this.exams = [];
    this.closeSse();

    this.service.getPrescription(id).subscribe({
      next: (p) => {
        this.prescription = p;
        this.exams = p.exams.map((e) => ({
          localId: crypto.randomUUID(),
          examCode: e.examCode,
          examName: e.examName,
          insertedAt: e.insertedAt,
          status: 'inserito',
        }));
        this.loadingPrescription = false;
        this.subscribeSse(id);
      },
      error: () => {
        this.loadError = 'Prescrizione non trovata.';
        this.loadingPrescription = false;
      },
    });
  }

  addExam(): void {
    if (!this.prescription || !this.examCode.trim() || !this.examName.trim()) return;

    const row: ExamRow = {
      localId: crypto.randomUUID(),
      examCode: this.examCode.trim().toUpperCase(),
      examName: this.examName.trim(),
      status: 'in_attesa',
    };

    this.exams = [...this.exams, row];
    this.examCode = '';
    this.examName = '';

    this.service.addExam(this.prescription.id, row.examCode, row.examName).subscribe({
      error: () => {
        this.exams = this.exams.map((e) =>
          e.localId === row.localId ? { ...e, status: 'rifiutato', rejectionReason: 'Errore di rete' } : e
        );
      },
    });
  }

  addExamsViaAi(): void {
    if (!this.prescription || !this.aiPrompt.trim()) return;

    this.sendingAi = true;
    this.aiError = '';

    this.service.addExamsViaAi(this.prescription.id, this.aiPrompt.trim()).subscribe({
      next: (resolved) => {
        this.sendingAi = false;
        this.aiPrompt = '';
        // Add one optimistic row per resolved exam
        const newRows: ExamRow[] = resolved.map((item) => ({
          localId: crypto.randomUUID(),
          examCode: item.examCode,
          examName: item.examName,
          status: 'in_attesa' as ExamStatus,
        }));
        this.exams = [...this.exams, ...newRows];
      },
      error: () => {
        this.sendingAi = false;
        this.aiError = 'Errore AI: nessun esame risolto o servizio non disponibile.';
      },
    });
  }

  private subscribeSse(prescriptionId: string): void {
    this.eventSource = this.service.openSseStream(prescriptionId);

    this.eventSource.addEventListener('ITEM_REQUESTED', (e: MessageEvent) => {
      const data: SseItemRequestedEvent = JSON.parse(e.data);
      this.exams = this.exams.map((row) => {
        if (row.status === 'in_attesa' && !row.correlationId && row.examCode === data.catalogItemId) {
          return { ...row, correlationId: data.correlationId, status: 'in_validazione' };
        }
        return row;
      });
    });

    this.eventSource.addEventListener('ITEM_VALIDATED', (e: MessageEvent) => {
      const data: SseItemValidatedEvent = JSON.parse(e.data);
      this.exams = this.exams.map((row) =>
        row.correlationId === data.correlationId
          ? { ...row, status: 'in_inserimento' }
          : row
      );
    });

    this.eventSource.addEventListener('ITEM_INSERTED', (e: MessageEvent) => {
      const data: SseItemInsertedEvent = JSON.parse(e.data);
      this.exams = this.exams.map((row) =>
        row.correlationId === data.correlationId
          ? { ...row, status: 'inserito', insertedAt: data.timestamp }
          : row
      );
    });

    this.eventSource.addEventListener('ITEM_REJECTED', (e: MessageEvent) => {
      const data: SseItemRejectedEvent = JSON.parse(e.data);
      this.exams = this.exams.map((row) =>
        row.correlationId === data.correlationId
          ? { ...row, status: 'rifiutato', rejectionReason: data.reason }
          : row
      );
    });

    this.eventSource.onerror = () => {
      // SSE auto-reconnects; close only if prescription changes
    };
  }

  private closeSse(): void {
    this.eventSource?.close();
    this.eventSource = null;
  }

  statusLabel(status: ExamStatus): string {
    return {
      in_attesa:      'In attesa',
      in_validazione: 'In validazione',
      in_inserimento: 'In inserimento',
      inserito:       'Inserito',
      rifiutato:      'Rifiutato',
    }[status];
  }

  isProcessing(status: ExamStatus): boolean {
    return status === 'in_attesa' || status === 'in_validazione' || status === 'in_inserimento';
  }

  onEnter(event: KeyboardEvent): void {
    if (event.key === 'Enter') this.addExam();
  }

  ngOnDestroy(): void {
    this.closeSse();
  }
}

