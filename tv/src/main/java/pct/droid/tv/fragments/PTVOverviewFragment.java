/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package pct.droid.tv.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import hugo.weaving.DebugLog;
import pct.droid.base.providers.media.EZTVProvider;
import pct.droid.base.providers.media.MediaProvider;
import pct.droid.base.providers.media.YTSProvider;
import pct.droid.base.providers.media.models.Media;
import pct.droid.base.providers.media.models.Movie;
import pct.droid.base.providers.media.models.Show;
import pct.droid.base.providers.subs.SubsProvider;
import pct.droid.base.providers.subs.YSubsProvider;
import pct.droid.base.torrent.StreamInfo;
import pct.droid.tv.BuildConfig;
import pct.droid.tv.R;
import pct.droid.tv.activities.PTVMediaDetailActivity;
import pct.droid.tv.activities.PTVSearchActivity;
import pct.droid.tv.activities.PTVVideoPlayerActivity;
import pct.droid.tv.presenters.MorePresenter;
import pct.droid.tv.presenters.MediaCardPresenter;
import pct.droid.tv.utils.BackgroundUpdater;

/*
 * Main class to show BrowseFragment with header and rows of videos
 */
public class PTVOverviewFragment extends BrowseFragment {


    private ArrayObjectAdapter mRowsAdapter;
    private ListRowPresenter mListRowPresenter;
    private ArrayObjectAdapter mShowAdapter;

    private YTSProvider mMoviesProvider = new YTSProvider();
    private EZTVProvider mShowsProvider = new EZTVProvider();
    private ArrayObjectAdapter mMoviesAdapter;

    private BackgroundUpdater mBackgroundUpdater;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //		loadVideoData();

        //setup background updater
        mBackgroundUpdater = new BackgroundUpdater();
        mBackgroundUpdater.initialise(getActivity(), R.drawable.default_row_background);

        //setup main adapter
        mListRowPresenter = new ListRowPresenter();
        mListRowPresenter.setShadowEnabled(false);
        mRowsAdapter = new ArrayObjectAdapter(mListRowPresenter);
        setAdapter(mRowsAdapter);


        setupUIElements();

        setupEventListeners();

        setupAdapters();

        loadData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundUpdater) mBackgroundUpdater.destroy();
    }


    private void setupUIElements() {
        setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.header_logo));
        setTitle(getString(R.string.app_name)); // Badge, when set, takes precedent over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.primary));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.primary_dark));
    }

    private void loadData() {
        final MediaProvider.Filters filters = new MediaProvider.Filters();
        filters.sort = MediaProvider.Filters.Sort.DATE;
        mShowsProvider.getList(null, filters, new MediaProvider.Callback() {
            @DebugLog
            @Override
            public void onSuccess(MediaProvider.Filters filters, ArrayList<Media> items, boolean changed) {
                List<MediaCardPresenter.MediaCardItem> list = MediaCardPresenter.convertMediaToOverview(items);
                mShowAdapter.clear();
                mShowAdapter.addAll(0, list);
            }

            @DebugLog
            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "error getting show list", Toast.LENGTH_SHORT).show();

            }
        });

        mMoviesProvider.getList(null, null, new MediaProvider.Callback() {
            @DebugLog
            @Override
            public void onSuccess(MediaProvider.Filters filters, ArrayList<Media> items, boolean changed) {
                List<MediaCardPresenter.MediaCardItem> list = MediaCardPresenter.convertMediaToOverview(items);
                mMoviesAdapter.clear();
                mMoviesAdapter.addAll(0, list);
            }

            @DebugLog
            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "error getting movie list", Toast.LENGTH_SHORT).show();

            }
        });
    }


    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                PTVSearchActivity.startActivity(getActivity());
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }


    private void setupAdapters() {
        setupMovies();
        setupShows();
        setupMore();
    }


    private void setupMovies() {
        HeaderItem moviesHeader = new HeaderItem(0, getString(R.string.top_movies));
        MediaCardPresenter mediaCardPresenter = new MediaCardPresenter(getActivity());
        mMoviesAdapter = new ArrayObjectAdapter(mediaCardPresenter);
        mMoviesAdapter.add(new MediaCardPresenter.MediaCardItem(true));
        mRowsAdapter.add(new ListRow(moviesHeader, mMoviesAdapter));
    }

    private void removeMovies() {
        mRowsAdapter.remove(mMoviesAdapter);
    }

    private void addMoviesLoading() {
        mMoviesAdapter.clear();
    }

    private void setupShows() {
        HeaderItem showsHeader = new HeaderItem(0, getString(R.string.latest_shows));
        MediaCardPresenter mediaCardPresenter = new MediaCardPresenter(getActivity());
        mShowAdapter = new ArrayObjectAdapter(mediaCardPresenter);
        mShowAdapter.add(new MediaCardPresenter.MediaCardItem(true));

        mRowsAdapter.add(new ListRow(showsHeader, mShowAdapter));
    }

    private void setupMore() {
        HeaderItem gridHeader = new HeaderItem(0, getString(R.string.more));

        MorePresenter gridPresenter = new MorePresenter(getActivity());
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
        gridRowAdapter.add(new MorePresenter.MoreItem(R.string.about, R.drawable.ic_about));
        gridRowAdapter.add(new MorePresenter.MoreItem(R.string.settings, R.drawable.ic_settings));
        if (BuildConfig.DEBUG)
            gridRowAdapter.add(new MorePresenter.MoreItem(R.string.tests, R.drawable.ic_av_play));
        mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof MediaCardPresenter.MediaCardItem) {

                MediaCardPresenter.MediaCardItem media = (MediaCardPresenter.MediaCardItem) item;
                if (media.isLoading()) return;
                Bundle options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        PTVMediaDetailActivity.SHARED_ELEMENT_NAME).toBundle();
                if (media.getMedia() instanceof Movie)
                    PTVMediaDetailActivity.startActivity(getActivity(), options, media.getMedia(), media.getMedia().headerImage,media.getMedia().image);
                else if (media.getMedia() instanceof Show)
                    PTVMediaDetailActivity.startActivity(getActivity(), options, media.getMedia(), media.getMedia().headerImage,media.getMedia().image);
            } else {
                openPlayerTestDialog();
            }

        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof MediaCardPresenter.MediaCardItem) {
                MediaCardPresenter.MediaCardItem overviewItem = (MediaCardPresenter.MediaCardItem) item;
                if (overviewItem.isLoading()) return;

                mBackgroundUpdater.updateBackgroundAsync(((MediaCardPresenter.MediaCardItem) item).getMedia().headerImage);
            }

        }
    }

    private void openPlayerTestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final String[] file_types = getResources().getStringArray(R.array.file_types);
        final String[] files = getResources().getStringArray(R.array.files);

        builder.setTitle("Player Tests")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setSingleChoiceItems(file_types, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                dialogInterface.dismiss();
                final String location = files[index];
                if (location.equals("dialog")) {
                    final EditText dialogInput = new EditText(getActivity());
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                            .setView(dialogInput)
                            .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Movie media = new Movie(new YTSProvider(), new YSubsProvider());

                                    media.videoId = "dialogtestvideo";
                                    media.title = "User input test video";

                                    PTVVideoPlayerActivity.startActivity(getActivity(), new StreamInfo(media, null, null, null, null, location), 0);
                                }
                            });
                }
                final Movie media = new Movie(new YTSProvider(), new YSubsProvider());
                media.videoId = "bigbucksbunny";
                media.title = file_types[index];
                media.subtitles = new HashMap<String, String>();
                media.subtitles.put("en", "http://sv244.cf/bbb-subs.srt");

                SubsProvider.download(getActivity(), media, "en", new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        PTVVideoPlayerActivity.startActivity(getActivity(), new StreamInfo(media, null, null, null, null, location));
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        PTVVideoPlayerActivity.startActivity(getActivity(), new StreamInfo(media, null, null, null, null, location));
                    }
                });
            }
        });

        builder.show();
    }
}
