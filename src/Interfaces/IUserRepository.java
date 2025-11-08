package Interfaces;

import Models.User;

import java.util.Optional;

public interface IUserRepository {
    Optional<User> findByLogin(String login);
    void save(User user);
}
