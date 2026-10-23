import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-request-error',
  template: `<div class="empty-state error-state" role="alert"><span class="material-symbols-rounded" aria-hidden="true">cloud_off</span><h2>{{ title() }}</h2><p>{{ message() }}</p><button class="button button-secondary" type="button" (click)="retry.emit()">Reintentar</button></div>`,
  styles: `:host{display:block;min-width:0}.empty-state{overflow-wrap:anywhere}h2{font-size:clamp(1.3rem,4vw,2rem)}`,
})
export class RequestError {
  readonly title = input('No pudimos cargar la información');
  readonly message = input('Comprueba tu conexión e inténtalo nuevamente.');
  readonly retry = output<void>();
}
