import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Professionals } from './professionals';

describe('Professionals', () => {
  let component: Professionals;
  let fixture: ComponentFixture<Professionals>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Professionals]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Professionals);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
