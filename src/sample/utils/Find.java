package sample.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sample.pojo.MyFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * Created by Stepan.Koledov on 18.04.2017.
 */
class Find {

    private static final Logger log = Logger.getLogger(Find.class.getName());

    public static class Finder extends SimpleFileVisitor<Path> {
        private final BlockingQueue<Path> queue
                = new ArrayBlockingQueue<>(100);
        private final PathMatcher matcher;
        private int numMatches = 0;
        ObservableList<MyFile> filesMatched = FXCollections.observableArrayList();


        public Finder(String pattern) {
            matcher = FileSystems.getDefault()
                    .getPathMatcher("glob:" + pattern);
        }

        // Compares the glob pattern against
        // the file or directory name.
        void find(Path file) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                numMatches++;
                filesMatched.add(new MyFile(file.toFile()));
                log.info(file.toString());
            }
        }

        // Prints the total number of
        // matches to standard out.
        public ObservableList<MyFile> done() {
            return filesMatched;
            //log.info("Matched: " + numMatches);
        }

        // Invoke the pattern matching
        // method on each file.
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            find(file);
            return FileVisitResult.CONTINUE;
        }

        // Invoke the pattern matching
        // method on each directory.
        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                                                 BasicFileAttributes attrs) {
            if (Thread.currentThread().isInterrupted()) {
                return FileVisitResult.TERMINATE;
            }
            find(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file,
                                               IOException exc) {
            System.err.println(exc);
            return FileVisitResult.CONTINUE;
        }
    }

}


