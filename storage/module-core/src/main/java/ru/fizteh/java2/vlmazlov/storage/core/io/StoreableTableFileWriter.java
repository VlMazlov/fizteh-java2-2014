package ru.fizteh.java2.vlmazlov.storage.core.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
public class StoreableTableFileWriter {

    private final Map<String, String> toWrite;
    private final File file;

    public StoreableTableFileWriter(File file) {
        toWrite = new HashMap<String, String>();
        this.file = file;
    }

    private int countFirstOffSet() throws IOException {
        int currentOffset = 0;

        for (Map.Entry<String, String> entry : toWrite.entrySet()) {
            currentOffset += entry.getKey().getBytes("UTF-8").length + 1 + 4;
        }

        return currentOffset;

    }

    public void writeKeyValue(String key, String value) {
        toWrite.put(key, value);
    }

    private void storeKey(RandomAccessFile dataBaseStorage, String key, int offSet) throws IOException {
        dataBaseStorage.write(key.getBytes("UTF-8"));
        dataBaseStorage.writeByte('\0');
        dataBaseStorage.writeInt(offSet);
    }

    public void flush() throws IOException {

        file.delete();

        try(RandomAccessFile dataBaseStorage = new RandomAccessFile(file, "rw")) {

            long currentOffset = countFirstOffSet();
            long writePosition;

            for (Map.Entry<String, String> entry : toWrite.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }

                storeKey(dataBaseStorage, entry.getKey(), (int) currentOffset);
                writePosition = dataBaseStorage.getFilePointer();

                dataBaseStorage.seek(currentOffset);
                dataBaseStorage.write(entry.getValue().getBytes("UTF-8"));
                currentOffset = dataBaseStorage.getFilePointer();

                dataBaseStorage.seek(writePosition);
            }

        }

        if (file.length() == 0) {
            file.delete();
        }
    }
}
