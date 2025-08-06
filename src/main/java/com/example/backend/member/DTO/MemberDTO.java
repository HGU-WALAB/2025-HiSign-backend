package com.example.backend.member.DTO;

import com.example.backend.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MemberDTO {
    private String uniqueId;
    private String name;
    private String email;
    private Boolean active;

    public MemberDTO(Member member) {
        this.uniqueId = member.getUniqueId();
        this.name = member.getName();
        this.email = member.getEmail();
        this.active = member.getActive() != null ? member.getActive() : false;
    }
}
