// frontend/src/app/pages/waitlist/waitlist.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { WaitlistService } from './waitlist.service';
import { AvailabilityService } from '../my-availability/availability.service';
import { NotificationService } from '../../notification.service';

@Component({
  selector: 'app-waitlist',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './waitlist.html'
})
export class WaitlistComponent implements OnInit {
  private waitlistService = inject(WaitlistService);
  private availabilityService = inject(AvailabilityService);
  private fb = inject(FormBuilder);
  private notify = inject(NotificationService);

  professionals = signal<any[]>([]);
  entradas = signal<any[]>([]);
  isLoading = signal(true);
  isSubmitting = signal(false);

  form: FormGroup;

  statusLabel: Record<string, string> = {
    AGUARDANDO: 'Aguardando',
    OFERECIDA: 'Vaga oferecida — confira seu e-mail',
    CONFIRMADA: 'Confirmada',
    EXPIRADA: 'Expirada',
    CANCELADA: 'Cancelada'
  };

  constructor() {
    this.form = this.fb.group({
      professionalId: ['', Validators.required],
      date: ['', Validators.required],
      time: ['', Validators.required]
    });
  }

  ngOnInit() {
    this.carregarProfissionais();
    this.carregarEntradas();
  }

  carregarProfissionais() {
    this.availabilityService.getProfissionaisDisponiveis().subscribe({
      next: (r: any) => this.professionals.set(r ?? [])
    });
  }

  carregarEntradas() {
    this.isLoading.set(true);
    this.waitlistService.minhasEntradas().subscribe({
      next: (r: any) => { this.entradas.set(r ?? []); this.isLoading.set(false); },
      error: () => { this.entradas.set([]); this.isLoading.set(false); }
    });
  }

  onSubmit() {
    if (this.form.invalid) return;

    this.isSubmitting.set(true);
    const dateTime = `${this.form.value.date}T${this.form.value.time}:00`;

    this.waitlistService.entrarNaFila(Number(this.form.value.professionalId), dateTime).subscribe({
      next: () => {
        this.notify.success('Você entrou na lista de espera!');
        this.form.reset();
        this.isSubmitting.set(false);
        this.carregarEntradas();
      },
      error: (msg: string) => {
        this.notify.error(msg);
        this.isSubmitting.set(false);
      }
    });
  }

  sair(id: number) {
    this.waitlistService.sairDaFila(id).subscribe({
      next: () => { this.notify.success('Você saiu da lista de espera.'); this.carregarEntradas(); },
      error: (msg: string) => this.notify.error(msg)
    });
  }
}
