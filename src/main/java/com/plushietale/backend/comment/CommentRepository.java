package com.plushietale.backend.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByPostIdOrderByCreatedAtAsc(Long postId);

    List<Comment> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
