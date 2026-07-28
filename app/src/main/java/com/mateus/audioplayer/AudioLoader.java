package com.mateus.audioplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import androidx.documentfile.provider.DocumentFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudioLoader {

    private static final String[] AUDIO_EXTENSIONS = {
        ".mp3", ".wav", ".ogg", ".flac", ".aac", ".m4a", ".wma", ".opus", ".aiff", ".alac"
    };

    /** Load all audio from MediaStore */
    public static List<AudioFile> loadAllAudio(Context context) {
        List<AudioFile> list = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
        };
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = null;
        try {
            cursor = cr.query(uri, projection, null, null, sortOrder);
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                int displayCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                int sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);

                do {
                    long duration = durationCol >= 0 ? cursor.getLong(durationCol) : 0;
                    if (duration <= 0) continue;

                    AudioFile af = new AudioFile();
                    af.id = idCol >= 0 ? cursor.getLong(idCol) : -1;
                    af.title = titleCol >= 0 ? cursor.getString(titleCol) : "Unknown";
                    af.artist = artistCol >= 0 ? cursor.getString(artistCol) : null;
                    af.album = albumCol >= 0 ? cursor.getString(albumCol) : null;
                    af.duration = duration;
                    af.displayName = displayCol >= 0 ? cursor.getString(displayCol) : "Unknown";
                    af.size = sizeCol >= 0 ? cursor.getLong(sizeCol) : 0;

                    String data = dataCol >= 0 ? cursor.getString(dataCol) : null;
                    if (data != null && !data.isEmpty()) {
                        File file = new File(data);
                        if (file.exists()) {
                            af.uri = Uri.fromFile(file);
                            af.folderPath = file.getParent();
                        } else {
                            af.uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(af.id));
                        }
                    } else {
                        af.uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(af.id));
                    }

                    list.add(af);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                try { cursor.close(); } catch (Exception ignored) {}
            }
        }
        return list;
    }

    /** Scan a SAF folder tree recursively for audio files */
    public static List<AudioFile> scanFolder(Context context, Uri folderUri) {
        List<AudioFile> list = new ArrayList<>();
        DocumentFile rootDoc = DocumentFile.fromTreeUri(context, folderUri);
        if (rootDoc != null) {
            scanDocumentTree(context, rootDoc, list);
        }
        return list;
    }

    private static void scanDocumentTree(Context context, DocumentFile dir, List<AudioFile> list) {
        DocumentFile[] children = dir.listFiles();
        for (DocumentFile child : children) {
            if (child.isDirectory()) {
                scanDocumentTree(context, child, list);
            } else if (child.isFile()) {
                String name = child.getName();
                if (name != null && isAudioFile(name)) {
                    AudioFile af = new AudioFile();
                    af.uri = child.getUri();
                    af.title = stripExtension(name);
                    af.displayName = name;
                    af.artist = null;
                    af.album = null;
                    af.duration = 0; // unknown from SAF
                    af.size = child.length();
                    af.id = -1;
                    af.folderPath = dir.getUri().toString();
                    list.add(af);
                }
            }
        }
    }

    private static boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
