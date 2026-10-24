package com.ecommerce.backend.service;

import com.ecommerce.backend.entity.Categoria;
import com.ecommerce.backend.entity.Marca;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.CategoriaRepository;
import com.ecommerce.backend.repository.MarcaRepository;
import com.ecommerce.backend.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminReferenceServiceTest {
    CategoriaRepository categorias; MarcaRepository marcas; ProductoRepository productos; AdminReferenceService service;
    UUID id; Marca marca; Categoria categoria;
    @BeforeEach void setup() {
        categorias=mock(CategoriaRepository.class);marcas=mock(MarcaRepository.class);productos=mock(ProductoRepository.class);
        service=new AdminReferenceService(categorias,marcas,productos);id=UUID.randomUUID();
        marca=Marca.builder().id(id).nombre("Corsair").slug("corsair").build();
        categoria=Categoria.builder().id(id).nombre("Memoria RAM").slug("memoria-ram").build();
    }
    @Test void eliminarMarcaEsLogicoYNoBorraProductos() {
        when(marcas.findById(id)).thenReturn(Optional.of(marca));service.eliminarMarca(id);
        assertTrue(marca.getIsDeleted());verify(marcas).save(marca);verify(productos).existsByMarca_Id(id);verify(marcas,never()).delete(any());
    }
    @Test void bloquearMarcaConProductosIncluidosArchivados() {
        when(marcas.findById(id)).thenReturn(Optional.of(marca));when(productos.existsByMarca_Id(id)).thenReturn(true);
        assertThrows(BadRequestException.class,()->service.eliminarMarca(id));assertFalse(marca.getIsDeleted());verify(marcas,never()).save(any());
    }
    @Test void bloquearCategoriaConProductos() {
        when(categorias.findById(id)).thenReturn(Optional.of(categoria));when(productos.existsByCategoria_Id(id)).thenReturn(true);
        assertThrows(BadRequestException.class,()->service.eliminarCategoria(id));verify(categorias,never()).save(any());
    }
    @Test void bloquearCategoriaConHijosActivos() {
        when(categorias.findById(id)).thenReturn(Optional.of(categoria));when(categorias.existsByParentIdAndIsDeletedFalse(id)).thenReturn(true);
        assertThrows(BadRequestException.class,()->service.eliminarCategoria(id));assertFalse(categoria.getIsDeleted());
    }
    @Test void eliminarCategoriaVaciaEsRecuperable() {
        when(categorias.findById(id)).thenReturn(Optional.of(categoria));service.eliminarCategoria(id);assertTrue(categoria.getIsDeleted());verify(categorias).save(categoria);verify(categorias,never()).delete(any());
    }
    @Test void repetirBorradoNoHaceOtroCambio() {
        marca.setIsDeleted(true);when(marcas.findById(id)).thenReturn(Optional.of(marca));service.eliminarMarca(id);verify(marcas,never()).save(any());verifyNoInteractions(productos);
    }
    @Test void referenciaInexistenteDevuelveNoEncontrado() { assertThrows(ResourceNotFoundException.class,()->service.eliminarMarca(id));assertThrows(ResourceNotFoundException.class,()->service.eliminarCategoria(id)); }
    @Test void restaurarMarca() {
        marca.setIsDeleted(true);when(marcas.findById(id)).thenReturn(Optional.of(marca));service.restaurarMarca(id);assertFalse(marca.getIsDeleted());verify(marcas).save(marca);
    }
    @Test void restaurarCategoriaSoloSiPadreEstaActivo() {
        categoria.setIsDeleted(true);categoria.setParentId(UUID.randomUUID());when(categorias.findById(id)).thenReturn(Optional.of(categoria));
        assertThrows(BadRequestException.class,()->service.restaurarCategoria(id));assertTrue(categoria.getIsDeleted());
    }
    @Test void restaurarCategoriaSinPadre() {
        categoria.setIsDeleted(true);when(categorias.findById(id)).thenReturn(Optional.of(categoria));service.restaurarCategoria(id);assertFalse(categoria.getIsDeleted());
    }
    @Test void restaurarConConflictoSeBloquea() {
        marca.setIsDeleted(true);when(marcas.findById(id)).thenReturn(Optional.of(marca));when(marcas.existsByNombreIgnoreCaseAndIsDeletedFalse("Corsair")).thenReturn(true);
        assertThrows(BadRequestException.class,()->service.restaurarMarca(id));assertTrue(marca.getIsDeleted());
    }
    @Test void listadoOcultaEliminadosSalvoSolicitudExplicita() {
        marca.setIsDeleted(true);when(marcas.findAll()).thenReturn(List.of(marca));when(categorias.findAll()).thenReturn(List.of(categoria));
        assertEquals(0,service.listar(false).marcas().size());assertEquals(1,service.listar(true).marcas().size());assertEquals(1,service.listar(false).categorias().size());
    }
}
