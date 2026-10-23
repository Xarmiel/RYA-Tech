import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-pending-page',
  template: `
    <section class="page-shell page-heading">
      <p class="eyebrow">MIGRACIÓN FRONTEND</p>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
      <div class="empty-state">
        <span aria-hidden="true">◇</span>
        <h2>La interfaz está preparada</h2>
        <p>Esta sección mostrará información real cuando su servicio de API esté disponible. No se cargarán datos de demostración.</p>
      </div>
    </section>
  `,
})
export class PendingPage {
  private readonly route = inject(ActivatedRoute);
  protected readonly title = this.route.snapshot.data['title'] as string;
  protected readonly description = this.route.snapshot.data['description'] as string;
}
