package com.mirhoseini.trakttv.view.activity;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mirhoseini.trakttv.R;
import com.mirhoseini.trakttv.di.component.ApplicationComponent;
import com.mirhoseini.trakttv.view.fragment.PopularMoviesFragment;
import com.mirhoseini.trakttv.view.fragment.SearchMoviesFragment;
import com.mirhoseini.utils.Utils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;
import tv.trakt.api.model.Movie;

/**
 * Created by Mohsen on 19/07/16.
 */

public class MainActivity extends BaseActivity implements PopularMoviesFragment.OnListFragmentInteractionListener, SearchMoviesFragment.OnListFragmentInteractionListener {

    public static final String TAG_POPULAR_MOVIES_FRAGMENT = "popular_movies_fragment";
    public static final String TAG_SEARCH_MOVIES_FRAGMENT = "search_movies_fragment";

    // injecting dependencies via Dagger
    @Inject
    Context context;
    @Inject
    Resources resources;

    // injecting views via ButterKnife
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.search_movies_fragment)
    ViewGroup searchContainer;
    AlertDialog internetConnectionDialog;
    private PopularMoviesFragment popularMoviesFragment;
    private SearchMoviesFragment searchMoviesFragment;
    private SearchView searchView = null;
    private MenuItem searchItem;


    @Override
    protected void injectDependencies(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // inject views using ButterKnife
        ButterKnife.bind(this);

        setupToolbar();

        Timber.d("Main Activity Created");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.while_logo);
    }

    @Override
    protected void onResume() {
        super.onResume();

        FragmentManager fragmentManager = getSupportFragmentManager();
        popularMoviesFragment = (PopularMoviesFragment) fragmentManager.findFragmentByTag(TAG_POPULAR_MOVIES_FRAGMENT);
        searchMoviesFragment = (SearchMoviesFragment) fragmentManager.findFragmentByTag(TAG_SEARCH_MOVIES_FRAGMENT);


        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (null == popularMoviesFragment || null == searchMoviesFragment) {
            createFragments();
        }

        attachFragments();

        Timber.d("Activity Resumed");
    }

    private void createFragments() {
        popularMoviesFragment = PopularMoviesFragment.newInstance();
        popularMoviesFragment.setRetainInstance(true);

        searchMoviesFragment = SearchMoviesFragment.newInstance();
        searchMoviesFragment.setRetainInstance(true);
    }

    private void attachFragments() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.popular_movies_fragment, popularMoviesFragment, TAG_POPULAR_MOVIES_FRAGMENT);
        fragmentTransaction.replace(R.id.search_movies_fragment, searchMoviesFragment, TAG_SEARCH_MOVIES_FRAGMENT);
        fragmentTransaction.commitAllowingStateLoss();
    }


    @Override
    public void onListFragmentInteraction(Movie movie) {
        /* Movie's full data can be loaded into another activity*/

//         Intent movieIntent = MovieActivity.newIntent(context, movie);
//         startActivity(movie);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        if (searchItem != null) {
            searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        }

        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    Timber.i("onQueryTextChange: %s", newText);

                    searchMoviesFragment.updateQuery(newText);
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    Timber.i("onQueryTextSubmit: %s", query);

                    searchMoviesFragment.updateQuery(query);
                    return true;
                }
            });

            searchView.setOnSearchClickListener(view -> showSearch());

            searchView.setOnCloseListener(() -> {
                hideSearch();
                return false;
            });

        }

        return true;
    }

    private void hideSearch() {
        searchContainer.setVisibility(View.GONE);
    }

    private void showSearch() {
        searchContainer.setVisibility(View.VISIBLE);
        //clear previous search results
        searchMoviesFragment.updateQuery("");
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void showMessage(String message) {
        Timber.d("Showing Message: %s", message);

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showOfflineMessage() {
        Timber.d("Showing Offline Message");

        Snackbar.make(toolbar, R.string.offline_message, Snackbar.LENGTH_LONG)
                .setAction(R.string.go_online, v -> {
                    startActivity(new Intent(
                            Settings.ACTION_WIFI_SETTINGS));
                })
                .setActionTextColor(Color.GREEN)
                .show();
    }

    @Override
    public void showConnectionError() {
        Timber.d("Showing Connection Error Message");

        hideInternetConnectionError();
        internetConnectionDialog = Utils.showNoInternetConnectionDialog(this, true);
    }

    public void hideInternetConnectionError() {
        if (internetConnectionDialog != null)
            internetConnectionDialog.dismiss();
    }

}
