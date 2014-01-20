package android.content.res;

import java.io.File;
import java.io.InputStream;

import static com.applang.Util.*;

public class AssetManager
{
	public InputStream open (String fileName) {
		File file = fileOf("/assets", fileName);
		return getClass().getResourceAsStream( file.getPath() );
	}
}
