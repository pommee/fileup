module com.fileup {
    requires javafx.controls;
    requires javafx.fxml;
            
        requires org.controlsfx.controls;
                    requires org.kordamp.ikonli.javafx;
    requires java.desktop;
    requires javafx.swing;

    opens com.fileup to javafx.fxml;
    exports com.fileup;
}