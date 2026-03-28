export interface PrescriptionResponse {
  id: string;
  patientName: string;
  doctorName: string;
  createdAt: string;
}

export interface PrescriptionDetail {  id: string;
  patientName: string;
  patientFiscalCode: string;
  doctorName: string;
  notes: string;
  createdAt: string;
  exams: ExamResponse[];
}

export interface ExamResponse {
  examCode: string;
  examName: string;
  notes: string;
  insertedAt: string;
}

export type ExamStatus = 'in_attesa' | 'in_validazione' | 'in_inserimento' | 'inserito' | 'rifiutato';

export interface ExamRow {
  localId: string;
  correlationId?: string;
  examCode: string;
  examName: string;
  insertedAt?: string;
  status: ExamStatus;
  rejectionReason?: string;
}

export interface SseItemRequestedEvent {
  containerId: string;
  catalogItemId: string;
  correlationId: string;
  timestamp: string;
}

export interface SseItemValidatedEvent {
  containerId: string;
  correlationId: string;
  timestamp: string;
}

export interface SseItemInsertedEvent {
  containerId: string;
  correlationId: string;
  timestamp: string;
}

export interface SseItemRejectedEvent {
  containerId: string;
  correlationId: string;
  reason: string;
  timestamp: string;
}

