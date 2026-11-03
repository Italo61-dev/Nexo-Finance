import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminService } from '../../core/services/admin.service';
import { AuthService } from '../../core/services/auth.service';
import { AdminUserSummary, PlatformMetrics, Role } from '../../core/models/auth.models';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';

type FilterTab = 'ALL' | 'ADMINS' | 'USERS' | 'INACTIVE';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatSnackBarModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  // Estados reativos
  readonly users = signal<AdminUserSummary[]>([]);
  readonly metrics = signal<PlatformMetrics | null>(null);
  readonly loading = signal<boolean>(false);
  readonly loadingMetrics = signal<boolean>(false);
  readonly processingUserId = signal<number | null>(null);

  // Filtros e Paginação
  searchTerm = '';
  selectedFilter: FilterTab = 'ALL';
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  ngOnInit(): void {
    this.loadMetrics();
    this.loadUsers();
  }

  loadMetrics(): void {
    this.loadingMetrics.set(true);
    this.adminService.getPlatformMetrics().subscribe({
      next: data => {
        this.metrics.set(data);
        this.loadingMetrics.set(false);
      },
      error: () => {
        this.loadingMetrics.set(false);
      }
    });
  }

  loadUsers(): void {
    this.loading.set(true);

    let roleFilter: Role | undefined;
    let activeFilter: boolean | undefined;

    if (this.selectedFilter === 'ADMINS') {
      roleFilter = 'ROLE_ADMIN';
    } else if (this.selectedFilter === 'USERS') {
      roleFilter = 'ROLE_USER';
    } else if (this.selectedFilter === 'INACTIVE') {
      activeFilter = false;
    }

    this.adminService.getUsers({
      search: this.searchTerm,
      role: roleFilter,
      active: activeFilter,
      page: this.currentPage,
      size: this.pageSize,
      sort: 'createdAt,desc'
    }).subscribe({
      next: response => {
        this.users.set(response.content || []);
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || 0;
        this.loading.set(false);
      },
      error: errorResponse => {
        this.loading.set(false);
        this.showError(errorResponse?.error?.message || 'Erro ao carregar usuários.');
      }
    });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadUsers();
  }

  onFilterChange(tab: FilterTab): void {
    this.selectedFilter = tab;
    this.currentPage = 0;
    this.loadUsers();
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadUsers();
    }
  }

  toggleRole(user: AdminUserSummary): void {
    if (this.isCurrentUser(user)) {
      this.showWarning('Regra de segurança: você não pode alterar seu próprio cargo.');
      return;
    }

    const newRole: Role = user.role === 'ROLE_ADMIN' ? 'ROLE_USER' : 'ROLE_ADMIN';
    const isPromoting = newRole === 'ROLE_ADMIN';

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: isPromoting ? 'Promover para Administrador' : 'Rebaixar para Usuário',
        message: isPromoting
          ? `Tem certeza de que deseja conceder acesso administrativo para "${user.name}"? O usuário terá permissões completas sobre o sistema.`
          : `Tem certeza de que deseja rebaixar "${user.name}" para o cargo de Usuário comum?`,
        confirmText: isPromoting ? 'Promover' : 'Rebaixar',
        cancelText: 'Cancelar',
        variant: isPromoting ? 'primary' : 'warning',
        icon: isPromoting ? 'security' : 'person'
      },
      width: '440px'
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;

      this.processingUserId.set(user.id);
      this.adminService.updateUserRole(user.id, newRole).subscribe({
        next: updatedUser => {
          this.users.update(currentList => currentList.map(existingUser => existingUser.id === updatedUser.id ? updatedUser : existingUser));
          this.processingUserId.set(null);
          this.showSuccess(`Perfil de "${updatedUser.name}" atualizado para ${updatedUser.role === 'ROLE_ADMIN' ? 'Administrador' : 'Usuário'}.`);
          this.loadMetrics();
        },
        error: errorResponse => {
          this.processingUserId.set(null);
          this.showError(errorResponse?.error?.message || 'Falha ao atualizar cargo do usuário.');
        }
      });
    });
  }

  toggleStatus(user: AdminUserSummary): void {
    if (this.isCurrentUser(user)) {
      this.showWarning('Regra de segurança: você não pode desativar sua própria conta.');
      return;
    }

    const newStatus = !user.active;
    const isActivating = newStatus;

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: isActivating ? 'Reativar Conta' : 'Bloquear Acesso',
        message: isActivating
          ? `Tem certeza de que deseja reativar o acesso de "${user.name}"? O usuário poderá acessar a plataforma normalmente.`
          : `Tem certeza de que deseja bloquear o acesso de "${user.name}"? O acesso será revogado preventivamente.`,
        confirmText: isActivating ? 'Ativar Conta' : 'Bloquear Acesso',
        cancelText: 'Cancelar',
        variant: isActivating ? 'primary' : 'danger',
        icon: isActivating ? 'check_circle' : 'block'
      },
      width: '440px'
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;

      this.processingUserId.set(user.id);
      this.adminService.updateUserStatus(user.id, newStatus).subscribe({
        next: updatedUser => {
          this.users.update(currentList => currentList.map(existingUser => existingUser.id === updatedUser.id ? updatedUser : existingUser));
          this.processingUserId.set(null);
          this.showSuccess(`Usuário "${updatedUser.name}" ${updatedUser.active ? 'reativado' : 'bloqueado'} com sucesso.`);
          this.loadMetrics();
        },
        error: errorResponse => {
          this.processingUserId.set(null);
          this.showError(errorResponse?.error?.message || 'Falha ao atualizar status do usuário.');
        }
      });
    });
  }

  isCurrentUser(user: AdminUserSummary): boolean {
    const currentEmail = this.authService.userEmail();
    if (!currentEmail || !user.email) return false;
    return user.email.trim().toLowerCase() === currentEmail.trim().toLowerCase();
  }

  getUserInitials(name: string): string {
    if (!name) return 'U';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  goToDashboard(): void {
    this.router.navigate(['/home']);
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'OK', {
      duration: 4000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['snackbar-success']
    });
  }

  private showWarning(message: string): void {
    this.snackBar.open(message, 'Entendido', {
      duration: 5000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['snackbar-warning']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Fechar', {
      duration: 6000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['snackbar-danger']
    });
  }
}
