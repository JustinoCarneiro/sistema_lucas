// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WaitlistOfertaComponent } from './waitlist-oferta';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';

describe('WaitlistOfertaComponent', () => {
  let component: WaitlistOfertaComponent;
  let fixture: ComponentFixture<WaitlistOfertaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WaitlistOfertaComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(WaitlistOfertaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('sem token na URL não faz chamada e encerra o carregamento', () => {
    expect(component.token).toBeNull();
    expect(component.isLoadingStatus()).toBe(false);
    expect(component.status()).toBeNull();
  });

  it('sucesso, isConfirming e erro começam com valor neutro', () => {
    expect(component.sucesso()).toBe(false);
    expect(component.isConfirming()).toBe(false);
    expect(component.erro()).toBe('');
  });

  it('confirmar não avança sem token', () => {
    component.confirmar();
    expect(component.isConfirming()).toBe(false);
  });
});
