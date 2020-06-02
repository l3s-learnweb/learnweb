package de.l3s.util.database;

public interface IColumn {

    default boolean isReadOnly() {
        return false;
    }
    /*
    @Override
    String toString();*/
}
