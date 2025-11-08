package Models;

import Enums.TxnType;

import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {
    private final String id;
    private final TxnType type;
    private final String category;
    private final double amount;
    private final String note;
    private final LocalDateTime at;

    public Transaction(TxnType type, String category, double amount, String note, LocalDateTime at) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.note = note;
        this.at = at;
    }

    public String getId() { return id; }
    public TxnType getType() { return type; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getNote() { return note; }
    public LocalDateTime getAt() { return at; }
}
