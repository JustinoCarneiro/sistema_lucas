import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyExams } from './my-exams';

describe('MyExams', () => {
  let component: MyExams;
  let fixture: ComponentFixture<MyExams>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyExams]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MyExams);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
