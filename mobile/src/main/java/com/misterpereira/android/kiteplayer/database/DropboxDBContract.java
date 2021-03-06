/*
 * Copyright (c) 2015 Rafael Pereira
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 *     https://mozilla.org/MPL/2.0/.
 */

package com.misterpereira.android.kiteplayer.database;

import android.provider.BaseColumns;

public final class DropboxDBContract {

    // Never to be instantiated
    private DropboxDBContract() {}

    public static abstract class Entry implements BaseColumns {

        public static final String TABLE_NAME = "entry";
        public static final String FTS4_TABLE_NAME = "entry_fts4";


        public static final String COLUMN_NAME_IS_DIR = "is_dir";
        public static final String COLUMN_NAME_ROOT = "root";
        public static final String COLUMN_NAME_PARENT_DIR = "parent_dir";
        public static final String COLUMN_NAME_FILENAME = "filename";

        public static final String COLUMN_NAME_SIZE = "size";
        public static final String COLUMN_NAME_BYTES = "bytes";

        public static final String COLUMN_NAME_MODIFIED = "modified";
        public static final String COLUMN_NAME_CLIENT_MTIME = "client_mtime";
        public static final String COLUMN_NAME_REV = "rev";
        public static final String COLUMN_NAME_HASH = "hash";

        public static final String COLUMN_NAME_MIME_TYPE = "mime_type";
        public static final String COLUMN_NAME_ICON = "icon";
        public static final String COLUMN_NAME_THUMB_EXISTS = "thumb_exists";
    }

    public static abstract class Song implements BaseColumns {

        public static final String TABLE_NAME = "song";
        public static final String FTS4_TABLE_NAME = "song_fts4";

        public static final String COLUMN_NAME_DOWNLOAD_URL = "download_url";
        public static final String COLUMN_NAME_DOWNLOAD_URL_EXPIRATION = "download_url_expiration";
        public static final String COLUMN_NAME_HAS_LATEST_METADATA = "has_latest_metadata";
        public static final String COLUMN_NAME_HAS_VALID_ALBUM_ART = "has_valid_album_art";

        public static final String COLUMN_NAME_ALBUM = "album";
        public static final String COLUMN_NAME_ALBUM_ARTIST = "album_artist";
        public static final String COLUMN_NAME_ARTIST = "artist";
        public static final String COLUMN_NAME_GENRE = "genre";
        public static final String COLUMN_NAME_TITLE = "title";

        public static final String COLUMN_NAME_DURATION = "duration";
        public static final String COLUMN_NAME_TRACK_NUMBER = "track_number";
        public static final String COLUMN_NAME_TOTAL_TRACKS = "total_tracks";

        public static final String COLUMN_NAME_ENTRY_ID = "entry_id";
    }
}
