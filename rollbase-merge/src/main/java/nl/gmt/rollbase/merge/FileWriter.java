package nl.gmt.rollbase.merge;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.Validate;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FileWriter implements Closeable {
    private final File target;
    private final Set<File> files = new HashSet<>();

    public FileWriter(File target) {
        this.target = target;
    }

    public void writeFile(File file, String content) throws IOException {
        Validate.notNull(file, "file");
        Validate.notNull(content, "content");

        File target = new File(this.target, file.getPath());

        if (files.contains(target)) {
            throw new IllegalArgumentException(String.format("File '%s' has already been written", file));
        }

        files.add(target);

        target.getParentFile().mkdirs();

        System.out.println("Writing " + target);

        // Don't overwrite the current file with the same content.

        if (target.exists()) {
            String currentContent = FileUtils.readFileToString(target);

            if (currentContent.equals(content)) {
                return;
            }
        }

        FileUtils.write(target, content, "UTF-8");
    }

    @Override
    public void close() throws IOException {
        // Delete all files that weren't written in this run.

        Collection<File> files = FileUtils.listFiles(target, HiddenFileFilter.VISIBLE, TrueFileFilter.INSTANCE);

        for (File file : files) {
            if (!this.files.contains(file)) {
                file.delete();

                deleteEmptyDirectory(file.getParentFile());
            }
        }
    }

    private void deleteEmptyDirectory(File file) {
        while (file != null && !file.equals(target)) {
            if (file.list().length > 0) {
                return;
            }

            file.delete();
            file = file.getParentFile();
        }
    }
}
