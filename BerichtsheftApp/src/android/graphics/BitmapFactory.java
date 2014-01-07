package android.graphics;

public class BitmapFactory
{

    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        if ((offset | length) < 0 || data.length < offset + length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return new Bitmap(data, offset, length);
    }
}
