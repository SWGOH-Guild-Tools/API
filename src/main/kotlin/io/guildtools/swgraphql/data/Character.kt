package io.guildtools.swgraphql.data

import com.fasterxml.jackson.annotation.JsonProperty

data class Character (
	@JsonProperty("name")
	val name : String,
	@JsonProperty("base_id")
	val base_id : String,
	@JsonProperty("texture")
	val texture : String,
	@JsonProperty("alignment")
	val alignment : String,
	@JsonProperty("categories")
	val categories : List<String>,
	@JsonProperty("role")
	val role : String,
	@JsonProperty("combatType")
	val combatType: Int
)