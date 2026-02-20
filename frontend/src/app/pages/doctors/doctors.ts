// 1. Adicione o ChangeDetectorRef aqui no import
import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core'; 
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DoctorService } from '../../services/doctor';

@Component({
  selector: 'app-doctors',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './doctors.html',
  styleUrl: './doctors.css'
})
export class DoctorsComponent implements OnInit {
  
  doctorsList: any[] = [];
  doctorForm: FormGroup;
  
  private doctorService = inject(DoctorService);
  private fb = inject(FormBuilder);
  
  // 2. Injete o "Despertador" do Angular aqui
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
        this.doctorsList = data.content ? data.content : data;
        
        // 3. O BELISCÃO! Avisa pro Angular desenhar a tela imediatamente
        this.cdr.detectChanges(); 
      },
      error: (err) => console.error('Erro ao buscar médicos', err)
    });
  }

  onSubmit() {
    if (this.doctorForm.valid) {
      this.doctorService.createDoctor(this.doctorForm.value).subscribe({
        next: () => {
          alert('Médico cadastrado com sucesso!');
          this.doctorForm.reset();
          this.loadDoctors(); // Isso vai chamar o loadDoctors e dar o detectChanges de novo
        },
        error: (err) => {
          console.error(err);
          alert('Erro: ' + (err.error?.message || 'Verifique os dados informados.'));
        }
      });
    }
  }
}