import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdminUserSummary,
  PageResponse,
  PlatformMetrics,
  Role,
  UpdateUserRoleRequest,
  UpdateUserStatusRequest
} from '../models/auth.models';

export interface AdminUserFilterParams {
  search?: string;
  role?: Role;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/admin/users';

  getUsers(filterParameters?: AdminUserFilterParams): Observable<PageResponse<AdminUserSummary>> {
    const queryParameters: Record<string, string> = {};

    if (filterParameters) {
      for (const [parameterName, parameterValue] of Object.entries(filterParameters)) {
        if (parameterValue !== undefined && parameterValue !== null) {
          const stringValue = typeof parameterValue === 'string'
            ? parameterValue.trim()
            : String(parameterValue);

          if (stringValue.length > 0) {
            queryParameters[parameterName] = stringValue;
          }
        }
      }
    }

    return this.http.get<PageResponse<AdminUserSummary>>(this.baseUrl, {
      params: new HttpParams({ fromObject: queryParameters })
    });
  }

  getPlatformMetrics(): Observable<PlatformMetrics> {
    return this.http.get<PlatformMetrics>(`${this.baseUrl}/metrics`);
  }

  updateUserRole(userId: number, role: Role): Observable<AdminUserSummary> {
    const payload: UpdateUserRoleRequest = { role };
    return this.http.patch<AdminUserSummary>(`${this.baseUrl}/${userId}/role`, payload);
  }

  updateUserStatus(userId: number, active: boolean): Observable<AdminUserSummary> {
    const payload: UpdateUserStatusRequest = { active };
    return this.http.patch<AdminUserSummary>(`${this.baseUrl}/${userId}/status`, payload);
  }
}
