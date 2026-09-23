package com.example.app.controller;

import com.example.app.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ユーザー関連のリクエストを受け付けるクラス。
 *
 * <p>このサンプルは Spring Boot として起動しないため、アノテーションは「業務コードと同じ見た目」にするためのものです。
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{name}")
    public String checkUser(@PathVariable String name) {
        return userService.exists(name) ? "OK: " + name : "NOT FOUND: " + name;
    }

    // Service を経由してデータを取得する
    @GetMapping
    public List<String> listUsers() {
        return userService.listUsers();
    }
}
