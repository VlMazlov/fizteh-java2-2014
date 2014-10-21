package ru.fizteh.java2.vlmazlov.storage.core.generics;

import ru.fizteh.java2.vlmazlov.storage.utils.ValidityChecker;
import ru.fizteh.java2.vlmazlov.storage.utils.ValidityCheckFailedException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

@SuppressWarnings("ALL")
public abstract class GenericTableProvider<V, T extends GenericTable<V>> {
    protected Map<String, T> tables;
    protected final boolean autoCommit;
    private final String root;

    protected GenericTableProvider(String root, boolean autoCommit) throws ValidityCheckFailedException {
        if (root == null) {
            throw new IllegalArgumentException("Directory not specified");
        }

        ValidityChecker.checkMultiTableDataBaseRoot(root);

        this.root = root;
        tables = new HashMap<String, T>();
        this.autoCommit = autoCommit;
    }

    protected abstract T instantiateTable(String name, Object[] args);

    public T getTable(String name) {
        try {
            ValidityChecker.checkMultiTableName(name);
        } catch (ValidityCheckFailedException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }


        return tables.get(name);
    }

    public synchronized T createTable(String name, Object[] args) {
        try {
            ValidityChecker.checkMultiTableName(name);
        } catch (ValidityCheckFailedException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }

        T newTable;

        if (tables.get(name) != null) {
            return null;
        }

        (new File(root, name)).mkdir();

        newTable = instantiateTable(name, args);

        tables.put(name, newTable);

        return newTable;
    }

    public synchronized void removeTable(String name) {
        try {
            ValidityChecker.checkMultiTableName(name);
        } catch (ValidityCheckFailedException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }

        T oldTable = tables.remove(name);

        if (oldTable == null) {
            throw new IllegalStateException("Table " + name + " doesn't exist");
        }

        FileUtils.deleteQuietly(new File(root, name));
    }

    public String getRoot() {
        return root;
    }

    public abstract void read() throws IOException, ValidityCheckFailedException;

    public abstract void write() throws IOException, ValidityCheckFailedException;

    public abstract V deserialize(T table, String value) throws ParseException;

    public abstract String serialize(T table, V value);
}
