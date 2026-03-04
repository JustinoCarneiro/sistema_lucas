import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfessionalAppointmentsComponent } from './professional-appointments';

describe('ProfessionalAppointments', () => {
  let component: ProfessionalAppointmentsComponent;
  let fixture: ComponentFixture<ProfessionalAppointmentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfessionalAppointmentsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProfessionalAppointmentsComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
