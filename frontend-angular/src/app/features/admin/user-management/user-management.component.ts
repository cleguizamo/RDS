import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { UserResponse, UserUpdateRequest, DocumentType } from '../../../core/models/user.model';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css'
})
export class UserManagementComponent implements OnInit {
  users = signal<UserResponse[]>([]);
  selectedUser = signal<UserResponse | null>(null);
  editingUser = signal<UserUpdateRequest | null>(null);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  documentTypes = Object.values(DocumentType);

  constructor(private userService: AdminUserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.errorMessage.set(error.error?.message || 'Error al cargar usuarios. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  selectUser(user: UserResponse): void {
    this.selectedUser.set(user);
    this.editingUser.set({
      name: user.name,
      lastName: user.lastName,
      documentType: user.documentType,
      documentNumber: user.documentNumber,
      phone: user.phone,
      email: user.email,
      dateOfBirth: user.dateOfBirth,
      points: user.points,
      totalSpent: user.totalSpent
    });
  }

  updateUser(): void {
    const selected = this.selectedUser();
    const editing = this.editingUser();
    if (!selected || !editing) return;

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.userService.updateUser(selected.id, editing).subscribe({
      next: () => {
        this.successMessage.set('Usuario actualizado exitosamente');
        this.loadUsers();
        this.selectedUser.set(null);
        this.editingUser.set(null);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error || 'Error al actualizar usuario');
        this.loading.set(false);
      }
    });
  }

  deleteUser(id: number): void {
    if (!confirm('¿Estás seguro de eliminar este usuario?')) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.userService.deleteUser(id).subscribe({
      next: () => {
        this.successMessage.set('Usuario eliminado exitosamente');
        this.loadUsers();
        this.selectedUser.set(null);
        this.editingUser.set(null);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error || 'Error al eliminar usuario');
        this.loading.set(false);
      }
    });
  }

  cancelEdit(): void {
    this.selectedUser.set(null);
    this.editingUser.set(null);
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }
}

