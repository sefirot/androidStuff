package android.content.res;

import java.io.InputStream;

import static com.applang.Util2.*;

public class AssetManager
{
	public InputStream open (String fileName) {
		String path = pathCombine("/assets", fileName);
		return getClass().getResourceAsStream( path );
	}
}
