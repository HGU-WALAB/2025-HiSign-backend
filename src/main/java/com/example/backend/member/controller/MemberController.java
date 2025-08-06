package com.example.backend.member.controller;

import com.example.backend.auth.dto.AuthDto;
import com.example.backend.member.DTO.MemberActiveUpdateRequest;
import com.example.backend.member.DTO.MemberDTO;
import com.example.backend.member.DTO.SearchMemberDTO;
import com.example.backend.member.service.MemberService;
import com.example.backend.member.DTO.BulkInsertResultDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/search")
    public ResponseEntity<List<SearchMemberDTO>> searchSigners(@RequestParam String query) {
        List<SearchMemberDTO> result = memberService.searchSignersByNameOrEmail(query);
        for (SearchMemberDTO searchMemberDTO : result) {
            System.out.println(searchMemberDTO.getEmail());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<SearchMemberDTO>> searchByName(@RequestParam String query) {
        List<SearchMemberDTO> result = memberService.searchSignersByName(query);
        for (SearchMemberDTO searchMemberDTO : result) {
            System.out.println(searchMemberDTO.getEmail());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search/email")
    public ResponseEntity<List<SearchMemberDTO>> searchByEmail(@RequestParam String query) {
        List<SearchMemberDTO> result = memberService.searchSignersByEmail(query);
        for (SearchMemberDTO searchMemberDTO : result) {
            System.out.println(searchMemberDTO.getEmail());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDto> fetchLoginUserData() {
        log.debug("fetching Logged in UserData...");
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof AuthDto) {
            AuthDto authDto = (AuthDto) principal;
            return ResponseEntity.ok(authDto);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/members")
    public ResponseEntity<List<MemberDTO>> fetchMembers() {
        List<MemberDTO> result = memberService.getMembers();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<MemberDTO> addMember(@RequestBody MemberDTO dto) {
        MemberDTO created = memberService.addMember(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkInsertResultDTO> addMembersFromString(@RequestBody String input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.addMembersFromString(input));
    }

    @PatchMapping("/{uniqueId}/active")
    public ResponseEntity<MemberDTO> updateActiveStatus(
            @PathVariable String uniqueId,
            @RequestBody MemberActiveUpdateRequest request) {

        MemberDTO updated = memberService.updateMemberActiveStatus(uniqueId, request.getActive());
        return ResponseEntity.ok(updated);
    }

}
