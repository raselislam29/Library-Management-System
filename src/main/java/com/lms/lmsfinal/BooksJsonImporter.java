package com.lms.lmsfinal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BooksJsonImporter {

    public static void main(String[] args) throws Exception {
        // 1. Initialize your existing FirebaseService
        FirebaseService service = new FirebaseService();

        // 2. Import books from JSON and upload to Firestore
        importBooksFromJson(service);

        System.out.println("✅ Finished importing books from books.json into Firestore 'books' collection");
    }

    private static void importBooksFromJson(FirebaseService service) throws Exception {
        // 1. Load books.json from root of resources
        InputStream is = BooksJsonImporter.class.getResourceAsStream("/com/lms/lmsfinal/books.json");
        if (is == null) {
            throw new IllegalStateException("books.json not found in src/main/resources/");
        }

        JsonObject root = JsonParser.parseReader(
                new InputStreamReader(is, StandardCharsets.UTF_8)
        ).getAsJsonObject();

        JsonObject booksObject = root.getAsJsonObject("Books");
        if (booksObject == null) {
            throw new IllegalStateException("JSON does not contain 'Books' object at root");
        }

        int count = 0;

        for (Map.Entry<String, JsonElement> entry : booksObject.entrySet()) {
            JsonObject b = entry.getValue().getAsJsonObject();

            Book book = new Book();
            book.setIsbn(b.get("isbn").getAsString());
            book.setTitle(b.get("title").getAsString());
            book.setAuthor(b.get("author").getAsString());
            book.setPublisher(b.get("publisher").getAsString());
            book.setCategory(b.get("category").getAsString());
            book.setTotalCopies(b.get("totalCopies").getAsInt());
            book.setAvailableCopies(b.get("availableCopies").getAsInt());

            // Uses your existing method -> writes to collection "books"
            service.addBook(book);
            count++;
        }

        System.out.println("📚 Imported " + count + " books.");
    }
}
