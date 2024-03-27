package com.codepath.articlesearch

import android.app.Application

// To interact with our database we will need to get an instance of it!

//We will only want to create this once per app, so we will use an Android Application class.

// The Application class in Android is the base class within an Android app that contains all other components such as activities and services.
class ArticleApplication : Application() {

    // A lazy initialization here just means we don't create this variable until it needs to be used
    val db by lazy {
        AppDatabase.getInstance(this)
    }
}