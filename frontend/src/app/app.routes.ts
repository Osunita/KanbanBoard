import { Routes } from '@angular/router';

/** Routes per SPEC §8.1. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'projects' },
  {
    path: 'projects',
    loadComponent: () => import('./features/projects/project-list-page').then((m) => m.ProjectListPage),
  },
  {
    path: 'projects/:id',
    loadComponent: () => import('./features/board/board-page').then((m) => m.BoardPage),
  },
  { path: '**', redirectTo: 'projects' },
];
