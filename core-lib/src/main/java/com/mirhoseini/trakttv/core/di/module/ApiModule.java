package com.mirhoseini.trakttv.core.di.module;

import com.google.gson.Gson;
import com.mirhoseini.trakttv.core.client.TraktApi;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

/**
 * Created by Mohsen on 19/07/16.
 */

@Module
public class ApiModule {

    @Provides
    @Singleton
    public TraktApi provideTraktApiService(Retrofit retrofit) {
        return retrofit.create(TraktApi.class);
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(HttpUrl baseUrl, Converter.Factory converterFactory, CallAdapter.Factory callAdapterFactory, OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(callAdapterFactory)
                .client(okHttpClient)
                .build();
    }

    @Provides
    @Singleton
    public Converter.Factory provideGsonConverterFactory(Gson gson) {
        return GsonConverterFactory.create(gson);
    }

    @Singleton
    @Provides
    public Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    public CallAdapter.Factory provideRxJavaCallAdapterFactory() {
        return RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());
    }

}
