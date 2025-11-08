package Interfaces;

import Models.Wallet;

import java.util.Optional;

public interface IWalletRepository {
    Optional<Wallet> findByOwner(String login);
    void save(Wallet wallet);
}
