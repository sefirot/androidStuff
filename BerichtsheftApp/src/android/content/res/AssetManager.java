package android.content.res;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static com.applang.Util2.*;

public class AssetManager
{
	public InputStream open (String fileName) throws FileNotFoundException {
		String path = Resources.getAbsolutePath(pathCombine("/assets", fileName));
		return new FileInputStream( path );
	}
}
