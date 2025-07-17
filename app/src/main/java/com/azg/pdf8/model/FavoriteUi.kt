package com.azg.pdf8.model

data class FavoriteUi(
  val document: FavoriteDocument,
  val isFavorite: Boolean
)
data class RecentUi(
  val document: RecentDocument,
  val isFavorite: Boolean
)