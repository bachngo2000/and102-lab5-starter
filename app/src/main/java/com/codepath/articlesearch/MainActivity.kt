package com.codepath.articlesearch

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepath.articlesearch.databinding.ActivityMainBinding
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Headers
import org.json.JSONException

fun createJson() = Json {
    isLenient = true
    ignoreUnknownKeys = true
    useAlternativeNames = false
}

private const val TAG = "MainActivity/"
private const val SEARCH_API_KEY = BuildConfig.API_KEY
private const val ARTICLE_SEARCH_URL =
    "https://api.nytimes.com/svc/search/v2/articlesearch.json?api-key=${SEARCH_API_KEY}"

class MainActivity : AppCompatActivity() {
    private val articles = mutableListOf<DisplayArticle>()
    private lateinit var articlesRecyclerView: RecyclerView
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        articlesRecyclerView = findViewById(R.id.articles)
        val articleAdapter = ArticleAdapter(this, articles)
        articlesRecyclerView.adapter = articleAdapter
        articlesRecyclerView.layoutManager = LinearLayoutManager(this).also {
            val dividerItemDecoration = DividerItemDecoration(this, it.orientation)
            articlesRecyclerView.addItemDecoration(dividerItemDecoration)
        }

        // when we set up our DAO, we used a Kotlin Flow return type on the getAll() function.
        // This means we can now set this up to:
        //  - Listen to any changes to items in the database
        //  - When we have a new list of items to display:
        //      + Map new items to DisplayArticles
        //      + Update our UI by passing the new list to our ArticleAdapter
        // Notice that for this coroutine, we didn't specify a Dispatcher -- so we're using the Main one by default.
        lifecycleScope.launch {
            (application as ArticleApplication).db.articleDao().getAll().collect { databaseList ->
                databaseList.map { entity ->
                    DisplayArticle(
                        entity.headline,
                        entity.articleAbstract,
                        entity.byline,
                        entity.mediaImageUrl
                    )
                }.also { mappedList ->
                    articles.clear()
                    articles.addAll(mappedList)
                    articleAdapter.notifyDataSetChanged()
                }
            }
        }

        val client = AsyncHttpClient()
        client.get(ARTICLE_SEARCH_URL, object : JsonHttpResponseHandler() {
            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                response: String?,
                throwable: Throwable?
            ) {
                Log.e(TAG, "Failed to fetch articles: $statusCode")
            }

            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                Log.i(TAG, "Successfully fetched articles: $json")
                try {
                    val parsedJson = createJson().decodeFromString(
                        SearchNewsResponse.serializer(),
                        json.jsonObject.toString()
                    )
                    // When we think about displaying data, we should think about the Single Source of Truth principle.
                    // It's a bad scenario if we update the UI from multiple sources at once, such as the database and the API. What if they conflict? How do we avoid duplicate data?
                    // We want to be displaying our cached data, so the cleanest way to solve this is to only display items from the database.
                    // When we make API calls, instead of showing that data, we load that data into the database. The database becomes our single source of truth.

                    // Before we do all of the above:
                    // database operations can take a long time and they are not allowed to run on the main UI thread because they can slow down the app and make user interactions laggy and unresponsive.
                    // We need to update any articleDao interaction to run on another thread. We can use Kotlin coroutines for this. They can be a lightweight and simple way to "launch" some code to run asynchronously on a different thread. In this case, we want to attach this coroutine to the lifecycle of our View. Any coroutine launched in this scope is cancelled when the Lifecycle is destroyed
                    // To specify where the coroutines should run,  Kotlin provides three dispatchers that you can use:
                    //     1. Dispatchers.Main - Use this dispatcher to run a coroutine on the main Android thread. This should be used only for interacting with the UI and performing quick work. Examples include calling suspend functions, running Android UI framework operations, and updating LiveData objects. The default if no dispatcher is specified.
                    //     2. Dispatchers.IO - This dispatcher is optimized to perform disk or network I/O outside of the main thread. Examples include using the Room component, reading from or writing to files, and running any network operations.
                    //     3. Dispatchers.Default - This dispatcher is optimized to perform CPU-intensive work outside of the main thread. Example use cases include sorting a list and parsing JSON.
                    // Since we are using Room and writing to files, we want to use the IO Dispatcher.
                    lifecycleScope.launch(IO) {
                        parsedJson.response?.docs?.let { list ->
                            // Delete everything previously in the database
                            (application as ArticleApplication).db.articleDao().deleteAll()
                            // Insert the new data into the database
                            // Next, we need to parse the data. We will have to map our API Article data type to an ArticleEntity type and clear out the existing cache.
                            (application as ArticleApplication).db.articleDao().insertAll( list.map {
                                ArticleEntity(
                                    headline = it.headline?.main,
                                    articleAbstract = it.abstract,
                                    byline = it.byline?.original,
                                    mediaImageUrl = it.mediaImageUrl
                                )
                            })
                        }
                    }
                } catch (e: JSONException) {
                    Log.e(TAG, "Exception: $e")
                }
            }

        })

    }
}