import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PrescriptionDetail, PrescriptionResponse } from '../models/prescription.model';

@Injectable({ providedIn: 'root' })
export class PrescriptionService {

  constructor(private http: HttpClient) {}

  getPrescription(id: string): Observable<PrescriptionDetail> {
    return this.http.get<PrescriptionDetail>(`/api/prescriptions/${id}`);
  }

  createPrescription(
    patientName: string,
    patientFiscalCode: string,
    doctorName: string,
    notes: string
  ): Observable<PrescriptionResponse> {
    return this.http.post<PrescriptionResponse>('/api/prescriptions', {
      patientName,
      patientFiscalCode,
      doctorName,
      notes,
    });
  }

  addExam(prescriptionId: string, examCode: string, examName: string): Observable<void> {
    return this.http.post<void>(`/api/prescriptions/${prescriptionId}/exams`, {
      examCode,
      examName,
      metadata: {}
    });
  }

  addExamsViaAi(prescriptionId: string, prompt: string): Observable<{ examCode: string; examName: string }[]> {
    return this.http.post<{ examCode: string; examName: string }[]>(
      `/api/prescriptions/${prescriptionId}/exams/ai`,
      { prompt }
    );
  }

  openSseStream(prescriptionId: string): EventSource {
    return new EventSource(`/api/stream/${prescriptionId}`);
  }
}

