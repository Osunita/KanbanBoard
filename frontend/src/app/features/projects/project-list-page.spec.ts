/** TEST-021 + delta scenarios — projects screen states: empty, error/retry (AC-019), edit+reload, delete confirm (AC-021). */
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { Project } from '../../core/models/project.model';
import { ProjectListPage } from './project-list-page';

const project: Project = {
  id: 1,
  name: 'Entrevista',
  description: 'Preparación',
  createdAt: '2026-10-10T09:00:00.000Z',
  updatedAt: '2026-10-10T09:00:00.000Z',
};

const button = (element: HTMLElement, label: string): HTMLButtonElement | undefined =>
  Array.from(element.querySelectorAll('button')).find(
    (candidate) => candidate.textContent?.trim() === label,
  );

describe('ProjectListPage', () => {
  let httpMock: HttpTestingController;

  beforeAll(() => {
    // jsdom lacks <dialog> modal semantics; shim for requestDelete/confirmDelete (F7).
    if (typeof HTMLDialogElement.prototype.showModal !== 'function') {
      HTMLDialogElement.prototype.showModal = function (this: HTMLDialogElement) {
        this.setAttribute('open', '');
      };
    }
    if (typeof HTMLDialogElement.prototype.close !== 'function') {
      HTMLDialogElement.prototype.close = function (this: HTMLDialogElement) {
        this.removeAttribute('open');
      };
    }
  });

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

  it('AC-019: 500 on the projects load shows the error text and Reintentar re-issues it', async () => {
    const fixture = TestBed.createComponent(ProjectListPage);
    fixture.detectChanges();

    httpMock
      .expectOne('/api/projects')
      .flush('boom', { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    // Guard: never read value() while status() === 'error' (batch-1 gotcha).
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No se pudieron cargar los datos.');
    const retry = button(element, 'Reintentar');
    expect(retry).toBeTruthy();

    // reload() issues the request through an effect; flush it explicitly.
    // Never await whenStable() with a request in flight — stability waits for HTTP.
    retry?.click();
    TestBed.flushEffects();
    httpMock.expectOne('/api/projects').flush([project]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.textContent).not.toContain('No se pudieron cargar los datos.');
    expect(element.textContent).toContain('Entrevista');
  });

  it('reload() after a successful edit fetches the updated list', async () => {
    const fixture = TestBed.createComponent(ProjectListPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/projects').flush([project]);
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.edit(project);
    fixture.componentInstance.saveProject({ name: 'Editado', description: null });
    httpMock.expectOne('/api/projects/1').flush({ ...project, name: 'Editado' });
    TestBed.flushEffects();

    httpMock.expectOne('/api/projects').flush([{ ...project, name: 'Editado' }]);
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Editado');
    expect(element.textContent).not.toContain('Entrevista');
  });

  it('AC-021: cancelling deletion sends no request; confirming deletes and reloads', async () => {
    const fixture = TestBed.createComponent(ProjectListPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/projects').flush([project]);
    await fixture.whenStable();
    fixture.detectChanges();

    // Cancel path: dialog shows the literal confirm text, then Cancelar closes it untouched.
    fixture.componentInstance.requestDelete(project);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain(
      'Se eliminará el proyecto y todas sus tareas. Esta acción no se puede deshacer.',
    );

    fixture.componentInstance.cancelDelete();
    httpMock.expectNone(() => true);
    expect(element.textContent).toContain('Entrevista');

    // Confirm path: DELETE then a reload of the list (effect-flushed).
    fixture.componentInstance.requestDelete(project);
    fixture.componentInstance.confirmDelete();
    httpMock
      .expectOne('/api/projects/1')
      .flush(null, { status: 204, statusText: 'No Content' });
    TestBed.flushEffects();

    httpMock.expectOne('/api/projects').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.textContent).toContain('Aún no hay proyectos. Crea el primero.');
  });
});
