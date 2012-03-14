package android.database.sqlite;

public class SQLiteException extends com.almworks.sqlite4java.SQLiteException {
    public SQLiteException(int errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public SQLiteException(int errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
}
