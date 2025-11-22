package com.lms.lmsfinal;

public class Book {
    private String isbn;
    private String title;
    private String author;
    private String category;      // 🔹 lower-case, consistent everywhere
    private String publisher;
    private int totalCopies;
    private int availableCopies;

    // Required no-arg constructor for Firestore
    public Book() {}

    public Book(String isbn,
                String title,
                String author,
                String category,
                String publisher,
                int totalCopies,
                int availableCopies) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publisher = publisher;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    @Override
    public String toString() {
        return title + " (" + isbn + ")";
    }
}
