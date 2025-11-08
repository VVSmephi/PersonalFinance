package Services;

import Enums.TxnType;
import Models.Wallet;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportingService {
    public String summary(Wallet w) {
        double income = w.getTransactions().stream().filter(t->t.getType()== TxnType.INCOME).mapToDouble(t->t.getAmount()).sum();
        double expense = w.getTransactions().stream().filter(t->t.getType()==TxnType.EXPENSE).mapToDouble(t->t.getAmount()).sum();
        return "Общий доход: " + String.format("%,.1f", income) + "\n" +
                "Общие расходы: " + String.format("%,.1f", expense) + "\n" +
                "Баланс: " + String.format("%,.1f", income - expense);
    }

    public String byCategory(Map<String, Double> map, String title) {
        String body = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(e -> e.getKey() + ": " + String.format("%,.1f", e.getValue()))
                .collect(Collectors.joining("\n"));
        return title + ":\n" + body;
    }
}
