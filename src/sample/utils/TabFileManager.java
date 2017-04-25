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
    private Thread threadSearching;
    private ProgressIndicator progressIndicator;
    private Label lblProcess;
    private ProgressIndicator processIndicator;
    private Button btnStop;

    private Object sync = new Object();
    private ExecutorService es1;
    final int THREAD_COUNT = 5;

    public Thread getThreadSearching() {
        return threadSearching;
    }

    public TabFileManager(
            Button btnAddNewDir,
            Button btnDel,
            Button btnCopy,
            Button btnSearch,
            TextField txtSearchField,
            ComboBox<File> rootDisks,
            Label directoryLabel,
            Label additionalInfoLbl,
            TableView<MyFile> tableFileManager,
            TableColumn<MyFile, String> nameColumn,
            TableColumn<MyFile, String> fileTypeColumn,
            TableColumn<MyFile, FileTime> modifiedDateColumn,
            TableColumn<MyFile, Long> sizeColumn,
            Button btnStopSearching,
            ProgressIndicator progressIndicator,
            Button btnBack,
            Button btnMoveTo,
            Label lblProcess,
            ProgressIndicator processIndicator,
            Button btnStop
    ) {
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
        this.btnStop = btnStop;
        this.processIndicator = processIndicator;
        this.lblProcess = lblProcess;
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
                            prepareCopyingOrMoving(fileList, row.getItem().getFile(), true);
                        } else {
                            prepareCopyingOrMoving(fileList, row.getItem().getFile(), false);
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
                            .filter(f -> !f.isHead())
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
                    alert.setContentText("Are you sure delete fileNames?");
                alert.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.OK) startDeleting(filesList);
                });
            }
        });

        btnCopyTo.setOnAction(action -> copyOrMoveByButton(true));

        btnMoveTo.setOnAction(action -> copyOrMoveByButton(false));

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

        btnStop.setOnAction(event -> {
            es1.shutdownNow();
            lblProcess.setVisible(false);
            processIndicator.setVisible(false);
            btnStop.setVisible(false);
        });

    }

    private void copyOrMoveByButton(boolean isCopy) {
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
                    File destinationFolder = new File(inputDirectory);
                    if (destinationFolder.exists())
                        prepareCopyingOrMoving(fileList, destinationFolder, !isCopy);
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

    private MyFile createDirectory(String dir) {
        File f = new File(dir);
        if (f.mkdir()) {
            tableFileManager.sort();
            return new MyFile(f);
        }
        return null;
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

    private Optional<ButtonType> showConfirmationWindowToReplace(File file) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("");
        String message = "";
        if (file.isDirectory()) {
            message = "The destination already contain folder named %s. Do you want to merge this folder?";
        } else {
            message = "The destination already contain file named %s. Do you want to replace this file?";
        }
        alert.setContentText(String.format(message, file.getName()));
        return alert.showAndWait();
    }

    private File recursiveCreateFolder(String string) {
        if (Files.exists(Paths.get(string)))
            return recursiveCreateFolder(string + "_copy");
        else {
            File f = new File(string);
            f.mkdir();
            return f;
        }
    }

    private void copyContentFolder(File srcFileFolder, File dstFileFolder) {
        File nextSrcFilename, nextDstFilename;
        Stack<File> files = new Stack<>();
        File[] fileNames = srcFileFolder.listFiles();
        if (fileNames != null) {
            files.addAll(Arrays.asList(fileNames));
            while (!files.isEmpty()) {
                File file = files.pop();
                nextSrcFilename = new File(srcFileFolder.getAbsolutePath()
                        + separator + file.getName());
                nextDstFilename = new File(dstFileFolder.getAbsolutePath()
                        + separator + file.getName());
                if (nextSrcFilename.isDirectory()) {
                    copyDir(nextSrcFilename, nextDstFilename, dstFileFolder);
                } else {
                    copyFile(nextSrcFilename, nextDstFilename);
                }
            }
        }
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

    private void prepareCopyingOrMoving(List<File> src, File destinationFolder, boolean isCopy) {
        List<File> duplicates = getDuplicates(src, destinationFolder);
        if (!duplicates.isEmpty()) {
            List<File> confirmFiles = getConfirmFiles(duplicates);
            src.removeAll(duplicates);
            src.addAll(confirmFiles);
        }
        startCopying(src, destinationFolder, isCopy);
    }

    private List<File> getConfirmFiles(List<File> duplicates) {
        return duplicates.stream().filter(file -> {
            Optional<ButtonType> buttonType = showConfirmationWindowToReplace(file);
            return buttonType.isPresent() || buttonType.get() == ButtonType.OK;
        }).collect(Collectors.toList());


    }

    private List<File> getDuplicates(List<File> files, File destinationFolder) {
        File[] content = destinationFolder.listFiles();
        if (content != null) {
            List<String> contentList = Arrays.stream(content).map(File::getName).collect(Collectors.toList());
            return files.stream().filter(file -> contentList.contains(file.getName())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private void startCopying(List<File> fileList, File destinationFolder, boolean flagDelete) {

        this.es1 = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<File>> tasks = new ArrayList<>();
        lblProcess.setVisible(true);
        processIndicator.setVisible(true);
        btnStop.setVisible(true);
        fileList.forEach(file -> {
                    Callable<File> callable = () -> {
                        //Thread.sleep(2000); imitation of hard work
                        File dstFile = new File(destinationFolder.getAbsoluteFile() + separator + file.getName());
                        copy(file, dstFile, destinationFolder);
                        if (flagDelete) recursiveDelete(file);
                        return null;
                    };
                    tasks.add(es1.submit(callable));
                }
        );
        es1.shutdown();
        Thread th = new Thread(() -> {
            process(tasks);
        });
        th.start();
    }

    private void copy(File src, File dst, File destinationFolder) {
        if (src.isDirectory())
            copyDir(src, dst, destinationFolder);
        else
            copyFile(src, dst);
    }

    private void copyDir(File srcFileFolder, File dstFileFolder, File destinationFolder) {
        if (!dstFileFolder.exists())
            dstFileFolder.mkdir();
        else if (destinationFolder.toPath().equals(currentDir.toPath()))
            dstFileFolder = recursiveCreateFolder(dstFileFolder.getPath());
        copyContentFolder(srcFileFolder, dstFileFolder);
    }

    private void copyFile(final File srcFile, final File dstFile) {
        try {
            Files.copy(srcFile.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startDeleting(List<File> filesList) {
        btnStop.setVisible(true);
        lblProcess.setVisible(true);
        processIndicator.setVisible(true);
        synchronized (sync) {

            ExecutorService es1 = Executors.newFixedThreadPool(THREAD_COUNT);
            List<Future<File>> tasks = new ArrayList<>();
            filesList.forEach(file -> {
                Callable<File> callable = () -> {
                    //Thread.sleep(2000); imitation of hard work
                    recursiveDelete(file);
                    return file;
                };
                tasks.add(es1.submit(callable));
            });
            es1.shutdown();
            Thread th = new Thread(() -> {
                process(tasks);
            });
            th.start();
        }
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

    private void process(List<Future<File>> tasks) {
        while (tasks.stream().filter(task -> !task.isDone()).count() > 0) ;
        btnStop.setVisible(false);
        lblProcess.setVisible(false);
        processIndicator.setVisible(false);
    }
}
