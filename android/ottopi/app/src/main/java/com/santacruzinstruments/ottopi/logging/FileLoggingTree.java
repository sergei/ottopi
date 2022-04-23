package com.santacruzinstruments.ottopi.logging;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import timber.log.Timber;

public class FileLoggingTree extends Timber.DebugTree {

    private static final String TAG = FileLoggingTree.class.getSimpleName();
    private static final int MAX_RUNS_TO_RETAIN = 10;
    private static final int MAX_LINES_IN_LOG = 10000;
    private static final int MAX_FILES_IN_RUN = 100;

    private static final String LOG_PREFIX = "ottopi";
    private static final String COUNTS_DELIMITER = "-";
    private static final String OUTBOX_DIR = "outbox";

    private File mLogFir = null;
    private int mLinesCount = 0;
    private UUID mRunUuid = null;
    private FileWriter mLogWriter;
    private String fileId = "";


    @SuppressLint("LogNotTimber")
    public FileLoggingTree(Context context){
        File logsDir = new File(context.getExternalCacheDir(), "logs");
        boolean dirExists = true;
        if (!logsDir.exists()) {
            dirExists = logsDir.mkdirs();
        }

        if (dirExists) {
            Log.i(TAG, String.format("Use %s directory to collect log files", logsDir));
            mLogFir = logsDir;

            // Rotate logs from the previous runs retaining the last
            rotatePreviousRunsLogs(logsDir);

            mRunUuid = UUID.randomUUID();

            // Open new log
            openNewLog();
        }else{
            Log.e(TAG, String.format("Failed to create %s directory. No log files will be collected", logsDir));
        }
    }

    @SuppressLint("LogNotTimber")
    private void openNewLog() {
        String fileNameTimeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm",
                Locale.US).format(new Date());
        fileId = String.format(Locale.US, "%08X_%s",
                mRunUuid.getLeastSignificantBits(), fileNameTimeStamp);
        String baseName = String.format(Locale.US, "%s_%s-1-1-log.txt",
                LOG_PREFIX, fileId);
        File file = new File(mLogFir, baseName);
        try {
            mLogWriter = new FileWriter(file, false);
            Log.i(TAG, String.format("Created new log %s", file.getAbsolutePath()));
        } catch (IOException e) {
            mLogWriter = null;
            Log.e(TAG, String.format("Failed to create new log %s", file.getAbsolutePath()), e);
        }
    }

    private void rotatePreviousRunsLogs(File logsDir) {
        String [] logs = logsDir.list();
        Map<String, File> renameMap = createPrevRunsRenameMap(logs, MAX_RUNS_TO_RETAIN-1);
        assert logs != null;
        renameAndDelete(logs, renameMap);
    }

    private void rotateThisRunLogs() {
        String [] logs = mLogFir.list();
        Map<String, File> renameMap = createThisRunRenameMap(logs, MAX_FILES_IN_RUN-1);
        assert logs != null;
        renameAndDelete(logs, renameMap);
    }

    @SuppressLint("LogNotTimber")
    private void renameAndDelete(String[] logs, Map<String, File> renameMap) {
        for( String oldName : logs){
            File newFile = renameMap.get(oldName);
            if ( newFile != null ) {
                File oldFullPath = new File(mLogFir,oldName);
                if ( newFile.getName().equals("delete") ){
                    boolean success = oldFullPath.delete();
                    Log.i(TAG, String.format("Deletion of %s %s", oldFullPath.getAbsolutePath(), success ? "successful":"failed"));
                }else if ( ! newFile.getName().equals("keep") ){
                    File newFullPath = new File(mLogFir, newFile.getName());
                    boolean success = oldFullPath.renameTo(newFullPath);
                    Log.i(TAG, String.format("Rename %s to %s %s", oldFullPath.getAbsolutePath(), newFullPath.getAbsolutePath(), success ? "successful":"failed"));
                }
            }
        }
    }

    @NotNull
    @VisibleForTesting
    public static  Map<String, File> createPrevRunsRenameMap(String[] logs, int runsToKeep) {
        HashMap<String, File> map = new HashMap<>();
        Arrays.sort(logs);
        for( String logPathName: logs){
            File logFile = new File(logPathName);
            Integer count = extractRunCount(logFile);
            if ( count != null ){
                if (count <= runsToKeep) {
                    File newFile = replaceRunCount(logFile, count +1 );
                    map.put(logPathName, newFile);
                }else{
                    map.put(logPathName, new File("/","delete"));
                }
            }
        }
        return map;
    }

    @NotNull
    @VisibleForTesting
    public static  Map<String, File> createThisRunRenameMap(String[] logs, int filesToKeep) {
        HashMap<String, File> map = new HashMap<>();
        Arrays.sort(logs);
        for( String logPathName: logs){
            File logFile = new File(logPathName);
            Integer runCount = extractRunCount(logFile);
            if ( runCount != null && runCount == 1){
                Integer fileCount = extractFileCount(logFile);
                if (fileCount != null && fileCount <= filesToKeep) {
                    File newFile = replaceFileCount(logFile, fileCount + 1 );
                    map.put(logPathName, newFile);
                }else{
                    map.put(logPathName, new File("/","delete"));
                }
            }else{
                map.put(logPathName, new File("/","keep"));
            }
        }
        return map;
    }

    private static Integer extractRunCount(File logFile) {
        String baseName = logFile.getName();
        String [] t = baseName.split(COUNTS_DELIMITER);
        if (t.length > 2 && t[0].startsWith(LOG_PREFIX) ){
            return Integer.parseInt(t[1]);
        }else{
            return null;
        }
    }

    private static Integer extractFileCount(File logFile) {
        String baseName = logFile.getName();
        String [] t = baseName.split(COUNTS_DELIMITER);
        if (t.length > 3 && t[0].startsWith(LOG_PREFIX) ){
            return Integer.parseInt(t[2]);
        }else{
            return null;
        }
    }

    private static File replaceRunCount(File logFile, int runCount) {
        String baseName = logFile.getName();
        String [] t = baseName.split(COUNTS_DELIMITER);
        t[1] = String.format(Locale.US,"%d",runCount);
        String newBaseName = join(COUNTS_DELIMITER, Arrays.asList(t));
        return new File(logFile.getParent(), newBaseName);
    }

    private static File replaceFileCount(File logFile, int fileCount) {
        String baseName = logFile.getName();
        String [] t = baseName.split(COUNTS_DELIMITER);
        t[2] = String.format(Locale.US,"%d",fileCount);
        String newBaseName = join(COUNTS_DELIMITER, Arrays.asList(t));
        return new File(logFile.getParent(), newBaseName);
    }

    @Override
    protected void log(int priority, String tag, @NotNull String message, Throwable t) {

        if ( mLogWriter != null ) {
            if ( mLinesCount >= MAX_LINES_IN_LOG ){
                try { mLogWriter.close(); } catch (IOException ignore) {}
                mLogWriter = null;
                mLinesCount = 0;
                rotateThisRunLogs();
                openNewLog();
            }
        }

        if ( mLogWriter != null ) {
            String logTimeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",
                    Locale.getDefault()).format(new Date());
            try {
                mLogWriter
                        .append(logTimeStamp)
                        .append(' ')
                        .append(tag)
                        .append(' ')
                        .append(message)
                        .append('\n');
                mLinesCount ++;
            } catch (IOException e) {
                super.log(Log.ERROR, TAG,"Error while logging into file : " + e);
                super.log(Log.ERROR, TAG,"No more logs will go to the files");
                mLinesCount = 0;
                mLogWriter = null;
            }
        }
        // Also print to logcat
        super.log(priority, tag, message, t);
    }

    @SuppressWarnings("SameParameterValue")
    private static String join(String separator, List<String> input) {
        if (input == null || input.size() <= 0) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.size(); i++) {
            sb.append(input.get(i));
            // if not the last item
            if (i != input.size() - 1) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    void flush() {
        if ( mLogWriter != null ){
            try { mLogWriter.flush(); } catch (IOException ignore) {}
            mLogWriter = null;
            mLinesCount = 0;
        }
    }

    @SuppressLint("LogNotTimber")
    private File prepareForUpload() {
        if ( mLogWriter != null ) {
            File outBoxDir = new File(mLogFir, OUTBOX_DIR);
            boolean dirExists = true;
            if (!outBoxDir.exists()) {
                dirExists = outBoxDir.mkdirs();
            }
            if (dirExists) {
                // Close and move old files
                try { mLogWriter.close(); } catch (IOException ignore) {}
                String [] logs = mLogFir.list();
                assert logs != null;
                for (String log :logs) {
                    File oldFullPath = new File(mLogFir,log);
                    File newFullPath = new File(outBoxDir, log);
                    boolean success = oldFullPath.renameTo(newFullPath);
                    Log.i(TAG, String.format("Moved %s to %s %s", oldFullPath.getAbsolutePath(), newFullPath.getAbsolutePath(), success ? "sucessfull":"failed"));
                }
                // Start the new log
                openNewLog();

                return outBoxDir;
            }
        }
        return null;
    }

    public void deleteUploadedFiles() {
        File outBoxDir = new File(mLogFir, OUTBOX_DIR);
        for (final File fileEntry : Objects.requireNonNull(outBoxDir.listFiles())) {
            if ( fileEntry.delete() )
                Timber.d("Deleted %s", fileEntry.getAbsolutePath());
            else
                Timber.d("Failed to delete %s", fileEntry.getAbsolutePath());
        }
    }

    File createUploadZip() {
        File logFolder =  prepareForUpload();
        if ( logFolder != null ) {
            String fileNameTimeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss",
                    Locale.US).format(new Date());
            String zipName = String.format(Locale.US, "logs-%s.zip", fileNameTimeStamp);
            File zipFile = new File(logFolder, zipName);

            try {
                ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
                byte [] buffer = new byte[4096];
                for (final File fileEntry : Objects.requireNonNull(logFolder.listFiles())) {
                    if (fileEntry.isFile() && fileEntry.getName().endsWith(".txt")){
                        ZipEntry e = new ZipEntry(fileEntry.getName());
                        zipOut.putNextEntry(e);
                        FileInputStream is = new FileInputStream(fileEntry);
                        while ( true ) {
                            int n = is.read(buffer);
                            if ( n >= 0 )
                                zipOut.write(buffer, 0, n);
                            else
                                break;
                        }
                        zipOut.closeEntry();
                    }
                }
                zipOut.close();
                return zipFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getLogFileId() {
        return fileId;
    }

}
