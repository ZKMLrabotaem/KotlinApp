data class GameDisc(
    val name: String = "",
    val description: String = "",
    val id: String = "",
    val price: Double = 0.0,
    val genre: String = "",
    val age: Long  = 0,
    val imageUrls: List<String> = emptyList(),
    val rating: List<Double> = emptyList(),
    val comments: List<String> = emptyList()
)
