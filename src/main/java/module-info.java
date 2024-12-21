module org.example.adatbazis {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.mongodb.bson;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.driver.core;
    requires java.desktop;

    opens org.example.adatbazis to javafx.fxml;
    exports org.example.adatbazis;
    exports org.example.adatbazis.models;
    opens org.example.adatbazis.models to javafx.fxml;
}