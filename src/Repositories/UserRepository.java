package Repositories;

import Interfaces.IUserRepository;
import Models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserRepository implements IUserRepository {
    private final Map<String, User> map = new HashMap<>();

    @Override
    public Optional<User> findByLogin(String login) {
        return Optional.ofNullable(map.get(login));
    }

    @Override
    public void save(User user) {
        map.put(user.getLogin(), user);
    }
}
