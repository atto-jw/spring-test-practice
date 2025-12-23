package com.codeit.library.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(unique = true)
    private String isbn;

    @Column(nullable = false)
    private Integer price;

    private LocalDate publishedDate;

    public Book(String title, String author, String isbn, Integer price) {
        validateTitle(title);
        validateAuthor(author);
        validatePrice(price);
        
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
    }

    public Book(Long id, String title, String author, String isbn, Integer price) {
        validateTitle(title);
        validateAuthor(author);
        validatePrice(price);

        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
    }

    public Book(String title, String author, String isbn, Integer price, LocalDate publishedDate) {
        this(title, author, isbn, price);
        this.publishedDate = publishedDate;
    }

    public void updateInfo(String title, Integer price) {
        if (title != null) {
            validateTitle(title);
            this.title = title;
        }
        if (price != null) {
            validatePrice(price);
            this.price = price;
        }
    }

    public void applyDiscount(int discountRate) {
        if (discountRate < 0 || discountRate > 50) {
            throw new IllegalArgumentException("할인율은 0~50% 사이여야 합니다");
        }
        this.price = this.price * (100 - discountRate) / 100;
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 필수입니다");
        }
    }

    private void validateAuthor(String author) {
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("저자는 필수입니다");
        }
    }

    private void validatePrice(Integer price) {
        if (price == null || price < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다");
        }
    }
}

