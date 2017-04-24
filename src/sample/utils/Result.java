package sample.utils;

import java.io.File;
import java.util.Stack;

/**
 * Created by Stepan.Koledov on 24.04.2017.
 */
public class Result {
    private File dstFile;
    private File sourceFile;
    Stack<String> fileNames;
    ResultCode resultCode;

    public Result(File dstFile, File sourceFile, Stack<String> fileNames, ResultCode resultCode) {
        this.dstFile = dstFile;
        this.sourceFile = sourceFile;
        this.resultCode = resultCode;
        this.fileNames = fileNames;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public File getDstFile() {
        return dstFile;
    }
}
