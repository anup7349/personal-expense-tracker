package com.anups.earntrack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int INK = Color.rgb(24, 34, 48);
    private static final int PAPER = Color.rgb(248, 250, 247);
    private static final int GREEN = Color.rgb(25, 138, 94);
    private static final int RED = Color.rgb(210, 73, 55);
    private static final int SOFT = Color.rgb(237, 242, 238);

    private AppDatabase db;
    private SharedPreferences session;
    private long currentUserId = -1;
    private String currentUserName = "";

    private EditText usernameInput;
    private EditText passwordInput;
    private TextView authMessageView;

    private EditText amountInput;
    private EditText noteInput;
    private TextView todayProfitView;
    private TextView todayIncomeView;
    private TextView todayExpenseView;
    private TextView monthIncomeView;
    private TextView monthExpenseView;
    private TextView monthProfitView;
    private TextView yearIncomeView;
    private TextView yearExpenseView;
    private TextView yearProfitView;
    private LinearLayout recentList;
    private TextView messageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new AppDatabase(this);
        session = getSharedPreferences("earntrack_session", MODE_PRIVATE);
        currentUserId = session.getLong("user_id", -1);
        currentUserName = session.getString("user_name", "");
        if (currentUserId > 0 && db.userExists(currentUserId)) {
            showHome();
        } else {
            showAuth();
        }
    }

    private void showAuth() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PAPER);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text("EarnTrack", 36, INK, true);
        root.addView(title);

        TextView subtitle = text("Sign in or register to keep your income and expense records in a local database.", 16, Color.rgb(82, 92, 105), false);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, margins(-1, -2, 0, 8, 0, 24));

        LinearLayout panel = panel();
        usernameInput = input("Username");
        panel.addView(usernameInput, margins(-1, dp(56), 0, 0, 0, 12));

        passwordInput = input("Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        panel.addView(passwordInput, margins(-1, dp(56), 0, 0, 0, 14));

        Button signIn = button("Sign in", GREEN);
        signIn.setOnClickListener(v -> signIn());
        panel.addView(signIn, margins(-1, dp(54), 0, 0, 0, 10));

        Button register = button("Register", INK);
        register.setOnClickListener(v -> register());
        panel.addView(register, margins(-1, dp(54), 0, 0, 0, 12));

        authMessageView = text("", 15, RED, false);
        authMessageView.setGravity(Gravity.CENTER);
        panel.addView(authMessageView);

        root.addView(panel, margins(-1, -2, 0, 0, 0, 0));
        setContentView(scroll);
    }

    private void signIn() {
        String username = clean(usernameInput.getText().toString());
        String password = passwordInput.getText().toString();
        if (!validAuth(username, password)) {
            return;
        }

        long userId = db.signIn(username, hashPassword(password));
        if (userId <= 0) {
            authMessageView.setText("Wrong username or password.");
            return;
        }

        startSession(userId, username);
    }

    private void register() {
        String username = clean(usernameInput.getText().toString());
        String password = passwordInput.getText().toString();
        if (!validAuth(username, password)) {
            return;
        }

        long userId = db.createUser(username, hashPassword(password));
        if (userId <= 0) {
            authMessageView.setText("That username already exists.");
            return;
        }

        startSession(userId, username);
    }

    private boolean validAuth(String username, String password) {
        if (username.length() < 3) {
            authMessageView.setText("Username must be at least 3 characters.");
            return false;
        }
        if (password.length() < 4) {
            authMessageView.setText("Password must be at least 4 characters.");
            return false;
        }
        return true;
    }

    private void startSession(long userId, String username) {
        currentUserId = userId;
        currentUserName = username;
        session.edit()
                .putLong("user_id", userId)
                .putString("user_name", username)
                .apply();
        showHome();
    }

    private void signOut() {
        session.edit().clear().apply();
        currentUserId = -1;
        currentUserName = "";
        showAuth();
    }

    private void showHome() {
        setContentView(buildHomeUi());
        refresh();
    }

    private View buildHomeUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PAPER);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(26), dp(20), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top, margins(-1, -2, 0, 0, 0, 6));

        TextView title = text("EarnTrack", 34, INK, true);
        title.setGravity(Gravity.START);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button signOut = button("Sign out", INK);
        signOut.setTextSize(12);
        signOut.setOnClickListener(v -> signOut());
        top.addView(signOut, new LinearLayout.LayoutParams(dp(94), dp(44)));

        TextView subtitle = text("Signed in as " + currentUserName + ". Add today's income and expenses, then see profit for today, this month, and this year.", 16, Color.rgb(82, 92, 105), false);
        subtitle.setGravity(Gravity.START);
        root.addView(subtitle, margins(-1, -2, 0, 0, 0, 20));

        LinearLayout hero = panel();
        hero.addView(label("TODAY PROFIT"));
        todayProfitView = text("Rs 0", 42, GREEN, true);
        todayProfitView.setGravity(Gravity.START);
        hero.addView(todayProfitView);
        root.addView(hero, margins(-1, -2, 0, 0, 0, 14));

        amountInput = input("Amount");
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(amountInput, margins(-1, dp(56), 0, 0, 0, 10));

        noteInput = input("Note, job, customer, item");
        root.addView(noteInput, margins(-1, dp(56), 0, 0, 0, 12));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, margins(-1, -2, 0, 0, 0, 18));

        Button addIncome = button("+ Add income", GREEN);
        addIncome.setOnClickListener(v -> addEntry(true));
        LinearLayout.LayoutParams incomeParams = new LinearLayout.LayoutParams(0, dp(58), 1);
        incomeParams.setMargins(0, 0, dp(5), 0);
        actions.addView(addIncome, incomeParams);

        Button addExpense = button("- Add expense", RED);
        addExpense.setOnClickListener(v -> addEntry(false));
        LinearLayout.LayoutParams expenseParams = new LinearLayout.LayoutParams(0, dp(58), 1);
        expenseParams.setMargins(dp(5), 0, 0, 0);
        actions.addView(addExpense, expenseParams);

        LinearLayout todayPanel = detailPanel("TODAY DETAILS");
        todayIncomeView = detailLine(todayPanel, "Earned");
        todayExpenseView = detailLine(todayPanel, "Spent");
        root.addView(todayPanel, margins(-1, -2, 0, 0, 0, 12));

        LinearLayout monthPanel = detailPanel("MONTH DETAILS");
        monthIncomeView = detailLine(monthPanel, "Earned");
        monthExpenseView = detailLine(monthPanel, "Spent");
        monthProfitView = detailLine(monthPanel, "Profit");
        root.addView(monthPanel, margins(-1, -2, 0, 0, 0, 12));

        LinearLayout yearPanel = detailPanel("YEAR DETAILS");
        yearIncomeView = detailLine(yearPanel, "Earned");
        yearExpenseView = detailLine(yearPanel, "Spent");
        yearProfitView = detailLine(yearPanel, "Profit");
        root.addView(yearPanel, margins(-1, -2, 0, 0, 0, 14));

        messageView = text("", 15, Color.rgb(82, 92, 105), false);
        messageView.setGravity(Gravity.START);
        root.addView(messageView, margins(-1, -2, 0, 2, 0, 14));

        LinearLayout recentPanel = panel();
        recentPanel.addView(label("RECENT ENTRIES"));
        recentList = new LinearLayout(this);
        recentList.setOrientation(LinearLayout.VERTICAL);
        recentPanel.addView(recentList, margins(-1, -2, 0, 8, 0, 0));
        root.addView(recentPanel, margins(-1, -2, 0, 0, 0, 12));

        return scroll;
    }

    private void addEntry(boolean income) {
        String rawAmount = amountInput.getText().toString().trim();
        if (rawAmount.length() == 0) {
            showMessage("Enter an amount first.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(rawAmount);
        } catch (NumberFormatException ex) {
            showMessage("Use numbers only for the amount.");
            return;
        }

        if (amount <= 0) {
            showMessage("Amount must be more than zero.");
            return;
        }

        long id = db.addEntry(
                currentUserId,
                amount,
                income,
                noteInput.getText().toString().trim(),
                dayKey(),
                monthKey(),
                yearKey(),
                new SimpleDateFormat("hh:mm a", Locale.US).format(new Date())
        );

        if (id <= 0) {
            showMessage("Could not save this entry.");
            return;
        }

        amountInput.setText("");
        noteInput.setText("");
        showMessage(income ? "Today's income added." : "Today's expense added.");
        refresh();
    }

    private void refresh() {
        String today = dayKey();
        String month = monthKey();
        String year = yearKey();
        double todayIncome = 0;
        double todayExpense = 0;
        double monthIncome = 0;
        double monthExpense = 0;
        double yearIncome = 0;
        double yearExpense = 0;
        int shown = 0;

        recentList.removeAllViews();

        Cursor cursor = db.entriesForUser(currentUserId);
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                boolean income = cursor.getInt(cursor.getColumnIndexOrThrow("income")) == 1;
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                String note = cursor.getString(cursor.getColumnIndexOrThrow("note"));
                String entryDay = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String entryMonth = cursor.getString(cursor.getColumnIndexOrThrow("month"));
                String entryYear = cursor.getString(cursor.getColumnIndexOrThrow("year"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));

                if (today.equals(entryDay)) {
                    if (income) {
                        todayIncome += amount;
                    } else {
                        todayExpense += amount;
                    }
                }
                if (month.equals(entryMonth)) {
                    if (income) {
                        monthIncome += amount;
                    } else {
                        monthExpense += amount;
                    }
                }
                if (year.equals(entryYear)) {
                    if (income) {
                        yearIncome += amount;
                    } else {
                        yearExpense += amount;
                    }
                }

                if (shown < 6) {
                    addRecentRow(id, income, amount, note, time);
                    shown++;
                }
            }
        } finally {
            cursor.close();
        }

        double todayProfit = todayIncome - todayExpense;
        double monthProfit = monthIncome - monthExpense;
        double yearProfit = yearIncome - yearExpense;

        todayProfitView.setText(money(todayProfit));
        todayProfitView.setTextColor(todayProfit >= 0 ? GREEN : RED);
        todayIncomeView.setText("Earned: " + money(todayIncome));
        todayExpenseView.setText("Spent: " + money(todayExpense));
        monthIncomeView.setText("Earned: " + money(monthIncome));
        monthExpenseView.setText("Spent: " + money(monthExpense));
        monthProfitView.setText("Profit: " + money(monthProfit));
        monthProfitView.setTextColor(monthProfit >= 0 ? GREEN : RED);
        yearIncomeView.setText("Earned: " + money(yearIncome));
        yearExpenseView.setText("Spent: " + money(yearExpense));
        yearProfitView.setText("Profit: " + money(yearProfit));
        yearProfitView.setTextColor(yearProfit >= 0 ? GREEN : RED);

        if (shown == 0) {
            TextView empty = text("No entries yet. Add today's income or expense.", 15, INK, false);
            empty.setGravity(Gravity.START);
            recentList.addView(empty, margins(-1, -2, 0, 8, 0, 0));
        }
    }

    private void addRecentRow(long id, boolean income, double amount, String note, String time) {
        String rowText = (income ? "+ " : "- ")
                + money(amount)
                + (note != null && note.length() > 0 ? "  " + note : "")
                + "  "
                + time;

        TextView row = text(rowText, 15, income ? GREEN : RED, true);
        row.setGravity(Gravity.START);
        row.setBackgroundColor(SOFT);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setOnLongClickListener(v -> {
            confirmRemoveEntry(id);
            return true;
        });
        recentList.addView(row, margins(-1, -2, 0, 8, 0, 0));
    }

    private void confirmRemoveEntry(long id) {
        new AlertDialog.Builder(this)
                .setTitle("Remove this detail?")
                .setMessage("Do you want to remove this income/expense entry?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.removeEntry(currentUserId, id);
                    showMessage("Entry removed.");
                    refresh();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showMessage(String value) {
        messageView.setText(value);
    }

    private String money(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(value == Math.rint(value) ? 0 : 2);
        return format.format(value).replace("\u20B9", "Rs ");
    }

    private String dayKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    private String monthKey() {
        return new SimpleDateFormat("yyyyMM", Locale.US).format(new Date());
    }

    private String yearKey() {
        return new SimpleDateFormat("yyyy", Locale.US).format(new Date());
    }

    private String clean(String value) {
        return value.trim().toLowerCase(Locale.US);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format(Locale.US, "%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            return password;
        }
    }

    private LinearLayout panel() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(18), dp(16), dp(18), dp(16));
        view.setBackgroundColor(Color.WHITE);
        return view;
    }

    private LinearLayout detailPanel(String title) {
        LinearLayout view = panel();
        view.addView(label(title));
        return view;
    }

    private TextView detailLine(LinearLayout parent, String label) {
        TextView view = text(label + ": Rs 0", 18, INK, true);
        view.setGravity(Gravity.START);
        view.setBackgroundColor(SOFT);
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        parent.addView(view, margins(-1, -2, 0, 8, 0, 0));
        return view;
    }

    private TextView label(String value) {
        TextView view = text(value, 12, Color.rgb(97, 108, 121), true);
        view.setGravity(Gravity.START);
        return view;
    }

    private EditText input(String hint) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setTextSize(16);
        view.setSingleLine(true);
        view.setPadding(dp(14), 0, dp(14), 0);
        view.setTextColor(INK);
        view.setHintTextColor(Color.rgb(126, 136, 148));
        view.setBackgroundColor(Color.WHITE);
        return view;
    }

    private Button button(String value, int color) {
        Button view = new Button(this);
        view.setText(value);
        view.setTextColor(Color.WHITE);
        view.setTextSize(14);
        view.setAllCaps(false);
        view.setBackgroundColor(color);
        return view;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        if (bold) {
            view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class AppDatabase extends SQLiteOpenHelper {
        AppDatabase(Activity activity) {
            super(activity, "earntrack.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT NOT NULL UNIQUE, " +
                    "password_hash TEXT NOT NULL)");
            database.execSQL("CREATE TABLE entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "income INTEGER NOT NULL, " +
                    "note TEXT, " +
                    "date TEXT NOT NULL, " +
                    "month TEXT NOT NULL, " +
                    "year TEXT NOT NULL, " +
                    "time TEXT NOT NULL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        }

        long createUser(String username, String passwordHash) {
            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("password_hash", passwordHash);
            return getWritableDatabase().insert("users", null, values);
        }

        long signIn(String username, String passwordHash) {
            Cursor cursor = getReadableDatabase().query(
                    "users",
                    new String[]{"id"},
                    "username = ? AND password_hash = ?",
                    new String[]{username, passwordHash},
                    null,
                    null,
                    null
            );
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
                return -1;
            } finally {
                cursor.close();
            }
        }

        boolean userExists(long id) {
            Cursor cursor = getReadableDatabase().query(
                    "users",
                    new String[]{"id"},
                    "id = ?",
                    new String[]{String.valueOf(id)},
                    null,
                    null,
                    null
            );
            try {
                return cursor.moveToFirst();
            } finally {
                cursor.close();
            }
        }

        long addEntry(long userId, double amount, boolean income, String note, String date, String month, String year, String time) {
            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("amount", amount);
            values.put("income", income ? 1 : 0);
            values.put("note", note);
            values.put("date", date);
            values.put("month", month);
            values.put("year", year);
            values.put("time", time);
            return getWritableDatabase().insert("entries", null, values);
        }

        Cursor entriesForUser(long userId) {
            return getReadableDatabase().query(
                    "entries",
                    null,
                    "user_id = ?",
                    new String[]{String.valueOf(userId)},
                    null,
                    null,
                    "id DESC"
            );
        }

        void removeEntry(long userId, long entryId) {
            getWritableDatabase().delete(
                    "entries",
                    "user_id = ? AND id = ?",
                    new String[]{String.valueOf(userId), String.valueOf(entryId)}
            );
        }
    }
}
