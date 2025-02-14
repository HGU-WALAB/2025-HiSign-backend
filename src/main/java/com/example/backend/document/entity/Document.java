package com.example.backend.document.entity;

import com.example.backend.member.entity.Member;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unique_id", referencedColumnName = "unique_id", nullable = false)
    private Member member; // 업로드한 회원

    @Column(nullable = false, length = 255)
    private String requestName;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(name = "saved_file_name", nullable = false)
    private String savedFileName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer status;

    @Column(length = 255, nullable = true)
    private String cancel_reason;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer isCanceled;

}

