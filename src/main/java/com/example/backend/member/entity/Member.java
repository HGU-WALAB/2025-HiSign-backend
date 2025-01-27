package com.example.backend.member.entity;


import javax.persistence.*;

import com.example.backend.auth.dto.AuthDto;
import com.example.backend.base.entity.BaseTime;
import com.example.backend.member.entity.enums.Role;
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

    @Column(nullable = false, length = 50)
    private String department;

    @Column(nullable = false, length = 50)
    private String major1;

    @Column(nullable = false, length = 50)
    private String major2;

    @Column(nullable = false)
    private Integer grade;

    @Column
    private Integer semester;

    @Column(nullable = false)
    private Integer level;


    public void update(AuthDto dto) {
        this.name = dto.getName();
        this.email = dto.getEmail();
        this.department = dto.getDepartment();
        this.major1 = dto.getMajor1();
        this.major2 = dto.getMajor2();
        this.grade = dto.getGrade();
        this.semester = dto.getSemester();
    }

    public static Member from(AuthDto dto) {
        return Member.builder()
                .uniqueId(dto.getUniqueId())
                .name(dto.getName())
                .email(dto.getEmail())
                .loginTime(LocalDateTime.now())
                .department(dto.getDepartment())
                .major1(dto.getMajor1())
                .major2(dto.getMajor2())
                .grade(dto.getGrade())
                .semester(dto.getSemester())
                .build();
    }

    public String getRole() {
        return Role.fromLevel(this.level).getRoleName();
    }
}
