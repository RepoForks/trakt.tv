package com.mirhoseini.trakttv.view.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.mirhoseini.trakttv.R;
import com.mirhoseini.trakttv.core.di.module.PopularMoviesModule;
import com.mirhoseini.trakttv.core.util.Constants;
import com.mirhoseini.trakttv.core.viewmodel.PopularMoviesViewModel;
import com.mirhoseini.trakttv.di.component.ApplicationComponent;
import com.mirhoseini.trakttv.util.EndlessRecyclerViewScrollListener;
import com.mirhoseini.trakttv.util.ItemSpaceDecoration;
import com.mirhoseini.trakttv.view.BaseView;
import com.mirhoseini.trakttv.view.adapter.PopularMoviesRecyclerViewAdapter;
import com.mirhoseini.utils.Utils;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;
import tv.trakt.api.model.Movie;

/**
 * Created by Mohsen on 19/07/16.
 */

public class PopularMoviesFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {

    @Inject
    public PopularMoviesViewModel viewModel;

    @Inject
    Context context;
    @BindView(R.id.list)
    RecyclerView recyclerView;
    @BindView(R.id.no_internet)
    ImageView noInternet;
    @BindView(R.id.progress)
    ProgressBar progress;
    @BindView(R.id.progress_more)
    ProgressBar progressMore;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefresh;

    int page;

    private OnListFragmentInteractionListener listener;
    private PopularMoviesRecyclerViewAdapter adapter;
    private CompositeSubscription subscriptions;
    private LinearLayoutManager layoutManager;

    public PopularMoviesFragment() {
    }

    public static PopularMoviesFragment newInstance() {
        PopularMoviesFragment fragment = new PopularMoviesFragment();
        fragment.setRetainInstance(true);

        return fragment;
    }

    @OnClick(R.id.no_internet)
    void onNoInternetClick() {
        loadPopularMoviesData();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_popular, container, false);

        ButterKnife.bind(this, view);

        // allow pull to refresh on list
        swipeRefresh.setOnRefreshListener(this);

//        if (null == adapter)
        initAdapter();

        initRecyclerView();

        initBindings();

//        if (null == savedInstanceState) {
        loadPopularMoviesData();
//        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void initAdapter() {
        adapter = new PopularMoviesRecyclerViewAdapter(listener);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnListFragmentInteractionListener) {
            listener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    protected void injectDependencies(ApplicationComponent component) {
        component
                .plus(new PopularMoviesModule())
                .inject(this);
    }

    protected void initBindings() {
        // Observable that emits when the RecyclerView is scrolled to the bottom
        Observable<Integer> infiniteScrollObservable = Observable.create(subscriber -> {
            recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
                @Override
                public void onLoadMore(int page, int totalItemsCount) {
                    int newPage = page + 1;
                    Timber.d("Loading more movies, Page: %d", newPage);

                    subscriber.onNext(newPage);
                }
            });
        });

        subscriptions = new CompositeSubscription();

        subscriptions.addAll(
                // Bind loading status to show/hide progress
                viewModel
                        .isLoadingObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::setIsLoading),

                //Bind list of movies
                viewModel
                        .moviesObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::setPopularMoviesValue, this::showErrorMessage),

                // Trigger next page load when RecyclerView is scrolled to the bottom
                infiniteScrollObservable.subscribe(page -> loadMorePopularMoviesData(page))
        );
    }

    private void showErrorMessage(Throwable throwable) {
        Timber.e(throwable, "Error happened!!");

        if (null != adapter && adapter.getItemCount() > 0) {
            showMessage(throwable.getMessage());
        } else if (Utils.isConnected(context)) {
            showRetryMessage();
        } else {
            showNetworkConnectionError();
        }
    }

    private void showMessage(String message) {
        if (null != listener)
            listener.showMessage(message);
    }

    public void setIsLoading(boolean isLoading) {
        if (isLoading)
            showProgress();
        else
            hideProgress();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        listener = null;
        subscriptions.unsubscribe();
    }

    public void showProgress() {
        if (page == 1) {
            progress.setVisibility(View.VISIBLE);
            swipeRefresh.setRefreshing(true);
        } else {
            progressMore.setVisibility(View.VISIBLE);
        }

        noInternet.setVisibility(View.GONE);
    }

    public void hideProgress() {
        progress.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        progressMore.setVisibility(View.GONE);
    }

    public void showOfflineMessage() {
        if (null != listener) {
            listener.showOfflineMessage();
        }

        if (null == adapter || adapter.getItemCount() == 0) {
            noInternet.setVisibility(View.VISIBLE);
        }
    }

    public void showNetworkConnectionError() {
        if (null != listener) {
            listener.showNetworkConnectionError();
        }
    }

    public void showRetryMessage() {
        Timber.d("Showing Retry Message");

        Snackbar.make(getView(), R.string.retry_message, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.load_retry, v -> loadPopularMoviesData())
                .setActionTextColor(Color.RED)
                .show();
    }

    private void loadPopularMoviesData() {
        loadMorePopularMoviesData(1);
    }

    private void loadMorePopularMoviesData(int newPage) {
        page = newPage;

        subscriptions.add(
                viewModel
                        .loadPopularMoviesDataObservable(page, Constants.PAGE_ROW_LIMIT)
                        .subscribe(new Subscriber<ArrayList<Movie>>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
//                                showErrorMessage(e);
                            }

                            @Override
                            public void onNext(ArrayList<Movie> movies) {

                            }
                        })
        );
    }

    public void setPopularMoviesValue(ArrayList<Movie> movies) {
        Timber.d("Loaded Page: %d", page);

        if (null != movies) {
            adapter.setMovies(movies);
            adapter.notifyDataSetChanged();
        }

        if (!Utils.isConnected(context))
            showOfflineMessage();

        page++;
    }

    private void initRecyclerView() {
        layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);

        // add material margins to list items card view
        recyclerView.addItemDecoration(new ItemSpaceDecoration(48));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRefresh() {
        loadPopularMoviesData();
    }

    public interface OnListFragmentInteractionListener extends BaseView {
        void onListFragmentInteraction(Movie movie);
    }
}
