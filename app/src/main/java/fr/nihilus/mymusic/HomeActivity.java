package fr.nihilus.mymusic;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import fr.nihilus.mymusic.settings.SettingsActivity;
import fr.nihilus.mymusic.ui.AlbumGridFragment;
import fr.nihilus.mymusic.ui.SongListFragment;
import fr.nihilus.mymusic.utils.MediaIDHelper;
import fr.nihilus.mymusic.utils.PermissionUtil;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setupNavigationDrawer();

        if (savedInstanceState == null) {
            /*if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this).setMessage(R.string.external_storage_rationale)
                        .setNeutralButton(R.string.ok, null).create().show();
            }*/

            PermissionUtil.requestExternalStoragePermission(this);
            if (PermissionUtil.hasExternalStoragePermission(this)) {
                mNavigationView.setCheckedItem(R.id.action_all);
                swapFragment(new SongListFragment());
            }
        }

        MediaBrowserFragment.getInstance(getSupportFragmentManager())
                .subscribe(MediaIDHelper.MEDIA_ID_DAILY, mSubscriptionCallback);
    }

    private void setupNavigationDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_opened, R.string.drawer_closed);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mNavigationView = (NavigationView) findViewById(R.id.navDrawer);
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        onOptionsItemSelected(item);
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.EXTERNAL_STORAGE_REQUEST) {
            mNavigationView.setCheckedItem(R.id.action_all);
            swapFragment(new SongListFragment());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_all:
                swapFragment(new SongListFragment());
                return true;
            case R.id.action_albums:
                swapFragment(new AlbumGridFragment());
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void swapFragment(Fragment newFrag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, newFrag)
                .commit();
    }

    private final SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            MediaItem daily = children.get(0);
            Uri artUri = daily.getDescription().getIconUri();
            CharSequence title = daily.getDescription().getTitle();
            CharSequence subtitle = daily.getDescription().getSubtitle();

            View header = mNavigationView.getHeaderView(0);
            ((TextView) header.findViewById(R.id.title)).setText(title);
            ((TextView) header.findViewById(R.id.subtitle)).setText(subtitle);

            ImageView albumArtView = (ImageView) header.findViewById(R.id.albumArt);
            if (artUri != null) {
                albumArtView.setImageURI(artUri);
            } else {
                albumArtView.setImageResource(R.drawable.dummy_album_art);
            }
        }
    };
}
