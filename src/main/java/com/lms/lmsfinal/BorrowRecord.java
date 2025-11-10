package com.lms.lmsfinal;

public class BorrowRecord {
    private String isbn;
    private String title;
    private String author;
    private String dueDate;

    public BorrowRecord() { }

    public BorrowRecord(String isbn, String title, String author, String dueDate) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.dueDate = dueDate;
    }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
}
