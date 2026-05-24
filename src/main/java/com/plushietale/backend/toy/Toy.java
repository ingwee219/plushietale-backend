package com.plushietale.backend.toy;

import com.plushietale.backend.global.BaseEntity;
import com.plushietale.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "toys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Toy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String personality;

    @Column(columnDefinition = "TEXT")
    private String description;

    public void updateName(String name) { this.name = name; }
    public void updateImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void updatePersonality(String personality) { this.personality = personality; }
    public void updateDescription(String description) { this.description = description; }
}
