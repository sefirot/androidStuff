package android.database.sqlite;

public class SQLiteException extends RuntimeException {
    @Override
	public String getMessage() {
		Throwable cause = getCause();
		if (cause != null)
			return cause.getMessage();
		else
			return super.getMessage();
	}

	public SQLiteException(int errorCode, String errorMessage) {
        this(errorCode, errorMessage, null);
    }

    public SQLiteException(int errorCode, String errorMessage, Throwable cause) {
        super("[" + errorCode + "] " + (errorMessage == null ? "sqlite error" : errorMessage), cause);
    }

    public SQLiteException(String errorMessage) {
        super(errorMessage);
    }
}
