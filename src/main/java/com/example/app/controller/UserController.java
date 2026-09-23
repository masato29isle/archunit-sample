package com.example.app.controller;

import com.example.app.repository.UserRepository;
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

    // NOTE: 本来 Controller が Repository を直接持つのはレイヤー違反。ハンズオンで直す箇所。
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{name}")
    public String checkUser(@PathVariable String name) {
        return userService.exists(name) ? "OK: " + name : "NOT FOUND: " + name;
    }

    // NOTE: Service を経由せず Repository を直接呼んでいる（レイヤー違反）
    @GetMapping
    public List<String> listUsers() {
        return userRepository.findAll();
    }
}
