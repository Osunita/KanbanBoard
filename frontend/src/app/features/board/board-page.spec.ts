/** TEST-022 — board load error state (SPEC §12.2, board R4) plus the 404 state (board R1). */
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { Project } from '../../core/models/project.model';
import { BoardPage } from './board-page';

const project: Project = {
  id: 1,
  name: 'Demo',
  description: null,
  createdAt: '2026-10-10T09:00:00.000Z',
  updatedAt: '2026-10-10T09:00:00.000Z',
};

const button = (element: HTMLElement, label: string): HTMLButtonElement | undefined =>
  Array.from(element.querySelectorAll('button')).find(
    (candidate) => candidate.textContent?.trim() === label,
  );

describe('BoardPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BoardPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('TEST-022: 500 on the tasks load shows the error text and Reintentar', () => {
    const fixture = TestBed.createComponent(BoardPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    httpMock.expectOne('/api/projects/1').flush(project);
    httpMock
      .expectOne('/api/projects/1/tasks')
      .flush('boom', { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No se pudieron cargar los datos.');
    const retry = button(element, 'Reintentar');
    expect(retry).toBeTruthy();

    // Reintentar re-issues the load.
    retry?.click();
    httpMock.expectOne('/api/projects/1').flush(project);
    httpMock.expectOne('/api/projects/1/tasks').flush([]);
    fixture.detectChanges();

    expect(element.textContent).not.toContain('No se pudieron cargar los datos.');
    expect(element.textContent).toContain('Nueva tarea');
    expect(element.textContent).toContain('Por hacer');
    expect(element.textContent).toContain('Sin tareas');
  });

  it('shows Proyecto no encontrado. when the API answers 404', () => {
    const fixture = TestBed.createComponent(BoardPage);
    fixture.componentRef.setInput('id', '999');
    fixture.detectChanges();

    httpMock
      .expectOne('/api/projects/999')
      .flush(null, { status: 404, statusText: 'Not Found' });
    httpMock
      .expectOne('/api/projects/999/tasks')
      .flush(null, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Proyecto no encontrado.');
    expect(element.textContent).toContain('Volver a proyectos');
  });

  it('shows Proyecto no encontrado. for a non-numeric id without calling the API', () => {
    const fixture = TestBed.createComponent(BoardPage);
    fixture.componentRef.setInput('id', 'abc');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Proyecto no encontrado.');
    expect(element.textContent).toContain('Volver a proyectos');
    httpMock.expectNone(() => true);
  });
});
