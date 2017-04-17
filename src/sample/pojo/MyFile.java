package sample.pojo;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * Created by Stepan.Koledov on 10.04.2017.
 */
@Getter
@Setter
public class MyFile {
    private String name;
    private File file;
    private Path path;
    private FileTime createdDate;
    private FileTime modifiedDate;
    private Long size;
    private String fileType;
    private BasicFileAttributes basicFileAttributes;

    @SneakyThrows
    public MyFile(File file) {
        this.file = file;
        basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        name = file.getName();
        fileType = basicFileAttributes.isDirectory() ? "DIR" :
                (basicFileAttributes.isRegularFile() ? "FILE" :
                        (basicFileAttributes.isSymbolicLink() ? "~" : ""));
        path = file.toPath();
        createdDate = basicFileAttributes.creationTime();
        modifiedDate = basicFileAttributes.lastModifiedTime();
        size = basicFileAttributes.size();
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
