package com.applang.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for PlantInfoProvider
 */
public final class PlantInfo {
    public static final String AUTHORITY = "com.applang.provider.PlantInfo";

    // This class cannot be instantiated
    private PlantInfo() {}
    
    /**
     * Plants table
     */
    public static final class Plants implements BaseColumns {
        // This class cannot be instantiated
        private Plants() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = 
        		Uri.parse("content://" + AUTHORITY + "/plants");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of plants.
         */
    	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.applang.plant";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single plant.
         */
    	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.applang.plant";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "name ASC";
        
        /**
         * The rowId sort order for this table
         */
        public static final String ROWID_SORT_ORDER = "_id ASC";
        
        /**
         * The family sort order for this table
         */
        public static final String FAMILY_SORT_ORDER = "family ASC";

        /**
         * The botanical name sort order for this table
         */
        public static final String BOTNAME_SORT_ORDER = "botname ASC";
        
        /**
         * The botanical family sort order for this table
         */
        public static final String BOTFAMILY_SORT_ORDER = "botfamily ASC";
        
        /**
         * The local name of the plant
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";

        /**
         * The local family name of the plant
         * <P>Type: TEXT</P>
         */
        public static final String FAMILY = "family";

        /**
         * The botanical name of the plant
         * <P>Type: TEXT</P>
         */
        public static final String BOTNAME = "botname";

        /**
         * The botanical family name of the plant
         * <P>Type: TEXT</P>
         */
        public static final String BOTFAMILY = "botfamily";

        /**
         * The crop category of the plant
         * <P>Type: TEXT</P>
         */
        public static final String GROUP = "crop_group";
    }
    
    /**
     * Pictures table
     */
    public static final class Pictures implements BaseColumns {
        // This class cannot be instantiated
        private Pictures() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = 
        		Uri.parse("content://" + AUTHORITY + "/pictures");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of plants.
         */
    	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.applang.picture";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single plant.
         */
    	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.applang.picture";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "name ASC";
        
        /**
         * The local name of the plant
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";

        /**
         * The local family name of the plant
         * <P>Type: TEXT</P>
         */
        public static final String TYPE = "type";

        /**
         * The botanical name of the plant
         * <P>Type: TEXT</P>
         */
        public static final String BLOB = "blob";
    }
}
