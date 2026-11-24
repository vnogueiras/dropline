import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { Footer } from '@core/layout/footer/footer';
import { Header } from '@core/layout/header/header';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [MatButtonModule, Header, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  title = 'Dropline';

  onClick(): void {
    console.log('Angular Material is working!');
  }
}
