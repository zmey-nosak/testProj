package sample.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.java.Log;
import sample.pojo.MyFile;
import sample.utils.TabFileManager;

import java.io.File;
import java.nio.file.attribute.FileTime;

/**
 * Created by Stepan.Koledov on 10.04.2017.
 */
@Log
public class Controller {
    @FXML
    private TableView<MyFile> tableFileManager;
    @FXML
    private TableView<MyFile> tableFileManager1;
    @FXML
    private Label additionalInfoLbl;
    @FXML
    private Label additionalInfoLbl1;
    @FXML
    private Label directoryLabel;
    @FXML
    private Label directoryLabel1;
    @FXML
    private ComboBox<File> rootDisks;
    @FXML
    private ComboBox<File> rootDisks1;
    @FXML
    private Button btnAddNewDir;
    @FXML
    private Button btnAddNewDir1;
    @FXML
    private Button btnDel;
    @FXML
    private Button btnDel1;
    @FXML
    private Button btnCopy;
    @FXML
    private Button btnCopy1;
    @FXML
    private Button btnMoveTo;
    @FXML
    private Button btnMoveTo1;
    @FXML
    private Button btnSearch;
    @FXML
    private Button btnSearch1;
    @FXML
    private Button btnBack;
    @FXML
    private Button btnBack1;
    @FXML
    private Button btnStopSearching;
    @FXML
    private Button btnStopSearching1;
    @FXML
    private TextField txtSearchField;
    @FXML
    private TextField txtSearchField1;
    private final Object sync = new Object();
    @FXML
    private TableColumn<MyFile, String> nameColumn;
    @FXML
    private TableColumn<MyFile, String> nameColumn1;
    @FXML
    private TableColumn<MyFile, String> fileTypeColumn;
    @FXML
    private TableColumn<MyFile, String> fileTypeColumn1;
    @FXML
    private TableColumn<MyFile, FileTime> modifiedDateColumn;
    @FXML
    private TableColumn<MyFile, FileTime> modifiedDateColumn1;
    @FXML
    private TableColumn<MyFile, Long> sizeColumn;
    @FXML
    private TableColumn<MyFile, Long> sizeColumn1;
    @FXML
    private SplitPane splitPane;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ProgressIndicator progressIndicator1;
    private Stage stage;
    private TabFileManager leftTab;
    private TabFileManager rightTab;

    @FXML
    private void initialize() {
        progressIndicator.setVisible(false);
        progressIndicator1.setVisible(false);
        btnStopSearching.setVisible(false);
        btnStopSearching1.setVisible(false);
        leftTab = new TabFileManager(btnAddNewDir, btnDel, btnCopy, btnSearch, txtSearchField, rootDisks, directoryLabel, additionalInfoLbl, tableFileManager, nameColumn, fileTypeColumn, modifiedDateColumn, sizeColumn, btnStopSearching, progressIndicator, btnBack, btnMoveTo);
        rightTab = new TabFileManager(btnAddNewDir1, btnDel1, btnCopy1, btnSearch1, txtSearchField1, rootDisks1, directoryLabel1, additionalInfoLbl1, tableFileManager1, nameColumn1, fileTypeColumn1, modifiedDateColumn1, sizeColumn1, btnStopSearching1, progressIndicator1, btnBack1, btnMoveTo1);
        splitPane.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        });
    }

    public void setStageAndSetupListeners(Stage stage) {
        this.stage = stage;
        stage.setOnCloseRequest(event -> {
            closeThreads(leftTab.getThreadSearching());
            closeThreads(rightTab.getThreadSearching());
        });
    }

    private void closeThreads(Thread th) {
        if (th != null && th.isAlive()) {
            th.interrupt();
        }
    }
}


