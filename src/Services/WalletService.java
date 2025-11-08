package Services;

import Enums.TxnType;
import Interfaces.IWalletRepository;
import Models.Transaction;
import Models.Wallet;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class WalletService {
    private final IWalletRepository wallets;

    public WalletService(IWalletRepository wallets) {
        this.wallets = wallets;
    }

    public Wallet ensureWallet(String login) {
        return wallets.findByOwner(login).orElseGet(() -> {
            Wallet w = new Wallet(login);
            wallets.save(w);
            return w;
        });
    }

    public void addIncome(String login, String category, double amount, String note, LocalDateTime at) {
        validateAmount(amount);
        validateCategory(category);
        Wallet w = ensureWallet(login);
        w.addTransaction(new Transaction(TxnType.INCOME, category, amount, note, at));
        wallets.save(w);
    }

    public void addExpense(String login, String category, double amount, String note, LocalDateTime at) {
        validateAmount(amount);
        validateCategory(category);
        Wallet w = ensureWallet(login);
        w.addTransaction(new Transaction(TxnType.EXPENSE, category, amount, note, at));
        wallets.save(w);
    }

    public void setBudget(String login, String category, double limit) {
        validateCategory(category);
        if (limit < 0) throw new IllegalArgumentException("Лимит не может быть отрицательным");
        Wallet w = ensureWallet(login);
        w.setBudget(category, limit);
        wallets.save(w);
    }

    public void editBudget(String login, String category, double limit) {
        setBudget(login, category, limit);
    }

    public double totalIncome(String login) {
        Wallet w = ensureWallet(login);
        return w.getTransactions().stream().filter(t -> t.getType()==TxnType.INCOME).mapToDouble(Transaction::getAmount).sum();
    }

    public double totalExpense(String login) {
        Wallet w = ensureWallet(login);
        return w.getTransactions().stream().filter(t -> t.getType()==TxnType.EXPENSE).mapToDouble(Transaction::getAmount).sum();
    }

    public Map<String, Double> incomeByCategory(String login) {
        Wallet w = ensureWallet(login);
        return w.getTransactions().stream()
                .filter(t -> t.getType()==TxnType.INCOME)
                .collect(Collectors.groupingBy(t -> Optional.ofNullable(t.getCategory()).orElse("Без категории"),
                        Collectors.summingDouble(Transaction::getAmount)));
    }

    public Map<String, Double> expenseByCategory(String login) {
        Wallet w = ensureWallet(login);
        return w.getTransactions().stream()
                .filter(t -> t.getType()==TxnType.EXPENSE)
                .collect(Collectors.groupingBy(t -> Optional.ofNullable(t.getCategory()).orElse("Без категории"),
                        Collectors.summingDouble(Transaction::getAmount)));
    }

    public Map<String, Double> expenseBySelectedCategories(String login, List<String> categories) {
        Wallet w = ensureWallet(login);
        Set<String> set = new HashSet<>(categories);
        Map<String, Double> result = new LinkedHashMap<>();
        for (String c : categories) {
            double sum = w.getTransactions().stream()
                    .filter(t -> t.getType()==TxnType.EXPENSE && c.equals(t.getCategory()))
                    .mapToDouble(Transaction::getAmount).sum();
            if (sum == 0) {
                System.out.println("Внимание: категория не найдена или нет расходов: " + c);
            }
            result.put(c, sum);
        }
        return result;
    }

    public Map<String, String> budgetStatus(String login) {
        Wallet w = ensureWallet(login);
        Map<String, Double> byCat = expenseByCategory(login);
        Map<String, String> res = new LinkedHashMap<>();
        w.getBudgets().forEach((cat, b) -> {
            double spent = byCat.getOrDefault(cat, 0.0);
            double left = b.getLimit() - spent;
            res.put(cat, String.format("Бюджет: %.2f, Остаток: %.2f", b.getLimit(), left));
        });
        return res;
    }

    public List<String> alerts(String login) {
        Wallet w = ensureWallet(login);
        List<String> alerts = new ArrayList<>();
        double income = totalIncome(login);
        double expense = totalExpense(login);
        if (expense > income) {
            alerts.add("Расходы превысили доходы! Текущий баланс: " + (income - expense));
        }
        Map<String, Double> byCat = expenseByCategory(login);
        w.getBudgets().forEach((cat, budget) -> {
            double spent = byCat.getOrDefault(cat, 0.0);
            if (spent >= 0.8 * budget.getLimit() && spent < budget.getLimit()) {
                alerts.add("Достигнуто 80% лимита по '" + cat + "': " + String.format("%.2f/%.2f", spent, budget.getLimit()));
            }
            if (spent > budget.getLimit()) {
                alerts.add("Превышен лимит по '" + cat + "': " + String.format("%.2f/%.2f", spent, budget.getLimit()));
            }
        });
        if (w.balance() == 0) {
            alerts.add("Баланс нулевой.");
        }
        return alerts;
    }

    private void validateCategory(String category) {
        if (category == null || category.isBlank()) throw new IllegalArgumentException("Пустая категория");
    }
    private void validateAmount(double amount) {
        if (Double.isNaN(amount) || amount <= 0) throw new IllegalArgumentException("Некорректная сумма");
    }
}
