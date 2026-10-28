import {EditorialContent} from '../../shared/editorial-content/editorial-content';
import { DOCUMENT } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-experiences',
  imports: [RouterLink, EditorialContent],
  templateUrl: './experiences.html',
  styleUrl: './experiences.scss',
})
export class Experiences {
  private readonly document = inject(DOCUMENT);
  protected readonly profiles = [
    {
      id: 'jugar', label: 'Para jugar', word: 'PLAY', number: '01', icon: 'sports_esports',
      title: 'Tu partida. Tu manera de vivirla.',
      description: 'Una partida con amigos, una historia que quieres explorar o una sesión competitiva. Empieza por lo que disfrutas y por el espacio en el que vas a jugar.',
      priorities: [
        { title: 'Piensa en tu pantalla', text: 'Define la resolución y el tipo de juegos que te interesan antes de elegir las piezas.' },
        { title: 'Mira el setup completo', text: 'Incluye monitor, audio y periféricos en tu presupuesto. También forman parte de cada sesión.' },
        { title: 'Elige con perspectiva', text: 'Anota qué necesitas hoy y qué te gustaría ampliar después para orientar tu configuración.' },
      ],
      prompt: '¿Cómo sería una sesión ideal para ti?',
    },
    {
      id: 'crear', label: 'Para crear', word: 'CREATE', number: '02', icon: 'movie_edit',
      title: 'Dale espacio a tu próxima idea.',
      description: 'Editar un video, diseñar una escena o preparar una transmisión son experiencias distintas. Tu punto de partida está en tus herramientas, tus proyectos y tu forma de trabajar.',
      priorities: [
        { title: 'Empieza por tus herramientas', text: 'Haz una lista del software que utilizas y revisa sus requisitos para tu tipo de proyecto.' },
        { title: 'Sigue el recorrido de tus archivos', text: 'Piensa dónde guardarás el material, cómo lo organizarás y qué necesitas para respaldarlo.' },
        { title: 'Cuenta con tu espacio de creación', text: 'Considera las pantallas, el audio y las conexiones que utilizas a lo largo del día.' },
      ],
      prompt: '¿Qué proyecto te gustaría empezar?',
    },
    {
      id: 'trabajar', label: 'Para trabajar', word: 'SCALE', number: '03', icon: 'workspaces',
      title: 'Un espacio que siga tu ritmo.',
      description: 'Tu jornada puede alternar entre documentos, reuniones, programación y tareas de equipo. Identifica esa rutina para construir un espacio que tenga sentido para ti.',
      priorities: [
        { title: 'Describe una jornada real', text: 'Enumera las aplicaciones, reuniones y tareas que sueles realizar al mismo tiempo.' },
        { title: 'Revisa tus conexiones', text: 'Considera tus monitores, periféricos y conexión de red al pensar en tu equipo.' },
        { title: 'Deja sitio para el siguiente paso', text: 'Incluye tus necesidades de mantenimiento y ampliación cuando organices el presupuesto.' },
      ],
      prompt: '¿Qué necesitas de tu espacio de trabajo?',
    },
  ];
  protected readonly selectedIndex = signal(0);
  protected readonly selected = computed(() => this.profiles[this.selectedIndex()]);

  protected select(index: number): void { this.selectedIndex.set(index); }

  protected tabKey(event: KeyboardEvent, index: number): void {
    let next = index;
    if (event.key === 'ArrowRight') next = (index + 1) % this.profiles.length;
    else if (event.key === 'ArrowLeft') next = (index + this.profiles.length - 1) % this.profiles.length;
    else if (event.key === 'Home') next = 0;
    else if (event.key === 'End') next = this.profiles.length - 1;
    else return;
    event.preventDefault();
    this.select(next);
    this.document.getElementById(`experience-tab-${this.profiles[next].id}`)?.focus();
  }

  protected explore(): void {
    this.document.getElementById('explora-experiencias')?.scrollIntoView({ block: 'start' });
    this.document.getElementById(`experience-tab-${this.selected().id}`)?.focus({ preventScroll: true });
  }
}
