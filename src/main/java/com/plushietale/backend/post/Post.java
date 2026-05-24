package com.plushietale.backend.post;

import com.plushietale.backend.global.BaseEntity;
import com.plushietale.backend.story.Story;
import com.plushietale.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // story_id에 UNIQUE 제약 → 같은 이야기로 게시글 중복 불가
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", unique = true)
    private Story story;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(nullable = false)
    private Integer viewCount = 0;

    @Formula("(select count(*) from post_likes pl where pl.post_id = id)")
    private int likeCount;

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
