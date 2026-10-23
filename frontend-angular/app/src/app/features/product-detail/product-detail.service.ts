import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ProductDetail, ProductReviews } from './product-detail.models';

@Injectable({ providedIn: 'root' })
export class ProductDetailService {
  private readonly http = inject(HttpClient);

  getById(id: string): Observable<ProductDetail> {
    return this.getProduct(`/api/v1/productos/${encodeURIComponent(id)}`);
  }

  getBySlug(slug: string): Observable<ProductDetail> {
    return this.getProduct(`/api/v1/productos/slug/${encodeURIComponent(slug)}`);
  }

  private getProduct(url: string): Observable<ProductDetail> {
    return this.http.get<BackendProductDetail>(url).pipe(
      map(product => ({
        id: product.id,
        sku: product.sku,
        name: product.nombre,
        slug: product.slug,
        description: product.descripcion,
        price: product.precioBase,
        brand: product.marca,
        brandSlug: product.marcaSlug,
        category: product.categoria,
        categorySlug: product.categoriaSlug,
        type: product.tipo,
        availableStock: product.stockDisponible,
        shippable: product.requiereEnvio,
        images: [...product.imagenes]
          .sort((left, right) => Number(right.principal) - Number(left.principal) || left.posicion - right.posicion)
          .map(image => ({
            id: image.id,
            url: image.url,
            alt: image.textoAlternativo,
            position: image.posicion,
            primary: image.principal,
          })),
        attributes: product.atributos.map(attribute => ({
          code: attribute.codigo,
          name: attribute.nombre,
          dataType: attribute.tipoDato,
          unit: attribute.unidad,
          value: attribute.valor,
        })),
      })),
    );
  }

  getReviews(productId: string): Observable<ProductReviews> {
    return this.http.get<BackendReviews>(`/api/v1/productos/${productId}/resenas`).pipe(
      map(response => ({
        average: response.promedio,
        total: response.total,
        items: response.resenas.map(review => ({
          id: review.id,
          rating: review.calificacion,
          comment: review.comentario,
          author: review.autor,
          date: review.fecha,
        })),
      })),
    );
  }
}

interface BackendProductDetail {
  id: string;
  sku: string;
  nombre: string;
  slug: string;
  descripcion: string | null;
  precioBase: number;
  marca: string | null;
  marcaSlug: string | null;
  categoria: string;
  categoriaSlug: string;
  tipo: string | null;
  stockDisponible: number;
  requiereEnvio: boolean;
  imagenes: Array<{ id: string; url: string; textoAlternativo: string | null; posicion: number; principal: boolean }>;
  atributos: Array<{ codigo: string; nombre: string; tipoDato: string; unidad: string | null; valor: string }>;
}

interface BackendReviews {
  promedio: number;
  total: number;
  resenas: Array<{ id: string; calificacion: number; comentario: string | null; autor: string; fecha: string }>;
}
