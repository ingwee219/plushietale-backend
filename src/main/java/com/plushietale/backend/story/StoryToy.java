package com.plushietale.backend.story;

import com.plushietale.backend.toy.Toy;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "story_toys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class StoryToy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toy_id", nullable = false)
    private Toy toy;

    @Column(length = 100)
    private String roleInStory;
}
