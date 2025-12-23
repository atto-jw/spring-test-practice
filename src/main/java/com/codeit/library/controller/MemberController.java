package com.codeit.library.controller;

import com.codeit.library.dto.request.MemberCreateRequest;
import com.codeit.library.dto.response.MemberResponse;
import com.codeit.library.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponse> createMember(
            @Valid @RequestPart("request") MemberCreateRequest request,
            @RequestPart("file") MultipartFile file) {
        MemberResponse response = memberService.createMember(request, file);
        return ResponseEntity
            .created(URI.create("/api/members/" + response.getId()))
            .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
        MemberResponse response = memberService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getMembers() {
        List<MemberResponse> response = memberService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<MemberResponse> getMemberByEmail(@PathVariable String email) {
        MemberResponse response = memberService.findByEmail(email);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<MemberResponse> updateName(
        @PathVariable Long id,
        @RequestParam String name
    ) {
        MemberResponse response = memberService.updateName(id, name);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }
}

