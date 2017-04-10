package sample.pojo;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.joda.time.DateTime;

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
    private Path path;
    private FileTime createdDate;
    private FileTime modifiedDate;
    private Long size;
    private BasicFileAttributes basicFileAttributes;

    @SneakyThrows
    public MyFile(File file) {
        name = file.getName();
        path = file.toPath();
        basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        createdDate = basicFileAttributes.creationTime();
        modifiedDate = basicFileAttributes.lastModifiedTime();
        size = basicFileAttributes.size();
    }


    public MyFile() {

    }
}
