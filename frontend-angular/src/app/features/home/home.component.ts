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
  // URL de la imagen de fondo en Cloudinary
  backgroundImage = 'https://res.cloudinary.com/drp8os7tp/image/upload/v1763599708/bamboo-background_mlegad.jpg';
  
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

  // Galería de imágenes
  galleryImages: Array<{ url: string; alt?: string }> = [
    { url: 'https://res.cloudinary.com/drp8os7tp/image/upload/v1765919998/2022-08-10_md3nnf.jpg', alt: 'Botella de whisky' },
    { url: 'https://res.cloudinary.com/drp8os7tp/image/upload/v1765920010/2022-09-12-2_y1drga.jpg', alt: 'Frente del local' },
    { url: 'https://res.cloudinary.com/drp8os7tp/image/upload/v1765920087/2022-09-12_rbms81.jpg', alt: 'Frente diagonal del negocio' },
    { url: 'https://res.cloudinary.com/drp8os7tp/image/upload/v1765920094/2022-10-16_bknd4m.jpg', alt: 'Vista diagonal exterior del negocio' },
    { url: 'https://res.cloudinary.com/drp8os7tp/image/upload/v1765920149/2024-06-29_trxx10.jpg', alt: 'Grupo de personas festejando en el negocio' },
    { url: 'https://res.cloudinary.com/drp8os7tp/image/upload/v1765920132/2023-07-08_x1cevv.jpg', alt: 'Exterior del negocio' }
  ];

  // Lightbox
  lightboxOpen = false;
  currentImageIndex = 0;

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

  openLightbox(index: number): void {
    this.currentImageIndex = index;
    this.lightboxOpen = true;
    // Prevenir scroll del body cuando el lightbox está abierto
    if (this.isBrowser) {
      document.body.style.overflow = 'hidden';
      // Agregar listener para teclado
      document.addEventListener('keydown', this.handleKeyDown);
    }
  }

  closeLightbox(): void {
    this.lightboxOpen = false;
    if (this.isBrowser) {
      document.body.style.overflow = '';
      // Remover listener de teclado
      document.removeEventListener('keydown', this.handleKeyDown);
    }
  }

  private handleKeyDown = (event: KeyboardEvent): void => {
    if (!this.lightboxOpen) return;

    switch (event.key) {
      case 'Escape':
        this.closeLightbox();
        break;
      case 'ArrowRight':
        this.nextImage(event);
        break;
      case 'ArrowLeft':
        this.prevImage(event);
        break;
    }
  };

  nextImage(event: Event): void {
    event.stopPropagation();
    this.currentImageIndex = (this.currentImageIndex + 1) % this.galleryImages.length;
  }

  prevImage(event: Event): void {
    event.stopPropagation();
    this.currentImageIndex = (this.currentImageIndex - 1 + this.galleryImages.length) % this.galleryImages.length;
  }
}

