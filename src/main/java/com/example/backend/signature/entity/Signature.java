package com.example.backend.signature.entity;

import com.example.backend.document.entity.Document;
import com.example.backend.member.entity.Member;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "signature")
@Getter
@Setter
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(nullable = false, length = 50)
    private String type; // 서명 유형 ("text", "image")

    @Column(length = 255)
    private String data; // Base64 이미지 또는 텍스트

    @Column(nullable = false)
    private Integer status; // 서명 상태 (0: 대기 중, 1: 완료)

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(nullable = false)
    private Float x;

    @Column(nullable = false)
    private Float y;

    @Column(nullable = false)
    private Float width;

    @Column(nullable = false)
    private Float height;

    private String description; // 서명 요청 설명
}