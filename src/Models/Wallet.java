package Models;

import Enums.TxnType;

import java.util.*;

public class Wallet {
    private final String ownerLogin;
    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<String, CategoryBudget> budgets = new HashMap<>();

    public Wallet(String ownerLogin) {
        this.ownerLogin = ownerLogin;
    }

    public String getOwnerLogin() { return ownerLogin; }
    public List<Transaction> getTransactions() { return Collections.unmodifiableList(transactions); }
    public Map<String, CategoryBudget> getBudgets() { return Collections.unmodifiableMap(budgets); }

    public void addTransaction(Transaction t) { transactions.add(t); }
    public void setBudget(String category, double limit) { budgets.put(category, new CategoryBudget(category, limit)); }
    public void editBudget(String category, double limit) {
        budgets.compute(category, (k, v) -> v == null ? new CategoryBudget(category, limit) : new CategoryBudget(category, limit));
    }

    public double balance() {
        double income = transactions.stream().filter(t -> t.getType()== TxnType.INCOME).mapToDouble(Transaction::getAmount).sum();
        double expense = transactions.stream().filter(t -> t.getType()==TxnType.EXPENSE).mapToDouble(Transaction::getAmount).sum();
        return income - expense;
    }
}
