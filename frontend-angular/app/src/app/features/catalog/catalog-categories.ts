export interface CategoryBubble {
  id: string;
  name: string;
  icon: string;
  imageUrl?: string;
  slug: string;
  aliases: string[];
  description: string;
}

export const CATALOG_CATEGORIES: CategoryBubble[] = [
  {id:'mouse', name:'Mouse', icon:'mouse', slug:'mouse-gaming', aliases:['mouse','mouses','ratones','mouse-para-gaming'], description:'Explora mouse para gaming, trabajo y uso diario. Elige precisión y comodidad para tu espacio.'},
  {id:'mousepads', name:'Mousepads para gaming', icon:'crop_landscape', slug:'mousepads-gaming', aliases:['mousepads','mousepad','mouse-pads','mouse-pad','alfombrillas','mousepads-para-gaming'], description:'Encuentra la superficie para tu setup: espacio, control y deslizamiento para cada movimiento.'},
  {id:'teclados', name:'Teclados para gaming', icon:'keyboard', slug:'teclados-gaming', aliases:['teclados','teclado','teclados-para-gaming'], description:'Cada tecla cuenta. Descubre teclados para jugar, crear y trabajar a tu ritmo.'},
  {id:'audio', name:'Audio y audífonos con micrófono para juegos', icon:'headphones', slug:'audio-audifonos', aliases:['audifonos','audifonos-con-microfono','auriculares','headsets','audio'], description:'Escucha cada detalle y comunícate con tu equipo. Explora las opciones de audio disponibles.'},
  {id:'volantes', name:'Volantes de carreras', icon:'sports_motorsports', slug:'volantes-carreras', aliases:['volantes','volantes-de-carreras'], description:'Acerca la pista a tu escritorio con los volantes disponibles para tu próxima carrera.'},
  {id:'microfonos', name:'Micrófonos', icon:'mic', slug:'microfonos', aliases:['microfono'], description:'Dale voz a tus partidas, llamadas y proyectos. Encuentra tu próximo micrófono.'},
  {id:'simulacion', name:'Simulación', icon:'sports_esports', slug:'simulacion', aliases:['simuladores','simulacion-de-vuelo','controles-de-simulacion'], description:'Explora controles y accesorios para construir una experiencia de simulación a tu medida.'},
  {id:'camaras', name:'Cámaras e iluminación', icon:'videocam', slug:'camaras-iluminacion', aliases:['camaras-e-iluminacion','camaras-web','camaras','webcams'], description:'Mejora cómo te ven en tus transmisiones y videollamadas. Descubre los productos disponibles para tu espacio.'},
];

export const categoryKey = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]/g,'');
export const categoryPresentation = (slug: string) => CATALOG_CATEGORIES.find(category => [category.slug,...category.aliases].some(alias => categoryKey(alias) === categoryKey(slug)));
