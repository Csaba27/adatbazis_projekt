package org.example.adatbazis.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.bson.types.ObjectId;

import java.time.LocalDate;

public class BookCopy {
    private final ObjectId id;
    private final Book book; // konkrét könyv ahová tartozik
    private String copyNum; // példány azonosítója
    private String status; // elérhető, kölcsönzött stb.
    private LocalDate borrowDate; // kikölcsönzés dátuma
    private LocalDate returnDate; // visszahozatal határidő
    private String borrower; // kölcsönző neve vagy azonosítója
    private String condition; // példány állapota
    private LocalDate printDate; // nyomtatás dátuma

    public BookCopy(ObjectId id, Book book) {
        this.id = id;
        this.book = book;
    }

    public StringProperty idProperty() {
        return new SimpleStringProperty(id != null ? id.toString() : "");
    }

    public StringProperty bookProperty() {
        return new SimpleStringProperty(book != null ? book.getTitle() : "");
    }

    public StringProperty copyNumProperty() {
        return new SimpleStringProperty(copyNum);
    }

    public String getCopyNum() {
        return copyNum;
    }

    public void setCopyNum(String copyNum) {
        this.copyNum = copyNum;
    }

    public StringProperty statusProperty() {
        return new SimpleStringProperty(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public StringProperty borrowDateProperty() {
        return new SimpleStringProperty(borrowDate != null ? borrowDate.toString() : "");
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public StringProperty returnDateProperty() {
        return new SimpleStringProperty(returnDate != null ? returnDate.toString() : "");
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public StringProperty borrowerProperty() {
        return new SimpleStringProperty(borrower);
    }

    public String getBorrower() {
        return borrower;
    }

    public void setBorrower(String borrower) {
        this.borrower = borrower;
    }

    public StringProperty conditionProperty() {
        return new SimpleStringProperty(condition);
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public StringProperty printDateProperty() {
        return new SimpleStringProperty(printDate != null ? printDate.toString() : "");
    }

    public LocalDate getPrintDate() {
        return printDate;
    }

    public void setPrintDate(LocalDate addedDate) {
        this.printDate = addedDate;
    }

    public ObjectId getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }
}
