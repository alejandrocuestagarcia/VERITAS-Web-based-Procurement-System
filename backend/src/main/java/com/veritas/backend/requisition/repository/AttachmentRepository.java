package com.veritas.backend.requisition.repository;

import com.veritas.backend.requisition.entity.Attachment;
import com.veritas.backend.requisition.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    java.util.List<Attachment> findByRequest(Request request);
}