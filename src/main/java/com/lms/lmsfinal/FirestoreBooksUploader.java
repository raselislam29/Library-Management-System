package com.lms.lmsfinal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class FirestoreBooksUploader {

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Starting book import...");

        // Use your existing FirebaseService (this also initializes Firestore)
        FirebaseService firebase = new FirebaseService();

        importBooksFromJson(firebase);

        System.out.println("✅ Finished importing books from books.json into Firestore 'books' collection");
    }

    private static void importBooksFromJson(FirebaseService firebase) throws Exception {
        // 1. Load books.json from the same package as serviceAccountKey.json
        InputStream is = FirestoreBooksUploader.class.getResourceAsStream(
                "/com/lms/lmsfinal/books.json"
        );

        if (is == null) {
            throw new IllegalStateException(
                    "books.json not found. Expected at: src/main/resources/com/lms/lmsfinal/books.json"
            );
        }

        JsonObject root = JsonParser.parseReader(
                new InputStreamReader(is, StandardCharsets.UTF_8)
        ).getAsJsonObject();

        JsonObject booksObject = root.getAsJsonObject("Books");
        if (booksObject == null) {
            throw new IllegalStateException("JSON does not contain 'Books' object at root");
        }

        int count = 0;

        // 2. Loop over each "Book 1", "Book 2", ...
        for (Map.Entry<String, JsonElement> entry : booksObject.entrySet()) {
            JsonObject b = entry.getValue().getAsJsonObject();

            String isbn       = b.get("isbn").getAsString();
            String title      = b.get("title").getAsString();
            String author     = b.get("author").getAsString();
            String category   = b.get("category").getAsString();
            String publisher  = b.get("publisher").getAsString();
            int totalCopies   = b.get("totalCopies").getAsInt();
            int availableCopy = b.get("availableCopies").getAsInt();

            Book book = new Book(
                    isbn,
                    title,
                    author,
                    category,
                    publisher,
                    totalCopies,
                    availableCopy
            );

            // 3. Save via your existing FirebaseService logic
            firebase.addBook(book);
            count++;

            System.out.println("   → Imported: " + title + " (" + isbn + ")");
        }

        System.out.println("📚 Total imported: " + count);
    }
}
