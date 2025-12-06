package com.lms.lmsfinal;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;

import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;

public class FirebaseService {

    // 🔑 Your Firebase project's Web API key
    private static final String FIREBASE_WEB_API_KEY =
            "AIzaSyAc4FexZjwL_Cp0Mi8UQN39LIlCMm3PImE";
    private static final String AUTH_RESET_PW_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + FIREBASE_WEB_API_KEY;

    private static final String AUTH_SIGN_IN_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FIREBASE_WEB_API_KEY;
    private static final String AUTH_SIGN_UP_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + FIREBASE_WEB_API_KEY;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final Firestore db;

    public FirebaseService() {
        try {
            // serviceAccountKey.json should be at:
            // src/main/resources/com/lms/lmsfinal/serviceAccountKey.json
            InputStream sa = getClass().getResourceAsStream("/com/lms/lmsfinal/serviceAccountKey.json");
            if (sa == null) {
                throw new IllegalStateException("serviceAccountKey.json not found in resources/");
            }

            GoogleCredentials creds = GoogleCredentials.fromStream(sa);
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(creds)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            db = FirestoreClient.getFirestore();
        } catch (Exception e) {
            throw new RuntimeException("Failed to init Firebase Admin SDK", e);
        }
    }

    // ✅ Expose Firestore if you ever need it directly
    public Firestore getDb() {
        return db;
    }

    // =====================================================================
    // AUTH (REST)
    // =====================================================================

    public Optional<String> validateLogin(String email, String password) {
        try {
            String body = String.format(
                    "{\"email\":\"%s\",\"password\":\"%s\",\"returnSecureToken\":true}",
                    email.replace("\"", "\\\""),
                    password.replace("\"", "\\\"")
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_SIGN_IN_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                AuthResponse ar = gson.fromJson(res.body(), AuthResponse.class);
                return Optional.ofNullable(ar.email);
            } else {
                System.err.println("[FirebaseService] Login Failed: " + res.body());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("[FirebaseService] Login interrupted: " + ie.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public SignUpResult signUpUser(String email, String password) {
        try {
            String body = String.format(
                    "{\"email\":\"%s\",\"password\":\"%s\",\"returnSecureToken\":true}",
                    email.replace("\"", "\\\""),
                    password.replace("\"", "\\\"")
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_SIGN_UP_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                AuthResponse ar = gson.fromJson(res.body(), AuthResponse.class);
                try {
                    createUserRole(ar.localId, ar.email, "MEMBER");
                } catch (Throwable ignored) { }
                return SignUpResult.ok(ar.email);
            } else {
                String friendly = mapAuthError(res.body());
                System.err.println("[FirebaseService] SignUp Failed: " + res.body());
                return SignUpResult.err(friendly);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return SignUpResult.err("Request interrupted. Please try again.");
        } catch (Exception e) {
            e.printStackTrace();
            return SignUpResult.err("Network or server error: " + e.getMessage());
        }
    }

    public static class PasswordResetResult {
        public final boolean ok;
        public final String errorText;

        private PasswordResetResult(boolean ok, String errorText) {
            this.ok = ok;
            this.errorText = errorText;
        }

        public static PasswordResetResult ok()         { return new PasswordResetResult(true, null); }
        public static PasswordResetResult err(String m){ return new PasswordResetResult(false, m); }
    }

    private String mapAuthError(String responseBody) {
        try {
            ErrorEnvelope env = gson.fromJson(responseBody, ErrorEnvelope.class);
            if (env != null && env.error != null && env.error.message != null) {
                String m = env.error.message;
                switch (m) {
                    case "EMAIL_EXISTS":          return "This email is already registered.";
                    case "INVALID_EMAIL":         return "Please enter a valid email address.";
                    case "OPERATION_NOT_ALLOWED": return "Email/password sign-in is disabled in Firebase.";
                    case "API_KEY_INVALID":       return "Firebase API key is invalid or mismatched.";
                    case "EMAIL_NOT_FOUND":       return "No account found with this email.";
                    default:
                        if (m.contains("WEAK_PASSWORD")) return "Password is too weak (min 6 chars).";
                        return m.replace('_', ' ');
                }
            }
        } catch (Exception ignore) { }
        return "Registration failed. Please check your email and password.";
    }

    public PasswordResetResult sendPasswordResetEmail(String email) {
        try {
            String body = String.format(
                    "{\"requestType\":\"PASSWORD_RESET\",\"email\":\"%s\"}",
                    email.replace("\"", "\\\"")
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_RESET_PW_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                return PasswordResetResult.ok();
            } else {
                String friendly = mapAuthError(res.body());
                System.err.println("[FirebaseService] Password reset failed: " + res.body());
                return PasswordResetResult.err(friendly);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return PasswordResetResult.err("Request interrupted. Please try again.");
        } catch (Exception e) {
            e.printStackTrace();
            return PasswordResetResult.err("Network or server error: " + e.getMessage());
        }
    }

    // =====================================================================
    // USERS (Firestore + Profile)
    // =====================================================================

    private void createUserRole(String uid, String email, String role) throws Exception {
        DocumentReference ref = db.collection("users").document(uid);
        DocumentSnapshot snap = ref.get().get();
        if (!snap.exists()) {
            Map<String, Object> data = new HashMap<>();
            data.put("email", email);
            data.put("user_role", role);
            data.put("join_date", Timestamp.now());
            // profile fields can be added later
            ref.set(data).get();
        }
    }

    public User getUserByEmail(String email) {
        try {
            UserRecord rec = FirebaseAuth.getInstance().getUserByEmail(email);
            if (rec == null) return null;

            DocumentSnapshot doc = db.collection("users").document(rec.getUid()).get().get();
            if (doc.exists()) {
                String e = doc.getString("email");
                String r = doc.getString("user_role");
                return new User(0, e, r, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setUserRoleByEmail(String email, String role) throws Exception {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email required");
        if (role == null || role.isBlank())   throw new IllegalArgumentException("Role required");

        UserRecord rec = FirebaseAuth.getInstance().getUserByEmail(email);
        if (rec == null) throw new IllegalArgumentException("No Auth user found for email: " + email);

        String uid = rec.getUid();
        DocumentReference ref = db.collection("users").document(uid);
        DocumentSnapshot snap = ref.get().get();

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("user_role", role);
        if (!snap.exists()) {
            data.put("join_date", Timestamp.now());
            ref.set(data).get();
        } else {
            ref.set(data, SetOptions.merge()).get();
        }
    }

    public List<UserSummary> getAllUsers() {
        List<UserSummary> out = new ArrayList<>();
        try {
            for (ExportedUserRecord ur : FirebaseAuth.getInstance().listUsers(null).iterateAll()) {
                String uid = ur.getUid();
                String email = ur.getEmail();
                if (email == null || email.isBlank()) continue;

                String role = "MEMBER";
                try {
                    DocumentSnapshot doc = db.collection("users").document(uid).get().get();
                    if (doc.exists()) {
                        String r = doc.getString("user_role");
                        if (r != null && !r.isBlank()) role = r;
                    }
                } catch (Exception ignored) { }

                out.add(new UserSummary(uid, email, role));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    // ---------- NEW: User Profile helpers ----------

    /**
     * Profile DTO for user settings screen.
     */
    public static class UserProfile {
        private final String email;
        private final String role;
        private final String firstName;
        private final String lastName;
        private final String displayName;
        private final Date   joinDate;

        public UserProfile(String email, String role,
                           String firstName, String lastName,
                           String displayName, Date joinDate) {
            this.email = email;
            this.role = role;
            this.firstName = firstName;
            this.lastName = lastName;
            this.displayName = displayName;
            this.joinDate = joinDate;
        }

        public String getEmail()       { return email; }
        public String getRole()        { return role; }
        public String getFirstName()   { return firstName; }
        public String getLastName()    { return lastName; }
        public String getDisplayName() { return displayName; }
        public Date   getJoinDate()    { return joinDate; }
    }

    /**
     * Load full profile (email, role, first/last name, display name, join date).
     */
    public UserProfile getUserProfile(String email) {
        if (email == null || email.isBlank()) return null;

        try {
            UserRecord rec = FirebaseAuth.getInstance().getUserByEmail(email);
            if (rec == null) return null;

            String uid = rec.getUid();
            DocumentSnapshot doc = db.collection("users").document(uid).get().get();

            String role        = "MEMBER";
            String firstName   = null;
            String lastName    = null;
            String displayName = null;
            Timestamp joinTs   = null;

            if (doc.exists()) {
                String r = doc.getString("user_role");
                if (r != null && !r.isBlank()) role = r;

                firstName   = doc.getString("first_name");
                lastName    = doc.getString("last_name");
                displayName = doc.getString("display_name");
                joinTs      = doc.getTimestamp("join_date");
            }

            // If no displayName in Firestore, try Firebase Auth display name
            if (displayName == null || displayName.isBlank()) {
                displayName = rec.getDisplayName();
            }

            // Determine join date from Firestore or Auth metadata
            Date joinDate = null;
            if (joinTs != null) {
                joinDate = joinTs.toDate();
            } else if (rec.getUserMetadata() != null) {
                joinDate = new Date(rec.getUserMetadata().getCreationTimestamp());
            }

            return new UserProfile(email, role, firstName, lastName, displayName, joinDate);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update first name, last name, and/or display name (profile screen).
     * Any null value is ignored (field not changed).
     */
    public void updateUserProfileNames(String email,
                                       String firstName,
                                       String lastName,
                                       String displayName) throws Exception {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email required");
        }

        UserRecord rec = FirebaseAuth.getInstance().getUserByEmail(email);
        if (rec == null) {
            throw new IllegalArgumentException("No Auth user found for email: " + email);
        }

        String uid = rec.getUid();
        DocumentReference ref = db.collection("users").document(uid);

        Map<String, Object> data = new HashMap<>();
        if (firstName != null)   data.put("first_name", firstName);
        if (lastName != null)    data.put("last_name", lastName);
        if (displayName != null) data.put("display_name", displayName);

        if (!data.isEmpty()) {
            ref.set(data, SetOptions.merge()).get();
        }

        // Also sync display name back to Firebase Auth
        if (displayName != null && !displayName.isBlank()) {
            FirebaseAuth.getInstance()
                    .updateUser(new UserRecord.UpdateRequest(uid).setDisplayName(displayName));
        }
    }

    // Convenience: from profile screen you can just call sendPasswordResetEmail(email)
    // for "Change password" – no extra method needed.

    // =====================================================================
    // BOOKS
    // =====================================================================

    public List<Book> getBooksLimit(int limit) {
        try {
            Query q = db.collection("books");
            if (limit > 0) q = q.limit(limit);
            List<QueryDocumentSnapshot> docs = q.get().get().getDocuments();
            List<Book> out = new ArrayList<>();
            for (QueryDocumentSnapshot d : docs) {
                out.add(d.toObject(Book.class));
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Book> getBooks() { return getBooksLimit(0); }

    public List<Book> searchBooks(String query) {
        String q = query == null ? "" : query.toLowerCase();
        List<Book> all = getBooks();
        List<Book> out = new ArrayList<>();
        for (Book b : all) {
            if ((b.getTitle()  != null && b.getTitle().toLowerCase().contains(q)) ||
                    (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(q)) ||
                    (b.getIsbn()   != null && b.getIsbn().toLowerCase().contains(q))) {
                out.add(b);
            }
        }
        return out;
    }

    public void addBook(Book b) throws Exception {
        if (b.getIsbn() == null || b.getIsbn().isBlank())
            throw new IllegalArgumentException("ISBN required");
        db.collection("books").document(b.getIsbn()).set(b).get();
    }

    public void updateBook(Book b) throws Exception {
        if (b.getIsbn() == null || b.getIsbn().isBlank())
            throw new IllegalArgumentException("ISBN required");
        db.collection("books").document(b.getIsbn()).set(b, SetOptions.merge()).get();
    }

    public void deleteBookByIsbn(String isbn) throws Exception {
        if (isbn == null || isbn.isBlank()) return;
        db.collection("books").document(isbn).delete().get();
    }

    // =====================================================================
    // BORROW / RETURN
    // =====================================================================

    public void borrowBook(String isbn, String userEmail, int loanDays) throws Exception {
        if (isbn == null || isbn.isBlank())      throw new IllegalArgumentException("ISBN required");
        if (userEmail == null || userEmail.isBlank()) throw new IllegalArgumentException("User email required");
        if (loanDays <= 0) loanDays = 14;

        DocumentReference bookRef = db.collection("books").document(isbn);
        int finalLoanDays = loanDays;

        db.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(bookRef).get();
            if (!snap.exists()) throw new RuntimeException("Book not found");

            Long avail = snap.getLong("availableCopies");
            if (avail == null) avail = 0L;
            if (avail <= 0) throw new RuntimeException("No copies available");

            String title = snap.getString("title");
            String author = snap.getString("author");

            tx.update(bookRef, "availableCopies", avail - 1);

            Instant dueInstant = Instant.now().plusSeconds(finalLoanDays * 24L * 3600L);
            Timestamp dueTs = Timestamp.ofTimeSecondsAndNanos(
                    dueInstant.getEpochSecond(), dueInstant.getNano()
            );

            Map<String, Object> borrow = new HashMap<>();
            borrow.put("isbn", isbn);
            borrow.put("title", title);
            borrow.put("author", author);
            borrow.put("user_email", userEmail);
            borrow.put("borrowed_at", Timestamp.now());
            borrow.put("due_date", dueTs);
            borrow.put("returned", false);

            tx.set(db.collection("borrows").document(), borrow);
            return null;
        }).get();
    }

    public void borrowBookWithStudentInfo(String isbn,
                                          String userEmail,
                                          int loanDays,
                                          String studentName,
                                          String studentId,
                                          String phone,
                                          String studentEmail) throws Exception {
        if (isbn == null || isbn.isBlank())      throw new IllegalArgumentException("ISBN required");
        if (userEmail == null || userEmail.isBlank()) throw new IllegalArgumentException("User email required");
        if (loanDays <= 0) loanDays = 14;

        DocumentReference bookRef = db.collection("books").document(isbn);
        int finalLoanDays = loanDays;

        db.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(bookRef).get();
            if (!snap.exists()) throw new RuntimeException("Book not found");

            Long avail = snap.getLong("availableCopies");
            if (avail == null) avail = 0L;
            if (avail <= 0) throw new RuntimeException("No copies available");

            String title = snap.getString("title");
            String author = snap.getString("author");

            tx.update(bookRef, "availableCopies", avail - 1);

            java.time.Instant dueInstant = java.time.Instant.now()
                    .plusSeconds(finalLoanDays * 24L * 3600L);
            com.google.cloud.Timestamp dueTs =
                    com.google.cloud.Timestamp.ofTimeSecondsAndNanos(
                            dueInstant.getEpochSecond(), dueInstant.getNano()
                    );

            Map<String, Object> borrow = new HashMap<>();
            borrow.put("isbn", isbn);
            borrow.put("title", title);
            borrow.put("author", author);
            borrow.put("user_email", userEmail);
            borrow.put("student_name", studentName);
            borrow.put("student_id", studentId);
            borrow.put("student_phone", phone);
            borrow.put("student_email", studentEmail);
            borrow.put("borrowed_at", com.google.cloud.Timestamp.now());
            borrow.put("due_date", dueTs);
            borrow.put("returned", false);

            tx.set(db.collection("borrows").document(), borrow);
            return null;
        }).get();
    }

    public void returnBook(String isbn, String userEmail) throws Exception {
        if (isbn == null || isbn.isBlank())      throw new IllegalArgumentException("ISBN required");
        if (userEmail == null || userEmail.isBlank()) throw new IllegalArgumentException("User email required");

        QuerySnapshot qs = db.collection("borrows")
                .whereEqualTo("isbn", isbn)
                .whereEqualTo("user_email", userEmail)
                .whereEqualTo("returned", false)
                .limit(1)
                .get().get();

        if (qs.isEmpty()) throw new RuntimeException("No active borrow found.");

        DocumentSnapshot borrowDoc = qs.getDocuments().get(0);
        DocumentReference bookRef = db.collection("books").document(isbn);
        DocumentReference borrowRef = borrowDoc.getReference();

        db.runTransaction(tx -> {
            DocumentSnapshot bookSnap = tx.get(bookRef).get();
            if (!bookSnap.exists()) throw new RuntimeException("Book not found");

            Long avail = bookSnap.getLong("availableCopies");
            if (avail == null) avail = 0L;
            tx.update(bookRef, "availableCopies", avail + 1);

            tx.update(borrowRef, Map.of(
                    "returned", true,
                    "returned_at", Timestamp.now()
            ));
            return null;
        }).get();
    }

    // =====================================================================
    // RECENT BORROWS (Dashboard, using BorrowRecord POJO)
    // =====================================================================

    public List<BorrowRecord> getRecentBorrowsForUser(String userEmail, int limit) {
        try {
            Query q = db.collection("borrows")
                    .whereEqualTo("user_email", userEmail)
                    .orderBy("due_date", Query.Direction.DESCENDING);
            if (limit > 0) q = q.limit(limit);

            List<QueryDocumentSnapshot> docs = q.get().get().getDocuments();
            List<BorrowRecord> out = new ArrayList<>();
            for (QueryDocumentSnapshot d : docs) {
                String isbn   = d.getString("isbn");
                String title  = d.getString("title");
                String author = d.getString("author");
                Timestamp ts  = d.getTimestamp("due_date");

                String due = "";
                if (ts != null) {
                    java.time.LocalDate date = ts.toDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                    due = date.toString();
                }
                out.add(new BorrowRecord(isbn, title, author, due));
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<BorrowRecord> getRecentBorrowsAll(int limit) {
        try {
            Query q = db.collection("borrows")
                    .orderBy("due_date", Query.Direction.DESCENDING);
            if (limit > 0) q = q.limit(limit);

            List<QueryDocumentSnapshot> docs = q.get().get().getDocuments();
            List<BorrowRecord> out = new ArrayList<>();
            for (QueryDocumentSnapshot d : docs) {
                String isbn   = d.getString("isbn");
                String title  = d.getString("title");
                String author = d.getString("author");
                Timestamp ts  = d.getTimestamp("due_date");

                String due = "";
                if (ts != null) {
                    java.time.LocalDate date = ts.toDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                    due = date.toString();
                }
                out.add(new BorrowRecord(isbn, title, author, due));
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // =====================================================================
    // USER BORROWS (User dashboard / user tab)
    // =====================================================================

    public List<UserBorrow> getBorrowsForUser(String email, Boolean returnedFilter) {
        List<UserBorrow> out = new ArrayList<>();
        try {
            Query q = db.collection("borrows")
                    .whereEqualTo("user_email", email);

            if (returnedFilter != null) {
                q = q.whereEqualTo("returned", returnedFilter);
            }

            // No orderBy here → avoids composite index requirement
            List<QueryDocumentSnapshot> docs = q.get().get().getDocuments();
            for (QueryDocumentSnapshot d : docs) {
                String id    = d.getId();
                String isbn  = d.getString("isbn");
                String title = d.getString("title");
                com.google.cloud.Timestamp bts = d.getTimestamp("borrowed_at");
                com.google.cloud.Timestamp dts = d.getTimestamp("due_date");
                com.google.cloud.Timestamp rts = d.getTimestamp("returned_at");
                boolean ret = Boolean.TRUE.equals(d.getBoolean("returned"));

                String borrowedAt = bts == null ? "" : bts.toDate().toString();
                String dueDate    = dts == null ? "" : dts.toDate().toString();
                String returnedAt = rts == null ? "" : rts.toDate().toString();

                out.add(new UserBorrow(id, isbn, title, borrowedAt, dueDate, returnedAt, ret));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }


    public List<AdminBorrow> getActiveBorrowsForUser(String userEmail) {
        List<AdminBorrow> out = new ArrayList<>();
        try {
            List<QueryDocumentSnapshot> docs = db.collection("borrows")
                    .whereEqualTo("user_email", userEmail)
                    .whereEqualTo("returned", false)
                    .get().get().getDocuments();

            for (QueryDocumentSnapshot d : docs) {
                AdminBorrow ab = new AdminBorrow();

                // For user-owned borrows, we may not have student_name / student_email
                String sName  = d.getString("student_name");
                String sEmail = d.getString("student_email");
                if (sEmail == null || sEmail.isBlank()) {
                    sEmail = d.getString("user_email");
                }

                ab.studentName  = (sName == null) ? "" : sName;
                ab.studentEmail = (sEmail == null) ? "" : sEmail;
                ab.isbn         = d.getString("isbn");
                ab.title        = d.getString("title");
                ab.due_date     = d.getTimestamp("due_date");
                ab.returned     = Boolean.TRUE.equals(d.getBoolean("returned"));

                out.add(ab);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Low stock books – where availableCopies < threshold
     */
    public List<Book> getLowStockBooks(int threshold) {
        List<Book> out = new ArrayList<>();
        try {
            List<QueryDocumentSnapshot> docs = db.collection("books")
                    .whereLessThan("availableCopies", threshold)
                    .get().get().getDocuments();

            for (QueryDocumentSnapshot d : docs) {
                out.add(d.toObject(Book.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    // =====================================================================
// ADMIN BORROWS (used for both active and returned)
// =====================================================================

    public static class AdminBorrow {
        String studentName;
        String studentEmail;
        String isbn;
        String title;
        com.google.cloud.Timestamp due_date;
        com.google.cloud.Timestamp returned_at;
        boolean returned;

        public String getStudentName()  { return studentName; }
        public String getStudentEmail() { return studentEmail; }
        public String getIsbn()         { return isbn; }
        public String getTitle()        { return title; }
        public boolean isReturned()     { return returned; }

        public java.util.Date getDueDate() {
            return (due_date == null) ? null : due_date.toDate();
        }

        public String getDueDateStr() {
            if (due_date == null) return "";
            java.time.LocalDate d = due_date.toDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            return d.toString();
        }

        public java.util.Date getReturnedAt() {
            return (returned_at == null) ? null : returned_at.toDate();
        }

        public String getReturnedAtStr() {
            if (returned_at == null) return "";
            java.time.LocalDate d = returned_at.toDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            return d.toString();
        }
    }

    public List<AdminBorrow> getActiveBorrows() {
        try {
            List<QueryDocumentSnapshot> docs = db.collection("borrows")
                    .whereEqualTo("returned", false)
                    .orderBy("due_date", Query.Direction.ASCENDING)
                    .get().get().getDocuments();

            List<AdminBorrow> out = new ArrayList<>();
            for (QueryDocumentSnapshot d : docs) {
                AdminBorrow ab = new AdminBorrow();

                String sName  = d.getString("student_name");
                String sEmail = d.getString("student_email");
                if (sEmail == null || sEmail.isBlank()) {
                    sEmail = d.getString("user_email");
                }

                ab.studentName  = (sName == null) ? "" : sName;
                ab.studentEmail = (sEmail == null) ? "" : sEmail;
                ab.isbn         = d.getString("isbn");
                ab.title        = d.getString("title");
                ab.due_date     = d.getTimestamp("due_date");
                ab.returned_at  = d.getTimestamp("returned_at");
                ab.returned     = Boolean.TRUE.equals(d.getBoolean("returned"));

                out.add(ab);
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }


    public List<AdminBorrow> getReturnedBorrows() {
        try {
            List<QueryDocumentSnapshot> docs = db.collection("borrows")
                    .whereEqualTo("returned", true)
                    .get().get().getDocuments();

            List<AdminBorrow> out = new ArrayList<>();
            for (QueryDocumentSnapshot d : docs) {
                AdminBorrow ab = new AdminBorrow();

                String sName  = d.getString("student_name");
                String sEmail = d.getString("student_email");
                if (sEmail == null || sEmail.isBlank()) {
                    sEmail = d.getString("user_email");
                }

                ab.studentName  = (sName == null) ? "" : sName;
                ab.studentEmail = (sEmail == null) ? "" : sEmail;
                ab.isbn         = d.getString("isbn");
                ab.title        = d.getString("title");
                ab.due_date     = d.getTimestamp("due_date");
                ab.returned_at  = d.getTimestamp("returned_at");
                ab.returned     = Boolean.TRUE.equals(d.getBoolean("returned"));

                out.add(ab);
            }

            // newest returns first (client-side sort to avoid extra index)
            out.sort((a, b) -> {
                java.util.Date da = a.getReturnedAt();
                java.util.Date db2 = b.getReturnedAt();
                if (da == null && db2 == null) return 0;
                if (da == null) return 1;
                if (db2 == null) return -1;
                return db2.compareTo(da);
            });

            return out;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }


    // =====================================================================
    // Helper classes for Auth responses & user summaries
    // =====================================================================

    static class AuthResponse {
        String localId;
        String email;
        String idToken;
    }

    static class ErrorEnvelope { ErrorDetail error; }
    static class ErrorDetail  { String message; }

    public static class SignUpResult {
        public final boolean ok;
        public final String  email;
        public final String  errorText;

        private SignUpResult(boolean ok, String email, String errorText) {
            this.ok = ok;
            this.email = email;
            this.errorText = errorText;
        }

        public static SignUpResult ok(String email)   { return new SignUpResult(true,  email, null); }
        public static SignUpResult err(String msg)    { return new SignUpResult(false, null, msg); }
    }

    public static class UserSummary {
        private final String uid;
        private final String email;
        private final String role;

        public UserSummary(String uid, String email, String role) {
            this.uid = uid;
            this.email = email;
            this.role = role;
        }

        public String getUid()   { return uid; }
        public String getEmail() { return email; }
        public String getRole()  { return role; }
    }
}
