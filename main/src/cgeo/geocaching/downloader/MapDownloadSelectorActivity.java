package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.MapdownloaderActivityBinding;
import cgeo.geocaching.databinding.MapdownloaderItemBinding;
import cgeo.geocaching.models.OfflineMap;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class MapDownloadSelectorActivity extends AbstractActionBarActivity {

    @NonNull
    private final List<OfflineMap> maps = new ArrayList<>();
    private ArrayList<CompanionFileUtils.DownloadedFileData> installedOfflineMaps;
    private final MapListAdapter adapter = new MapListAdapter(this);
    private MapdownloaderActivityBinding binding;
    private AbstractDownloader current;
    private ArrayList<OfflineMap.OfflineMapTypeDescriptor> spinnerData = new ArrayList<>();

    protected class MapListAdapter extends RecyclerView.Adapter<MapListAdapter.ViewHolder> {
        @NonNull private final MapDownloadSelectorActivity activity;

        protected final class ViewHolder extends AbstractRecyclerViewHolder {
            private final MapdownloaderItemBinding binding;

            ViewHolder(final View view) {
                super(view);
                binding = MapdownloaderItemBinding.bind(view);
            }
        }

        MapListAdapter(@NonNull final MapDownloadSelectorActivity activity) {
            this.activity = activity;
        }

        @Override
        public int getItemCount() {
            return activity.getQueries().size();
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mapdownloader_item, parent, false);
            final ViewHolder viewHolder = new ViewHolder(view);
            viewHolder.binding.retrieve.setOnClickListener(v -> {
                final OfflineMap offlineMap = activity.getQueries().get(viewHolder.getAdapterPosition());
                    new MapListTask(activity, offlineMap.getUri(), offlineMap.getName()).execute();
            });
            viewHolder.binding.download.setOnClickListener(v -> {
                final OfflineMap offlineMap = activity.getQueries().get(viewHolder.getAdapterPosition());
                // return to caller with URL chosen
                final Intent intent = new Intent();
                intent.putExtra(MapDownloaderUtils.RESULT_CHOSEN_URL, offlineMap.getUri());
                intent.putExtra(MapDownloaderUtils.RESULT_SIZE_INFO, offlineMap.getSizeInfo());
                intent.putExtra(MapDownloaderUtils.RESULT_DATE, offlineMap.getDateInfo());
                intent.putExtra(MapDownloaderUtils.RESULT_TYPEID, offlineMap.getType().id);
                setResult(RESULT_OK, intent);
                finish();
            });
            return viewHolder;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final OfflineMap offlineMap = activity.getQueries().get(position);
            holder.binding.download.setVisibility(!offlineMap.getIsDir() ? View.VISIBLE : View.GONE);
            holder.binding.retrieve.setVisibility(offlineMap.getIsDir() ? View.VISIBLE : View.GONE);
            holder.binding.label.setText(offlineMap.getName());
            if (offlineMap.getIsDir()) {
                holder.binding.info.setText(R.string.downloadmap_directory);
            } else {
                final String addInfo = offlineMap.getAddInfo();
                final String sizeInfo = offlineMap.getSizeInfo();
                holder.binding.info.setText(getString(R.string.downloadmap_mapfile)
                    + Formatter.SEPARATOR + offlineMap.getDateInfoAsString()
                    + (StringUtils.isNotBlank(addInfo) ? " (" + addInfo + ")" : "")
                    + (StringUtils.isNotBlank(sizeInfo) ? Formatter.SEPARATOR + offlineMap.getSizeInfo() : "")
                    + Formatter.SEPARATOR + offlineMap.getTypeAsString());
            }
        }
    }

    private class MapListTask extends AsyncTaskWithProgressText<Void, List<OfflineMap>> {
        private final Uri uri;
        private final String newSelectionTitle;

        MapListTask(final Activity activity, final Uri uri, final String newSelectionTitle) {
            super(activity, newSelectionTitle, getString(R.string.downloadmap_retrieving_directory_data));
            this.uri = uri;
            this.newSelectionTitle = newSelectionTitle;
            Log.i("starting MapDownloaderTask: " + uri.toString());
        }

        @Override
        protected List<OfflineMap> doInBackgroundInternal(final Void[] none) {
            final Parameters params = new Parameters();

            String page = "";
            try {
                final Response response = Network.getRequest(uri.toString(), params).blockingGet();
                page = Network.getResponseData(response, true);
            } catch (final Exception e) {
                return Collections.emptyList();
            }

            if (StringUtils.isBlank(page)) {
                Log.e("getMap: No data from server");
                return Collections.emptyList();
            }
            final List<OfflineMap> list = new ArrayList<>();

            try {
                current.analyzePage(uri, list, page);
                Collections.sort(list, (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName()));
                return list;
            } catch (final Exception e) {
                Log.e("Map downloader: error parsing parsing html page", e);
                return Collections.emptyList();
            }
        }

        @Override
        protected void onPostExecuteInternal(final List<OfflineMap> result) {
            setUpdateButtonVisibility();
            setMaps(result, newSelectionTitle, false);
        }
    }

    private class MapUpdateCheckTask extends AsyncTaskWithProgressText<Void, List<OfflineMap>> {
        private final ArrayList<CompanionFileUtils.DownloadedFileData> installedOfflineMaps;
        private final String newSelectionTitle;

        MapUpdateCheckTask(final Activity activity, final ArrayList<CompanionFileUtils.DownloadedFileData> installedOfflineMaps, final String newSelectionTitle) {
            super(activity, newSelectionTitle, activity.getString(R.string.downloadmap_checking_for_updates));
            this.installedOfflineMaps = installedOfflineMaps;
            this.newSelectionTitle = newSelectionTitle;
            Log.i("starting MapUpdateCheckTask");
        }

        @Override
        protected List<OfflineMap> doInBackgroundInternal(final Void[] none) {
            final List<OfflineMap> result = new ArrayList<>();
            result.add(new OfflineMap(getString(R.string.downloadmap_title), current.mapBase, true, "", "", current.offlineMapType));
            for (CompanionFileUtils.DownloadedFileData installedOfflineMap : installedOfflineMaps) {
                final OfflineMap offlineMap = checkForUpdate(installedOfflineMap);
                if (offlineMap != null && offlineMap.getDateInfo() > installedOfflineMap.remoteDate) {
                    offlineMap.setAddInfo(CalendarUtils.yearMonthDay(installedOfflineMap.remoteDate));
                    result.add(offlineMap);
                }
            }
            return result;
        }

        @Nullable
        private OfflineMap checkForUpdate(final CompanionFileUtils.DownloadedFileData offlineMapData) {
            final AbstractDownloader downloader = OfflineMap.OfflineMapType.getInstance(offlineMapData.remoteParsetype);
            if (downloader == null) {
                Log.e("Map update checker: Cannot find map downloader of type " + offlineMapData.remoteParsetype + " for file " + offlineMapData.localFile);
                return null;
            }

            final Parameters params = new Parameters();
            String page = "";
            try {
                final Response response = Network.getRequest(downloader.getUpdatePageUrl(offlineMapData.remotePage), params).blockingGet();
                page = Network.getResponseData(response, true);
            } catch (final Exception e) {
                return null;
            }

            if (StringUtils.isBlank(page)) {
                Log.e("getMap: No data from server");
                return null;
            }

            try {
                return downloader.checkUpdateFor(page, offlineMapData.remotePage, offlineMapData.remoteFile);
            } catch (final Exception e) {
                Log.e("Map update checker: error parsing parsing html page", e);
                return null;
            }
        }

        @Override
        protected void onPostExecuteInternal(final List<OfflineMap> result) {
            setMaps(result, newSelectionTitle, result.size() < 2);
        }
    }


    @SuppressLint("MissingSuperCall")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.mapdownloader_activity);
        binding = MapdownloaderActivityBinding.bind(findViewById(R.id.mapdownloader_activity_viewroot));

        spinnerData = OfflineMap.OfflineMapType.getOfflineMapTypes();
        final ArrayAdapter<OfflineMap.OfflineMapTypeDescriptor> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerData);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.downloaderType.setAdapter(spinnerAdapter);
        binding.downloaderType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                changeSource(position);
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // deliberately left empty
            }
        });
        binding.likeIt.setOnClickListener(v -> ShareUtils.openUrl(this, current.likeItUrl));
        binding.downloaderInfo.setOnClickListener(v -> {
            if (StringUtils.isNotBlank(current.projectUrl)) {
                ShareUtils.openUrl(this, current.projectUrl);
            }
        });
    }

    private void changeSource(final int position) {
        this.setTitle(R.string.downloadmap_title);
        maps.clear();
        adapter.notifyDataSetChanged();

        current = spinnerData.get(position).instance;
        installedOfflineMaps = CompanionFileUtils.availableOfflineMaps();

        binding.downloaderInfo.setVisibility(StringUtils.isNotBlank(current.mapSourceInfo) ? View.VISIBLE : View.GONE);
        binding.downloaderInfo.setText(current.mapSourceInfo);

        setUpdateButtonVisibility();
        binding.checkForUpdates.setOnClickListener(v -> {
            binding.checkForUpdates.setVisibility(View.GONE);
            new MapUpdateCheckTask(this, installedOfflineMaps, getString(R.string.downloadmap_available_updates)).execute();
        });

        MapDownloaderUtils.checkMapDirectory(this, true, (path, isWritable) -> {
            if (isWritable) {
                final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.mapdownloader_list, true, true);
                view.setAdapter(adapter);
                new MapListTask(this, current.mapBase, "").execute();
            } else {
                finish();
            }
        });
    }

    public List<OfflineMap> getQueries() {
        return maps;
    }

    private void setUpdateButtonVisibility() {
        binding.checkForUpdates.setVisibility (installedOfflineMaps != null && installedOfflineMaps.size() > 0 ? View.VISIBLE : View.GONE);
    }

    private synchronized void setMaps(final List<OfflineMap> maps, @NonNull final String selectionTitle, final boolean noUpdatesFound) {
        this.maps.clear();
        this.maps.addAll(maps);
        adapter.notifyDataSetChanged();
        this.setTitle(selectionTitle);

        final boolean showSpinner = !selectionTitle.equals(getString(R.string.downloadmap_available_updates));
        binding.downloaderType.setVisibility(showSpinner ? View.VISIBLE : View.GONE);
        binding.downloaderInfo.setVisibility(showSpinner ? View.VISIBLE : View.GONE);

        if (noUpdatesFound) {
            Dialogs.message(this, R.string.downloadmap_no_updates_found);
            new MapListTask(this, current.mapBase, "").execute();
        }
    }

}
