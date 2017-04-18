package sample.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import lombok.extern.java.Log;
import sample.pojo.MyFile;
import sample.utils.Find;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static java.util.Optional.ofNullable;

/**
 * Created by Stepan.Koledov on 10.04.2017.
 */
@Log
public class Controller {


    private ObservableList<MyFile> filesData = FXCollections.observableArrayList();
    private volatile File currentDir;
    private final Object sync = new Object();
    private PseudoClass up = PseudoClass.getPseudoClass("up");

    @FXML
    private TableView<MyFile> tableFileManager;

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
    private Button btnSearch;

    @FXML
    private TextField txtSearchField;


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

    // инициализируем форму данными
    @FXML
    private void initialize() {
        startDaemon();
        tableInit();
        cmbBoxInit();
        buttonsInit();
    }

    private void startDaemon() {
        Thread th = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (sync) {
                    if (currentDir != null) {

                        List<MyFile> fl = Arrays.stream(currentDir.listFiles())
                                .map(MyFile::new)
                                .collect(Collectors.toList());

                        fl.removeAll(filesData);

                        fl.stream()
                                .peek(f -> log.info("ADDED TO LIST:" + f.getName()))
                                .forEach(f -> filesData.add(f));

                        filesData.stream()
                                .filter(f -> (!f.getName().equals("..")) && (!f.getFile().exists()))
                                .collect(Collectors.toList())
                                .stream()
                                .peek(f -> log.info("DELETED FROM LIST:" + f.getName()))
                                .forEach(f -> filesData.remove(f));

                        tableFileManager.sort();
                    }
                }
            }
        });
        th.setDaemon(true);
        th.start();
    }

    private void tableInit() {
        tableFileManager.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableFileManager.setRowFactory(tv -> {
            TableRow<MyFile> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    MyFile selectedRecord = tableFileManager.getItems().get(row.getIndex());
                    if (!selectedRecord.getBasicFileAttributes().isDirectory()) {
                        return;
                        //TODO find method to open any file
                    } else {
                        initData(new File(selectedRecord.getPath().toString()));
                    }
                }
            });

            row.setOnDragExited(event -> {
                log.info("dragOverExited:" + row.getItem().getName());
                row.pseudoClassStateChanged(up, false);
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                log.info("dragOverDetected:" + row.getItem().getName());
                if (db.hasFiles()) {
                    row.pseudoClassStateChanged(up, true);
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    event.consume();
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasFiles()) {
                    List<File> fileList = db.getFiles();
                    long cnt = fileList.stream().filter(f -> f.getPath().equals(row.getItem().getFile().getPath())).count();
                    if (cnt == 0) {
                        if (event.getAcceptedTransferMode() == TransferMode.MOVE) {
                            asyncCopy(fileList, row.getItem().getPath().toString(), true);
                        } else {
                            asyncCopy(fileList, row.getItem().getPath().toString(), false);
                        }
                    }
                    event.setDropCompleted(true);
                    tableFileManager.getSelectionModel().select(row.getIndex());
                    event.consume();
                }
            });
            return row;
        });

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        fileTypeColumn.setCellValueFactory(new PropertyValueFactory<>("fileType"));
        createdDateColumn.setCellValueFactory(new PropertyValueFactory<>("createdDate"));
        modifiedDateColumn.setCellValueFactory(new PropertyValueFactory<>("modifiedDate"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        tableFileManager.setItems(filesData);

        tableFileManager.setOnDragDetected(mouseEvent -> {
                    log.info("setOnDragDetected");
                    Dragboard dragBoard = tableFileManager.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putFiles(tableFileManager
                            .getSelectionModel()
                            .getSelectedItems()
                            .stream()
                            .map(MyFile::getFile)
                            .collect(Collectors.toList()));
                    dragBoard.setContent(content);
                }
        );

    }

    private void cmbBoxInit() {
        rootDisks.getItems().addAll(File.listRoots());
        rootDisks.getSelectionModel().
                selectedItemProperty().
                addListener((file, oldVal, newVal) -> initData(newVal));
    }

    private void buttonsInit() {
        btnAddNewDir.setOnAction(action ->
        {
            TextInputDialog dialog = new TextInputDialog("walter");
            dialog.setTitle("New directory");
            dialog.setHeaderText("");
            dialog.setContentText("Please enter new directory:");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent())
                ofNullable(createDirectory(String.format("%s%s%s", currentDir, separator, result.get())));
        });

        btnDel.setOnAction(action ->
        {
            synchronized (sync) {
                List<File> filesList = tableFileManager.getSelectionModel().getSelectedItems().stream().map(MyFile::getFile).collect(Collectors.toList());
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmation Dialog");
                alert.setHeaderText("");
                if (filesList.size() > 1)
                    alert.setContentText("Are you sure delete file: " + filesList.get(0).getName() + "?");
                else
                    alert.setContentText("Are you sure delete files?");
                alert.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.OK) asyncDelete(filesList);
                });
            }
        });

        btnCopy.setOnAction(action ->
        {
            List<File> fileList = tableFileManager
                    .getSelectionModel()
                    .getSelectedItems()
                    .stream()
                    .map(MyFile::getFile)
                    .collect(Collectors.toList());
            TextInputDialog dialog = new TextInputDialog(currentDir.getPath());
            dialog.setTitle("New directory");
            dialog.setHeaderText("");
            dialog.setContentText("Copy file to directory:");
            dialog.showAndWait()
                    .ifPresent(inputDirectory -> asyncCopy(fileList, inputDirectory, false));
        });

        btnSearch.setOnAction(action -> {
            Find.Finder finder = new Find.Finder(txtSearchField.getText());
            try {
                Files.walkFileTree(currentDir.toPath(), finder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            tableFileManager.setItems(finder.done());

        });
    }

    private void asyncDelete(List<File> filesList) {
        ExecutorService es1 = Executors.newFixedThreadPool(filesList.size());
        List<Future<File>> list = new ArrayList<>();
        filesList.forEach(file -> {
                    Callable<File> callable = () -> {
                        recursiveDelete(file);
                        return file;
                    };
                    list.add(es1.submit(callable));
                }
        );
        es1.shutdown();
    }

    private void asyncCopy(List<File> fileList, String inputDirectory, boolean flagDelete) {
        ExecutorService es1 = Executors.newFixedThreadPool(fileList.size());
        List<Future<File>> list = new ArrayList<>();
        fileList.forEach(file -> {
                    Callable<File> callable = () -> {
                        copy(file, new File(inputDirectory + separator + file.getName()));
                        return file;
                    };
                    list.add(es1.submit(callable));
                    if (flagDelete) {
                        list.stream().parallel().forEach(f -> {
                            try {
                                recursiveDelete(f.get());
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
        );
        es1.shutdown();
    }

    private MyFile createDirectory(String dir) {
        File f = new File(dir);
        if (f.mkdir())
            return new MyFile(f);
        return null;
    }

    private void copy(File src, File dst) {
        log.info("copy file " + src.getName() + " to " + dst.getPath());
        if (src.isDirectory())
            copyDir(src, dst);
        else
            copyFile(src, dst);
    }

    private void recursiveDelete(File file) {
        log.info("deleting file " + file.getName() + " ...");
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

    private void initData(File file) {
        synchronized (sync) {
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

}

