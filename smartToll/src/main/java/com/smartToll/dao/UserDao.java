
package com.smartToll.dao;

import java.util.List;
import java.util.Optional;
import com.smartToll.entity.User;

public interface UserDao {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> login(String email);

    List<User> findAll();

    void deleteById(Long id);
}