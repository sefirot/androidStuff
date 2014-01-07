package android.graphics;

import java.io.OutputStream;

public class Bitmap
{
    public enum CompressFormat {
        JPEG    (0),
        PNG     (1);

        CompressFormat(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

    public Bitmap(byte[] data, int offset, int length) {
		// TODO Auto-generated constructor stub
	}

	public boolean compress(CompressFormat format, int quality, OutputStream stream) {
    	return true;
    }
}
