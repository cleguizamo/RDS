import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { GoogleMapsModule } from '@angular/google-maps';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, GoogleMapsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  // Información del bar (puedes mover esto a un servicio después)
  barInfo = {
    name: 'El Rincón del Sabor',
    slogan: 'Donde cada sorbo es una experiencia',
    address: 'Garagoa, Boyacá, Colombia',
    phone: '+57 300 123 4567',
    hours: {
      weekdays: 'Lun - Jue: 5:00 PM - 2:00 AM',
      weekend: 'Vie - Dom: 4:00 PM - 3:00 AM'
    },
    description: 'El mejor lugar para disfrutar de bebidas excepcionales, comida deliciosa y momentos inolvidables en un ambiente acogedor y vibrante.'
  };

  // Coordenadas del restaurante El Rincón del Sabor en Garagoa, Boyacá
  center: { lat: number; lng: number } = {
    lat: 5.0821692,
    lng: -73.364131
  };

  zoom = 17;
  
  mapOptions: any = {
    disableDefaultUI: false,
    zoomControl: true,
    scrollwheel: true,
    disableDoubleClickZoom: false,
    mapTypeId: 'roadmap',
    styles: [
      {
        featureType: 'poi',
        elementType: 'labels',
        stylers: [{ visibility: 'off' }]
      },
      {
        featureType: 'water',
        elementType: 'geometry',
        stylers: [{ color: '#e9ecef' }]
      },
      {
        featureType: 'road',
        elementType: 'geometry',
        stylers: [{ color: '#ffffff' }]
      },
      {
        featureType: 'road',
        elementType: 'labels.text.fill',
        stylers: [{ color: '#757575' }]
      }
    ]
  };

  markerOptions: any = {
    draggable: false,
    icon: {
      url: 'http://maps.google.com/mapfiles/ms/icons/red-dot.png'
    }
  };

  markerPosition: { lat: number; lng: number } = this.center;
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    // Inicializar opciones que dependen de google.maps solo en el navegador
    if (this.isBrowser && typeof google !== 'undefined' && google.maps) {
      this.markerOptions = {
        draggable: false,
        animation: google.maps.Animation.DROP,
        icon: {
          url: 'http://maps.google.com/mapfiles/ms/icons/red-dot.png'
        }
      };

      this.mapOptions = {
        disableDefaultUI: false,
        zoomControl: true,
        scrollwheel: true,
        disableDoubleClickZoom: false,
        mapTypeId: google.maps.MapTypeId.ROADMAP,
        styles: [
          {
            featureType: 'poi',
            elementType: 'labels',
            stylers: [{ visibility: 'off' }]
          },
          {
            featureType: 'water',
            elementType: 'geometry',
            stylers: [{ color: '#e9ecef' }]
          },
          {
            featureType: 'road',
            elementType: 'geometry',
            stylers: [{ color: '#ffffff' }]
          },
          {
            featureType: 'road',
            elementType: 'labels.text.fill',
            stylers: [{ color: '#757575' }]
          }
        ]
      };
    }
  }
}

