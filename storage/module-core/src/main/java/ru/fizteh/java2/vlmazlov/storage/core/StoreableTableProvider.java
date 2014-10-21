package ru.fizteh.java2.vlmazlov.storage.core;

import ru.fizteh.java2.vlmazlov.storage.api.ColumnFormatException;
import ru.fizteh.java2.vlmazlov.storage.api.Storeable;
import ru.fizteh.java2.vlmazlov.storage.api.Table;
import ru.fizteh.java2.vlmazlov.storage.api.TableProvider;
import ru.fizteh.java2.vlmazlov.storage.core.generics.GenericTableProvider;
import ru.fizteh.java2.vlmazlov.storage.core.io.*;
import ru.fizteh.java2.vlmazlov.storage.utils.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StoreableTableProvider extends GenericTableProvider<Storeable, StoreableTable>
        implements TableProvider, AutoCloseable {

    private boolean isClosed;

    public StoreableTableProvider(String name, boolean autoCommit) throws ValidityCheckFailedException {
        super(name, autoCommit);
        isClosed = false;
    }

    @Override
    protected StoreableTable instantiateTable(String name, Object[] args) {
        checkClosed();
        try {
            return new StoreableTable(this, name, autoCommit, (List) args[0]);
        } catch (ValidityCheckFailedException | IOException ex) {
            throw new RuntimeException("Validity check failed: " + ex.getMessage());
        }
    }

    public StoreableTable getTable(String name) {
        checkClosed();

        StoreableTable table = super.getTable(name);

        if (table == null) {
            try {
                table = loadTable(name);
            } catch (IOException | ValidityCheckFailedException ex) {
                throw new RuntimeException("Unable to load table " + name + ": " + ex.getMessage());
            }
        }

        return table;
    }

    @Override
    public synchronized void removeTable(String name) {
        checkClosed();
        super.removeTable(name);
    }

    public String getRoot() {
        checkClosed();
        return super.getRoot();
    }

    private StoreableTable loadTable(String name) throws IOException, ValidityCheckFailedException {

        if (!Arrays.asList(new File(getRoot()).list()).contains(name)) {
            return null;
        }

        File tableDir = new File(getRoot(), name);  
        ValidityChecker.checkMultiStoreableTableRoot(tableDir);

        StoreableTable table = new StoreableTable(this, name, autoCommit, 
            StoreableTableFileManager.getTableSignature(name, this));
        
        tables.put(name, table);

        return table;
    }

    @Override
    public synchronized StoreableTable createTable(String name, List<Class<?>> columnTypes) throws IOException {
        checkClosed();

        if ((columnTypes == null) || (columnTypes.isEmpty())) {
            throw new IllegalArgumentException("wrong type (column types not specified)");
        }

        try {
            for (Class<?> type : columnTypes) {
                ValidityChecker.checkColumnType(type);
            }
        } catch (ValidityCheckFailedException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }

        if (getTable(name) != null) {
            return null;
        }

        StoreableTable table = super.createTable(name, new Object[]{columnTypes});
        StoreableTableFileManager.writeSignature(table, this);
        //StoreableTableFileManager.writeSize(table, this);

        return table;
    }

    @Override
    public Storeable deserialize(StoreableTable table, String value) throws ParseException {
        checkClosed();
        return this.deserialize((Table) table, value);
    }

    @Override
    public String serialize(StoreableTable table, Storeable value) throws ColumnFormatException {
        checkClosed();
        return this.serialize((Table) table, value);
    }

    @Override
    public Storeable deserialize(Table table, String value) throws ParseException {
        checkClosed();

        List<Object> values = new ArrayList<Object>();

        try {
            XMLStoreableReader reader = new XMLStoreableReader(value);
            for (int i = 0; i < table.getColumnsCount(); ++i) {
                values.add(reader.readColumn(table.getColumnType(i)));
            }


        } catch (XMLStreamException ex) {
            throw new ParseException(ex.getMessage(), 0);
        }

        return createFor(table, values);
    }

    @Override
    public String serialize(Table table, Storeable value) throws ColumnFormatException {
        checkClosed();

        try {
            ValidityChecker.checkValueFormat(table, value);
        } catch (ValidityCheckFailedException ex) {
            throw new ColumnFormatException(ex.getMessage());
        }

        try {
            return XMLStoreableWriter.serialize(value);
        } catch (XMLStreamException ex) {
            throw new RuntimeException("Unable to write XML: " + ex.getMessage());
        }
    }

    @Override
    public Storeable createFor(Table table) {
        checkClosed();

        if (table == null) {
            throw new IllegalArgumentException("table not specified");
        }

        List<Class<?>> valueTypes = new ArrayList<Class<?>>(table.getColumnsCount());

        for (int i = 0; i < table.getColumnsCount(); ++i) {
            valueTypes.add(table.getColumnType(i));
        }

        return new TableRow(valueTypes);
    }

    @Override
    public Storeable createFor(Table table, List<?> values) throws ColumnFormatException, IndexOutOfBoundsException {
        checkClosed();

        if (table == null) {
            throw new IllegalArgumentException("table not specified");
        }

        if (values == null) {
            throw new IllegalArgumentException("values not specified");
        }

        if (values.size() > table.getColumnsCount()) {
            throw new IndexOutOfBoundsException("Too many columns passed");
        }

        if (values.size() < table.getColumnsCount()) {
            throw new IndexOutOfBoundsException("Too few columns passed");
        }

        Storeable result = createFor(table);

        for (int i = 0; i < values.size(); ++i) {
            result.setColumnAt(i, values.get(i));
        }

        return result;
    }

    @Override
    public void read() throws IOException, ValidityCheckFailedException {
        checkClosed();

        for (File file : ProviderReader.getTableDirList(this)) {
            @SuppressWarnings("UnusedAssignment") StoreableTable table = loadTable(file.getName());
        }
    }

    @Override
    public void write() throws IOException, ValidityCheckFailedException {
        checkClosed();
        
        File rootDir = new File(getRoot());

        for (File entry : rootDir.listFiles()) {

            StoreableTable curTable = getTable(entry.getName());

            if (curTable == null) {
                throw new IOException(entry.getName() + " doesn't match any database");
            }

            curTable.checkRoot(entry);

            curTable.commit();
        }
    }

    public void closeTable(String name) {
        checkClosed();
        tables.remove(name);
    }

    public void close() {
        //necessary for factory.close() to work
        if (isClosed) {
            return;
        }

        for (Map.Entry<String, StoreableTable> entry : tables.entrySet()) {
            entry.getValue().close();
        }

        isClosed = true;

    }

    public void checkClosed() {
        if (isClosed) {
            throw new IllegalStateException("trying to operate on a closed table provider");
        }
    }

    public String toString() {
        checkClosed();

        return getClass().getSimpleName() + "[" + getRoot() + "]";
    }
}
