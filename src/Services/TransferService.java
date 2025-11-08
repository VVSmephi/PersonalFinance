package Services;

import java.time.LocalDateTime;

public class TransferService {
    private final WalletService walletService;

    public TransferService(WalletService walletService) {
        this.walletService = walletService;
    }

    public void transfer(String fromLogin, String toLogin, double amount, String note) {
        if (fromLogin.equals(toLogin)) throw new IllegalArgumentException("Нельзя перевести самому себе");
        if (amount <= 0) throw new IllegalArgumentException("Сумма перевода должна быть > 0");
        walletService.addExpense(fromLogin, "Переводы", amount, note + " -> " + toLogin, LocalDateTime.now());
        walletService.addIncome(toLogin, "Переводы", amount, note + " <- " + fromLogin, LocalDateTime.now());
    }
}
