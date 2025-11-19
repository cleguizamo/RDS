import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  // Información del bar (puedes mover esto a un servicio después)
  barInfo = {
    name: 'El Buen Sabor',
    slogan: 'Donde cada sorbo es una experiencia',
    address: 'Calle 123 #45-67, Ciudad',
    phone: '+57 300 123 4567',
    hours: {
      weekdays: 'Lun - Jue: 5:00 PM - 2:00 AM',
      weekend: 'Vie - Dom: 4:00 PM - 3:00 AM'
    },
    description: 'El mejor lugar para disfrutar de bebidas excepcionales, comida deliciosa y momentos inolvidables en un ambiente acogedor y vibrante.'
  };
}

