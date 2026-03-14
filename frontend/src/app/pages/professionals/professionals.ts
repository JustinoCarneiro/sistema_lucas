// frontend/src/app/pages/professionals/professionals.ts
import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProfessionalService } from './professionals.service';

@Component({
  selector: 'app-professionals',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './professionals.html'
})
export class ProfessionalsComponent implements OnInit {

  professionals: any[] = [];
  professionalForm: FormGroup;
  isEditing = false;
  currentProfessionalId: number | null = null;

  private professionalService = inject(ProfessionalService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  constructor() {
    this.professionalForm = this.fb.group({
      name:             ['', Validators.required],
      email:            ['', [Validators.required, Validators.email]],
      password:         ['', Validators.required],
      tipoRegistro:     ['CRP', Validators.required], // ✅ padrão CRP
      registroConselho: ['', Validators.required],    // ✅ era crm
      specialty:        ['']
    });
  }

  ngOnInit() {
    this.loadProfessionals();
  }

  loadProfessionals() {
    this.professionalService.getProfessionals().subscribe({
      next: (data: any) => {
        this.professionals = data.content ?? data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erro ao buscar profissionais', err)
    });
  }

  editProfessional(prof: any) {
    this.isEditing = true;
    this.currentProfessionalId = prof.id;

    this.professionalForm.patchValue({
      name:             prof.name,
      email:            prof.email,
      tipoRegistro:     prof.tipoRegistro,
      registroConselho: prof.registroConselho,
      specialty:        prof.specialty,
      password:         ''
    });

    // Na edição a senha é opcional
    this.professionalForm.get('password')?.clearValidators();
    this.professionalForm.get('password')?.updateValueAndValidity();

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit() {
    this.isEditing = false;
    this.currentProfessionalId = null;
    this.professionalForm.reset({ tipoRegistro: 'CRP' });

    this.professionalForm.get('password')?.setValidators([Validators.required]);
    this.professionalForm.get('password')?.updateValueAndValidity();
  }

  deleteProfessional(id: number) {
    if (confirm('Tem certeza que deseja remover este profissional? Esta ação não pode ser desfeita.')) {
      this.professionalService.deleteProfessional(id).subscribe({
        next: () => {
          alert('Profissional removido com sucesso!');
          this.loadProfessionals();
        },
        error: (err: any) => alert('Erro ao remover: ' + (err.error?.message || 'Erro desconhecido'))
      });
    }
  }

  onSubmit() {
    if (this.professionalForm.valid) {
      const dados = this.professionalForm.value;

      if (this.isEditing && this.currentProfessionalId) {
        this.professionalService.updateProfessional(this.currentProfessionalId, dados).subscribe({
          next: () => {
            alert('Profissional atualizado com sucesso!');
            this.cancelEdit();
            this.loadProfessionals();
          },
          error: (err: any) => alert('Erro ao atualizar: ' + (err.error?.message || 'Erro.'))
        });
      } else {
        this.professionalService.createProfessional(dados).subscribe({
          next: () => {
            alert('Profissional cadastrado com sucesso!');
            this.professionalForm.reset({ tipoRegistro: 'CRP' });
            this.loadProfessionals();
          },
          error: (err: any) => alert('Erro ao cadastrar: ' + (err.error?.message || 'Erro.'))
        });
      }
    }
  }
}