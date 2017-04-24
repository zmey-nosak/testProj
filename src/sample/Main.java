package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sample.controller.Controller;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("views/main.fxml"));
        //Parent root = FXMLLoader.load(getClass().getResource("views/main.fxml"));
        Parent root=loader.load();
        primaryStage.setTitle("File manager");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        Controller controller = loader.getController();
        controller.setStageAndSetupListeners(primaryStage);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
