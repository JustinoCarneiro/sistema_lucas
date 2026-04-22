// frontend/src/app/pages/professionals/professionals.ts
import { Component, inject, OnInit, signal } from '@angular/core';
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
  private professionalService = inject(ProfessionalService);
  private fb = inject(FormBuilder);

  professionals = signal<any[]>([]);
  isEditing = signal(false);
  currentProfessionalId = signal<number | null>(null);
  mostrarFormulario = signal(false);
  selectedItem: any = null;

  openDetails(item: any) {
    this.selectedItem = item;
  }

  closeDetails() {
    this.selectedItem = null;
  }
  professionalForm: FormGroup;

  constructor() {
    this.professionalForm = this.fb.group({
      name:             ['', Validators.required],
      email:            ['', [Validators.required, Validators.email]],
      password:         ['', Validators.required],
      tipoRegistro:     ['CRP', Validators.required],
      registroConselho: ['', Validators.required],
      specialty:        ['']
    });
  }

  ngOnInit() { this.loadProfessionals(); }

  loadProfessionals() {
    this.professionalService.getProfessionals().subscribe({
      next: (data: any) => this.professionals.set(data.content ?? data),
      error: (err: any) => console.error('Erro ao buscar profissionais', err)
    });
  }

  editProfessional(prof: any) {
    this.isEditing.set(true);
    this.mostrarFormulario.set(true);
    this.currentProfessionalId.set(prof.id);
    this.professionalForm.patchValue({
      name:             prof.name,
      email:            prof.email,
      tipoRegistro:     prof.tipoRegistro,
      registroConselho: prof.registroConselho,
      specialty:        prof.specialty,
      password:         ''
    });
    this.professionalForm.get('password')?.clearValidators();
    this.professionalForm.get('password')?.updateValueAndValidity();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit() {
    this.isEditing.set(false);
    this.mostrarFormulario.set(false);
    this.currentProfessionalId.set(null);
    this.professionalForm.reset({ tipoRegistro: 'CRP' });
    this.professionalForm.get('password')?.setValidators([Validators.required]);
    this.professionalForm.get('password')?.updateValueAndValidity();
  }

  deleteProfessional(id: number) {
    if (confirm('ATENÇÃO: Exclusão Forçada!\n\nTem certeza que deseja remover este profissional?\n\nIsso apagará DE FORMA PERMANENTE todas as consultas, prontuários, horários e documentos vinculados a ele. Essa ação não pode ser desfeita.')) {
      this.professionalService.deleteProfessional(id).subscribe({
        next: () => { alert('Profissional removido com sucesso!'); this.loadProfessionals(); },
        error: (msg: string) => alert('Erro: ' + msg) // ✅ string direta do service
      });
    }
  }

  onSubmit() {
    if (this.professionalForm.valid) {
      const dados = this.professionalForm.value;
      const id = this.currentProfessionalId();

      if (this.isEditing() && id) {
        this.professionalService.updateProfessional(id, dados).subscribe({
          next: () => {
            alert('Profissional atualizado com sucesso!');
            this.cancelEdit();
            this.loadProfessionals();
          },
          error: (msg: string) => alert('Erro: ' + msg) // ✅ string direta do service
        });
      } else {
        this.professionalService.createProfessional(dados).subscribe({
          next: () => {
            alert('Profissional cadastrado com sucesso!');
            this.professionalForm.reset({ tipoRegistro: 'CRP' });
            this.mostrarFormulario.set(false);
            this.loadProfessionals();
          },
          error: (msg: string) => alert('Erro: ' + msg) // ✅ string direta do service
        });
      }
    }
  }
}