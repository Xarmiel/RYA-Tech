# Campos técnicos adicionales por categoría

## Activación

Aplicar `backend/db/manual/campos-ficha-personalizados.sql` en Supabase **antes de arrancar o desplegar este backend**. La consulta de metadatos necesita esta tabla. El script no crea categorías, campos ni valores de ejemplo. La tabla tiene RLS habilitado; las escrituras se hacen desde el backend administrativo.

## Dónde se administran

- Al crear/editar un producto: selecciona su categoría y abre **Añadir o gestionar campos técnicos**, debajo de su ficha.
- Sin crear un producto: **Marcas y categorías → Campos de ficha técnica**, selecciona una categoría y abre el administrador de campos.

Crear un campo guarda una definición compartida para esa categoría y sus subcategorías. No rellena productos. Después, en cada producto, introduce su valor real en el formulario técnico.

## Definición

- Nombre: hasta 100 caracteres; no duplica un campo visible de esa categoría o un ancestro.
- Tipo: Texto, Número o Sí/No/Desconocido.
- Unidad opcional para números: hasta 20 caracteres.
- Solo enteros: opción de los números. Si no se marca, se admiten hasta cuatro decimales. Los campos adicionales admiten números positivos o negativos; los campos predefinidos mantienen sus restricciones.
- Ejemplo opcional: hasta 120 caracteres. Debe coincidir con el tipo. Es una guía de formato, nunca un valor predeterminado.
- Hasta 30 campos propios por categoría; las subcategorías también muestran los heredados.

Ejemplo de uso: definir **Longitud del cable**, Número, unidad `m`, ejemplo `1.8`, en Audífonos. Cada audífono puede guardar una longitud distinta o dejarla desconocida.

## Conservación y relación con productos

- Cada definición tiene una clave estable generada por el servidor, con formato `extra_` más un identificador. No se eligen claves de los campos certificados ni se reemplazan los campos predefinidos.
- Los valores se guardan por producto en el sistema de atributos existente, vinculados a su ficha y a la categoría que admite la definición.
- Las definiciones de una categoría superior se editan desde esa categoría superior.
- Nombre y ejemplo se pueden editar con control de versión. Renombrar actualiza el nombre mostrado sin alterar valores.
- Tipo, unidad y condición de entero permanecen fijos: para otra interpretación se crea un campo distinto.
- Cambiar de categoría conserva los campos compartidos; los ajenos se descartan del formulario con confirmación y se archivan en la ficha al guardar, conservando los atributos heredados/certificados fuera de esta ficha comercial.
- Los valores aparecen en la ficha pública. Para información privada se usa el campo **Notas internas**, que sigue sin publicarse.
- No se agregan automáticamente reglas al asesor ni filtros comerciales: un campo adicional no certifica compatibilidad.

## Nuevas categorías e importación

Una categoría sin familia predefinida obtiene el tipo de ficha `custom` cuando se crea su primer campo. Una categoría como Audífonos conserva `headset` y añade sus campos extra. El importador y el editor consultan el mismo esquema.

Para importar valores, copia la **Clave para JSON** desde el administrador del campo y úsala dentro de `fichaTecnica.valores`. Usa el tipo de ficha de la categoría y valores JSON del tipo correcto; `null` o una clave omitida significan desconocido. Los ejemplos de las definiciones no se importan como datos del producto.

## API administrativa

- GET `/api/v1/admin/categorias/{categoriaId}/campos-ficha`: definiciones propias/heredadas y esquema completo.
- POST en la misma ruta: nombre, tipoDato (`text`, `number`, `boolean`), unidad, ejemplo, soloEnteros.
- PUT `.../{id}`: mismos datos y versión vigente; solo nombre y ejemplo se pueden modificar.

Todas las rutas requieren el rol administrador de `app_metadata`, validado por el servidor. Las claves ajenas a la categoría se rechazan al guardar productos o importar fichas.
