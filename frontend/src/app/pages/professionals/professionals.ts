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
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      crm: ['', Validators.required],
      specialty: ['', Validators.required]
    });
  }

  ngOnInit() {
    this.loadProfessionals();
  }

  loadProfessionals() {
    this.professionalService.getProfessionals().subscribe({
      next: (data: any) => {
        // Trata se o backend devolver uma Page ou uma List direta
        this.professionals = data.content ? data.content : data;
        this.cdr.detectChanges(); 
      },
      error: (err) => console.error('Erro ao buscar médicos', err)
    });
  }

  editProfessional(doc: any) {
    this.isEditing = true;
    this.currentProfessionalId = doc.id;
    
    // Preenche o form com os dados do médico
    this.professionalForm.patchValue({
      name: doc.name,
      email: doc.email,
      crm: doc.crm,
      specialty: doc.specialty,
      password: '' // Senha não vem do backend por segurança
    });

    // Na edição, a senha deixa de ser obrigatória (só muda se quiser)
    this.professionalForm.get('password')?.clearValidators();
    this.professionalForm.get('password')?.updateValueAndValidity();
    
    // Scroll suave para o topo para ver o formulário
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit() {
    this.isEditing = false;
    this.currentProfessionalId = null;
    this.professionalForm.reset();
    
    // Volta a tornar a senha obrigatória para novos cadastros
    this.professionalForm.get('password')?.setValidators([Validators.required]);
    this.professionalForm.get('password')?.updateValueAndValidity();
  }

  deleteProfessional(id: number) {
    if (confirm('Tem certeza que deseja remover este médico? Esta ação não pode ser desfeita.')) {
      this.professionalService.deleteProfessional(id).subscribe({
        next: () => {
          alert('Médico removido com sucesso! 🗑️');
          this.loadProfessionals();
        },
        error: (err) => alert('Erro ao remover: ' + (err.error?.message || 'Erro desconhecido'))
      });
    }
  }

  onSubmit() {
    if (this.professionalForm.valid) {
      const professionalData = this.professionalForm.value;

      if (this.isEditing && this.currentProfessionalId) {
        // Lógica de Atualização
        this.professionalService.updateProfessional(this.currentProfessionalId, professionalData).subscribe({
          next: () => {
            alert('Médico atualizado com sucesso! ✨');
            this.cancelEdit();
            this.loadProfessionals();
          },
          error: (err) => alert('Erro ao atualizar: ' + (err.error?.message || 'Erro.'))
        });
      } else {
        // Lógica de Criação
        this.professionalService.createProfessional(professionalData).subscribe({
          next: () => {
            alert('Médico cadastrado com sucesso! ➕');
            this.professionalForm.reset();
            this.loadProfessionals();
          },
          error: (err) => alert('Erro ao cadastrar médico.')
        });
      }
    }
  }
}