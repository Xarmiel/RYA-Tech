import { Injectable, computed, signal } from '@angular/core';
import { Session, User, createClient } from '@supabase/supabase-js';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly client = createClient(environment.supabaseUrl, environment.supabaseAnonKey, {
    auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true },
  });
  private initialization?: Promise<Session | null>;

  readonly session = signal<Session | null>(null);
  readonly ready = signal(false);
  readonly user = computed<User | null>(() => this.session()?.user ?? null);
  readonly isAuthenticated = computed(() => !!this.user());

  constructor() {
    this.client.auth.onAuthStateChange((_event, session) => {
      this.session.set(session);
      this.ready.set(true);
    });
  }

  initialize(): Promise<Session | null> {
    if (!this.initialization) {
      this.initialization = this.client.auth.getSession().then(({ data, error }) => {
        if (error) throw error;
        this.session.set(data.session);
        this.ready.set(true);
        return data.session;
      });
    }
    return this.initialization;
  }

  async signIn(email: string, password: string): Promise<void> {
    const { data, error } = await this.client.auth.signInWithPassword({ email: email.trim(), password });
    if (error) throw error;
    this.session.set(data.session);
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
    this.session.set(data.session);
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
    this.session.set(null);
  }

  accessToken(): string | null { return this.session()?.access_token ?? null; }
}
