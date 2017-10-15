package fr.nihilus.music.ui.artists;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import fr.nihilus.music.Constants;
import fr.nihilus.music.R;
import fr.nihilus.music.di.ActivityScoped;
import fr.nihilus.music.library.BrowserViewModel;
import fr.nihilus.music.library.NavigationController;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.recyclerfragment.RecyclerFragment;

@ActivityScoped
public class ArtistsFragment extends RecyclerFragment implements ArtistAdapter.OnArtistSelectedListener {

    private static final String TAG = "ArtistsFragment";

    private ArtistAdapter mAdapter;
    private BrowserViewModel mViewModel;

    @Inject NavigationController mNavigation;

    public static ArtistsFragment newInstance() {
        Bundle args = new Bundle(1);
        args.putInt(Constants.FRAGMENT_ID, R.id.action_artists);
        ArtistsFragment fragment = new ArtistsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private final SubscriptionCallback mCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> artists) {
            Log.d(TAG, "onChildrenLoaded: loaded " + artists.size() + " artists.");
            mAdapter.updateArtists(artists);
            setRecyclerShown(true);
        }
    };

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new ArtistAdapter(this);
        mAdapter.setOnArtistSelectedListener(this);
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getRecyclerView().setHasFixedSize(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.action_artists);
        mViewModel.subscribe(MediaID.ID_ARTISTS, mCallback);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(getActivity()).get(BrowserViewModel.class);

        setAdapter(mAdapter);
        if (savedInstanceState == null) {
            setRecyclerShown(false);
        }
    }

    @Override
    public void onStop() {
        mViewModel.unsubscribe(MediaID.ID_ARTISTS);
        super.onStop();
    }

    @Override
    public void onArtistSelected(ArtistAdapter.ArtistHolder holder, MediaItem artist) {
        mNavigation.navigateToArtistDetail(artist);
    }
}
