// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ExportService } from './export.service';
import { environment } from '../../../environments/environment';

describe('ExportService', () => {
  let service: ExportService;
  let http: HttpTestingController;
  const base = `${environment.apiUrl}/export`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ExportService);
    http = TestBed.inject(HttpTestingController);

    vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:mock');
    vi.spyOn(window.URL, 'revokeObjectURL').mockReturnValue(undefined);
  });

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  function flushDownload(url: string, filename: string) {
    const link = { href: '', download: '', click: vi.fn() } as any;
    const spy = vi.spyOn(document, 'createElement').mockReturnValue(link);
    const req = http.expectOne(url);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['a,b,c'], { type: 'text/csv' }));
    expect(link.download).toBe(filename);
    expect(link.click).toHaveBeenCalled();
    spy.mockRestore();
  }

  it('exportAdmin faz GET /export/admin com filename relatorio_admin.csv', () => {
    service.exportAdmin();
    flushDownload(`${base}/admin`, 'relatorio_admin.csv');
  });

  it('exportProfessional faz GET /export/professional com filename meus_atendimentos.csv', () => {
    service.exportProfessional();
    flushDownload(`${base}/professional`, 'meus_atendimentos.csv');
  });

  it('exportPatient faz GET /export/patient com filename meus_dados_clinicos.csv', () => {
    service.exportPatient();
    flushDownload(`${base}/patient`, 'meus_dados_clinicos.csv');
  });

  it('exportPatients faz GET /export/patients com filename lista_pacientes.csv', () => {
    service.exportPatients();
    flushDownload(`${base}/patients`, 'lista_pacientes.csv');
  });

  it('exportProfessionals faz GET /export/professionals com filename lista_profissionais.csv', () => {
    service.exportProfessionals();
    flushDownload(`${base}/professionals`, 'lista_profissionais.csv');
  });

  it('revoga blob URL após o clique no link', () => {
    vi.spyOn(document, 'createElement').mockReturnValue({ href: '', download: '', click: vi.fn() } as any);
    service.exportAdmin();
    http.expectOne(`${base}/admin`).flush(new Blob(['x']));
    expect(window.URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock');
  });

  it('exibe alerta quando o backend retorna erro de rede', () => {
    const alertSpy = vi.spyOn(window, 'alert').mockReturnValue(undefined);
    service.exportAdmin();
    http.expectOne(`${base}/admin`).error(new ErrorEvent('network'));
    expect(alertSpy).toHaveBeenCalledWith('Não foi possível gerar a exportação no momento.');
  });
});
