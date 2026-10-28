import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './features/admin/admin-access';
import { technicalEntryGuard, adminLeaveGuard, adminSectionLeaveGuard } from './features/admin/technical-entry';
import { categoryPresentation } from './features/catalog/catalog-categories';

export const routes: Routes = [
  { path: 'admin/diseno', title: 'Diseño administrador | RYA', data: {design:true}, canDeactivate:[adminLeaveGuard], loadComponent: () => import('./features/admin/admin-page').then(m=>m.AdminPage) },
  { path: 'admin/diseno/:section', title: 'Diseño administrador | RYA', data: {design:true}, canActivate:[adminSectionLeaveGuard,technicalEntryGuard], canDeactivate:[adminLeaveGuard], loadComponent: () => import('./features/admin/admin-page').then(m=>m.AdminPage) },
  { path: 'admin', title: 'Administrador | RYA', canActivate:[adminGuard], canDeactivate:[adminLeaveGuard], loadComponent: () => import('./features/admin/admin-page').then(m=>m.AdminPage) },
  { path: 'admin/:section', title: 'Administrador | RYA', canActivate:[adminSectionLeaveGuard,technicalEntryGuard,adminGuard], canDeactivate:[adminLeaveGuard], loadComponent: () => import('./features/admin/admin-page').then(m=>m.AdminPage) },
  { path: '', title: 'RYA Tech', loadComponent: () => import('./features/home/home').then(m => m.Home) },
  { path: 'catalogo', title: 'Catálogo | RYA Tech', loadComponent: () => import('./features/catalog/catalog').then(m => m.Catalog) },
  { path: 'catalogo/:categoria', title: route => `${categoryPresentation(route.paramMap.get('categoria') ?? '')?.name ?? 'Categoría'} | RYA Tech`, loadComponent: () => import('./features/catalog/catalog').then(m => m.Catalog) },
  { path: 'producto/:slug', title: 'Producto | RYA Tech', loadComponent: () => import('./features/product-detail/product-detail').then(m => m.ProductDetailComponent) },
  { path: 'login', title: 'Iniciar sesión | RYA Tech', loadComponent: () => import('./features/auth/auth-page').then(m => m.AuthPage), data: { mode: 'login' } },
  { path: 'registro', title: 'Crear cuenta | RYA Tech', loadComponent: () => import('./features/auth/auth-page').then(m => m.AuthPage), data: { mode: 'register' } },
  { path: 'recuperar-clave', title: 'Recuperar contraseña | RYA Tech', loadComponent: () => import('./features/auth/auth-page').then(m => m.AuthPage), data: { mode: 'recover' } },
  { path: 'restablecer-clave', title: 'Nueva contraseña | RYA Tech', loadComponent: () => import('./features/auth/auth-page').then(m => m.AuthPage), data: { mode: 'reset' } },
  { path: 'cuenta', title: 'Mi cuenta | RYA Tech', canActivate: [authGuard], loadComponent: () => import('./features/account/account-page').then(m => m.AccountPage) },
  { path: 'ofertas', title: 'Ofertas | RYA Tech', loadComponent: () => import('./features/offers/offers').then(m => m.Offers) },
  { path: 'asesorias', title: 'Asesorías | RYA Tech', loadComponent: () => import('./features/advisor/advisor').then(m => m.Advisor) },
  { path: 'experiencias', title: 'Experiencias | RYA Tech', loadComponent: () => import('./features/experiences/experiences').then(m => m.Experiences) },
  { path: 'favoritos', title: 'Favoritos | RYA Tech', loadComponent: () => import('./features/favorites/favorites').then(m => m.Favorites) },
  { path: 'checkout', title: 'Finalizar compra | RYA Tech', loadComponent: () => import('./features/checkout/checkout').then(m => m.Checkout) },
  { path: 'compras/:id', title: 'Detalle del pedido | RYA Tech', canActivate: [authGuard], loadComponent: () => import('./features/purchases/purchases').then(m => m.Purchases) },
  { path: 'compras', title: 'Mis compras | RYA Tech', canActivate: [authGuard], loadComponent: () => import('./features/purchases/purchases').then(m => m.Purchases) },
  { path: 'ayuda', title: 'Centro de ayuda | RYA Tech', loadComponent: () => import('./features/help/help').then(m => m.Help) },
  { path: '**', redirectTo: '' },
];
