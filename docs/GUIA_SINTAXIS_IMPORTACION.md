# Manual de Especificación y Sintaxis Estricta: Importación de Productos JSON

Este documento establece las reglas obligatorias de sintaxis, formato y restricciones de datos para la importación por lotes de productos en **RYA-Tech** mediante la opción **Importar productos JSON / CSV**.

---

## 1. Reglas Generales del Archivo

1. **Formato:** Archivo `.json` codificado en **UTF-8 sin BOM**.
2. **Tamaño máximo del archivo:** 2 MB.
3. **Cantidad de productos por lote:** Entre **1** y **100** productos.
4. **Estructura raíz:** Un array de objetos JSON:
   ```json
   [
     { /* Producto 1 */ },
     { /* Producto 2 */ }
   ]
   ```
   *(También se acepta el formato encapsulado `{ "productos": [ ... ] }`).*

---

## 2. Campos Principales del Producto

| Campo | Tipo | Requerido | Longitud / Restricción | Descripción y Reglas de Sintaxis |
| :--- | :---: | :---: | :--- | :--- |
| `nombre` | String | **Sí** | 1 a 220 caracteres | Nombre comercial completo (incluir marca, modelo y características clave). Debe ser único en el lote. |
| `sku` | String | **Sí** | 1 a 80 caracteres | Código SKU en mayúsculas sin espacios (usar guiones medios `-`). Debe ser único en todo el catálogo. |
| `slug` | String | Opcional | 1 a 120 caracteres | Identificador amigable para URL en minúsculas separado por guiones (ej. `amd-ryzen-7-7800x3d`). |
| `categoria` | String | **Sí** | Exacta según BD | Debe coincidir exactamente con una de las categorías registradas en el sistema. |
| `marca` | String | **Sí** | Exacta según BD | Debe coincidir exactamente con uno de los fabricantes registrados en el sistema. |
| `precio` | Number | **Sí** | $\ge 0.00$, máx. 2 decimales | Precio base de venta al público en Soles (S/). Usar punto decimal `.` (ej. `1849.00`). |
| `costoReferencia` | Number | Opcional | $\ge 0.00$, máx. 2 decimales | Costo de adquisición referencial en Soles (ej. `1580.00`). |
| `stockInicial` | Integer | **Sí** | Entero $\ge 0$ | Cantidad de unidades físicas disponibles para inventario inicial (ej. `12`). |
| `estado` | String | **Sí** | `"active"` o `"draft"` | Estado inicial de publicación: `"active"` (publicado en tienda) o `"draft"` (borrador administrativo). |
| `requiereSerie` | Boolean | **Sí** | `true` o `false` | `true`: requiere registro individual de número de serie por unidad al ingresar stock. |
| `requiereEnvio` | Boolean | **Sí** | `true` o `false` | `true`: producto físico que requiere despacho o entrega logística. |
| `descripcion` | String | Opcional | Texto libre | Descripción técnica, ventajas del producto y contenido del paquete. |
| `imagenes` | Array | Opcional | Máx. 8 URLs | Lista de URLs HTTPS públicas y válidas sin credenciales embebidas (la primera URL se asigna como principal). |
| `fichaTecnica` | Object | Opcional | Objeto estricto | Estructura de especificaciones técnicas según la categoría del producto. |

---

## 3. Catálogo de Categorías y Fabricantes Permitidos

### Categorías de Hardware Oficiales:
* `"Procesadores"` *(Esquema técnico: `cpu`)*
* `"Placas Madre"` *(Esquema técnico: `board`)*
* `"Memorias RAM"` *(Esquema técnico: `ram`)*
* `"Almacenamiento"` *(Esquema técnico: `storage`)*
* `"Tarjetas Gráficas"` *(Esquema técnico: `gpu`)*
* `"Fuentes de Poder"` *(Esquema técnico: `psu`)*
* `"Gabinetes"` *(Esquema técnico: `case`)*

### Marcas / Fabricantes Válidos en Base de Datos:
`AMD`, `Intel`, `ASUS`, `ASUS ROG`, `ASUS TUF`, `ASUS Prime`, `MSI`, `Gigabyte`, `Gigabyte AORUS`, `ASRock`, `Corsair`, `Kingston`, `Kingston FURY`, `G.Skill`, `TeamGroup`, `TeamGroup T-Force`, `Samsung`, `Western Digital`, `Western Digital WD_BLACK`, `Crucial`, `Seagate`, `NVIDIA`, `Seasonic`, `be quiet!`, `Cooler Master`, `NZXT`, `Lian Li`, `Fractal Design`, `Montech`, `Phanteks`.

---

## 4. Especificación Estricta de Fichas Técnicas por Categoría

La propiedad `fichaTecnica` debe respetar la estructura:
```json
"fichaTecnica": {
  "tipo": "<identificador_de_tipo>",
  "fuenteUrl": "https://enlace-oficial-del-fabricante.com/especificaciones",
  "valores": {
    /* campos técnicos específicos */
  }
}
```
* **Regla de vacíos:** Si un campo es desconocido, debe omitirse o colocarse en `null`. No usar `0` ni `false` para indicar dato desconocido.

---

### 4.1. Procesadores (`tipo: "cpu"`)

| Clave en `valores` | Tipo | Unidad | Restricción | Ejemplo de Sintaxis Estricta |
| :--- | :---: | :---: | :--- | :--- |
| `socket` | Text | - | Máx. 250 caracteres | `"AM5"`, `"AM4"`, `"LGA1700"`, `"LGA1200"` |
| `generacion` | Text | - | Máx. 250 caracteres | `"Ryzen 7000"`, `"Ryzen 5000"`, `"Intel 14"`, `"Intel 13"` |
| `cores` | Number | - | Entero positivo $> 0$ | `8`, `16`, `24` |
| `threads` | Number | - | Entero positivo $> 0$ | `16`, `24`, `32` |
| `base_mhz` | Number | MHz | Número positivo | `4200`, `3400`, `2500` |
| `boost_mhz` | Number | MHz | Número positivo | `5000`, `5600`, `4600` |
| `l3_mb` | Number | MB | Número positivo | `96`, `32`, `33`, `20` |
| `tdp_w` | Number | W | Número positivo | `120`, `105`, `65`, `125` |
| `base_w` | Number | W | Número positivo | `120`, `65`, `125` |
| `max_w` | Number | W | Número positivo | `162`, `142`, `253`, `148` |
| `power_notes` | Text | - | Máx. 250 caracteres | `"PPT máximo de 162W según especificación oficial"` |
| `igpu` | Boolean | - | `true` o `false` | `true` (con video), `false` (ej. serie F de Intel) |
| `memory_type` | Text | - | Máx. 250 caracteres | `"DDR5"`, `"DDR4"`, `"DDR4 / DDR5"` |
| `cooler_included` | Boolean | - | `true` o `false` | `true` (incluye disipador de fábrica), `false` |

---

### 4.2. Placas Madre (`tipo: "board"`)

| Clave en `valores` | Tipo | Unidad | Restricción | Ejemplo de Sintaxis Estricta |
| :--- | :---: | :---: | :--- | :--- |
| `socket` | Text | - | Máx. 250 caracteres | `"AM5"`, `"AM4"`, `"LGA1700"` |
| `chipset` | Text | - | Máx. 250 caracteres | `"AMD B650"`, `"Intel Z790"`, `"AMD B550"` |
| `form_factor` | Text | - | Máx. 250 caracteres | `"ATX"`, `"Micro-ATX"`, `"Mini-ITX"`, `"E-ATX"` |
| `memory_type` | Text | - | Máx. 250 caracteres | `"DDR5"`, `"DDR4"` |
| `memory_slots` | Number | - | Entero positivo $> 0$ | `4`, `2` |
| `memory_max_gb` | Number | GB | Número positivo | `192`, `128`, `64` |
| `power_connectors`| Text | - | Máx. 250 caracteres | `"1x 24-pin ATX, 2x 8-pin EPS 12V"` |
| `pcie` | Text | - | Máx. 250 caracteres | `"1x PCIe 5.0 x16, 1x PCIe 4.0 x16 (x4)"` |
| `storage_interfaces` | Text | - | Máx. 250 caracteres | `"3x M.2 (1x PCIe 5.0, 2x PCIe 4.0), 4x SATA 6Gb/s"` |
| `m2_pcie_version`| Text | - | Máx. 250 caracteres | `"PCIe 5.0 x4 / PCIe 4.0 x4"`, `"PCIe 4.0 x4"` |
| `revision` | Text | - | Máx. 250 caracteres | `"Rev 1.0"`, `"Rev 1.2"` |
| `estimated_w` | Number | W | Número positivo | `50`, `55`, `60` |
| `m2_slots` | Number | - | Entero $\ge 0$ | `3`, `4`, `2` |
| `sata_ports` | Number | - | Entero $\ge 0$ | `4`, `6` |
| `wifi` | Boolean | - | `true` o `false` | `true`, `false` |
| `bluetooth` | Boolean | - | `true` o `false` | `true`, `false` |
| `ethernet_mbps` | Number | Mbps | Número positivo | `2500` (2.5G), `1000` (Gigabit) |
| `memory_oc_mts` | Number | MT/s | Número positivo | `7600`, `7200`, `6000` |

---

### 4.3. Memorias RAM (`tipo: "ram"`)

| Clave en `valores` | Tipo | Unidad | Restricción | Ejemplo de Sintaxis Estricta |
| :--- | :---: | :---: | :--- | :--- |
| `memory_type` | Text | - | Máx. 250 caracteres | `"DDR5"`, `"DDR4"` |
| `form_factor` | Text | - | Máx. 250 caracteres | `"DIMM 288-pin"`, `"SO-DIMM"` |
| `capacity_gb` | Number | GB | Capacidad total del kit | `32`, `16`, `64` |
| `modules` | Number | - | Entero positivo $> 0$ | `2` (kit 2x), `1` (single rank) |
| `speed_mts` | Number | MT/s | Frecuencia de prueba | `6000`, `5600`, `3200` |
| `spd_mts` | Number | MT/s | Frecuencia base JEDEC | `4800`, `2400` |
| `cas` | Number | - | Entero positivo $> 0$ | `30`, `32`, `36`, `16` |
| `voltage_v` | Number | V | Máx. 4 decimales | `1.35`, `1.40`, `1.20` |
| `profiles` | Text | - | Máx. 250 caracteres | `"AMD EXPO / Intel XMP 3.0"`, `"Intel XMP 2.0"` |
| `height_mm` | Number | mm | Altura del disipador | `44.0`, `34.0`, `42.2` |
| `power_w` | Number | W | Consumo publicado | `6.0`, `4.5` |

---

### 4.4. Almacenamiento (`tipo: "storage"`)

| Clave en `valores` | Tipo | Unidad | Restricción | Ejemplo de Sintaxis Estricta |
| :--- | :---: | :---: | :--- | :--- |
| `drive_type` | Text | - | `"SSD"` o `"HDD"` | `"SSD"`, `"HDD"` |
| `capacity_gb` | Number | GB | Capacidad en GB | `1000` (1TB), `2000` (2TB), `500` (500GB) |
| `interface` | Text | - | Máx. 250 caracteres | `"PCIe 4.0 x4, NVMe 2.0"`, `"SATA III 6Gb/s"` |
| `form_factor` | Text | - | Máx. 250 caracteres | `"M.2 2280"`, `"3.5 pulgadas"`, `"2.5 pulgadas"` |
| `m2_key` | Text | - | Clave ranura M.2 | `"M Key"`, `"B+M Key"`, `"N/A"` |
| `read_mbs` | Number | MB/s | Velocidad secuencial | `7450`, `7300`, `5000`, `220` |
| `write_mbs` | Number | MB/s | Velocidad secuencial | `6900`, `6600`, `4200`, `190` |
| `power_w` | Number | W | Consumo publicado | `5.5`, `6.2`, `4.5` |

---

### 4.5. Tarjetas Gráficas (`tipo: "gpu"`)

| Clave en `valores` | Tipo | Unidad | Restricción | Ejemplo de Sintaxis Estricta |
| :--- | :---: | :---: | :--- | :--- |
| `interface` | Text | - | Máx. 250 caracteres | `"PCIe 4.0 x16"`, `"PCIe 4.0 x8"` |
| `vram_gb` | Number | GB | Cantidad de VRAM | `16`, `12`, `8` |
| `memory_type` | Text | - | Tipo de VRAM | `"GDDR6X"`, `"GDDR6"` |
| `length_mm` | Number | mm | Longitud física | `305.0`, `272.0`, `199.0` |
| `height_mm` | Number | mm | Altura física | `138.0`, `115.0`, `120.0` |
| `thickness_mm` | Number | mm | Grosor físico | `65.0`, `40.0`, `56.0` |
| `slots` | Number | - | Ranuras ocupadas | `3.25`, `2.0`, `2.8`, `3.5` |
| `board_w` | Number | W | Potencia TGP / TBP | `285`, `263`, `160`, `115` |
| `power_notes` | Text | - | Máx. 250 caracteres | `"TGP oficial de 285W con carga completa"` |
| `recommended_psu_w`| Number | W | Fuente sugerida | `750`, `700`, `650`, `550` |
| `power_connectors` | Text | - | Conectores PCI-e | `"1x 16-pin 12VHPWR"`, `"2x 8-pin PCIe"` |
| `video_outputs` | Text | - | Puertos de salida | `"2x HDMI 2.1a, 3x DisplayPort 1.4a"` |

---

### 4.6. Fuentes de Poder (`tipo: "psu"`)

| Clave en `valores` | Tipo | Unidad | Restricción | Ejemplo de Sintaxis Estricta |
| :--- | :---: | :---: | :--- | :--- |
| `continuous_w` | Number | W | Salida continua | `750`, `850`, `650`, `1000` |
| `form_factor` | Text | - | Formato de fuente | `"ATX"`, `"SFX"`, `"SFX-L"` |
| `atx_version` | Text | - | Estándar de diseño | `"ATX 3.0 / PCIe 5.0"`, `"ATX 2.52"` |
| `efficiency` | Text | - | Certificación | `"80 Plus Gold"`, `"80 Plus Bronze"`, `"80 Plus Platinum"` |
| `modularity` | Text | - | Tipo de cableado | `"Full Modular"`, `"Semi Modular"`, `"No Modular"` |
| `connectors` | Text | - | Resumen conectores | `"1x 24-pin ATX, 2x 8-pin EPS, 1x 16-pin 12VHPWR, 4x 8-pin PCIe, 8x SATA"` |
| `length_mm` | Number | mm | Profundidad de fuente | `140`, `160`, `180` |

---

### 4.7. Gabinetes (`tipo: "case"`)

| Clave en `valores` | Tipo | Unidad | Restricción | Ejemplo de Sintaxis Estricta |
| :--- | :---: | :---: | :--- | :--- |
| `form_factor` | Text | - | Placas compatibles | `"ATX, Micro-ATX, Mini-ITX"`, `"E-ATX, ATX, Micro-ATX"` |
| `height_mm` | Number | mm | Altura del gabinete | `464.0`, `491.0`, `498.0` |
| `length_mm` | Number | mm | Longitud / Profundidad| `446.0`, `482.0`, `478.0` |
| `width_mm` | Number | mm | Ancho frontal | `227.0`, `235.0`, `230.0` |
| `psu_max_length_mm` | Number | mm | Profundidad máx. PSU | `185`, `220`, `240` |
| `gpu_max_length_mm` | Number | mm | Longitud máx. GPU | `365`, `392`, `400` |
| `cooler_max_height_mm` | Number | mm | Altura máx. Disipador| `165`, `180`, `170` |
| `max_fans` | Number | - | Entero positivo $> 0$ | `6`, `8`, `9` |

---

## 5. Validación y Consejos de Seguridad

1. **Evitar duplicados en el mismo archivo:** Si dos productos tienen el mismo `sku`, `nombre` o `slug`, el importador rechazará el lote completo.
2. **URLs de imágenes:** Solo enlaces `https://` públicos que carguen directamente la imagen. No incluir credenciales ni URLs con parámetros de sesión.
3. **Plantilla de referencia:** Puedes consultar y utilizar directamente el archivo de ejemplo listo para duplicar en:
   [`plantilla_importacion_productos.json`](./plantilla_importacion_productos.json).
