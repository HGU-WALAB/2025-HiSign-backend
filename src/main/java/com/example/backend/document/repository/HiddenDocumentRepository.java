package com.example.backend.document.repository;

import com.example.backend.document.entity.HiddenDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HiddenDocumentRepository extends JpaRepository<HiddenDocument, Long> {

    // 해당 유저가 숨긴 문서 ID 리스트 반환
    @Query("SELECT h.documentId FROM HiddenDocument h WHERE h.memberId = :uniqueId")
    List<Long> findDocumentIdsByMemberId(@Param("uniqueId") String uniqueId);

    // 특정 문서에 대해 이미 숨김 처리되었는지 확인 boolean existsByDocumentIdAndMemberIdAndViewType(Long documentId, String memberId, HiddenDocument.ViewType viewType);
    boolean existsByDocumentIdAndMemberIdAndViewType(Long documentId, String memberId, HiddenDocument.ViewType viewType);
}
