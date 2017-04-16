package sample.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import sample.pojo.MyFile;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static java.io.File.separator;
import static java.util.Optional.*;

/**
 * Created by Stepan.Koledov on 10.04.2017.
 */
public class Controller {

    private ObservableList<MyFile> filesData = FXCollections.observableArrayList();
    private File currentDir;

    @FXML
    private TableView<MyFile> tableUsers;

    @FXML
    private Label directoryLabel;

    @FXML
    private ComboBox<File> rootDisks;

    @FXML
    private Button btnAddNewDir;

    @FXML
    private Button btnDel;

    @FXML
    private Button btnCopy;

    @FXML
    private TableColumn<MyFile, String> nameColumn;

    @FXML
    private TableColumn<MyFile, String> fileTypeColumn;

    @FXML
    private TableColumn<MyFile, FileTime> createdDateColumn;

    @FXML
    private TableColumn<MyFile, FileTime> modifiedDateColumn;

    @FXML
    private TableColumn<MyFile, Long> sizeColumn;

    private static final int BUFFER_SIZE = 1024;

    // инициализируем форму данными
    @FXML
    private void initialize() {
        tableUsers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableUsers.setRowFactory(tv -> {
            TableRow<MyFile> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    MyFile selectedRecord = tableUsers.getItems().get(row.getIndex());
                    if (!selectedRecord.getBasicFileAttributes().isDirectory()) {
                        return;
                        //TODO find method to open any file
                    } else {
                        initData(new File(selectedRecord.getPath().toString()));
                    }
                }
            });
            return row;
        });

        nameColumn.setCellValueFactory(new PropertyValueFactory<MyFile, String>("name"));
        fileTypeColumn.setCellValueFactory(new PropertyValueFactory<MyFile, String>("fileType"));
        createdDateColumn.setCellValueFactory(new PropertyValueFactory<MyFile, FileTime>("createdDate"));
        modifiedDateColumn.setCellValueFactory(new PropertyValueFactory<MyFile, FileTime>("modifiedDate"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<MyFile, Long>("size"));
        tableUsers.setItems(filesData);

        rootDisks.getItems().addAll(File.listRoots());
        rootDisks.getSelectionModel().selectedItemProperty().addListener((file, oldVal, newVal) -> initData(newVal));

        btnAddNewDir.setOnAction(action -> {
            TextInputDialog dialog = new TextInputDialog("walter");
            dialog.setTitle("New directory");
            dialog.setHeaderText("");
            dialog.setContentText("Please enter new directory:");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent())
                ofNullable(createDirectory(String.format("%s%s%s", currentDir, separator, result.get())))
                        .ifPresent(f -> filesData.add(f));
        });

        btnDel.setOnAction(action -> {
            ObservableList<MyFile> mf = tableUsers.getSelectionModel().getSelectedItems();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("");
            if (mf.size() > 1)
                alert.setContentText("Are you sure delete file: " + mf.get(0).getName() + "?");
            else
                alert.setContentText("Are you sure delete files?");
            Optional<ButtonType> result = alert.showAndWait();
            ExecutorService es1 = Executors.newFixedThreadPool(mf.size());
            List<Future<MyFile>> list = new ArrayList<>();
            if (result.get() == ButtonType.OK) {
                mf.forEach(file -> {
                            Callable<MyFile> callable = () -> {
                                recursiveDelete(file.getFile());
                                return file;
                            };
                            list.add(es1.submit(callable));
                        }
                );
                list.stream().parallel().forEach(future -> {
                    try {
                        MyFile mff = future.get();
                        synchronized (this) {
                            filesData.remove(mff);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                });
                es1.shutdown();
            } else {
            }
        });


        btnCopy.setOnAction(action ->

        {
            ObservableList<MyFile> fileList = tableUsers.getSelectionModel().getSelectedItems();
            TextInputDialog dialog = new TextInputDialog(currentDir.getPath());
            dialog.setTitle("New directory");
            dialog.setHeaderText("");
            dialog.setContentText("Copy file to directory:");
            dialog.showAndWait().ifPresent(inputDirectory ->
                    fileList.forEach(file -> copy(file.getFile(), new File(inputDirectory + separator + file.getName()))));
        });
    }

    private MyFile createDirectory(String dir) {
        File f = new File(dir);
        if (f.mkdir())
            return new MyFile(f);
        return null;
    }

    private void copy(File src, File dst) {
        if (src.isDirectory())
            copyDir(src, dst);
        else
            copyFile(src, dst);
    }

    private void recursiveDelete(File file) {
        System.out.println(file.getName());
        if (!file.exists())
            return;
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                recursiveDelete(f);
            }
        }
        file.delete();


    }

    private Optional<ButtonType> showConfirmationWindowToReplace() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("");
        alert.setContentText("File with the same name already exist. Do you want replace?");
        return alert.showAndWait();
    }

    private boolean copyDir(final File srcFile, final File dstFile) {
        if (srcFile.exists() && srcFile.isDirectory()) {
            if (!dstFile.exists())
                dstFile.mkdir();
            File nextSrcFilename, nextDstFilename;
            for (String filename : srcFile.list()) {
                nextSrcFilename = new File(srcFile.getAbsolutePath()
                        + separator + filename);
                nextDstFilename = new File(dstFile.getAbsolutePath()
                        + separator + filename);
                if (nextSrcFilename.isDirectory()) {
                    copyDir(nextSrcFilename, nextDstFilename);
                } else {
                    copyFile(nextSrcFilename, nextDstFilename);
                }
            }
            return true;
        }
        return false;
    }

    private boolean copyFile(final File srcFile, final File dstFile) {
        if (srcFile.exists() && srcFile.isFile() && !dstFile.exists()) {
            try {
                Files.copy(srcFile.toPath(), dstFile.toPath());
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (dstFile.exists()) {
            return copyWithReplacement(srcFile, dstFile);
        }
        return false;
    }


    private boolean copyWithReplacement(File srcFile, File dstFile) {
        Optional<ButtonType> buttonType = showConfirmationWindowToReplace();
        if (buttonType.isPresent() && buttonType.get() == ButtonType.OK) {
            try {
                Files.copy(srcFile.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    public void initData(File file) {
        currentDir = file;
        directoryLabel.setText(currentDir.toPath().toString());
        filesData.clear();
        if (file.getParentFile() != null) {
            MyFile dir = new MyFile(file.getParentFile());
            dir.setName("..");
            filesData.add(dir);
        }
        File[] fl = file.listFiles();
        if (fl != null)
            Arrays.stream(fl).map(MyFile::new).forEach(f -> filesData.add(f));
    }


}

