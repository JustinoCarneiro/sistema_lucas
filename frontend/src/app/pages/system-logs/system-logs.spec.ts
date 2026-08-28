// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SystemLogsComponent } from './system-logs';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

describe('SystemLogsComponent', () => {
  let component: SystemLogsComponent;
  let fixture: ComponentFixture<SystemLogsComponent>;
  let httpMock: HttpTestingController;

  const paginaVazia = { content: [], totalPages: 0, totalElements: 0, number: 0 };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SystemLogsComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(SystemLogsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should create e carrega a primeira página sem filtro', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(r => r.url === `${environment.apiUrl}/system-logs`);
    expect(req.request.params.get('level')).toBeNull();
    req.flush(paginaVazia);

    expect(component).toBeTruthy();
    expect(component.logs()).toEqual([]);
  });

  it('lista eventos retornados e expõe badge por nível', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(r => r.url === `${environment.apiUrl}/system-logs`);
    req.flush({
      content: [
        { id: 1, level: 'ERROR', loggerName: 'com.sistema.lucas.service.EmailService', message: 'Falha de SMTP', stackTrace: 'trace...', criadoEm: '2026-08-27T10:00:00' }
      ],
      totalPages: 1, totalElements: 1, number: 0
    });

    expect(component.logs().length).toBe(1);
    expect(component.badgeClass('ERROR')).toContain('red');
    expect(component.badgeClass('WARN')).toContain('amber');
  });

  it('setFiltro reinicia a página e envia o parâmetro level', () => {
    fixture.detectChanges();
    httpMock.expectOne(r => r.url === `${environment.apiUrl}/system-logs`).flush(paginaVazia);

    component.setFiltro('ERROR');
    const req = httpMock.expectOne(r => r.url === `${environment.apiUrl}/system-logs`);
    expect(req.request.params.get('level')).toBe('ERROR');
    req.flush(paginaVazia);

    expect(component.paginaAtual()).toBe(0);
  });

  it('toggleStackTrace alterna a expansão de um evento', () => {
    fixture.detectChanges();
    httpMock.expectOne(r => r.url === `${environment.apiUrl}/system-logs`).flush(paginaVazia);

    expect(component.isExpandido(1)).toBe(false);
    component.toggleStackTrace(1);
    expect(component.isExpandido(1)).toBe(true);
    component.toggleStackTrace(1);
    expect(component.isExpandido(1)).toBe(false);
  });

  it('proximaPagina só avança quando há mais páginas', () => {
    fixture.detectChanges();
    httpMock.expectOne(r => r.url === `${environment.apiUrl}/system-logs`).flush({ ...paginaVazia, totalPages: 2 });

    component.proximaPagina();
    const req = httpMock.expectOne(r => r.url === `${environment.apiUrl}/system-logs`);
    expect(req.request.params.get('page')).toBe('1');
    req.flush({ ...paginaVazia, totalPages: 2 });

    component.proximaPagina(); // já na última página, não deve disparar nova request
    httpMock.expectNone(r => r.url === `${environment.apiUrl}/system-logs`);
  });
});
