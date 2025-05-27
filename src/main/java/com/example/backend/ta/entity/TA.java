package com.example.backend.ta.entity;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ta")
@Getter
@Setter
@NoArgsConstructor
public class TA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ta_name", nullable = false, length = 50)
    private String taName;

    @Column(name = "lecture", nullable = false, length = 50)
    private String lecture;
}
