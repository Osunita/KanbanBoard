/** Delta scenarios — project dialog validation: blank name, length text, double submit (SPEC §8.6, E-21). */
import { TestBed } from '@angular/core/testing';
import { ProjectFormDialog, type ProjectFormValue } from './project-form-dialog';

const button = (element: HTMLElement, label: string): HTMLButtonElement | undefined =>
  Array.from(element.querySelectorAll('button')).find(
    (candidate) => candidate.textContent?.trim() === label,
  );

describe('ProjectFormDialog', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ProjectFormDialog] }).compileComponents();
  });

  const openFixture = async () => {
    const fixture = TestBed.createComponent(ProjectFormDialog);
    fixture.detectChanges();
    fixture.componentInstance.open(null);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  };

  const type = (fixture: { nativeElement: HTMLElement; detectChanges: () => void }, value: string) => {
    const input = fixture.nativeElement.querySelector<HTMLInputElement>('#project-name');
    input!.value = value;
    input!.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  it('shows Este campo es obligatorio. and disables Guardar for empty or whitespace-only names', async () => {
    const fixture = await openFixture();
    const element = fixture.nativeElement as HTMLElement;

    // Empty name (fresh form).
    expect(element.textContent).toContain('Este campo es obligatorio.');
    expect(button(element, 'Guardar')?.disabled).toBe(true);

    // Whitespace-only name must stay invalid (SPEC §7.3 / E-04 trim rule).
    type(fixture, '   ');
    expect(element.textContent).toContain('Este campo es obligatorio.');
    expect(button(element, 'Guardar')?.disabled).toBe(true);
  });

  it('shows Máximo 100 caracteres. for names longer than 100 characters', async () => {
    const fixture = await openFixture();
    const element = fixture.nativeElement as HTMLElement;

    type(fixture, 'x'.repeat(101));

    expect(element.textContent).toContain('Máximo 100 caracteres.');
    expect(button(element, 'Guardar')?.disabled).toBe(true);

    // Exactly 100 characters is valid (SPEC §7.3 boundary).
    type(fixture, 'x'.repeat(100));
    expect(element.textContent).not.toContain('Máximo 100 caracteres.');
    expect(button(element, 'Guardar')?.disabled).toBe(false);
  });

  it('disables Guardar while a request is in flight and emits nothing (E-21)', async () => {
    const fixture = await openFixture();
    const element = fixture.nativeElement as HTMLElement;
    type(fixture, 'Válido');

    const emitted: ProjectFormValue[] = [];
    fixture.componentInstance.save.subscribe((value) => emitted.push(value));

    // Request in flight (saving input bound by the page).
    fixture.componentRef.setInput('saving', true);
    fixture.detectChanges();

    expect(button(element, 'Guardar')?.disabled).toBe(true);
    button(element, 'Guardar')?.click();
    fixture.detectChanges();

    expect(emitted).toEqual([]);
  });
});
