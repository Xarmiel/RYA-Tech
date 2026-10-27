# Importación de productos y fichas técnicas

El importador admite JSON y CSV. Las plantillas son ejemplos de estructura: verifica nombres, SKU, marca/categoría existentes, fuente, medidas e imágenes antes de importar. No se crean categorías ni marcas por parecido de nombre.

## Ficha técnica

```json
{
  "tipo": "headset",
  "fuenteUrl": null,
  "valores": {
    "design": "Abierto",
    "impedance_ohm": null,
    "microphone": false,
    "sensitivity": null
  }
}
```

- La categoría concreta determina el formulario. `Periféricos` y `Cámaras e iluminación` son categorías mixtas; usa Audífonos, Cámaras web o Iluminación, o una subcategoría que herede de ellas.
- Todos los campos son opcionales. Omitir una clave o usar `null` significa desconocido; `false` y `0` son valores explícitos, no sustitutos de un dato faltante.
- Los números JSON usan punto decimal y no incluyen la unidad en el valor. Los conteos deben ser enteros y respetar el mínimo del campo; botones, ejes y ventiladores permiten cero cuando el fabricante declara su ausencia.
- Sensibilidad se guarda como texto con su unidad y referencia; puede incluir valores negativos como dBV/Pa. No se convierten automáticamente referencias diferentes.
- Las fuentes son enlaces HTTPS sin credenciales de hasta 250 caracteres. Los textos técnicos admiten hasta 250 caracteres.
- Frecuencia mínima/máxima, RPM y temperatura de color deben respetar el orden del intervalo cuando ambos extremos se conocen.
- `internal_notes` se conserva solo en administración. La ficha comercial se guarda pendiente de revisión; no certifica por sí misma compatibilidad CPU–placa.

## Familias admitidas

| Categoría | tipo |
|---|---|
| Procesadores | cpu |
| Placas Madre | board |
| Memorias RAM | ram |
| Almacenamiento | storage |
| Tarjetas Gráficas | gpu |
| Fuentes de Poder | psu |
| Gabinetes | case |
| Teclados | keyboard |
| Mouse | mouse |
| Audífonos | headset |
| Micrófonos | microphone |
| Cámaras web | webcam |
| Mousepads | mousepad |
| Monitores | monitor |
| Volantes | racing |
| Simulación | simulation |
| Refrigeración | cooler |
| Iluminación | lighting |

`plantilla_fichas_tecnicas_categorias.json` contiene los campos de las nueve familias añadidas con valores desconocidos. Sus nombres, SKU y marca son marcadores que debes reemplazar. No importes los ejemplos como productos reales.

## Acciones por SKU exacto

- `crear`: crea un producto nuevo, con revisión previa y sin sobrescribir duplicados.
- `actualizar-ficha`: reemplaza la ficha comercial existente; conserva precio, stock, publicación e imágenes. Si necesitas cambiar solo ciertas claves, incluye `camposFicha` con exactamente las claves de `fichaTecnica.valores`.
- `agregar-imagenes`: conserva las actuales y agrega enlaces no repetidos, hasta ocho en total. No modifica la ficha.

Para CSV, el objeto fichaTecnica se utiliza en JSON; no inventes columnas técnicas ajenas al formato de la aplicación. Para fichas estructuradas utiliza las plantillas JSON y la vista previa del importador.

## Campos añadidos desde el panel

Las categorías pueden incorporar campos propios o heredados. Consulta [CAMPOS-FICHA-PERSONALIZADOS.md](CAMPOS-FICHA-PERSONALIZADOS.md). Copia la clave estable del campo desde el panel y úsala en `fichaTecnica.valores`; las nuevas categorías sin familia predefinida usan `tipo: "custom"`. Los ejemplos no son valores predeterminados.
