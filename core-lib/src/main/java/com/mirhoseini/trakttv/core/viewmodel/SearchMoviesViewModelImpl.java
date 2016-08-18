package com.mirhoseini.trakttv.core.viewmodel;

import com.mirhoseini.trakttv.core.client.TraktApi;
import com.mirhoseini.trakttv.core.util.Constants;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

import rx.Observable;
import rx.subjects.BehaviorSubject;
import tv.trakt.api.model.SearchMovieResult;

/**
 * Created by Mohsen on 19/07/16.
 */

public class SearchMoviesViewModelImpl implements SearchMoviesViewModel {

    private TraktApi api;

    private BehaviorSubject<ArrayList<SearchMovieResult>> subject = BehaviorSubject.create(new ArrayList());
    private BehaviorSubject<Boolean> isLoadingSubject = BehaviorSubject.create(false);

    @Inject
    public SearchMoviesViewModelImpl(TraktApi api) {
        this.api = api;
    }

    @Override
    public Observable<ArrayList<SearchMovieResult>> searchMoviesObservable(String query, int page, int limit) {

        // Don't try and load if we're already loading or query is empty
        if (isLoadingSubject.getValue() || query.isEmpty()) {
            return Observable.empty();
        }

        //show loading progress
        isLoadingSubject.onNext(true);

        // stop previous search
//        if (null != moviesSubscription && !moviesSubscription.isUnsubscribed())
//            moviesSubscription.unsubscribe();

        return api.searchMovies(query, page, limit, Constants.API_EXTENDED_FULL_IMAGES)
                //convert Movies array to List
                .map(searchMovieResults -> new ArrayList<>(Arrays.asList(searchMovieResults)))
                // Concatenate the new movies to the current posts list, then emit it via the subject
                .doOnNext(searchMovieResultArrayList -> {
                    ArrayList fullList = new ArrayList(Arrays.asList(subject.getValue()));
                    fullList.addAll(searchMovieResultArrayList);
                    subject.onNext(fullList);
                })
                .doOnTerminate(() -> isLoadingSubject.onNext(false));

//            subscription = interactor.searchMovies(query, page, limit).subscribe(searchResult ->
//                    {
//                        if (null != view) {
//                            view.hideProgress();
//                            view.setSearchMoviesValue(searchResult);
//
//                            if (!isConnected)
//                                view.showOfflineMessage();
//                        }
//                    },
//                    throwable -> {
//                        if (null != view) {
//                            view.hideProgress();
//                        }
//
//                        if (isConnected) {
//                            if (null != view) {
//                                view.showRetryMessage();
//                            }
//                        } else {
//                            if (null != view) {
//                                view.showOfflineMessage();
//                            }
//                        }
//                    });

    }

    @Override
    public Observable<ArrayList<SearchMovieResult>> moviesObservable() {
        return subject.asObservable();
    }

    @Override
    public Observable<Boolean> isLoadingObservable() {
        return isLoadingSubject.asObservable();
    }

}
