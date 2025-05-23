package com.example.backend.document.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "hidden_document", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"document_id", "member_id"})
})
public class HiddenDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "member_id", nullable = false)
    private String memberId;  // uniqueId 기준

    @Enumerated(EnumType.STRING)
    @Column(name = "view_type", nullable = false)
    private ViewType viewType;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    public enum ViewType {
        sent, received, admin
    }
}
