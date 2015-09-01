/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.peartree.android.kiteplayer.model;

import android.app.Application;
import android.content.Context;
import android.media.MediaMetadata;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.peartree.android.kiteplayer.database.DropboxDBEntry;
import com.peartree.android.kiteplayer.database.DropboxDBEntryDAO;
import com.peartree.android.kiteplayer.database.DropboxDBSong;
import com.peartree.android.kiteplayer.dropbox.DropboxSyncService;
import com.peartree.android.kiteplayer.utils.LogHelper;
import com.peartree.android.kiteplayer.utils.NetworkHelper;
import com.peartree.android.kiteplayer.utils.PrefUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Utility class to get a list of MusicTrack's based on a server-side JSON
 * configuration.
 */
@Singleton
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);

    public static final String CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__";
    public static final String CUSTOM_METADATA_FILENAME = "__FILENAME__";
    public static final String CUSTOM_METADATA_DIRECTORY = "__DIRECTORY__";
    public static final String CUSTOM_METADATA_IS_DIRECTORY = "__IS_DIRECTORY__";

    public static final int FLAG_SONG_PLAY_READY = 1 << 0;
    public static final int FLAG_SONG_METADATA_TEXT = 1 << 1;
    public static final int FLAG_SONG_METADATA_IMAGE = 1 << 2;
    public static final int FLAG_SONG_METADATA_ALL = FLAG_SONG_METADATA_TEXT | FLAG_SONG_METADATA_IMAGE;

    private Context mApplicationContext;

    private DropboxAPI<AndroidAuthSession> mDBApi = null;
    private DropboxDBEntryDAO mEntryDao;
    private DropboxSyncService mDBSyncService;

    private volatile State mCurrentState = State.NON_INITIALIZED;

    @Inject
    public MusicProvider(Application application,
                         DropboxAPI<AndroidAuthSession> dbApi,
                         DropboxDBEntryDAO entryDao,
                         DropboxSyncService syncService) {

        this.mApplicationContext = application.getApplicationContext();

        this.mDBApi = dbApi;
        this.mEntryDao = entryDao;
        this.mDBSyncService = syncService;
    }

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     */
    public Observable<Boolean> init() {

        return mDBSyncService
                .synchronizeEntryDB()
                .subscribeOn(Schedulers.io())
                .lift(s -> new Subscriber<Long>(s) {

                    @Override
                    public void onCompleted() {
                        if (!s.isUnsubscribed()) {
                            mCurrentState = State.INITIALIZED;
                            s.onCompleted();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!s.isUnsubscribed()) {
                            mCurrentState = State.NON_INITIALIZED;
                            s.onError(e);
                        }
                    }

                    @Override
                    public void onNext(Long entryId) {
                        if (!s.isUnsubscribed()) {
                            mCurrentState = State.INITIALIZING;
                        }
                    }
                });
    }

    /**
     * Get media by parent folder
     * Results can include folders and audio files
     */
    public Observable<MediaMetadata> getMusicByFolder(@NonNull String parentFolder, int cacheFlags) {

        // TODO Sort results

        return mDBSyncService.fillSongMetadata(mEntryDao.findByParentDir(parentFolder), cacheFlags)
                .map(entry -> buildMetadataFromDBEntry(
                        entry,
                        mDBSyncService.getCachedSongFile(entry),
                        NetworkHelper.canStream(mApplicationContext)));
    }

    /**
     * Very basic implementation of a search that filter music tracks with title containing
     * the given query.
     *
     */
    public Iterable<MediaMetadata> searchMusicByGenre(String query) {
        return searchMusic(MediaMetadata.METADATA_KEY_TITLE, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with title containing
     * the given query.
     *
     */
    public Iterable<MediaMetadata> searchMusicBySongTitle(String query) {
        return searchMusic(MediaMetadata.METADATA_KEY_TITLE, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with album containing
     * the given query.
     *
     */
    public Iterable<MediaMetadata> searchMusicByAlbum(String query) {
        return searchMusic(MediaMetadata.METADATA_KEY_ALBUM, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with artist containing
     * the given query.
     *
     */
    public Iterable<MediaMetadata> searchMusicByArtist(String query) {
        return searchMusic(MediaMetadata.METADATA_KEY_ARTIST, query);
    }

    Iterable<MediaMetadata> searchMusic(String metadataField, String query) {

        // TODO Implement searchMusic
        return Collections.emptyList();

    }


    /**
     * Return the MediaMetadata for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    public Observable<MediaMetadata> getMusic(String musicId, int cacheFlags) {

        return mDBSyncService
                .fillSongMetadata(Observable.just(mEntryDao.findById(Long.valueOf(musicId))), cacheFlags)
                .map(entry -> buildMetadataFromDBEntry(
                        entry,
                        mDBSyncService.getCachedSongFile(entry),
                        NetworkHelper.canStream(mApplicationContext)));

    }

    public synchronized void updateMusic(String musicId, MediaMetadata metadata) {

        // TODO Implement updateMusic? This is intended to cache mm containing album art data
    }

    public void setFavorite(String musicId, boolean favorite) {

        // TODO Implement setFavorite
    }

    public boolean isFavorite(String musicId) {

        // TODO Implement isFavorite
        return false;
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    public static MediaMetadata buildMetadataFromDBEntry(DropboxDBEntry entry, @Nullable File cachedSongFile, boolean canStream) {

        MediaMetadata.Builder builder = new MediaMetadata.Builder();

        builder
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, Long.toString(entry.getId()))
                .putString(CUSTOM_METADATA_FILENAME, entry.getFilename())
                .putString(CUSTOM_METADATA_DIRECTORY, entry.getParentDir())
                .putString(CUSTOM_METADATA_IS_DIRECTORY, Boolean.toString(entry.isDir()));

        if (!entry.isDir() && entry.getSong() != null) {

            DropboxDBSong song = entry.getSong();

            if (cachedSongFile != null) {
                builder.putString(CUSTOM_METADATA_TRACK_SOURCE, cachedSongFile.getAbsolutePath());
            } else if (song.getDownloadURL() != null && canStream) {
                builder.putString(CUSTOM_METADATA_TRACK_SOURCE, song.getDownloadURL().toString());
            } else {
                // TODO Disable entry if missing source
            }

            builder
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, song.getAlbum() != null ? song.getAlbum() : entry.getParentDir())
                    .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, song.getAlbumArtist())
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, song.getArtist())
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, song.getDuration())
                    .putString(MediaMetadata.METADATA_KEY_GENRE, song.getGenre())
                    .putString(MediaMetadata.METADATA_KEY_TITLE, song.getTitle()!=null ? song.getTitle() : entry.getFilename())
                    .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, song.getTrackNumber())
                    .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, song.getTotalTracks());

        }

        return builder.build();
    }

    public static boolean willBePlayable(Context context, MediaMetadata mm) {

        boolean hasSource;
        boolean isSourceRemote;
        boolean canStream;

        hasSource = mm.getString(CUSTOM_METADATA_TRACK_SOURCE) != null;

        try {
            new URL(mm.getString(CUSTOM_METADATA_TRACK_SOURCE));
            isSourceRemote = true;
        } catch (MalformedURLException e) {
            isSourceRemote = false;
        }

        canStream = NetworkHelper.canStream(context);

        return (hasSource && !isSourceRemote) || canStream;

    }

}
