package com.example.backend.member.entity;


import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Setter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    private Integer level;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "unique_id", nullable = false, unique = true, length = 50)
    private String uniqueId;

    private String department;
    private Integer grade;
    private String major1;
    private String major2;
    private Integer semester;
}
