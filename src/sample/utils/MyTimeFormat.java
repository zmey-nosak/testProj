package sample.utils;

import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Created by Stepan.Koledov on 19.04.2017.
 */


public class MyTimeFormat implements Comparable {
    private String date;
    private FileTime fileTime;

    public MyTimeFormat(FileTime fileTime) {
        this.fileTime = fileTime;
        date = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault()).format(fileTime.toInstant());
    }

    @Override
    public int compareTo(Object o) {
        return this.fileTime.compareTo(((MyTimeFormat) o).fileTime);
    }

    public String getDate() {
        return date;
    }

    @Override
    public String toString() {
        return date;
    }
}
