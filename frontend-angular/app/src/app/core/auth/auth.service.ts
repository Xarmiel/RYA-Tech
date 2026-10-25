import { Injectable, InjectionToken, computed, inject, signal } from '@angular/core';
import { Session, User, createClient } from '@supabase/supabase-js';
import { environment } from '../../../environments/environment';

export const SUPABASE_AUTH_CLIENT = new InjectionToken<ReturnType<typeof createClient>>('Supabase auth client', {
  providedIn: 'root',
  factory: () => createClient(environment.supabaseUrl, environment.supabaseAnonKey, {
    auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true },
  }),
});

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly client = inject(SUPABASE_AUTH_CLIENT);
  private initialization?: Promise<Session | null>;
  private sessionRevision = 0;

  readonly session = signal<Session | null>(null);
  readonly ready = signal(false);
  readonly user = computed<User | null>(() => this.session()?.user ?? null);
  readonly isAuthenticated = computed(() => !!this.user());

  constructor() {
    this.client.auth.onAuthStateChange((_event, session) => {
      this.acceptSession(session);
    });
  }

  initialize(): Promise<Session | null> {
    if (this.ready()) return Promise.resolve(this.session());
    if (!this.initialization) {
      const revision = this.sessionRevision;
      this.initialization = this.client.auth.getSession().then(({ data, error }) => {
        if (error) throw error;
        // A late bootstrap response must not overwrite a newer login/logout event.
        if (this.sessionRevision === revision) this.acceptSession(data.session);
        return this.session();
      }).catch(error => {
        this.initialization = undefined;
        if (this.ready()) return this.session();
        throw error;
      });
    }
    // Cache only the bootstrap work, never the old session value returned by it.
    return this.initialization.then(() => this.session());
  }

  private acceptSession(session: Session | null): void {
    this.sessionRevision++;
    this.session.set(session);
    this.ready.set(true);
  }

  async signIn(email: string, password: string): Promise<void> {
    const { data, error } = await this.client.auth.signInWithPassword({ email: email.trim(), password });
    if (error) throw error;
    this.acceptSession(data.session);
  }

  async signUp(name: string, phone: string, email: string, password: string): Promise<boolean> {
    const { data, error } = await this.client.auth.signUp({
      email: email.trim(), password,
      options: {
        emailRedirectTo: `${location.origin}/login?confirmed=1`,
        data: { nombre: name.trim(), telefono: phone.trim() },
      },
    });
    if (error) throw error;
    if (data.user && Array.isArray(data.user.identities) && data.user.identities.length === 0) {
      throw new Error('User already registered');
    }
    this.acceptSession(data.session);
    return !!data.session;
  }

  async requestPasswordReset(email: string): Promise<void> {
    const { error } = await this.client.auth.resetPasswordForEmail(email.trim(), {
      redirectTo: `${location.origin}/restablecer-clave`,
    });
    if (error) throw error;
  }

  async updatePassword(password: string): Promise<void> {
    const { error } = await this.client.auth.updateUser({ password });
    if (error) throw error;
  }

  async signOut(): Promise<void> {
    const { error } = await this.client.auth.signOut();
    if (error) throw error;
    this.acceptSession(null);
  }

  accessToken(): string | null {
    const memToken = this.session()?.access_token;
    if (memToken) return memToken;
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        // Direct check for standard Supabase project key
        const projectKey = `sb-rasudbiaxwyoojxxdutz-auth-token`;
        const directRaw = localStorage.getItem(projectKey);
        if (directRaw) {
          try {
            const parsed = JSON.parse(directRaw);
            if (parsed?.access_token) return parsed.access_token;
            if (parsed?.currentSession?.access_token) return parsed.currentSession.access_token;
          } catch {
            // ignore
          }
        }

        // Generic fallback scan
        for (let i = 0; i < localStorage.length; i++) {
          const key = localStorage.key(i);
          if (key && (key.includes('auth-token') || key.startsWith('sb-') || key.includes('supabase.auth'))) {
            const raw = localStorage.getItem(key);
            if (raw && raw.includes('access_token')) {
              try {
                const parsed = JSON.parse(raw);
                if (parsed?.access_token) return parsed.access_token;
                if (parsed?.currentSession?.access_token) return parsed.currentSession.access_token;
              } catch {
                // not json
              }
            }
          }
        }
      }
    } catch {
      // Ignore localStorage read errors
    }
    return null;
  }
}
