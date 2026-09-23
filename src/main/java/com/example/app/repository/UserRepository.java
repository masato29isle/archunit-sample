package com.example.app.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** ユーザー情報の永続化を担うクラス（サンプルなのでインメモリ）。 */
@Repository
public class UserRepository {

    private final List<String> users = List.of("alice", "bob", "carol");

    public Optional<String> findByName(String name) {
        return users.stream().filter(u -> u.equals(name)).findFirst();
    }

    public List<String> findAll() {
        return users;
    }
}
