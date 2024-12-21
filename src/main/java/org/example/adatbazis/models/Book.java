package org.example.adatbazis.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.bson.types.ObjectId;

import java.util.ArrayList;

public class Book {
    private ObjectId id;
    private String title;
    private String author;
    private int borrowCount;
    private int copies;
    private ArrayList<Category> categories;

    public Book(ObjectId id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.categories = new ArrayList<>();
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = new ObjectId(id);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public ArrayList<Category> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<Category> categories) {
        this.categories = categories;
    }

    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }

    public StringProperty idProperty() {
        return new SimpleStringProperty(id.toString());
    }

    public StringProperty titleProperty() {
        return new SimpleStringProperty(title);
    }

    public StringProperty authorProperty() {
        return new SimpleStringProperty(author);
    }

    public StringProperty copiesProperty() {
        return new SimpleStringProperty(String.valueOf(copies));
    }
}
