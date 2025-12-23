package com.codeit.library.repository;

import com.codeit.library.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByAuthor(String author);

    List<Book> findByTitleContaining(String keyword);

    Optional<Book> findByIsbn(String isbn);

    List<Book> findByPublishedDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT b FROM Book b WHERE b.price > :minPrice AND b.publishedDate > :date ORDER BY b.price DESC")
    List<Book> findExpensiveRecentBooks(@Param("minPrice") Integer minPrice, @Param("date") LocalDate date);

    @Query("SELECT b FROM Book b WHERE " +
           "(:author IS NULL OR b.author = :author) AND " +
           "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR b.price <= :maxPrice)")
    List<Book> searchBooks(
        @Param("author") String author,
        @Param("minPrice") Integer minPrice,
        @Param("maxPrice") Integer maxPrice
    );
}

