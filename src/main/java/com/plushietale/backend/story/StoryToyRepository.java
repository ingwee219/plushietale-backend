package com.plushietale.backend.story;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryToyRepository extends JpaRepository<StoryToy, Long> {

    List<StoryToy> findAllByStoryId(Long storyId);

    void deleteAllByToyId(Long toyId);
}
