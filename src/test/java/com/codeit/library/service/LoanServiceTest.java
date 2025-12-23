package com.codeit.library.service;

import com.codeit.library.domain.Book;
import com.codeit.library.domain.Loan;
import com.codeit.library.domain.Member;
import com.codeit.library.dto.request.LoanCreateRequest;
import com.codeit.library.dto.response.LoanResponse;
import com.codeit.library.exception.LoanLimitExceededException;
import com.codeit.library.exception.MemberNotFoundException;
import com.codeit.library.repository.BookRepository;
import com.codeit.library.repository.LoanRepository;
import com.codeit.library.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Mockito 활성화
@DisplayName("대출 서비스 테스트")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks // 테스트 대상에게 가짜 객체를 주입
    private LoanService loanService;

    @Nested
    @DisplayName("대출 생성")
    class CreateLoan{
        
        @Test
        @DisplayName("정상적으로 대출할 수 있다")
        void createLoan_Success() {
            // given
            Long memberId = 1L;
            Long bookId = 1L;

            Member member = new Member(memberId,"홍길동", "abc@abc.com");
            Book book = new Book(bookId,"클린 코드", "로버트 마틴", "123-1234", 30000);

            // Mock 동작 정의
            // loanService.createLoan()을 호출하면서 발생할 수 있는 모든 상황을 가정하여 Mock 객체에 세팅
            when(memberRepository.findById(memberId))
                    .thenReturn(Optional.of(member));

            when(bookRepository.findById(bookId))
                    .thenReturn(Optional.of(book));
            when(loanRepository.countByMemberIdAndReturnDateIsNull(memberId))
                    .thenReturn(2L);
            when(loanRepository.existsByBookIdAndReturnDateIsNull(bookId))
                    .thenReturn(false);
            when(loanRepository.existsOverdueLoan(memberId))
                    .thenReturn(false);

            Loan loan = new Loan(member, book, LocalDate.now());
            when(loanRepository.save(any(Loan.class)))
                    .thenReturn(loan);

            LoanCreateRequest request = new LoanCreateRequest(memberId, bookId);

            // when
            LoanResponse response = loanService.createLoan(request);


            // then
            assertThat(response).isNotNull();
            assertThat(response.getMemberId()).isEqualTo(memberId);
            assertThat(response.getBookId()).isEqualTo(bookId);

            //이 메서드가 실제로 호출되었니? 를 확인
            //가짜 객체는 실제로 DB 연동하지 않기 때문에 서비스의 특정 메서드가 호출될 때
            //실수로 메서드 호출 상황을 연출하지 않아도 테스트를 통과할 수 있기 때문에 확인하는 용도로 사용
            verify(loanRepository).countByMemberIdAndReturnDateIsNull(memberId);
            verify(loanRepository).existsByBookIdAndReturnDateIsNull(bookId);
            verify(loanRepository).existsOverdueLoan(memberId);
            verify(loanRepository).save(any(Loan.class));
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 예외가 발생한다")
        void createLoan_MemberNotFound() {
            // given
            Long memberId = 999L;
            Long bookId = 1L;

            Member member = new Member(memberId,"홍길동", "abc@abc.com");
            Book book = new Book(bookId,"클린 코드", "로버트 마틴", "123-1234", 30000);

            when(memberRepository.findById(memberId))
                    .thenReturn(Optional.empty()); // 회원이 없다는 것을 가정

            LoanCreateRequest request = new LoanCreateRequest(memberId, bookId);

            // when & then
            assertThatThrownBy(() -> loanService.createLoan(request))
                    .isInstanceOf(MemberNotFoundException.class)
                    .hasMessage(String.valueOf(memberId));

            // 회원이 없는데 대출일 실행될 리는 없기 때문에, loanRepository의 save가 절대 불리지 않았다 라는 검증
            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("대출 한도를 초과하면 예외가 발생한다.")
        void createLoan_ExceedLimit() {
            // given
            Long memberId = 1L;
            Long bookId = 1L;

            Member member = new Member(memberId,"홍길동", "abc@abc.com");
            Book book = new Book(bookId,"클린 코드", "로버트 마틴", "123-1234", 30000);

            when(memberRepository.findById(memberId))
                    .thenReturn(Optional.of(member));
            when(bookRepository.findById(bookId))
                    .thenReturn(Optional.of(book));
            when(loanRepository.countByMemberIdAndReturnDateIsNull(memberId))
                    .thenReturn(4L);

            LoanCreateRequest request = new LoanCreateRequest(memberId, bookId);
            // when & then
            assertThatThrownBy(() -> loanService.createLoan(request))
                    .isInstanceOf(LoanLimitExceededException.class)
                            .hasMessageContaining("최대 3권");

            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("대출 시 반납예정일은 14일 후로 설정된다")
        void createdLoan_checkDueDate() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate expectDueDate = today.plusDays(14);

            Long memberId = 1L;
            Long bookId = 1L;

            Member member = new Member(memberId,"홍길동", "abc@abc.com");
            Book book = new Book(bookId,"클린 코드", "로버트 마틴", "123-1234", 30000);

            when(memberRepository.findById(memberId))
                    .thenReturn(Optional.of(member));
            when(bookRepository.findById(bookId))
                    .thenReturn(Optional.of(book));
            when(loanRepository.countByMemberIdAndReturnDateIsNull(memberId))
                    .thenReturn(2L);
            when(loanRepository.existsByBookIdAndReturnDateIsNull(bookId))
                    .thenReturn(false);
            when(loanRepository.existsOverdueLoan(memberId))
                    .thenReturn(false);

            Loan loan = new Loan(member, book, LocalDate.now());

            // 단순히 호출만 되었냐만 확인 -> any()
            // 전달된 객체의 값 검증 필요 -> ArgumentCaptor
            ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);

            when(loanRepository.save(any(Loan.class)))
                    .thenReturn(loan);

            LoanCreateRequest request = new LoanCreateRequest(memberId, bookId);

            // when
            LoanResponse response = loanService.createLoan(request);

            // then
            // save에 전달된 loan 객체를 Captor가 잡아챈다.
            verify(loanRepository).save(loanCaptor.capture());

            // getValue()를 통해 꺼내서 검증 (내부의 값을 하나씩 검증)
            Loan captorValue = loanCaptor.getValue();

            assertThat(captorValue.getLoanDate()).isEqualTo(today);
            assertThat(captorValue.getLoanDate().plusDays(14)).isEqualTo(expectDueDate);
        }
    }

    @Nested
    @DisplayName("반납 기능")
    class ReturnBook {

        @Test
        @DisplayName("대출한 책을 반납할 수 있다.")
        void returnBook_Success() {
            //given
            Long loanId = 1L;
            Long memberId = 1L;
            Long bookId = 1L;

            Member member = new Member(memberId,"홍길동", "abc@abc.com");
            Book book = new Book(bookId,"클린 코드", "로버트 마틴", "123-1234", 30000);
            Loan loan = new Loan(member, book, LocalDate.now().minusDays(5));

            when(loanRepository.findById(loanId))
                    .thenReturn(Optional.of(loan));

            //when
            LoanResponse response = loanService.returnBook(loanId);

            //then
            assertThat(response).isNotNull();
            assertThat(response.getReturnDate()).isEqualTo(LocalDate.now());
        }
    }

    /*
    1. given-when-then 패턴은 항상 명확하게 작성하자!

    2. Mock 설정 누락 조심!

    3. any()와 정확한 값의 선택을 주의!
    - 조회: 보통 정확한 값
    - 저장/수정: 보통 any()
    - 숫자 세기/존재 여부 확인: 정확한 값

    4. verify() 사용 시기
    - 중요한 비즈니스 규칙 검증: 대출 한도 체크, 연체 여부 확인
    - 부작용이 있는 동작: save, delete, 외부 API 호출
    - 예외 케이스 작성 시에는 never()를 사용해서 호출되지 않았는지를 검증.

    5. 검증 시 메시지까지 검증 권장 (hasMessageContaining을 통해 핵심 키워드만 확인해도 좋아요.)
    */


}