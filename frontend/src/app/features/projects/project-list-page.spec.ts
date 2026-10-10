/** TEST-021 — projects empty state (SPEC §12.2, foundation/project R1). */
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ProjectListPage } from './project-list-page';

describe('ProjectListPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectListPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('TEST-021: shows Aún no hay proyectos. Crea el primero. for an empty list', async () => {
    const fixture = TestBed.createComponent(ProjectListPage);
    fixture.detectChanges();

    httpMock.expectOne('/api/projects').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Aún no hay proyectos. Crea el primero.');
    expect(element.textContent).not.toContain('Cargando…');
  });
});
