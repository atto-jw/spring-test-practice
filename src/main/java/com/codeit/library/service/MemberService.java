package com.codeit.library.service;

import com.codeit.library.domain.Member;
import com.codeit.library.dto.request.MemberCreateRequest;
import com.codeit.library.dto.response.MemberResponse;
import com.codeit.library.exception.DuplicateEmailException;
import com.codeit.library.exception.MemberNotFoundException;
import com.codeit.library.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final S3FileService s3FileService;
    private final S3PrivateFileService s3PrivateFileService;

    @Transactional
    public MemberResponse createMember(MemberCreateRequest request, MultipartFile file) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        // 멤버 객체 생성 하기전에 AWS S3와 연동해서 프로필 파일을 전송하는 로직 작성
        try {
            // 이 url을 DB에 회원정보와 함께 저장
            // 나중에 프론트에서 회원 정보를 요청할 때 Url도 같이 전달 -> 프론트에서 <img> 등으로 요청을 보낼 것
            // 만약 presigned url을 사용한다면 굳이 DB에 url을 저장할 필요가 없다.
            // 파일명(객체 Key)을 DB에 저장하세요. 그리고 데이터를 불러올 일이 있다면 그 때마다
            // presigned url을 얻어내서 프론트쪽을 전달.
            String url = s3PrivateFileService.uploadToS3Bucket(file);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        Member member = new Member(request.getName(), request.getEmail());
        Member saved = memberRepository.save(member);
        
        return MemberResponse.from(saved);
    }

    public MemberResponse findById(Long id) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new MemberNotFoundException(id));
        return MemberResponse.from(member);
    }

    public List<MemberResponse> findAll() {
        return memberRepository.findAll().stream()
            .map(MemberResponse::from)
            .collect(Collectors.toList());
    }

    public MemberResponse findByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new MemberNotFoundException("이메일 " + email + "인 회원을 찾을 수 없습니다"));
        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse updateName(Long id, String name) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new MemberNotFoundException(id));
        
        member.updateName(name);
        
        return MemberResponse.from(member);
    }

    @Transactional
    public void deleteMember(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new MemberNotFoundException(id);
        }
        memberRepository.deleteById(id);
    }
}

