package Services;

import Enums.TxnType;
import Models.Transaction;
import Models.Wallet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileStorage {
    private final Path dir = Paths.get("data");

    public FileStorage() {
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
    }

    public void saveUserWallet(Wallet w) {
        // JSON для транзакций + бюджетов
        Path p = dir.resolve(w.getOwnerLogin() + ".json");
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            String txns = w.getTransactions().stream().map(t ->
                    String.format(Locale.US,
                            "{\"id\":\"%s\",\"type\":\"%s\",\"category\":%s,\"amount\":%.2f,\"note\":%s,\"at\":\"%s\"}",
                            t.getId(), t.getType().name(),
                            t.getCategory()==null? "null" : "\"" + escape(t.getCategory()) + "\"",
                            t.getAmount(),
                            t.getNote()==null? "null" : "\"" + escape(t.getNote()) + "\"",
                            t.getAt().toString()
                    )
            ).collect(Collectors.joining(","));
            String budgets = w.getBudgets().entrySet().stream().map(e ->
                    String.format(Locale.US, "{\"category\":\"%s\",\"limit\":%.2f}", escape(e.getKey()), e.getValue().getLimit())
            ).collect(Collectors.joining(","));
            String json = "{"
                    + "\"owner\":\"" + escape(w.getOwnerLogin()) + "\","
                    + "\"transactions\":[" + txns + "],"
                    + "\"budgets\":[" + budgets + "]"
                    + "}";
            bw.write(json);
        } catch (IOException e) {
            System.err.println("Ошибка записи файла: " + e.getMessage());
        }
    }

    public Optional<Wallet> loadUserWallet(String login) {
        Path p = dir.resolve(login + ".json");
        if (!Files.exists(p)) return Optional.empty();
        try {
            String json = Files.readString(p);
            Wallet w = new Wallet(login);
            String txBlock = extractArray(json, "transactions");
            if (txBlock != null && !txBlock.isBlank()) {
                String[] items = splitTopLevel(txBlock);
                for (String it : items) {
                    String type = extractString(it, "type");
                    String category = extractNullableString(it, "category");
                    double amount = extractDouble(it, "amount");
                    String note = extractNullableString(it, "note");
                    String at = extractString(it, "at");
                    w.addTransaction(new Transaction(TxnType.valueOf(type), category, amount, note, LocalDateTime.parse(at)));
                }
            }
            String budBlock = extractArray(json, "budgets");
            if (budBlock != null && !budBlock.isBlank()) {
                String[] items = splitTopLevel(budBlock);
                for (String it : items) {
                    String category = extractString(it, "category");
                    double limit = extractDouble(it, "limit");
                    w.setBudget(category, limit);
                }
            }
            return Optional.of(w);
        } catch (Exception e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void exportCsv(Wallet w) {
        Path p = dir.resolve(w.getOwnerLogin() + "-txns.csv");
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            bw.write("id,type,category,amount,note,at\n");
            for (var t : w.getTransactions()) {
                bw.write(String.join(",",
                        t.getId(),
                        t.getType().name(),
                        safeCsv(t.getCategory()),
                        String.valueOf(t.getAmount()),
                        safeCsv(t.getNote()),
                        t.getAt().toString()));
                bw.write("\n");
            }
        } catch (IOException e) {
            System.err.println("Ошибка экспорта CSV: " + e.getMessage());
        }
    }

    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"","\\\""); }
    private static String safeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) return "\"" + s.replace("\"","\"\"") + "\"";
        return s;
    }

    private static String extractArray(String json, String key) {
        String marker = "\"" + key + "\":[";
        int i = json.indexOf(marker);
        if (i < 0) return null;
        int start = i + marker.length();
        int depth = 1;
        for (int j = start; j < json.length(); j++) {
            char c = json.charAt(j);
            if (c == '[') depth++;
            if (c == ']') depth--;
            if (depth == 0) return json.substring(start, j);
        }
        return null;
    }
    private static String extractString(String obj, String key) {
        String marker = "\"" + key + "\":\"";
        int i = obj.indexOf(marker);
        if (i < 0) return null;
        int start = i + marker.length();
        int end = obj.indexOf("\"", start);
        return obj.substring(start, end).replace("\\\"","\"").replace("\\\\","\\");
    }
    private static String extractNullableString(String obj, String key) {
        String marker1 = "\"" + key + "\":null";
        if (obj.contains(marker1)) return null;
        return extractString(obj, key);
    }
    private static double extractDouble(String obj, String key) {
        String marker = "\"" + key + "\":";
        int i = obj.indexOf(marker);
        if (i < 0) return 0;
        int start = i + marker.length();
        int end = start;
        while (end < obj.length() && "0123456789.-".indexOf(obj.charAt(end)) >= 0) end++;
        return Double.parseDouble(obj.substring(start, end));
    }
    private static String[] splitTopLevel(String arrayContent) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) out.add(arrayContent.substring(start, i+1));
            }
        }
        return out.toArray(new String[0]);
    }
}
