package com.example.backend.member.entity;


import javax.persistence.*;

import com.example.backend.auth.dto.AuthDto;
import com.example.backend.base.entity.BaseTime;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unique_id", unique = true, length = 50)
    private String uniqueId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    public void update(AuthDto dto) {
        this.name = dto.getName();
        this.email = dto.getEmail();
    }

    public static Member from(AuthDto dto) {
        return Member.builder()
                .uniqueId(dto.getUniqueId())
                .name(dto.getName())
                .email(dto.getEmail())
                .build();
    }
}
