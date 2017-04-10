package sample.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import sample.pojo.MyFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Optional;

/**
 * Created by Stepan.Koledov on 10.04.2017.
 */
public class Controller {

    private ObservableList<MyFile> filesData = FXCollections.observableArrayList();

    @FXML
    private TableView<MyFile> tableUsers;

    @FXML
    private TableColumn<MyFile, String> nameColumn;

    @FXML
    private TableColumn<MyFile, String> isDirColumn;

    @FXML
    private TableColumn<MyFile, FileTime> createdDateColumn;

    @FXML
    private TableColumn<MyFile, FileTime> modifiedDateColumn;

    @FXML
    private TableColumn<MyFile, Long> sizeColumn;


    // инициализируем форму данными
    @FXML
    private void initialize() {
        tableUsers.setRowFactory(tv -> {
            TableRow<MyFile> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    MyFile selectedRecord = (MyFile) tableUsers.getItems().get(row.getIndex());
                    if (selectedRecord.getIsDir().equals("D")) {
                        initData(new File(selectedRecord.getPath().toString()));
                    } else {
                        Runtime r = Runtime.getRuntime();
                        try {
                            r.exec(selectedRecord.getPath().toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            return row;
        });

        initData(new File("C:\\"));

        nameColumn.setCellValueFactory(new PropertyValueFactory<MyFile, String>("name"));
        isDirColumn.setCellValueFactory(new PropertyValueFactory<MyFile, String>("isDir"));
        createdDateColumn.setCellValueFactory(new PropertyValueFactory<MyFile, FileTime>("createdDate"));
        modifiedDateColumn.setCellValueFactory(new PropertyValueFactory<MyFile, FileTime>("modifiedDateColumn"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<MyFile, Long>("size"));
        tableUsers.setItems(filesData);
    }

    public void initData(File file) {
        filesData.clear();
        if (file.getParentFile() != null) {
            MyFile dir = new MyFile(file.getParentFile());
            dir.setName("");
            filesData.add(dir);
        }
        Arrays.asList(file.listFiles()).stream().map(f -> {
            return new MyFile(f);
        }).forEach(e -> filesData.add(e));
    }


}

