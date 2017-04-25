package sample.pojo;

import lombok.SneakyThrows;
import sample.utils.MyTimeFormat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Logger;

/**
 * Created by Stepan.Koledov on 10.04.2017.
 */
public class MyFile {
    private static final Logger log = Logger.getLogger(MyFile.class.getName());
    private String name;
    private File file;
    private Path path;
    private MyTimeFormat createdDate;
    private MyTimeFormat modifiedDate;
    private Long size;
    private BasicFileAttributes basicFileAttributes;
    private boolean head;
    private enum FileType {DIR, FILE, SYMLINK}
    private FileType fileType;

    public BasicFileAttributes getBasicFileAttributes() {
        return basicFileAttributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() {
        return file;
    }

    public MyTimeFormat getCreatedDate() {
        return createdDate;
    }

    public MyTimeFormat getModifiedDate() {
        return modifiedDate;
    }

    public void setHead(boolean head) {
        this.head = head;
    }

    public boolean isHead() {
        return head;
    }


    public MyFile(File file) {
        this.file = file;

        try {
            basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        name = file.getName();
        fileType = basicFileAttributes.isDirectory() ? FileType.DIR :
                (basicFileAttributes.isRegularFile() ? FileType.FILE :
                        (basicFileAttributes.isSymbolicLink() ? FileType.SYMLINK : null));
        path = file.toPath();

        createdDate = new MyTimeFormat(basicFileAttributes.creationTime());
        modifiedDate = new MyTimeFormat(basicFileAttributes.creationTime());
        size = fileType.equals(FileType.DIR) ? null : basicFileAttributes.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyFile myFile = (MyFile) o;

        return path != null ? path.equals(myFile.path) : myFile.path == null;

    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }
}
