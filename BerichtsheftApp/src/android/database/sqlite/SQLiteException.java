package android.database.sqlite;

public class SQLiteException extends Exception {
    public SQLiteException(int errorCode, String errorMessage) {
        this(errorCode, errorMessage, null);
    }

    public SQLiteException(int errorCode, String errorMessage, Throwable cause) {
        super("[" + errorCode + "] " + (errorMessage == null ? "sqlite error" : errorMessage), cause);
    }
}
