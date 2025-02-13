package com.example.backend.document.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 문서 버전 ID (Primary Key)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;  // 문서 엔티티 참조 (FK)

    @Column(nullable = false)
    private Integer version;  // 문서의 서명 버전 (1부터 시작)

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;  // 서명된 문서 파일명

    @Column(name = "signed_by", length = 255)
    private String signedBy;  // 서명한 사용자 이메일

    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;  // 서명된 시간 (기본값: 현재 시간)

    @PrePersist
    protected void onCreate() {
        this.signedAt = LocalDateTime.now();
    }
}
