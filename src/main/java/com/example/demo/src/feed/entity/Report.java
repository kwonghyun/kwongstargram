package com.example.demo.src.feed.entity;

import com.example.demo.src.user.entity.User;
import lombok.*;

import javax.persistence.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Builder
@Getter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@Entity
public abstract class Report {
    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Reason reason;

    @Column(nullable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    public enum Reason {
        SPAM, HATRED, VIOLENCE, BULLYING, HARASSMENT;
    }

}