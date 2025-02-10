package com.example.backend.signature.entity;

import com.example.backend.document.entity.Document;
import com.example.backend.member.entity.Member;
import javax.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "signature")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, length = 50)
    private String signerEmail;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer type;

    @Column(length = 255)
    private String image_data;

    @Column(length = 255)
    private String text_data;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer status;

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

    private String description;
}