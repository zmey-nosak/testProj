package sample.controller;

import com.sun.glass.ui.View;
import com.sun.xml.internal.ws.api.server.Container;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.SneakyThrows;
import org.joda.time.DateTime;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.MouseEvent;
import sample.pojo.MyFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;

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
                    MyFile selectedRecord = (MyFile)tableUsers.getItems().get(row.getIndex());
                    initData(new File(selectedRecord.getPath().toString()));
                }
            });
            return row;
        });
        initData(new File("C:\\"));


        // устанавливаем тип и значение которое должно хранится в колонке
        nameColumn.setCellValueFactory(new PropertyValueFactory<MyFile, String>("name"));
        createdDateColumn.setCellValueFactory(new PropertyValueFactory<MyFile, FileTime>("createdDate"));
        modifiedDateColumn.setCellValueFactory(new PropertyValueFactory<MyFile, FileTime>("modifiedDateColumn"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<MyFile, Long>("size"));

        // заполняем таблицу данными
        tableUsers.setItems(filesData);
    }

    // подготавливаем данные для таблицы
    // вы можете получать их с базы данных


    public void initData(File file) {
        filesData.clear();
        if(file.getParentFile()!=null)
            filesData.add(new MyFile());
        Arrays.asList(file.listFiles()).stream().map(f -> {
            return new MyFile(f);
        }).forEach(e -> filesData.add(e));
    }


}

