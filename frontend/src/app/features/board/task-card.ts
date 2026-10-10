/** Task card: title, priority badge, 2-line clamped description, Editar/Eliminar (SPEC §8.4). */
import { Component, input, output } from '@angular/core';
import type { Task } from '../../core/models/task.model';

@Component({
  selector: 'app-task-card',
  styleUrl: './task-card.css',
  templateUrl: './task-card.html',
})
export class TaskCard {
  readonly task = input.required<Task>();

  readonly edit = output<Task>();
  readonly remove = output<Task>();
}
