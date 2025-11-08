package Repositories;

import Interfaces.IWalletRepository;
import Models.Wallet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WalletRepository implements IWalletRepository {
    private final Map<String, Wallet> map = new HashMap<>();

    @Override
    public Optional<Wallet> findByOwner(String login) {
        return Optional.ofNullable(map.get(login));
    }

    @Override
    public void save(Wallet wallet) {
        map.put(wallet.getOwnerLogin(), wallet);
    }

    public Map<String, Wallet> internal() { return map; }
}
