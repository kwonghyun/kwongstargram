package com.example.demo.src.user.model;


import com.example.demo.src.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetUserRes {
    private Long id;
    private String loginId;
    private String name;

    public GetUserRes(User user) {
        id = user.getId();
        loginId = user.getLoginId();
        name = user.getName();
    }
}
