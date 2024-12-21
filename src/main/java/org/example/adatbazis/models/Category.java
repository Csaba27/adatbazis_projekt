package org.example.adatbazis.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.bson.types.ObjectId;

import java.util.Objects;

public class Category {
    private ObjectId id;
    private String name;
    private String description;

    public Category(ObjectId id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public StringProperty idProperty() {
        return new SimpleStringProperty(id.toString());
    }

    public StringProperty nameProperty() {
        return new SimpleStringProperty(name);
    }

    public StringProperty descriptionProperty() {
        return new SimpleStringProperty(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.getId()) && id.equals(category.getId());
    }
}
