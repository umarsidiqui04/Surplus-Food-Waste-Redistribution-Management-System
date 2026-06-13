module com.sfwrms.sfwrms {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.sfwrms.sfwrms to javafx.fxml;
    exports com.sfwrms.sfwrms;
}