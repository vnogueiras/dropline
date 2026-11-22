import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  title = 'Dropline';

  onClick(): void {
    console.log('Angular Material is working!');
  }
}
