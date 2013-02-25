/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.applang.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for WeatherInfoProvider
 */
public final class WeatherInfo {
    public static final String AUTHORITY = "com.applang.provider.WeatherInfo";

    // This class cannot be instantiated
    private WeatherInfo() {}
    
    /**
     * Weather table
     */
    public static final class Weathers implements BaseColumns {
        // This class cannot be instantiated
        private Weathers() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/weathers");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of weathers.
         */
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.applang.weather";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single weather.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.applang.weather";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "created ASC";

        /**
         * The description of the weather
         * <P>Type: TEXT</P>
         */
        public static final String DESCRIPTION = "description";

        /**
         * The location of the weather
         * <P>Type: TEXT</P>
         */
        public static final String LOCATION = "location";

        /**
         * The precipitation
         * <P>Type: FLOAT</P>
         */
        public static final String PRECIPITATION = "precipitation";

        /**
         * The temperature maximum 
         * <P>Type: FLOAT</P>
         */
        public static final String MAXTEMP = "maxtemp";

        /**
         * The temperature minimum 
         * <P>Type: FLOAT</P>
         */
        public static final String MINTEMP = "mintemp";

        /**
         * The timestamp for when the weather happened
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the weather was last modified
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String MODIFIED_DATE = "modified";
    }
}
