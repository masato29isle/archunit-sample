package com.example.app.service;

import com.example.app.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** ユーザーに関する業務ロジックを担うクラス。 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean exists(String name) {
        return userRepository.findByName(name).isPresent();
    }

    public List<String> listUsers() {
        return userRepository.findAll();
    }
}
