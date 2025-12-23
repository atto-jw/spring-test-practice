package com.codeit.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("도서 도메인 테스트")
class BookTest {

    @Test
    @DisplayName("")
    void createBook() {
        // given
        String title = "클린 코드";
        String author = "로버트 마틴";
        String isbn = "9788966266283";
        int price = 30000;

        // when
        Book book = new Book(title, author, isbn, price);


        // then
        assertThat(book.getTitle()).isEqualTo(title);
        assertThat(book.getPrice()).isNotEqualTo(50000);
    }

    @Nested
    @DisplayName("할인 기능")
    class DiscountFeature{

        private Book book;

        @BeforeEach
        void setUp() {
            book = new Book("클린 코드", "로버트 마틴", "978-0123456789", 30000);
        }

        /*
        # 메서드 이름 작성 관례
        1. Snake Case
        가장 전통적이고 가독성이 좋아 흔하게 사용하는 방식
        형식: 테스트대상_테스트조건_예상결과
        ex): void signUp_invalidEmail_throwException() { ... }
             void findById_exists_returnMember() { ... }

        2. BDD 스타일
        행위 주도 개발의 영향을 받은 스타일로, 외국에서 많이 사용하는 방식 (should - when)
        형식: should_예상행위_when_테스트조건
        ex): should_throwException_when_EmailIsInvalid() { ... }

        3. 한글 메서드 이름
        가독성을 최우선으로 하여 한글로 메서드 이름을 짓습니다.
        형식: 테스트내용_한글로서술
        ex): void 회원가입_실패_중복된_이메일() { ... }
             void 주문_성공_재고_차감_확인() { ... }

         결론: 협업 규칙이 1순위 입니다. -> 본인이 소속된 회사의 컨벤션을 따르는 것이 법입니다.
         메서드 이름을 고민하느라 시간을 소요하지 마세요. @DisplayName이 있으니까요.
         Given-When-Then 패턴을 잘 지켜주시고, 주석은 꼭 남겨 놓으세요.
         */
        @Test
        @DisplayName("10% 할인을 적용할 수 있다")
        void applyDiscount_10Percent() {

            // when
            book.applyDiscount(10);

            // then
            assertThat(book.getPrice()).isEqualTo(27000);
        }

        @Test
        @DisplayName("0% 할인율이면 가격이 변하지 않는다.")
        void applyDiscount_0Percent() {

            // when
            book.applyDiscount(0);

            // then
            assertThat(book.getPrice()).isEqualTo(30000);
        }

        @Test
        @DisplayName("할인율이 50%를 초과하면 예외기 발생한다.")
        void applyDiscount_ExceedMaxRate() {

            // when & then
            assertThatThrownBy(() -> book.applyDiscount(60))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("할인율은 0~50% 사이여야 합니다");
        }

        @Test
        @DisplayName("할인율이 음수면 예외가 발생한다.")
        void applyDiscount_NegativeRate() {

            // when & then
            assertThatThrownBy(() -> book.applyDiscount(-10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("할인율은 0~50% 사이여야 합니다");
        }

        @Nested
        @DisplayName("정보 수정")
        class updateBookInfo{
            private Book book;

            @BeforeEach
            void setUp() {
                book = new Book("클린 코드", "로버트 마틴", "978-0132350884", 30000);
            }

            @Test
            @DisplayName("제목을 수정할 수 있다")
            void updateTitle() {
                // when
                book.updateInfo("클린 코드 2판", null);

                // then
                assertThat(book.getTitle()).isEqualTo("클린 코드 2판");
                assertThat(book.getPrice()).isEqualTo(30000); // 가격은 변경 안됨
            }

            @Test
            @DisplayName("가격을 수정할 수 있다")
            void updatePrice() {
                // when
                book.updateInfo(null, 35000);

                // then
                assertThat(book.getTitle()).isEqualTo("클린 코드"); // 제목은 변경 안됨
                assertThat(book.getPrice()).isEqualTo(35000);
            }

            @Test
            @DisplayName("제목과 가격을 동시에 수정할 수 있다")
            void updateBoth() {
                // when
                book.updateInfo("클린 코드 2판", 35000);

                // then
                assertThat(book.getTitle()).isEqualTo("클린 코드 2판");
                assertThat(book.getPrice()).isEqualTo(35000);
            }


        }



    }

}