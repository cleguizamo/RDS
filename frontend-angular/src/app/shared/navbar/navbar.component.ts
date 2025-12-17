import { Component, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CartDropdownComponent } from './cart-dropdown/cart-dropdown.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, CartDropdownComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent implements OnDestroy {
  isMenuOpen = signal(false);
  isDropdownOpen = signal(false);
  private dropdownTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnDestroy(): void {
    if (this.dropdownTimeout) {
      clearTimeout(this.dropdownTimeout);
    }
  }

  toggleMenu(): void {
    this.isMenuOpen.set(!this.isMenuOpen());
  }

  openDropdown(): void {
    if (this.dropdownTimeout) {
      clearTimeout(this.dropdownTimeout);
      this.dropdownTimeout = null;
    }
    this.isDropdownOpen.set(true);
  }

  closeDropdown(): void {
    // Agregar un pequeño delay para permitir que el usuario mueva el mouse al dropdown
    if (this.dropdownTimeout) {
      clearTimeout(this.dropdownTimeout);
    }
    this.dropdownTimeout = setTimeout(() => {
      this.isDropdownOpen.set(false);
      this.dropdownTimeout = null;
    }, 200);
  }

  keepDropdownOpen(): void {
    if (this.dropdownTimeout) {
      clearTimeout(this.dropdownTimeout);
      this.dropdownTimeout = null;
    }
    this.isDropdownOpen.set(true);
  }

  logout(): void {
    this.authService.logout();
  }
}

