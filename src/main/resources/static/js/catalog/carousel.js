function inicializarCarruseles() {
  const seccionesCarrusel = document.querySelectorAll('.carousel-section');

  seccionesCarrusel.forEach(seccion => {
    const track = seccion.querySelector('.carousel-track');
    const btnPrev = seccion.querySelector('.btn-prev');
    const btnNext = seccion.querySelector('.btn-next');

    if (track && btnPrev && btnNext) {
      if (track.dataset.inicializado === "true") return; 
      track.dataset.inicializado = "true";

      const getScrollAmount = () => {
        const item = track.querySelector('.carousel-item');
        if (!item) return 280;
        const style = window.getComputedStyle(track);
        const gap = parseFloat(style.gap) || 24; 
        return item.offsetWidth + gap; 
      };

      btnNext.addEventListener('click', () => {
        track.scrollLeft += getScrollAmount();
      });

      btnPrev.addEventListener('click', () => {
        track.scrollLeft -= getScrollAmount();
      });
    }
  });
}

if (typeof window !== 'undefined') {
  window.inicializarCarruseles = inicializarCarruseles;
}