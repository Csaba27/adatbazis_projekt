package org.example.adatbazis;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.adatbazis.models.Book;
import org.example.adatbazis.models.BookCopy;
import org.example.adatbazis.models.Category;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

public class MongoDBConnection {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "library";
    private static MongoDatabase database;

    // Kapcsolat inicializálására
    public static void connect() {
        try {
            MongoClient mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase(DATABASE_NAME);

            System.out.println("Kapcsolódás sikeres az adatbázishoz: " + database.getName());
        } catch (Exception e) {
            System.err.println("Hiba a kapcsolódás során: " + e.getMessage());
        }
    }

    public static MongoDatabase getDatabase() {
        return database;
    }

    // Könyvek kilistázása
    public static ObservableList<Book> listBooks() {
        MongoCollection<Document> collection = database.getCollection("books");
        MongoCollection<Document> copiessCollection = database.getCollection("book_copies");
        MongoCursor<Document> cursor = collection.find().iterator();
        ObservableList<Book> books = FXCollections.observableArrayList();
        try {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String title = doc.getString("title");

                long count = copiessCollection.countDocuments(eq("book", doc.getObjectId("_id")));

                Book book = new Book(
                        doc.getObjectId("_id"),
                        title,
                        doc.getString("author")
                );

                book.setCopies((int) count);
                books.add(book);
            }
        } finally {
            cursor.close();
        }
        return books;
    }

    // Könyv keresése cím vagy szerző alapján
    public static ObservableList<Book> searchBooks(String searchTerm) {
        MongoCollection<Document> collection = database.getCollection("books");
        MongoCursor<Document> cursor = collection.find(
                Filters.or(
                        Filters.regex("title", searchTerm, "i"),
                        Filters.regex("author", searchTerm, "i")
                )
        ).iterator();
        ObservableList<Book> books = FXCollections.observableArrayList();
        try {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                books.add(new Book(
                        doc.getObjectId("_id"),
                        doc.getString("title"),
                        doc.getString("author")
                ));
            }
        } finally {
            cursor.close();
        }
        return books;
    }

    // Könyv törlése
    public static void deleteBook(ObjectId id) {
        // Könyvek
        database.getCollection("books").deleteOne(eq("_id", id));
        System.out.println("Könyv törölve: " + id);
        // Példányok
        database.getCollection("book_copies").deleteOne(in("book", id));
        System.out.println("Könyvhöz tartozó példányok törölve: " + id);
    }

    // Könyv hozzáadása vagy frissítése
    public static void saveOrUpdateBook(Book book) {
        MongoCollection<Document> collection = database.getCollection("books");

        // Kategória ID-k gyűjtése
        List<ObjectId> categoryIds = book.getCategories().stream()
                .map(Category::getId) // Minden Category objektumból kinyerjük az ObjectId-t
                .distinct()
                .collect(Collectors.toList());

        // Ha a könyv ID nem null, frissítés, különben új könyv beszúrása
        if (book.getId() != null) {
            // Frissítés az ID alapján
            collection.updateOne(
                    eq("_id", book.getId()),
                    new Document("$set", new Document("title", book.getTitle())
                            .append("author", book.getAuthor())
                            .append("categories", categoryIds))
            );
        } else {
            // Új könyv hozzáadása
            Document newBook = new Document()
                    .append("title", book.getTitle())
                    .append("author", book.getAuthor())
                    .append("categories", categoryIds);

            InsertOneResult result = collection.insertOne(newBook);

            // Beállítjuk az újonnan létrehozott ObjectId-t a könyv objektumhoz
            book.setId(newBook.getObjectId("_id"));
        }
    }

    // Kategória lista
    public static ObservableList<Category> listCategories() {
        ObservableList<Category> categories = FXCollections.observableArrayList();
        MongoCollection<Document> collection = database.getCollection("categories");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                ObjectId id = doc.getObjectId("_id");
                String name = doc.getString("name");
                String description = doc.getString("description");
                categories.add(new Category(id, name, description));
            }
        }
        return categories;
    }

    // Könyv kategóriái
    public static ArrayList<Category> getCategoriesByBookId(ObjectId bookId) {
        MongoCollection<Document> booksCollection = database.getCollection("books");
        MongoCollection<Document> categoriesCollection = database.getCollection("categories");

        // Lekérjük a könyvet az ID alapján
        Document bookDoc = booksCollection.find(eq("_id", bookId)).first();
        // Ha a könyv nem létezik vagy nincsenek kategóriái, üres listát adunk vissza
        if (bookDoc == null || bookDoc.get("categories") == null) {
            return new ArrayList<>();
        }

        // Kinyerjük a kategóriák ObjectId listáját
        ArrayList<ObjectId> categoryIds = (ArrayList<ObjectId>) bookDoc.get("categories");

        // Lekérjük a kategóriákat a categories kollekcióból az ObjectId-k alapján
        FindIterable<Document> categoryDocs = categoriesCollection.find(in("_id", categoryIds));

        // Átalakítjuk a kategóriákat Category objektumok listájává
        ArrayList<Category> categories = new ArrayList<>();
        for (Document categoryDoc : categoryDocs) {
            ObjectId id = categoryDoc.getObjectId("_id");
            String name = categoryDoc.getString("name");
            String description = categoryDoc.getString("descriptinion");
            Category category = new Category(id, name, description);
            categories.add(category);
        }

        return categories;
    }

    // Kategória hozzáadása
    public static void addCategory(String name, String description) {
        MongoCollection<Document> collection = database.getCollection("categories");

        Document newCategory = new Document()
                .append("name", name)
                .append("description", description);

        collection.insertOne(newCategory);
        System.out.println("Könyv kategória hozzáadva: " + newCategory.getObjectId("_id"));
    }

    // Kategória szerkesztése
    public static void updateCategory(Category category) {
        MongoCollection<Document> collection = database.getCollection("categories");

        collection.updateOne(
                eq("_id", category.getId()),
                new Document("$set", new Document("name", category.getName())
                        .append("description", category.getDescription()))
        );

        System.out.println("Könyv kategória szerkesztve: " + category.getId());
    }

    // Kategória törlése
    public static void deleteCategory(ObjectId id) {
        database.getCollection("categories").deleteOne(eq("_id", id));
        System.out.println("Könyv kategória törölve: " + id);
    }

    // Könyv példány hozzáadása
    public static void addBookCopy(BookCopy bookCopy) {
        MongoCollection<Document> collection = database.getCollection("book_copies");

        Document bookCopyDoc = new Document("book", bookCopy.getBook().getId())
                .append("copy_num", bookCopy.getCopyNum())
                .append("status", bookCopy.getStatus())
                .append("borrow_date", bookCopy.getBorrowDate())
                .append("return_date", bookCopy.getReturnDate())
                .append("borrower", bookCopy.getBorrower())
                .append("condition", bookCopy.getCondition())
                .append("print_date", bookCopy.getPrintDate())
                .append("added", new Date());

        collection.insertOne(bookCopyDoc);
        System.out.println("Könyv példány hozzáadva: " + bookCopyDoc.getObjectId("_id"));
    }

    // Könyv példány hozzáadása
    public static void updateBookCopy(BookCopy bookCopy) {
        MongoCollection<Document> collection = database.getCollection("book_copies");

        Document bookCopyDoc = new Document("book", bookCopy.getBook().getId())
                .append("copy_num", bookCopy.getCopyNum())
                .append("status", bookCopy.getStatus())
                .append("borrow_date", bookCopy.getBorrowDate())
                .append("return_date", bookCopy.getReturnDate())
                .append("borrower", bookCopy.getBorrower())
                .append("condition", bookCopy.getCondition())
                .append("print_date", bookCopy.getPrintDate());

        collection.updateOne(eq("_id", bookCopy.getId()), new Document("$set", bookCopyDoc));

        System.out.println("Könyv példány szerkesztve: " + bookCopy.getId());
    }

    // Könyv példányok lista könyv azonosító alapján
    public static ObservableList<BookCopy> getBookCopiesByBook(Book book) {
        ObservableList<BookCopy> copies = FXCollections.observableArrayList();
        MongoCollection<Document> collection = database.getCollection("book_copies");

        try (MongoCursor<Document> cursor = collection.find(eq("book", book.getId())).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                ObjectId id = doc.getObjectId("_id");
                String copyNum = doc.getString("copy_num");
                String status = doc.getString("status");
                String borrower = doc.getString("borrower");
                String condition = doc.getString("condition");

                Date printDateRaw = doc.getDate("print_date");
                LocalDate printDate = printDateRaw != null
                        ? Instant.ofEpochMilli(printDateRaw.getTime()).atZone(ZoneId.systemDefault()).toLocalDate()
                        : null;

                Date borrowDateRaw = doc.getDate("borrow_date");
                LocalDate borrowDate = borrowDateRaw != null
                        ? Instant.ofEpochMilli(borrowDateRaw.getTime()).atZone(ZoneId.systemDefault()).toLocalDate()
                        : null;

                Date returnDateRaw = doc.getDate("return_date");
                LocalDate returnDate = returnDateRaw != null
                        ? Instant.ofEpochMilli(returnDateRaw.getTime()).atZone(ZoneId.systemDefault()).toLocalDate()
                        : null;

                BookCopy bookCopy = new BookCopy(id, book);
                bookCopy.setCopyNum(copyNum);
                bookCopy.setStatus(status);
                bookCopy.setBorrowDate(borrowDate);
                bookCopy.setReturnDate(returnDate);
                bookCopy.setPrintDate(printDate);
                bookCopy.setBorrower(borrower);
                bookCopy.setCondition(condition);
                copies.add(bookCopy);
            }
        }
        return copies;
    }

    // Könyv példány törlése
    public static void deleteBookCopy(ObjectId id) {
        database.getCollection("book_copies").deleteOne(eq("_id", id));
        System.out.println("Könyv példány törölve: " + id);
    }
}