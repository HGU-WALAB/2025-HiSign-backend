package com.example.backend.member.controller;

import com.example.backend.member.DTO.SearchMemberDTO;
import com.example.backend.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
