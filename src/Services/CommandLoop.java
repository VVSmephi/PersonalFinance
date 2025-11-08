package Services;

import Interfaces.IUserRepository;
import Interfaces.IWalletRepository;
import Models.Wallet;
import Repositories.UserRepository;
import Repositories.WalletRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class CommandLoop {
    private final AuthService auth;
    private final WalletService wallet;
    private final ReportingService reporting;
    private final TransferService transfer;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final FileStorage fileStorage;

    private String currentLogin;

    public CommandLoop() {
        this.userRepo = new UserRepository();
        this.walletRepo = new WalletRepository();
        this.auth = new AuthService(userRepo);
        this.wallet = new WalletService(walletRepo);
        this.reporting = new ReportingService();
        this.transfer = new TransferService(wallet);
        this.fileStorage = new FileStorage();
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Личный Финансы CLI. Введите 'help' для команд.");
        while (true) {
            System.out.print("> ");
            String line = sc.nextLine().trim();
            if (line.isBlank()) continue;
            String[] parts = parseArgs(line);
            String cmd = parts[0].toLowerCase(Locale.ROOT);

            try {
                switch (cmd) {
                    case "help" -> printHelp();
                    case "register" -> cmdRegister(parts);
                    case "login" -> cmdLogin(parts);
                    case "logout" -> cmdLogout();
                    case "income" -> cmdIncome(parts);
                    case "expense" -> cmdExpense(parts);
                    case "budget-set" -> cmdBudgetSet(parts);
                    case "budget-edit" -> cmdBudgetEdit(parts);
                    case "summary" -> cmdSummary();
                    case "income-by-cat" -> cmdIncomeByCat();
                    case "expense-by-cat" -> cmdExpenseByCat();
                    case "budget-status" -> cmdBudgetStatus();
                    case "alerts" -> cmdAlerts();
                    case "filter-expense" -> cmdFilterExpense(parts);
                    case "transfer" -> cmdTransfer(parts);
                    case "export-csv" -> cmdExportCsv();
                    case "save" -> cmdSave();
                    case "exit" -> { cmdSave(); System.out.println("Выход."); return; }
                    default -> System.out.println("Неизвестная команда. 'help' для списка.");
                }
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void printHelp() {
        System.out.println("""
        Команды:
          register <login> <password>
          login <login> <password>              (загружает кошелёк из файла при наличии)
          logout
          income <category> <amount> [note]
          expense <category> <amount> [note]
          budget-set <category> <limit>
          budget-edit <category> <limit>
          summary
          income-by-cat
          expense-by-cat
          budget-status
          alerts
          filter-expense <cat1,cat2,...>
          transfer <toLogin> <amount> [note]
          export-csv
          save
          exit
        """);
    }

    private void cmdRegister(String[] a) {
        ensureArgs(a, 3, "register <login> <password>");
        auth.register(a[1], a[2].toCharArray());
        System.out.println("Регистрация успешно выполнена.");
    }

    private void cmdLogin(String[] a) {
        ensureArgs(a, 3, "login <login> <password>");
        boolean ok = auth.authenticate(a[1], a[2].toCharArray());
        if (!ok) { System.out.println("Неверные учетные данные"); return; }
        currentLogin = a[1];
        // загрузить кошелёк из файла (если есть)
        fileStorage.loadUserWallet(currentLogin).ifPresent(walletRepo::save);
        // инициализировать кошелек при отсутствии
        wallet.ensureWallet(currentLogin);
        System.out.println("Вход выполнен.");
    }

    private void cmdLogout() {
        ensureAuth();
        cmdSave();
        currentLogin = null;
        System.out.println("Вы вышли из аккаунта.");
    }

    private void cmdIncome(String[] a) {
        ensureAuth();
        ensureArgs(a, 3, "income <category> <amount> [note]");
        String cat = a[1];
        double amount = parseAmount(a[2]);
        String note = a.length >= 4 ? joinTail(a, 3) : null;
        wallet.addIncome(currentLogin, cat, amount, note, LocalDateTime.now());
        notifyBudget(cat);
    }

    private void cmdExpense(String[] a) {
        ensureAuth();
        ensureArgs(a, 3, "expense <category> <amount> [note]");
        String cat = a[1];
        double amount = parseAmount(a[2]);
        String note = a.length >= 4 ? joinTail(a, 3) : null;
        wallet.addExpense(currentLogin, cat, amount, note, LocalDateTime.now());
        notifyBudget(cat);
    }

    private void cmdBudgetSet(String[] a) {
        ensureAuth();
        ensureArgs(a, 3, "budget-set <category> <limit>");
        wallet.setBudget(currentLogin, a[1], parseAmount(a[2]));
        System.out.println("Бюджет установлен.");
    }

    private void cmdBudgetEdit(String[] a) {
        ensureAuth();
        ensureArgs(a, 3, "budget-edit <category> <limit>");
        wallet.editBudget(currentLogin, a[1], parseAmount(a[2]));
        System.out.println("Бюджет обновлен.");
    }

    private void cmdSummary() {
        ensureAuth();
        Wallet w = wallet.ensureWallet(currentLogin);
        System.out.println(reporting.summary(w));
    }

    private void cmdIncomeByCat() {
        ensureAuth();
        System.out.println(reporting.byCategory(wallet.incomeByCategory(currentLogin), "Доходы по категориям"));
    }

    private void cmdExpenseByCat() {
        ensureAuth();
        System.out.println(reporting.byCategory(wallet.expenseByCategory(currentLogin), "Расходы по категориям"));
    }

    private void cmdBudgetStatus() {
        ensureAuth();
        wallet.budgetStatus(currentLogin).forEach((k,v) -> System.out.println(k + ": " + v));
    }

    private void cmdAlerts() {
        ensureAuth();
        var list = wallet.alerts(currentLogin);
        if (list.isEmpty()) System.out.println("Оповещений нет.");
        else list.forEach(System.out::println);
    }

    private void cmdFilterExpense(String[] a) {
        ensureAuth();
        ensureArgs(a, 2, "filter-expense <cat1,cat2,...>");
        List<String> cats = Arrays.stream(a[1].split(",")).map(String::trim).filter(s->!s.isBlank()).toList();
        var map = wallet.expenseBySelectedCategories(currentLogin, cats);
        map.forEach((k,v) -> System.out.println(k + ": " + String.format("%,.1f", v)));
    }

    private void cmdTransfer(String[] a) {
        ensureAuth();
        ensureArgs(a, 3, "transfer <toLogin> <amount> [note]");
        String to = a[1];
        double amount = parseAmount(a[2]);
        String note = a.length >= 4 ? joinTail(a, 3) : "Перевод";
        transfer.transfer(currentLogin, to, amount, note);
        System.out.println("Перевод выполнен.");
    }

    private void cmdExportCsv() {
        ensureAuth();
        var w = wallet.ensureWallet(currentLogin);
        new FileStorage().exportCsv(w);
        System.out.println("Экспортирован CSV в папку data/.");
    }

    private void cmdSave() {
        if (currentLogin == null) return;
        var w = wallet.ensureWallet(currentLogin);
        fileStorage.saveUserWallet(w);
        System.out.println("Данные сохранены.");
    }

    private void ensureAuth() {
        if (currentLogin == null) throw new IllegalStateException("Необходимо выполнить login");
    }
    private void ensureArgs(String[] a, int n, String usage) {
        if (a.length < n) throw new IllegalArgumentException("Использование: " + usage);
    }
    private double parseAmount(String s) {
        try {
            double v = Double.parseDouble(s.replace(",", "."));
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректная сумма");
        }
    }
    private String joinTail(String[] a, int idx) {
        StringBuilder sb = new StringBuilder();
        for (int i = idx; i < a.length; i++) {
            if (i > idx) sb.append(' ');
            sb.append(a[i]);
        }
        return sb.toString();
    }
    private String[] parseArgs(String line) {
        return line.split("\\s+");
    }

    private void notifyBudget(String cat) {
        var alerts = wallet.alerts(currentLogin);
        alerts.stream().filter(m -> m.contains("'" + cat + "'")).forEach(System.out::println);
    }
}
