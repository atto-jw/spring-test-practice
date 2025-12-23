package com.codeit.library.controller;

import com.codeit.library.dto.request.BookCreateRequest;
import com.codeit.library.dto.response.BookResponse;
import com.codeit.library.exception.BookNotFoundException;
import com.codeit.library.service.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@DisplayName("도서 Controller 테스트")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc; // 가짜 HTTP 요청을 보내는 도구

    @Autowired
    private ObjectMapper objectMapper; // JSON <-> Java 객체 변환

    @MockBean
    private BookService bookService; // 가짜 service
    
    @Nested
    @DisplayName("책 조회")
    class GetBooks {

        @Test
        @DisplayName("모든 책을 조회할 수 있다")
        void getAllBooks() throws Exception {
            // given
            List<BookResponse> books = List.of(
                    new BookResponse(1L, "클린 코드", "로버트 마틴", "123", 30000, null),
                    new BookResponse(2L, "아무책", "아토", "456", 25000, null)
            );

            when(bookService.findAll())
                    .thenReturn(books);


            // when & then
            mockMvc.perform(get("/api/books")) // 가짜 요청 보내기
                    .andDo(print()) // 콘솔에 요청/응답 출력 (디버깅용)
                    .andExpect(status().isOk()) // HTTP 상태 코드는 200 (ok) 일 것이다.
                    .andExpect(jsonPath("$", hasSize(2))) // $ 전체 JSON 표시, index 사용하듯이 $[0] 지정 가능
                    .andExpect(jsonPath("$[0].title").value("클린 코드"))
                    .andExpect(jsonPath("$[1].title").value("아무책"));

            verify(bookService).findAll();
        }

        @Test
        @DisplayName("ID로 책을 조회할 수 있다")
        void getBookById() throws Exception {
            // given
            BookResponse book =
                    new BookResponse(1L, "클린 코드", "로버트 마틴", "123", 30000, null);

            when(bookService.findById(1L))
                    .thenReturn(book);


            // when & then
            mockMvc.perform(get("/api/books/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("클린 코드"))
                    .andExpect(jsonPath("$.author").value("로버트 마틴"))
                    .andExpect(jsonPath("$.isbn").value("123"));
        }

        @Test
        @DisplayName("존재하지 않는 책을 조회하면 404를 반환한다")
        void getBookById_Notfound() throws Exception {
            // given
            when(bookService.findById(999L))
                    .thenThrow(new BookNotFoundException(999L));


            // when & then
            mockMvc.perform(get("/api/books/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("ID 999인 책을 찾을 수 없습니다"));
        }

        @Test
        @DisplayName("검색 조건으로 책을 조회할 수 있다")
        void searchBooks() throws Exception {
            mockMvc.perform(get("/api/books/search")
                            .param("author", "로버트 마틴")
                            .param("minPrice", "20000")
                            .param("maxPrice", "40000"))
                    .andExpect(status().isOk());

        }
    }
    @Nested
    @DisplayName("책 생성")
    class CreateBook {

        @Test
        @DisplayName("유효한 정보로 책을 생성할 수 있다")
        void createBook_Success() throws Exception {
            // given
            BookCreateRequest request =
                    new BookCreateRequest("클린 코드", "로버트 마틴", "123-0123456789", 30000, null);
            BookResponse response =
                    new BookResponse(1L, "클린 코드", "로버트 마틴", "123-0123456789", 30000, null);

            when(bookService.createBook(any(BookCreateRequest.class)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(post("/api/books") //post 요청 보낼거야
                    .contentType(MediaType.APPLICATION_JSON) // JSON 데이터 보낼거야
                    .content(objectMapper.writeValueAsString(request)) // 자바 객체를 JSON 문자열로 변환해서 보내줘
            )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location")) // header에 Location이라는 정보가 있을 것이다.
                    .andExpect(header().string("Location", "/api/books/1"))
                    .andExpect(jsonPath("$.id").value(1));

            verify(bookService).createBook(any(BookCreateRequest.class));
        }
    }

    @Nested
    @DisplayName("Content-Type 검증")
    class ContentTypeValidation {

        @Test
        @DisplayName("JSON이 아닌 요청은 거부된다.")
        void rejectNonJsonRequest() throws Exception {
            mockMvc.perform(post("/api/books")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("invalid content"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }
    @Test
    @DisplayName("Content-Type이 없으면 거부된다")
    void rejectWithoutContentType() throws Exception {
        // when & then
        mockMvc.perform(post("/api/books")
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }
}