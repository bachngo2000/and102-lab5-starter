package com.codepath.articlesearch

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Data entities represent tables in your app's database. Each instance of an ArticleEntity data class represents a row in a table for articles in the app's database.
// Essentially, we're creating a "template" for each row in our future database table, by specifying the columns. (This is somewhat similar to the models we used when serializing JSON!)
// create our ArticleEntity with the values we want to save in the database
@Entity(tableName = "article_table")
class ArticleEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "headline")
    val headline: String?,

    @ColumnInfo(name = "articleAbstract")
    val articleAbstract: String?,

    @ColumnInfo(name = "byline")
    val byline: String?,

    @ColumnInfo(name = "mediaImageUrl")
    val mediaImageUrl: String?
)
