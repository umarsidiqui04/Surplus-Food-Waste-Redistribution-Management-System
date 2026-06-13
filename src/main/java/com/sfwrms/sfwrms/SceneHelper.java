package com.sfwrms.sfwrms;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneHelper {
    public static void switchTo(Node node, String fxml, int w, int h) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneHelper.class.getResource(fxml));
        Parent root = loader.load();
        Stage stage = (Stage) node.getScene().getWindow();
        stage.setScene(new Scene(root, w, h));
        stage.centerOnScreen();
    }
}
