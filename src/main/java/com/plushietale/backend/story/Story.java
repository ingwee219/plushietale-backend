package com.plushietale.backend.story;

import com.plushietale.backend.global.BaseEntity;
import com.plushietale.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Story extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Integer targetAge;

    @Builder.Default
    private Boolean isShared = false;

    // 이야기 삭제 시 연결된 StoryToy 레코드도 함께 삭제
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoryToy> storyToys = new ArrayList<>();

    public void updateSharedStatus(Boolean isShared) {
        this.isShared = isShared;
    }
}
