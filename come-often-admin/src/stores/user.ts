import { defineStore } from 'pinia';
import { authApi } from '@/api/auth';
import type { MenuItem, UserInfo } from '@/api/auth';
import { clearToken, getToken, setToken } from '@/api/request';

interface UserState {
  token: string;
  info: UserInfo | null;
  menus: MenuItem[];
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken() ?? '',
    info: null,
    menus: [],
  }),
  getters: {
    permissions: (state): string[] => state.info?.permissions ?? [],
  },
  actions: {
    async login(username: string, password: string): Promise<void> {
      const result = await authApi.login(username, password);
      setToken(result.token);
      this.token = result.token;
      await this.bootstrap();
    },
    async bootstrap(): Promise<void> {
      this.info = await authApi.info();
      this.menus = await authApi.menus();
    },
    async logout(): Promise<void> {
      try {
        await authApi.logout();
      } finally {
        clearToken();
        this.$reset();
      }
    },
  },
});
