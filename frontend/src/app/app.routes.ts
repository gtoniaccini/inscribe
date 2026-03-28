import { Routes } from '@angular/router';
import { PrescriptionComponent } from './components/prescription/prescription.component';

export const routes: Routes = [
  { path: '', redirectTo: 'prescriptions', pathMatch: 'full' },
  { path: 'prescriptions', component: PrescriptionComponent },
];
