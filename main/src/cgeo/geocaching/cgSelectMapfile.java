package cgeo.geocaching;

import cgeo.geocaching.files.FileList;
import cgeo.geocaching.files.IFileSelectionView;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.ui.FileSelectionListAdapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.util.List;

public class cgSelectMapfile extends FileList<FileSelectionListAdapter> implements IFileSelectionView {

    public cgSelectMapfile() {
        super("map");
    }

    String mapFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapFile = Settings.getMapFile();
    }

    @Override
    public void close() {

        Intent intent = new Intent();
        intent.putExtra("mapfile", mapFile);

        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    protected FileSelectionListAdapter getAdapter(List<File> files) {
        return new FileSelectionListAdapter(this, files);
    }

    @Override
    protected File[] getBaseFolders() {
        final File base = Environment.getExternalStorageDirectory();
        return new File[] {
                new File(base, "mfmaps"),
                new File(new File(base, "Locus"), "mapsVector"),
                new File(base, LocalStorage.cache)
        };
    }

    @Override
    protected void setTitle() {
        setTitle(res.getString(R.string.map_file_select_title));
    }

    @Override
    public String getCurrentFile() {
        return mapFile;
    }

    @Override
    public void setCurrentFile(String newFile) {
        mapFile = newFile;
    }

    @Override
    public Context getContext() {
        return this;
    }

}
