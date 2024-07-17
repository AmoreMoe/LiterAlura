package com.alura.Literalura.principal;

import com.alura.Literalura.model.Autor;
import com.alura.Literalura.model.Datos;
import com.alura.Literalura.model.DatosLibro;
import com.alura.Literalura.model.Idioma;
import com.alura.Literalura.model.Libro;
import com.alura.Literalura.repo.AutorRepository;
import com.alura.Literalura.repo.LibroRepository;
import com.alura.Literalura.servicios.ConsumoAPI;
import com.alura.Literalura.servicios.ConvierteDatos;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Principal{

    private Scanner lectura = new Scanner(System.in);
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private final String URL_BASE = "https://gutendex.com/books/";
    private ConvierteDatos conversor = new ConvierteDatos();
    private Set<Libro> librosEncontrados = new HashSet<>();
    private LibroRepository libroRepository;
    private AutorRepository autorRepository;
    private Set<Libro> libros;

    @Autowired
    public Principal(LibroRepository libroRepository, AutorRepository autorRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    **************************************
                    1 - Buscar un libro por título
                    2 - Mostrar una lista de libros registrados
                    3 - Mostrar una lista de autores registrados
                    4 - Mostrar una lista de autores vivos en un determinado año
                    5 - Mostrar una lista de libros por idioma
                    0 - Salir
                    ***************************************
                    """;
            System.out.println(menu);
            opcion = lectura.nextInt();
            lectura.nextLine();

            switch (opcion) {
                case 1:
                    buscarLibroPorTitulo();
                    break;
                case 2:
                    listarLibros();
                    break;
                case 3:
                    listarAutores();
                    break;
                case 4:
                    autorAnoConcreto();
                    break;
                case 5:
                    librosPorIdioma();
                    break;
                case 0:
                    System.out.println("Gracias por ulilizar LiterAlura...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Opción no válida");
            }
        }
    }

    private DatosLibro getDatosLibro() {
        System.out.println("Escribe el nombre del libro que quieres buscar");
        var nombreLibro = lectura.nextLine();
        var json = consumoAPI.obtenerDatos(URL_BASE + "?search=" + nombreLibro.replace(" ", "+"));
        Datos datos = conversor.obtenerDatos(json, Datos.class);

        if (datos.resultados().isEmpty()) {
            throw new RuntimeException("No se han encontraron libros para el título que ingresaste.");
        }

        DatosLibro datosLibro = datos.resultados().get(0); // obtener el primer libro de los resultados
        return new DatosLibro(
                datosLibro.id(),
                datosLibro.titulo(),
                datosLibro.autores(),
                datosLibro.numeroDeDescargas(),
                datosLibro.lenguajes()
        );
    }

    private void buscarLibroPorTitulo() {
        DatosLibro datos = getDatosLibro();
        Set<Autor> autores = datos.autores().stream()
                .map(datoAutor -> new Autor(datoAutor.nombre(), datoAutor.fechaNacimiento(), datoAutor.fechaFallecimiento()))
                .collect(Collectors.toSet());
        Libro libro = new Libro(datos.titulo(), autores, datos.numeroDeDescargas(), new HashSet<>(datos.lenguajes()));
        libroRepository.save(libro);
        librosEncontrados.add(libro);
        System.out.println(libro.toString());
    }

    @Transactional(readOnly = true)
    private void listarLibros() {
        libros = new HashSet<>(libroRepository.findAll());

        // Inicializa las colecciones de cada libro
        libros.forEach(libro -> {
            libroRepository.findByIdWithAutoresAndLenguajes(libro.getId()).ifPresent(libroConAutores -> {
                libro.setAutores(libroConAutores.getAutores());
                libro.setLenguajes(libroConAutores.getLenguajes());
            });
        });

        // Comparador para ordenar libros por el nombre del primer autor
        Comparator<Libro> comparador = Comparator.comparing(libro ->
                libro.getAutores().isEmpty() ? "" : libro.getAutores().iterator().next().getNombreAutor()
        );

        libros.stream()
                .sorted(comparador)
                .forEach(System.out::println);
    }


    @Transactional(readOnly = true)
    private void listarAutores() {
        List<Autor> autores = autorRepository.findAllWithLibros();
        if (autores.isEmpty()) {
            System.out.println("No hay autores registrados");
        } else {
            autores.forEach(autor -> {
                System.out.println("Nombre del autor: " + autor.getNombreAutor());
                System.out.println("Fecha de Nacimiento: " + autor.getFechaDeNacimiento());
                System.out.println("Fecha de Fallecimiento: " + (autor.getFechaDeFallecimiento() != null ? autor.getFechaDeFallecimiento() : "N/A"));
                if (autor.getLibro() != null) {
                    System.out.println("Nombre del libro: " + autor.getLibro().getTitulo());
                } else {
                    System.out.println("Libro: N/A");
                }
                System.out.println();
            });
        }
    }

    @Transactional(readOnly = true)
    private void autorAnoConcreto() {
        System.out.println("Ingresa el año que desees buscar:");
        var anio = lectura.nextInt();
        lectura.nextLine();
        List<Autor> autors = autorRepository.findByFechaDeNacimientoLessThanEqualAndFechaDeFallecimientoGreaterThanEqual(anio);
        if (autors.isEmpty()) {
            System.out.println("No se pudo encontrar a un autor que haya vivido en el año:  " + anio);
        } else {
            autors.forEach(autor -> {
                System.out.println("Nombre: " + autor.getNombreAutor());
                System.out.println("Fecha de Nacimiento: " + autor.getFechaDeNacimiento());
                System.out.println("Fecha de Fallecimiento: " + (autor.getFechaDeFallecimiento() != null ? autor.getFechaDeFallecimiento() : "N/A"));
                System.out.println("Libro: " + (autor.getLibro() != null ? autor.getLibro().getTitulo() : "N/A"));
                System.out.println();
            });
        }
    }

    @Transactional(readOnly = true)
    private void librosPorIdioma() {
        System.out.println("Por favor ingrese el idioma en el cual desea buscar los libros (es, en, fr, pt, etc.):");
        var idiomaInput = lectura.nextLine().toLowerCase();

        try {
            Idioma idioma = Idioma.fromString(idiomaInput);
            List<Libro> libros = libroRepository.findByIdioma(idioma);

            if (libros.isEmpty()) {
                System.out.println("No se encontraron libros en el idioma: " + idiomaInput);
            } else {
                libros.forEach(libro -> {
                    Hibernate.initialize(libro.getAutores());
                    Hibernate.initialize(libro.getLenguajes());

                    System.out.println(
                            "------ LIBRO ------" +
                                    "\nTitulo: " + libro.getTitulo() +
                                    "\nAutores: " + libro.getAutores().stream().map(Autor::getNombreAutor).collect(Collectors.joining(", ")) +
                                    "\nIdiomas: " + libro.getLenguajes().stream().map(Idioma::getDescripcion).collect(Collectors.joining(", ")) +
                                    "\nNúmero de descargas: " + libro.getNumeroDeDescargas() +
                                    "\n------------------"
                    );
                });
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Idioma no reconocido: " + idiomaInput);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Principal.class, args);
    }
}

