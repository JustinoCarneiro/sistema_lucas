// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PanelComponent } from './panel';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../security/auth.service';
import { vi, describe, beforeAll, beforeEach, it, expect } from 'vitest';

describe('PanelComponent', () => {
  let component: PanelComponent;
  let fixture: ComponentFixture<PanelComponent>;
  let authService: AuthService;

  beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: false, media: query, onchange: null,
        addListener: vi.fn(), removeListener: vi.fn(),
        addEventListener: vi.fn(), removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }))
    });
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PanelComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(PanelComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('isSidebarOpen começa fechado', () => {
    expect(component.isSidebarOpen).toBe(false);
  });

  it('toggleSidebar alterna o estado da sidebar', () => {
    component.toggleSidebar();
    expect(component.isSidebarOpen).toBe(true);
    component.toggleSidebar();
    expect(component.isSidebarOpen).toBe(false);
  });

  it('closeSidebar fecha a sidebar independente do estado', () => {
    component.isSidebarOpen = true;
    component.closeSidebar();
    expect(component.isSidebarOpen).toBe(false);
  });

  it('logout chama authService.logout', () => {
    const logoutSpy = vi.spyOn(authService, 'logout').mockImplementation(() => {});
    component.logout();
    expect(logoutSpy).toHaveBeenCalledOnce();
  });

  it('ngOnInit sem role no localStorage mantém userRole null', () => {
    localStorage.removeItem('role');
    component.ngOnInit();
    expect(component.userRole).toBeNull();
  });
});
