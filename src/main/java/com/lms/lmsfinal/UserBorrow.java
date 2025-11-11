package com.lms.lmsfinal;

public class UserBorrow {
    private String id;
    private String isbn;
    private String title;
    private String borrowedAt;  // formatted
    private String dueDate;     // formatted
    private String returnedAt;  // formatted (nullable)
    private boolean returned;

    public UserBorrow() {}

    public UserBorrow(String id, String isbn, String title,
                      String borrowedAt, String dueDate, String returnedAt, boolean returned) {
        this.id = id; this.isbn = isbn; this.title = title;
        this.borrowedAt = borrowedAt; this.dueDate = dueDate; this.returnedAt = returnedAt;
        this.returned = returned;
    }

    public String getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getBorrowedAt() { return borrowedAt; }
    public String getDueDate() { return dueDate; }
    public String getReturnedAt() { return returnedAt; }
    public boolean isReturned() { return returned; }
}
