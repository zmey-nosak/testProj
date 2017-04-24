package sample.utils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import lombok.extern.java.Log;
import sample.pojo.MyFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

/**
 * Created by Stepan.Koledov on 21.04.2017.
 */
@Log
public class TabFileManager {
    private Button btnAddNewDir;
    private Button btnDel;
    private Button btnCopyTo;
    private Button btnSearch;
    private Button btnBack;
    private Button btnStopSearching;
    private Button btnMoveTo;
    private TextField txtSearchField;
    private ComboBox<File> rootDisks;
    private Label directoryLabel;
    private Label additionalInfoLbl;
    private TableView<MyFile> tableFileManager;
    private volatile File currentDir;
    private ObservableList<MyFile> filesData = FXCollections.observableArrayList();
    private PseudoClass up = PseudoClass.getPseudoClass("up");
    private TableColumn<MyFile, String> nameColumn;
    private TableColumn<MyFile, String> fileTypeColumn;
    private TableColumn<MyFile, FileTime> modifiedDateColumn;
    private TableColumn<MyFile, Long> sizeColumn;

    public Thread getThreadSearching() {
        return threadSearching;
    }

    private Thread threadSearching;
    private ProgressIndicator progressIndicator;


    Object sync = new Object();

    public TabFileManager(Button btnAddNewDir, Button btnDel, Button btnCopy, Button btnSearch, TextField txtSearchField, ComboBox<File> rootDisks, Label directoryLabel, Label additionalInfoLbl, TableView<MyFile> tableFileManager, TableColumn<MyFile, String> nameColumn, TableColumn<MyFile, String> fileTypeColumn, TableColumn<MyFile, FileTime> modifiedDateColumn, TableColumn<MyFile, Long> sizeColumn, Button btnStopSearching, ProgressIndicator progressIndicator, Button btnBack, Button btnMoveTo) {
        this.btnAddNewDir = btnAddNewDir;
        this.btnDel = btnDel;
        this.btnCopyTo = btnCopy;
        this.btnSearch = btnSearch;
        this.txtSearchField = txtSearchField;
        this.rootDisks = rootDisks;
        this.directoryLabel = directoryLabel;
        this.additionalInfoLbl = additionalInfoLbl;
        this.tableFileManager = tableFileManager;
        this.nameColumn = nameColumn;
        this.fileTypeColumn = fileTypeColumn;
        this.modifiedDateColumn = modifiedDateColumn;
        this.sizeColumn = sizeColumn;
        this.btnStopSearching = btnStopSearching;
        this.progressIndicator = progressIndicator;
        this.btnBack = btnBack;
        this.btnMoveTo = btnMoveTo;
        tableInit();
        cmbBoxInit();
        buttonsInit();
        startDaemon();
    }

    private void tableInit() {
        tableFileManager.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableFileManager.setRowFactory(tv -> {
            TableRow<MyFile> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    MyFile selectedRecord = tableFileManager.getItems().get(row.getIndex());
                    if (!selectedRecord.getBasicFileAttributes().isDirectory()) {
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
        modifiedDateColumn.setCellValueFactory(new PropertyValueFactory<>("modifiedDate"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));

        tableFileManager.setItems(filesData);

        tableFileManager.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            Platform.runLater(() -> {
                if (newSelection != null && newSelection.getFile() != null) {
                    log.info(String.format("selection %s", newSelection.getFile().toString()));
                    additionalInfoLbl.setText(String.format("Creation date:%s; Modified date:%s", newSelection.getCreatedDate(), newSelection.getModifiedDate()));
                } else {
                    additionalInfoLbl.setText("");
                }
            });
        });

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

        tableFileManager.sortPolicyProperty().set(t -> {
            Comparator<MyFile> comparator = (r1, r2)
                    -> r1.isHead() ? -1 //rowTotal at the bottom
                    : r2.isHead() ? 1 //rowTotal at the bottom
                    : t.getComparator() == null ? 0 //no column sorted: don't change order
                    : t.getComparator().compare(r1, r2); //columns are sorted: sort accordingly
            FXCollections.sort(tableFileManager.getItems(), comparator);
            return true;
        });
    }

    private void cmbBoxInit() {
        rootDisks.getItems().clear();
        rootDisks.getItems().addAll(File.listRoots());
        rootDisks.getSelectionModel().
                selectedItemProperty().
                addListener((file, oldVal, newVal) -> initData(newVal));
        rootDisks.getSelectionModel().select(0);
    }

    private void buttonsInit() {
        btnAddNewDir.setOnAction(action ->
        {
            TextInputDialog dialog = new TextInputDialog("New folder");
            dialog.setTitle("New folder");
            dialog.setHeaderText("");
            dialog.setContentText("Please enter new folder's name:");
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

        btnCopyTo.setOnAction(action -> processCopyOrMoveByButton(true));

        btnMoveTo.setOnAction(action -> processCopyOrMoveByButton(false));

        btnSearch.setOnAction(action -> {
            if (!txtSearchField.getText().isEmpty()) {
                progressIndicator.setVisible(true);
                btnStopSearching.setVisible(true);
                threadSearching = new Thread(this::searchingProcess);
                threadSearching.start();
            }
        });

        btnStopSearching.setOnAction(event -> {
            threadSearching.interrupt();
            progressIndicator.setVisible(false);
            btnStopSearching.setVisible(false);
        });

        btnBack.setOnAction(event -> {
            if (threadSearching != null && threadSearching.isAlive()) {
                progressIndicator.setVisible(false);
                btnStopSearching.setVisible(false);
                initData(currentDir);
            } else {
                File file = currentDir.getParentFile();
                if (file != null) {
                    initData(file);
                } else {
                    initData(currentDir);
                }
            }
        });
    }

    private void processCopyOrMoveByButton(boolean isCopy) {
        List<File> fileList = tableFileManager
                .getSelectionModel()
                .getSelectedItems()
                .stream()
                .map(MyFile::getFile)
                .collect(Collectors.toList());
        TextInputDialog dialog = new TextInputDialog(currentDir.getPath());
        dialog.setTitle("");
        dialog.setHeaderText("");
        dialog.setContentText(String.format("%s file to directory:", isCopy ? "Copy" : "Move"));
        dialog.showAndWait()
                .ifPresent(inputDirectory -> {
                    if (Files.exists(Paths.get(inputDirectory)))
                        asyncCopy(fileList, inputDirectory, !isCopy);
                });
    }


    private void searchingProcess() {
        synchronized (sync) {
            filesData.clear();
            Find.Finder finder = new Find.Finder(txtSearchField.getText());
            try {
                Files.walkFileTree(currentDir.toPath(), finder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            tableFileManager.setItems(finder.done());
        }
    }

    private void asyncDelete(List<File> filesList) {
        synchronized (sync) {
            ExecutorService es1 = Executors.newFixedThreadPool(5);
            List<Callable<File>> tasks = new LinkedList<>();
            filesList.forEach(file -> {
                        tasks.add(() -> {
                            recursiveDelete(file);
                            return file;
                        });
                    }
            );
            try {
                es1.invokeAll(tasks).stream().map(f -> {
                    try {
                        log.info(String.format("%s process of deleting", f.get().getName()));
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).forEach(r -> log.info(String.format("%s was deleted", r.getName())));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("all files deleted");
            es1.shutdown();


        }
    }


    private void asyncCopy(List<File> fileList, String inputDirectory, boolean flagDelete) {
        ExecutorService es1 = Executors.newFixedThreadPool(5);
        final Result[] result = {null};
        List<Future<Result>> tasks = new ArrayList<>();
        fileList.forEach(file -> {
                    Callable<Result> callable = () -> {
                        File dstFile = new File(inputDirectory + separator + file.getName());
                        return copy(file, dstFile, inputDirectory);
                    };
                    tasks.add(es1.submit(callable));
                }
        );
        tasks.forEach(f -> {
            try {
                if (f.get().resultCode == ResultCode.ALREADY_EXIST) {
                    Optional<ButtonType> buttonType = showConfirmationWindowToReplace();
                    if (buttonType.isPresent() && buttonType.get() == ButtonType.OK) {
                        result[0] = f.get();

                    }
                }
                if (flagDelete) {
                    recursiveDelete(f.get().getSourceFile());
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        es1.shutdown();
        if (result[0] != null) {
            asyncCopy(result[0].fileNames.stream().map(File::new).collect(Collectors.toList()), result[0].getDstFile().toString(), false);
        }
    }

    private MyFile createDirectory(String dir) {
        File f = new File(dir);
        if (f.mkdir()) {
            tableFileManager.sort();
            return new MyFile(f);
        }
        return null;
    }

    private Result copy(File src, File dst, String destinationFolder) {
        log.info("copy file " + src.getName() + " to " + dst.getPath());
        if (src.isDirectory())
            return copyDir(src, dst, destinationFolder);
        else return new Result(dst, src, null, ResultCode.DONE);
        //copyFile(src, dst);
    }

    private void recursiveDelete(File file) {
        log.info("deleting file " + file.getName() + " ...");
        if (!file.exists())
            return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    recursiveDelete(f);
                }
            }
        }
        if (!file.delete()) {
            log.info(String.format("Something wrong with deleting file %s", file.toPath()));
        }
    }

    private void startDaemon() {
        Thread th1 = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (sync) {
                    if (currentDir != null) {
                        File[] files = currentDir.listFiles();
                        if (files != null) {
                            List<MyFile> fl = Arrays.stream(files)
                                    .map(MyFile::new)
                                    .collect(Collectors.toList());

                            fl.removeAll(filesData);

                            fl.stream()
                                    .peek(f -> log.info("ADDED TO LIST:" + f.getName()))
                                    .forEach(filesData::add);

                            filesData.stream()
                                    .filter(f -> (!f.getName().equals("..")) && (!f.getFile().exists()))
                                    .collect(Collectors.toList())
                                    .stream()
                                    .peek(f -> log.info("DELETED FROM LIST:" + f.getName()))
                                    .forEach(filesData::remove);
                            tableFileManager.sort();
                        }
                    }
                }
            }
        });
        th1.setDaemon(true);
        th1.start();
    }

    private Optional<ButtonType> showConfirmationWindowToReplace() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("");
        alert.setContentText("File with the same name already exist. Do you want replace?");
        return alert.showAndWait();
    }

    private File recursiveCreateDir(String string) {
        if (Files.exists(Paths.get(string)))
            return recursiveCreateDir(string + "_copy");
        else {
            File f = new File(string);
            f.mkdir();
            return f;
        }
    }

    private Result copyContainsFolder(File srcFileFolder, File dstFileFolder) {
        File nextSrcFilename, nextDstFilename;
        Stack<String> files = new Stack<>();
        String[] fileNames = srcFileFolder.list();
        if (fileNames != null) {
            files.addAll(Arrays.asList(fileNames));
            while (!files.isEmpty()) {
                String filename = files.pop();

                nextSrcFilename = new File(srcFileFolder.getAbsolutePath()
                        + separator + filename);
                nextDstFilename = new File(dstFileFolder.getAbsolutePath()
                        + separator + filename);
                if (nextSrcFilename.isDirectory()) {
                    Result res = copyDir(nextSrcFilename, nextDstFilename, dstFileFolder.getAbsolutePath());
                    if (res.resultCode == ResultCode.ALREADY_EXIST) {
                        res.fileNames = files;
                        return res;
                    }
                } else {
                    //  return copyFile(nextSrcFilename, nextDstFilename);
                }
            }
        }
        return new Result(dstFileFolder, srcFileFolder, null, ResultCode.DONE);
    }

    private Result copyDir(File srcFileFolder, File dstFileFolder, String destinationFolder) {
        if (!dstFileFolder.exists())
            dstFileFolder.mkdir();
        else {
            if (destinationFolder.equals(currentDir.getPath())) {
                dstFileFolder = recursiveCreateDir(dstFileFolder.getPath());
            } else {
                return new Result(dstFileFolder, srcFileFolder, null, ResultCode.ALREADY_EXIST);
            }
        }
        return copyContainsFolder(srcFileFolder, dstFileFolder);
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
        if (file.canRead()) {
            synchronized (sync) {
                currentDir = file;
                directoryLabel.setText(currentDir.toPath().toString());
                filesData.clear();
                if (file.getParentFile() != null) {
                    MyFile dir = new MyFile(file.getParentFile());
                    dir.setName("..");
                    dir.setHead(true);
                    filesData.add(dir);
                }
                File[] fl = file.listFiles();
                if (fl != null)
                    Arrays.stream(fl).map(MyFile::new).forEach(f -> filesData.add(f));
                tableFileManager.setItems(filesData);
                tableFileManager.sort();

            }
        } else {
            log.info(String.format("cant read directory %s", file.toPath()));
        }
    }

}
