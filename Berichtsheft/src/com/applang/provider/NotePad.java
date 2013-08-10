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
 * Convenience definitions for NotePadProvider
 */
public final class NotePad {
    public static final String AUTHORITY = "com.applang.provider.NotePad";

    // This class cannot be instantiated
    private NotePad() {}
    
    /**
     * Notes table
     */
    public static final class NoteColumns implements BaseColumns {
		// This class cannot be instantiated
        private NoteColumns() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.applang.note";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.applang.note";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "created ASC";
        public static final String TITLE_SORT_ORDER = "title COLLATE NOCASE";

        /**
         * The title of the note
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";

        /**
         * The note itself
         * <P>Type: TEXT</P>
         */
        public static final String NOTE = "note";

        /**
         * The timestamp for when the note was created
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String CREATED_DATE = "created";
        public static final String REF_ID = CREATED_DATE;

        /**
         * The timestamp for when the note was last modified
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String MODIFIED_DATE = "modified";
        public static final String REF_ID2 = MODIFIED_DATE;
    }
}
