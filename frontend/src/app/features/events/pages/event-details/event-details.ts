import { Component, input } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-event-details',
  styleUrl: './event-details.css',
  templateUrl: './event-details.html',
})
export class EventDetails {
  id = input.required<string>();
}
