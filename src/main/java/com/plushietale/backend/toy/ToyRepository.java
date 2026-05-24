package com.plushietale.backend.toy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToyRepository extends JpaRepository<Toy, Long> {

    List<Toy> findAllByUserId(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
