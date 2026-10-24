import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService, SUPABASE_AUTH_CLIENT } from './auth.service';
import { authGuard } from './auth.guard';

const sdk = {
  getSession:vi.fn<(...args:any[])=>any>(), signInWithPassword:vi.fn<(...args:any[])=>any>(),
  signUp:vi.fn<(...args:any[])=>any>(),signOut:vi.fn<(...args:any[])=>any>(),
  onAuthStateChange:vi.fn<(...args:any[])=>any>(), updateUser:vi.fn(),resetPasswordForEmail:vi.fn(),
};
@Component({template:'<h1>Cuenta de prueba</h1>'}) class TestPage {}

describe('Current auth session and route regression',()=>{
  const session={user:{id:'user-test'},access_token:'fake-test-token'};
  let event:(event:string,session:any)=>void;
  let networkGuard:any;
  const makeAuth=()=>TestBed.runInInjectionContext(()=>new AuthService());
  beforeEach(()=>{
    TestBed.configureTestingModule({providers:[{provide:SUPABASE_AUTH_CLIENT,useValue:{auth:sdk}}]});
    networkGuard=vi.spyOn(globalThis,'fetch').mockRejectedValue(new Error('Tests must not use real authentication requests'));
    sdk.getSession.mockReset().mockResolvedValue({data:{session:null},error:null});
    sdk.signInWithPassword.mockReset().mockResolvedValue({data:{session},error:null});
    sdk.signUp.mockReset().mockResolvedValue({data:{session},error:null});
    sdk.signOut.mockReset().mockResolvedValue({error:null});
    sdk.onAuthStateChange.mockReset().mockImplementation(callback=>{event=callback;});
  });
  afterEach(()=>{expect(networkGuard).not.toHaveBeenCalled();networkGuard.mockRestore();TestBed.resetTestingModule();});
  it('returns the current session after bootstrap without a session and then login',async()=>{
    const auth=makeAuth();expect(await auth.initialize()).toBeNull();await auth.signIn('test@example.invalid','test-password');
    expect(await auth.initialize()).toBe(session);expect(auth.user()?.id).toBe('user-test');expect(sdk.getSession).toHaveBeenCalledTimes(1);
  });
  it('returns a newly created registration session instead of cached null',async()=>{
    const auth=makeAuth();await auth.initialize();expect(await auth.signUp('Test','','test@example.invalid','test-password')).toBe(true);
    expect(await auth.initialize()).toBe(session);
  });
  it('does not invent a session for registration requiring email confirmation',async()=>{
    sdk.signUp.mockResolvedValue({data:{session:null},error:null});const auth=makeAuth();await auth.initialize();
    expect(await auth.signUp('Test','','test@example.invalid','test-password')).toBe(false);expect(await auth.initialize()).toBeNull();
  });
  it('does not return a stale logged-in session after logout',async()=>{
    sdk.getSession.mockResolvedValue({data:{session},error:null});const auth=makeAuth();await auth.initialize();await auth.signOut();
    expect(await auth.initialize()).toBeNull();expect(auth.accessToken()).toBeNull();
  });
  it('tracks refresh, recovery and logout events without reloading the page',async()=>{
    const auth=makeAuth();await auth.initialize();event('PASSWORD_RECOVERY',session);expect(await auth.initialize()).toBe(session);
    const refreshed={...session,access_token:'fake-refreshed-token'};event('TOKEN_REFRESHED',refreshed);expect(await auth.initialize()).toBe(refreshed);
    event('SIGNED_OUT',null);expect(await auth.initialize()).toBeNull();
  });
  it('does not overwrite a newer login with an older bootstrap response',async()=>{
    let resolve:any;sdk.getSession.mockReturnValue(new Promise(r=>{resolve=r;}));const auth=makeAuth();const pending=auth.initialize();
    await auth.signIn('test@example.invalid','test-password');resolve({data:{session:null},error:null});expect(await pending).toBe(session);expect(auth.user()?.id).toBe('user-test');
  });
  it('does not restore an old session if logout happens while initializing',async()=>{
    let resolve:any;sdk.getSession.mockReturnValue(new Promise(r=>{resolve=r;}));const auth=makeAuth();const pending=auth.initialize();
    event('SIGNED_OUT',null);resolve({data:{session},error:null});expect(await pending).toBeNull();
  });
  it('retries a failed bootstrap instead of caching its rejection forever',async()=>{
    sdk.getSession.mockRejectedValueOnce(new Error('temporary failure'));const auth=makeAuth();await expect(auth.initialize()).rejects.toThrow('temporary');
    expect(await auth.initialize()).toBeNull();expect(sdk.getSession).toHaveBeenCalledTimes(2);
  });
  it('shares concurrent bootstrap requests',async()=>{
    const auth=makeAuth();await Promise.all([auth.initialize(),auth.initialize(),auth.initialize()]);expect(sdk.getSession).toHaveBeenCalledTimes(1);
  });
  it('does not authenticate after rejected credentials',async()=>{
    sdk.signInWithPassword.mockResolvedValue({data:{session:null},error:new Error('Invalid login credentials')});const auth=makeAuth();await auth.initialize();
    await expect(auth.signIn('test@example.invalid','wrong-test-password')).rejects.toThrow('Invalid');expect(await auth.initialize()).toBeNull();
  });
  it('actually navigates to guarded routes after login, and blocks them again after logout',async()=>{
    TestBed.configureTestingModule({providers:[provideRouter([
      {path:'cuenta',component:TestPage,canActivate:[authGuard]},
      {path:'compras',component:TestPage,canActivate:[authGuard]},
      {path:'login',component:TestPage},
    ])]});
    const auth=TestBed.inject(AuthService);await auth.initialize();const harness=await RouterTestingHarness.create();
    await harness.navigateByUrl('/cuenta');expect(TestBed.inject(Router).url).toContain('/login');
    await auth.signIn('test@example.invalid','test-password');await harness.navigateByUrl('/cuenta');expect(TestBed.inject(Router).url).toBe('/cuenta');
    await auth.signOut();await harness.navigateByUrl('/compras');expect(TestBed.inject(Router).url).toContain('/login');
  });
});
