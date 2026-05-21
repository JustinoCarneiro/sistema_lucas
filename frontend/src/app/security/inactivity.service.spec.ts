// @vitest-environment jsdom
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { InactivityService } from './inactivity.service';
import { provideRouter } from '@angular/router';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';

describe('InactivityService', () => {
  let service: InactivityService;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();
    service = TestBed.inject(InactivityService);
  });

  afterEach(() => {
    service.stop();
    vi.useRealTimers();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('stop() não lança erro quando chamado sem start() anterior', () => {
    expect(() => service.stop()).not.toThrow();
  });

  it('start() não lança erro', () => {
    expect(() => service.start()).not.toThrow();
  });

  it('start() é idempotente — chamadas duplas não criam dois intervalos', () => {
    const setIntervalSpy = vi.spyOn(globalThis, 'setInterval');
    service.start();
    service.start(); // Segunda chamada deve ser ignorada
    expect(setIntervalSpy).toHaveBeenCalledTimes(1);
  });

  it('stop() após start() limpa o intervalo', () => {
    const clearIntervalSpy = vi.spyOn(globalThis, 'clearInterval');
    service.start();
    service.stop();
    expect(clearIntervalSpy).toHaveBeenCalled();
  });
});
