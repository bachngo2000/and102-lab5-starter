package com.codepath.articlesearch

// We already have an Article data class from the previous lab, and we now have an ArticleEntity data class for our database.
// Unfortunately these classes don't quite match up... so it makes passing the data around complicated.
//To make it easier for ourselves, it's standard to create a common data class.

// create the simplest data class of the Article type that we can
data class DisplayArticle(
    val headline: String?,
    val abstract: String?,
    val byline: String?,
    val mediaImageUrl: String?
) : java.io.Serializable