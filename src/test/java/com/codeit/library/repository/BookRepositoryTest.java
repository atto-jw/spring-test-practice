package com.codeit.library.repository;

import com.codeit.library.domain.Book;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest // JPA 관련 컴포넌트만 로딩
@DisplayName("도서 Repository 테스트")
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Nested
    @DisplayName("검색 관련 기능")
    class search{

        @Test
        @DisplayName("저자 이름으로 책을 검색할 수 있다.")
        void findByAuthor() {
            // given
            bookRepository.save(new Book("클린 코드", "로버트 마틴", "123", 30000));
            bookRepository.save(new Book("클린 아키텍처", "로버트 마틴", "321", 32000));
            bookRepository.save(new Book("리팩토링", "마틴 파울러", "958", 35000));

            // when
            List<Book> books = bookRepository.findByAuthor("로버트 마틴");

            // then
            assertThat(books).hasSize(2);
            assertThat(books)
                    .extracting("title")
                    .containsExactlyInAnyOrder("클린 코드", "클린 아키텍처");
        } // 데이터베이스와 직접적으로 연관하지 않고 이 코드안에서만 진행하기 때문에 테스코드 자체는 h2기반으로 실행하기 때문에
        // 데이터를 집어넣고, 삭제하고 로직이기 때문에 data.sql이 없어도 성공할 수 있다. 특정 코드 의존하는것을 안좋다.
        // given 에서 직접 데이터를 넣는 것으로 권장, gradlew build가 테스트코드도 테스트빌드함
    }

    @Test
    @DisplayName("ISBN으로 책을 검색할 수 있다.")
    void findByIsbn() {
        // given
        bookRepository.save(new Book("클린 코드", "로버트 마틴", "123-0123456789", 30000));
        bookRepository.save(new Book("클린 아키텍처", "로버트 마틴", "321", 32000));
        bookRepository.save(new Book("리팩토링", "마틴 파울러", "958", 35000));

        // when
        Optional<Book> book = bookRepository.findByIsbn("123-0123456789");

        // then
        assertThat(book).isPresent(); //book이 존재하느냐
        assertThat(book.get().getTitle()).isEqualTo("클린 코드");
    }

    @Nested
    @DisplayName("직접 작성한 JPQL 검증")
    class ManualQuery {

        @Test
        @DisplayName("가격이 높고 최근에 출판된 책을 조회한다.")
        void findExpensive_RecentBooks() {
            // given
            LocalDate cutoffDate = LocalDate.of(2020, 1, 1);

            Book oldExpensiveBook = new Book("오래된 비싼 책", "저자1", "111", 60000,
                    LocalDate.of(2019, 1, 1));
            Book newExpensiveBook = new Book("최신 비싼 책", "저자2", "222", 60000,
                    LocalDate.of(2021, 1, 1));
            Book newCheapBook = new Book("최신 싼 책", "저자3", "333", 10000,
                    LocalDate.of(2021, 1, 1));

            bookRepository.save(oldExpensiveBook);
            bookRepository.save(newExpensiveBook);
            bookRepository.save(newCheapBook);

            // when
            List<Book> books = bookRepository.findExpensiveRecentBooks(50000, cutoffDate);

            // then
            assertThat(books).hasSize(1);
            assertThat(books.get(0).getTitle()).isEqualTo("최신 비싼 책");
        }
    }

    @Test
    @DisplayName("저자만 지정하여 검색한다")
    void searchByAuthorOnly() {
        // given
        bookRepository.save(new Book("클린 아키텍처", "로버트 마틴", "321", 32000));
        bookRepository.save(new Book("리팩토링", "마틴 파울러", "958", 35000));

        // when
        List<Book> books = bookRepository.searchBooks(null, 25000, 35000);

        // then
        assertThat(books).hasSize(2);
    }

    @Test
    @DisplayName("조건을 지정하지 않으면 모든 책을 조회한다")
    void searchAllIfNotCondition() {
        // given
        bookRepository.save(new Book("클린 아키텍처", "로버트 마틴", "321", 32000));
        bookRepository.save(new Book("리팩토링", "마틴 파울러", "958", 35000));
        bookRepository.save(new Book("리팩토링2", "마틴 파울러", "492", 65000));

        // when
        List<Book> books = bookRepository.searchBooks(null, null, null);

        // then
        assertThat(books).hasSize(3);



    }

}