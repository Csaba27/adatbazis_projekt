package org.example.adatbazis;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.bson.types.ObjectId;
import org.example.adatbazis.models.Book;
import org.example.adatbazis.models.BookCopy;
import org.example.adatbazis.models.Category;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MainApp extends Application {

    private TabPane mainTabPane;
    private TableView<Book> bookTableView;
    private TableView<Category> categoryTableView;
    private TableView<BookCopy> bookCopyTableView;
    private ProgressIndicator loadingIndicator;
    private Label toastLabel;

    @Override
    public void start(Stage primaryStage) {
        // MongoDB kapcsolat megkezdése
        MongoDBConnection.connect();

        mainTabPane = new TabPane();

        createLoadingIndicator();
        createToastLabel();

        // Könyvek
        Tab bookTab = new Tab("Könyvek", createBookTabContent(primaryStage));
        bookTab.setClosable(false);

        // Kategóriák
        Tab categoryTab = new Tab("Kategóriák", createCategoryTabContent(primaryStage));
        categoryTab.setClosable(false);

        mainTabPane.getTabs().addAll(bookTab, categoryTab);

        BorderPane borderPane = new BorderPane();
        borderPane.setLeft(toastLabel);
        borderPane.setRight(loadingIndicator);

        VBox mainLayout = new VBox(10);
        mainLayout.getChildren().addAll(mainTabPane, borderPane);

        Scene scene = new Scene(mainLayout, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Könyvkezelő Rendszer");
        primaryStage.show();
    }

    private Button createAddBookButton(Stage parentStage) {
        Button addButton = new Button("Új könyv hozzáadása");
        addButton.setOnAction(e -> openBookWindow(parentStage, null));
        return addButton;
    }

    private VBox createBookTabContent(Stage parentStage) {
        VBox layout = new VBox(10);

        Button refreshButton = new Button("Újratölt");
        refreshButton.setOnAction(e -> refreshBookTable());

        HBox controlBox = new HBox(10, refreshButton);
        controlBox.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(createSearchField(), createBookTableView(parentStage), controlBox, createAddBookButton(parentStage));
        layout.setPadding(new Insets(10));

        return layout;
    }

    private TableView<Book> createBookTableView(Stage parentStage) {
        bookTableView = new TableView<>();

        List<TableColumn<Book, ?>> columns = bookTableView.getColumns();

        TableColumn<Book, String> idColumn = new TableColumn<>("ID") {{
            setCellValueFactory(data -> data.getValue().idProperty());
        }};

        TableColumn<Book, String> titleColumn = new TableColumn<>("Cím") {{
            setCellValueFactory(data -> data.getValue().titleProperty());
        }};

        TableColumn<Book, String> authorColumn = new TableColumn<>("Szerző") {{
            setCellValueFactory(data -> data.getValue().authorProperty());
        }};

        TableColumn<Book, String> copiesColumn = new TableColumn<>("Példányok") {{
            setCellValueFactory(data -> data.getValue().copiesProperty());
        }};

        columns.add(idColumn);
        columns.add(titleColumn);
        columns.add(authorColumn);
        columns.add(copiesColumn);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem editMenuItem = new MenuItem("Szerkesztés");
        MenuItem deleteMenuItem = new MenuItem("Törlés");
        // MenuItem copyValueItem = new MenuItem("Érték másolása");
        MenuItem copyRowItem = new MenuItem("Sor másolása");
        MenuItem addCopyMenuItem = new MenuItem("Példány hozzáadása");

        contextMenu.getItems().addAll(editMenuItem, deleteMenuItem, addCopyMenuItem, copyRowItem);

        bookTableView.setRowFactory(tv -> {
            TableRow<Book> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                // Dupla kattintás esemény a szerkesztéshez
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    Book item = row.getItem();
                    openBookCopiesTabContent(parentStage, item);
                }
            });

            // ContextMenu
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    // Itt megjelenítjük a context menüt
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });

            return row;
        });

        loadBooks();

        // Törlés és szerkesztés műveletek
        deleteMenuItem.setOnAction(e -> {
            Book item = bookTableView.getSelectionModel().getSelectedItem();
            if (item != null)
                deleteBook(item);
        });

        // Szerkesztés
        editMenuItem.setOnAction(e -> {
            Book item = bookTableView.getSelectionModel().getSelectedItem();
            if (item != null)
                openBookWindow(parentStage, item);
        });

        // Másolás vágólapra (sor)
        copyRowItem.setOnAction(e -> {
            Book item = bookTableView.getSelectionModel().getSelectedItem();
            if (item != null) {
                String rowContent = item.getId() + "\t" + item.getTitle() + "\t" + item.getAuthor();
                copyToClipBoard(rowContent);
            }
        });

        // Példány hozzáadás
        addCopyMenuItem.setOnAction(e -> {
            Book item = bookTableView.getSelectionModel().getSelectedItem();
            if (item != null) {
                openBookCopyFormWindow(parentStage, item, null);
            }
        });

        return bookTableView;
    }

    private VBox createCategoryTabContent(Stage parentStage) {
        VBox layout = new VBox(10);
        categoryTableView = new TableView<>();

        ContextMenu contextMenu = new ContextMenu();
        MenuItem editMenuItem = new MenuItem("Szerkesztés");
        MenuItem deleteMenuItem = new MenuItem("Törlés");

        contextMenu.getItems().addAll(editMenuItem, deleteMenuItem);

        TableColumn<Category, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> data.getValue().idProperty());

        TableColumn<Category, String> nameColumn = new TableColumn<>("Kategória neve");
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<Category, String> descColumn = new TableColumn<>("Leírás");
        descColumn.setCellValueFactory(data -> data.getValue().descriptionProperty());

        categoryTableView.getColumns().addAll(idColumn, nameColumn, descColumn);

        Button addCategoryButton = new Button("Új kategória hozzáadása");
        addCategoryButton.setOnAction(e -> openCategoryFormWindow(parentStage, null));

        categoryTableView.setRowFactory(tv -> {
            TableRow<Category> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });

        layout.getChildren().addAll(categoryTableView, addCategoryButton);
        layout.setPadding(new Insets(10));

        deleteMenuItem.setOnAction(e -> {
            Category item = categoryTableView.getSelectionModel().getSelectedItem();
            deleteCategory(item);
        });

        editMenuItem.setOnAction(e -> {
            Category item = categoryTableView.getSelectionModel().getSelectedItem();
            openCategoryFormWindow(parentStage, item);
        });

        loadCategories();
        return layout;
    }

    private void openBookCopiesTabContent(Stage parentStage, Book book) {
        // Új fül a könyvpéldányok információihoz
        Tab copiesTab = new Tab(book.getTitle() + " - Könyvpéldányok");

        VBox layout = new VBox(15);
        bookCopyTableView = new TableView<>();

        // Contextmenu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editMenuItem = new MenuItem("Szerkesztés");
        MenuItem deleteMenuItem = new MenuItem("Törlés");

        contextMenu.getItems().addAll(editMenuItem, deleteMenuItem);

        TableColumn<BookCopy, String> copyNumColumn = new TableColumn<>("Példány azonosító");
        copyNumColumn.setCellValueFactory(data -> data.getValue().copyNumProperty());

        TableColumn<BookCopy, String> statusColumn = new TableColumn<>("Állapot");
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());

        TableColumn<BookCopy, String> addedDateColumn = new TableColumn<>("Nyomtatés dátuma");
        addedDateColumn.setCellValueFactory(data ->
                data.getValue().printDateProperty()
        );

        TableColumn<BookCopy, String> borrowDateColumn = new TableColumn<>("Kölcsönzés dátuma");
        borrowDateColumn.setCellValueFactory(data ->
                data.getValue().borrowDateProperty()
        );

        TableColumn<BookCopy, String> returnDateColumn = new TableColumn<>("Visszahoz határidő");
        returnDateColumn.setCellValueFactory(data ->
                data.getValue().returnDateProperty()
        );

        TableColumn<BookCopy, String> borrowerColumn = new TableColumn<>("Kölcsönző");
        borrowerColumn.setCellValueFactory(data -> data.getValue().borrowerProperty());

        TableColumn<BookCopy, String> conditionColumn = new TableColumn<>("Állapot");
        conditionColumn.setCellValueFactory(data -> data.getValue().conditionProperty());

        bookCopyTableView.getColumns().addAll(copyNumColumn, statusColumn, addedDateColumn,
                borrowDateColumn, returnDateColumn, borrowerColumn, conditionColumn);

        bookCopyTableView.setRowFactory(tv -> {
            TableRow<BookCopy> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });

        deleteMenuItem.setOnAction(e -> {
            BookCopy item = bookCopyTableView.getSelectionModel().getSelectedItem();
            deleteBookCopy(bookCopyTableView, item);
        });

        editMenuItem.setOnAction(e -> {
            BookCopy item = bookCopyTableView.getSelectionModel().getSelectedItem();
            openBookCopyFormWindow(parentStage, book, item);
        });

        Button addButton = new Button("Példány hozzáadása");
        layout.getChildren().addAll(bookCopyTableView, addButton);

        addButton.setOnAction(e -> {
            openBookCopyFormWindow(parentStage, book, null);
        });

        // Fül tartalmának beállítása
        copiesTab.setContent(layout);

        // Fül hozzáadása a fő lapcsoporthoz
        mainTabPane.getTabs().add(copiesTab);
        mainTabPane.getSelectionModel().select(copiesTab);

        loadBookCopies(book);
    }

    private TextField createSearchField() {
        TextField searchField = new TextField();
        searchField.setPromptText("Keresés cím vagy szerző alapján...");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateTableView(newValue));
        return searchField;
    }

    private void copyToClipBoard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);

        showToast("Vágolapra másolva: " + text);
    }

    private void deleteBook(Book book) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Törlés megerősítése");
        alert.setHeaderText("Biztosan törölni szeretnéd ezt a könyvet és a hozzátartozó példányokat is?");
        alert.setContentText("A törlés véglegesen eltávolítja az adatbázisból.");

        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);

        okButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold");
        okButton.setText("Törlés");
        cancelButton.setText("Mégse");

        // Ha a felhasználó igent választ, akkor töröljük
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                MongoDBConnection.deleteBook(book.getId());
                loadBooks();  // Frissítjük a táblát
            }
        });
    }

    private void deleteCategory(Category category) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Törlés megerősítése");
        alert.setHeaderText("Biztosan törölni szeretnéd ezt a kategóriát?");
        alert.setContentText("A törlés véglegesen eltávolítja az adatbázisból.");

        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);

        okButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold");
        okButton.setText("Törlés");
        cancelButton.setText("Mégse");

        // Ha a felhasználó igent választ, akkor töröljük
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                MongoDBConnection.deleteCategory(category.getId());
                loadCategories();  // Frissítjük a táblát
            }
        });
    }

    public void deleteBookCopy(TableView<BookCopy> view, BookCopy copy) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Törlés megerősítése");
        alert.setHeaderText("Biztosan törölni szeretnéd ezt a könyv példányt?");
        alert.setContentText("A törlés véglegesen eltávolítja az adatbázisból.");

        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);

        okButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold");
        okButton.setText("Törlés");
        cancelButton.setText("Mégse");

        // Ha a felhasználó igent választ, akkor töröljük
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                MongoDBConnection.deleteBookCopy(copy.getId());
                loadBookCopies(copy.getBook());  // Frissítjük a táblát
            }
        });
    }

    private void openBookWindow(Stage parentStage, Book book) {
        Stage currentStage = new Stage();
        currentStage.setTitle(book == null ? "Könyv hozzáadása" : "Könyv szerkesztése");

        TextField titleField = new TextField(book != null ? book.getTitle() : "");
        titleField.setPromptText("Cím");

        TextField authorField = new TextField(book != null ? book.getAuthor() : "");
        authorField.setPromptText("Szerző");

        // Összes kategória lekérése
        ObservableList<Category> categories = MongoDBConnection.listCategories();
        if (book != null) {
            // Betöltjük adatbázisból a kategóriákat a használathoz
            book.setCategories(MongoDBConnection.getCategoriesByBookId(book.getId()));
        }
        ArrayList<Category> selectedCategories = book != null ? book.getCategories() : new ArrayList<>();

        VBox categoryCheckboxes = new VBox(5);
        Label categoryLabel = new Label("Kategóriák (" + selectedCategories.size() + ")");

        // Kategóriák checkbox
        AtomicInteger selectedCount = new AtomicInteger(selectedCategories.size());
        for (Category category : categories) {
            CheckBox checkBox = new CheckBox(category.getName());
            categoryCheckboxes.getChildren().add(checkBox);
            if (selectedCategories.contains(category)) {
                checkBox.setSelected(true);
            }

            checkBox.setOnAction(event -> {
                if (checkBox.isSelected()) {
                    selectedCount.incrementAndGet();
                } else {
                    selectedCount.decrementAndGet();
                }
                categoryLabel.setText(String.format("Kategóriák (%d)", selectedCount.get()));
            });
        }

        ScrollPane scrollPane = new ScrollPane(categoryCheckboxes);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(150);

        Button saveButton = new Button("Mentés");
        HBox saveButtonContainer = new HBox(saveButton);
        saveButtonContainer.setAlignment(Pos.CENTER);

        saveButton.setOnAction(e -> {
            String title = titleField.getText();
            String author = authorField.getText();

            if (!title.isEmpty() && !author.isEmpty()) {
                for (int i = 0; i < categories.size(); i++) {
                    Category category = categories.get(i);  // Kategória az aktuális index szerint
                    Node node = categoryCheckboxes.getChildren().get(i);  // Checkbox az aktuális index szerint

                    if (node instanceof CheckBox checkBox) {  // Ellenőrizzük, hogy a node valóban CheckBox típusú
                        if (checkBox.isSelected() && !selectedCategories.contains(category)) {
                            // Hozzáadjuk a listához
                            selectedCategories.add(category);
                        } else if (!checkBox.isSelected()) {
                            selectedCategories.remove(category);
                        }
                    }
                }

                if (book == null) {
                    Book newBook = new Book(null, title, author);
                    newBook.setCategories(selectedCategories);
                    MongoDBConnection.saveOrUpdateBook(newBook);
                } else {
                    book.setTitle(title);
                    book.setAuthor(author);
                    book.setCategories(selectedCategories);
                    MongoDBConnection.saveOrUpdateBook(book);
                }

                loadBooks();
                currentStage.close();
            } else {
                showAlert(Alert.AlertType.WARNING, "Hiba", "Minden mezőt ki kell tölteni!");
            }
        });

        VBox layout = new VBox(10,
                new Label("Cím:"),
                titleField,
                new Label("Szerző:"),
                authorField,
                categoryLabel,
                scrollPane,
                saveButtonContainer);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 350, 400);
        currentStage.setScene(scene);
        currentStage.initOwner(parentStage);
        currentStage.show();
    }

    private void openCategoryFormWindow(Stage parentStage, Category category) {
        Stage currentStage = new Stage();

        currentStage.setTitle(category == null ? "Új kategória hozzáadása" : "Kategória szerkesztése");

        // Kategória neve és leírása mezők
        TextField nameField = new TextField();
        nameField.setPromptText("Kategória neve");
        TextField descField = new TextField();
        descField.setPromptText("Leírás");

        // Ha van átadott kategória, akkor azokkal töltsük be
        if (category != null) {
            nameField.setText(category.getName());
            descField.setText(category.getDescription());
        }

        Button saveButton = new Button("Mentés");
        saveButton.setOnAction(e -> {
            String name = nameField.getText();
            String description = descField.getText();

            // Ha mindkét mező ki van töltve
            if (!name.isEmpty() && !description.isEmpty()) {
                if (category == null) {
                    // Új kategória hozzáadása
                    MongoDBConnection.addCategory(name, description);
                    loadCategories(); // frissítjük a kategóriákat
                } else {
                    // Kategória szerkesztése
                    category.setName(name);
                    category.setDescription(description);
                    MongoDBConnection.updateCategory(category); // Módosított kategória mentése
                    loadCategories(); // frissítjük a táblát
                }
                currentStage.close(); // Ablak bezárása
            } else {
                showAlert(Alert.AlertType.WARNING, "Hiba", "Minden mezőt ki kell tölteni!");
            }
        });

        VBox layout = new VBox(10, new Label(category == null ? "Új kategória" : "Kategória szerkesztése"), nameField, descField, saveButton);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 300, 200);
        currentStage.setScene(scene);
        currentStage.initOwner(parentStage);
        currentStage.show();
    }

    private void openBookCopyFormWindow(Stage parentStage, Book book, BookCopy existingCopy) {
        Stage curentStage = new Stage();

        String windowTitle = (existingCopy == null) ? "Új könyv példány hozzáadása" : "Könyv példány szerkesztése";
        curentStage.setTitle(windowTitle);

        TextField copyNumField = new TextField();
        copyNumField.setPromptText("Példány azonosító");

        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("Elérhető", "Kölcsönzött", "Karbantartás alatt");
        statusComboBox.setPromptText("Státusz");

        DatePicker borrowDatePicker = new DatePicker();
        borrowDatePicker.setPromptText("Kikölcsönözve");

        DatePicker returnDatePicker = new DatePicker();
        returnDatePicker.setPromptText("Visszavitel");

        TextField borrowerField = new TextField();
        borrowerField.setPromptText("Kölcsönző");

        TextField conditionField = new TextField();
        conditionField.setPromptText("Állapot");

        DatePicker printDatePicker = new DatePicker();
        printDatePicker.setPromptText("Nyomtatás dátuma");

        // Töltsük be az adatokat, ha meglévő példányt szerkesztünk
        if (existingCopy != null) {
            copyNumField.setText(existingCopy.getCopyNum());
            statusComboBox.setValue(existingCopy.getStatus());
            borrowDatePicker.setValue(existingCopy.getBorrowDate());
            returnDatePicker.setValue(existingCopy.getReturnDate());
            borrowerField.setText(existingCopy.getBorrower());
            conditionField.setText(existingCopy.getCondition());
            printDatePicker.setValue(existingCopy.getPrintDate());
        }

        Button saveButton = new Button("Mentés");
        HBox saveButtonContainer = new HBox(saveButton);
        saveButtonContainer.setAlignment(Pos.CENTER);

        saveButton.setOnAction(e -> {
            String copyNum = copyNumField.getText();
            String status = statusComboBox.getValue();
            LocalDate borrowDate = borrowDatePicker.getValue();
            LocalDate returnDate = returnDatePicker.getValue();
            String borrower = borrowerField.getText();
            String condition = conditionField.getText();
            LocalDate printDate = printDatePicker.getValue();

            if (copyNum.isEmpty() || status == null || printDate == null) {
                showAlert(Alert.AlertType.WARNING, "Hiba", "A példányazonosító, státusz illetve nyomtatási idő kitöltése kötelező!");
            } else {
                ObjectId exitingId = existingCopy != null ? existingCopy.getId() : null;
                BookCopy copy = new BookCopy(exitingId, book);

                copy.setCopyNum(copyNum);
                copy.setStatus(status);
                copy.setBorrowDate(borrowDate);
                copy.setReturnDate(returnDate);
                copy.setPrintDate(printDate);
                copy.setBorrower(borrower);
                copy.setCondition(condition);

                if (existingCopy == null) {
                    // Új példány hozzáadása
                    MongoDBConnection.addBookCopy(copy);
                } else {
                    // Meglévő példány frissítése
                    MongoDBConnection.updateBookCopy(copy);
                }
                loadBookCopies(book);
                curentStage.close();
            }
        });

        VBox layout = new VBox(10, new Label("Példány azonosító:"), copyNumField, new Label("Státusz:"), statusComboBox,
                new Label("Kikölcsönözve:"), borrowDatePicker, new Label("Visszavitel:"), returnDatePicker,
                new Label("Kölcsönző:"), borrowerField, new Label("Állapot:"), conditionField,
                new Label("Nyomtatás dátuma:"), printDatePicker, saveButtonContainer);
        layout.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(scrollPane, 300, 400);
        curentStage.setScene(scene);
        curentStage.initOwner(parentStage);
        curentStage.show();
    }

    private void createLoadingIndicator() {
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setMaxSize(50, 50);
    }

    private void createToastLabel() {
        toastLabel = new Label();
        toastLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-text-fill: white; -fx-padding: 10px;");
        toastLabel.setVisible(false);
    }

    private void showToast(String message) {
        if (toastLabel != null) {
            toastLabel.setText(message);
            toastLabel.setVisible(true);
            toastLabel.setOpacity(1);

            // Fade out animation
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(3), toastLabel);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> toastLabel.setVisible(false));
            fadeOut.play();
        }
    }

    private void loadBooks() {
        showToast("Könyvek betöltve");
        showLoadingIndicator(true);
        ObservableList<Book> books = MongoDBConnection.listBooks();
        ObservableList<Book> bookData = FXCollections.observableArrayList(books);
        bookTableView.setItems(bookData);
        showLoadingIndicator(false);
    }

    private void loadCategories() {
        showLoadingIndicator(true);
        ObservableList<Category> categories = MongoDBConnection.listCategories();
        ObservableList<Category> categoryData = FXCollections.observableArrayList(categories);
        categoryTableView.setItems(categoryData);
        showLoadingIndicator(false);
        showToast("Kategóriák betöltve");
    }

    private void loadBookCopies(Book book) {
        if (bookCopyTableView != null) {
            showLoadingIndicator(true);
            ObservableList<BookCopy> items = FXCollections.observableArrayList(MongoDBConnection.getBookCopiesByBook(book));
            bookCopyTableView.setItems(items);
            showLoadingIndicator(false);
            showToast("Könyv példányok betöltve");
        }
    }

    private void showLoadingIndicator(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateTableView(String query) {
        ObservableList<Book> filteredBooks = FXCollections.observableArrayList();
        for (Book book : MongoDBConnection.listBooks()) {
            if (book.getTitle().toLowerCase().contains(query.toLowerCase()) || book.getAuthor().toLowerCase().contains(query.toLowerCase())) {
                filteredBooks.add(book);
            }
        }
        bookTableView.setItems(filteredBooks);
    }

    private void refreshBookTable() {
        showLoadingIndicator(true);

        Task<ObservableList<Book>> task = new Task<>() {
            @Override
            protected ObservableList<Book> call() throws Exception {
                Thread.sleep(1000);
                return FXCollections.observableArrayList(MongoDBConnection.listBooks());
            }
        };

        task.setOnSucceeded(e -> {
            bookTableView.setItems(task.getValue()); // Az új adatok betöltése a táblába
            showLoadingIndicator(false);
        });

        task.setOnFailed(e -> {
            showLoadingIndicator(false); // Hibakezelés, ha a betöltés sikertelen
            showToast("Hiba történt az adatok frissítése közben!");
        });

        new Thread(task).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
