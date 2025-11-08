package Services;

import Interfaces.IUserRepository;
import Models.User;

import java.util.Optional;

public class AuthService {
    private final IUserRepository users;

    public AuthService(IUserRepository users) { this.users = users; }

    public void register(String login, char[] password) {
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Логин пуст");
        if (password == null || password.length < 4) throw new IllegalArgumentException("Пароль слишком короткий");
        if (users.findByLogin(login).isPresent()) throw new IllegalArgumentException("Пользователь уже существует");
        byte[] salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(password, salt);
        users.save(new User(login, hash, salt));
    }

    public boolean authenticate(String login, char[] password) {
        Optional<User> u = users.findByLogin(login);
        if (u.isEmpty()) return false;
        String h = PasswordHasher.hash(password, u.get().getSalt());
        return h.equals(u.get().getPasswordHash());
    }
}
