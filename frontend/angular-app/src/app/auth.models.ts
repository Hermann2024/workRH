export interface AuthSession {
  tenantId: string;
  accessToken: string;
  email: string;
  roles: string[];
}

export interface LoginApiResponse {
  accessToken?: string;
  token?: string;
  tenantId: string;
  roles: string[];
}
