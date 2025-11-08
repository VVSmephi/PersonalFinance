package Models;

import java.util.Objects;

public class User {
    private final String login;
    private final String passwordHash;
    private final byte[] salt;

    public User(String login, String passwordHash, byte[] salt) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public String getLogin() { return login; }
    public String getPasswordHash() { return passwordHash; }
    public byte[] getSalt() { return salt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User u)) return false;
        return Objects.equals(login, u.login);
    }
    @Override
    public int hashCode() { return Objects.hash(login); }
}
