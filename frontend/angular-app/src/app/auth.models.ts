export interface AuthSession {
  tenantId: string;
  accessToken: string;
  email: string;
  roles: string[];
}

export interface LoginApiResponse {
  token: string;
  tenantId: string;
  roles: string[];
}
