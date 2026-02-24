import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core'; 
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DoctorService } from './doctor.service';

@Component({
  selector: 'app-doctors',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './doctors.html'
})
export class DoctorsComponent implements OnInit {
  
  doctorsList: any[] = [];
  doctorForm: FormGroup;
  isEditing = false;
  currentDoctorId: number | null = null;
  
  private doctorService = inject(DoctorService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef); 

  constructor() {
    this.doctorForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      crm: ['', Validators.required],
      specialty: ['', Validators.required]
    });
  }

  ngOnInit() {
    this.loadDoctors();
  }

  loadDoctors() {
    this.doctorService.getDoctors().subscribe({
      next: (data: any) => {
        // Trata se o backend devolver uma Page ou uma List direta
        this.doctorsList = data.content ? data.content : data;
        this.cdr.detectChanges(); 
      },
      error: (err) => console.error('Erro ao buscar médicos', err)
    });
  }

  editDoctor(doc: any) {
    this.isEditing = true;
    this.currentDoctorId = doc.id;
    
    // Preenche o form com os dados do médico
    this.doctorForm.patchValue({
      name: doc.name,
      email: doc.email,
      crm: doc.crm,
      specialty: doc.specialty,
      password: '' // Senha não vem do backend por segurança
    });

    // Na edição, a senha deixa de ser obrigatória (só muda se quiser)
    this.doctorForm.get('password')?.clearValidators();
    this.doctorForm.get('password')?.updateValueAndValidity();
    
    // Scroll suave para o topo para ver o formulário
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit() {
    this.isEditing = false;
    this.currentDoctorId = null;
    this.doctorForm.reset();
    
    // Volta a tornar a senha obrigatória para novos cadastros
    this.doctorForm.get('password')?.setValidators([Validators.required]);
    this.doctorForm.get('password')?.updateValueAndValidity();
  }

  deleteDoctor(id: number) {
    if (confirm('Tem certeza que deseja remover este médico? Esta ação não pode ser desfeita.')) {
      this.doctorService.deleteDoctor(id).subscribe({
        next: () => {
          alert('Médico removido com sucesso! 🗑️');
          this.loadDoctors();
        },
        error: (err) => alert('Erro ao remover: ' + (err.error?.message || 'Erro desconhecido'))
      });
    }
  }

  onSubmit() {
    if (this.doctorForm.valid) {
      const doctorData = this.doctorForm.value;

      if (this.isEditing && this.currentDoctorId) {
        // Lógica de Atualização
        this.doctorService.updateDoctor(this.currentDoctorId, doctorData).subscribe({
          next: () => {
            alert('Médico atualizado com sucesso! ✨');
            this.cancelEdit();
            this.loadDoctors();
          },
          error: (err) => alert('Erro ao atualizar: ' + (err.error?.message || 'Erro.'))
        });
      } else {
        // Lógica de Criação
        this.doctorService.createDoctor(doctorData).subscribe({
          next: () => {
            alert('Médico cadastrado com sucesso! ➕');
            this.doctorForm.reset();
            this.loadDoctors();
          },
          error: (err) => alert('Erro ao cadastrar médico.')
        });
      }
    }
  }
}