package sample.utils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Stack;

/**
 * Created by Stepan.Koledov on 24.04.2017.
 */
public class Result {
    private File dstFile;


    private File sourceFile;
    List<File> files;
    ResultCode resultCode;

    public Result(File dstFile, File sourceFile, List<File> files, ResultCode resultCode) {
        this.dstFile = dstFile;
        this.sourceFile = sourceFile;
        this.resultCode = resultCode;
        this.files = files;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public File getDstFile() {
        return dstFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }
}
