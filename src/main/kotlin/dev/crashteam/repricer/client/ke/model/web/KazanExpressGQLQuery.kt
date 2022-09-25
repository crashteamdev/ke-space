package dev.crashteam.repricer.client.ke.model.web

data class KazanExpressGQLQuery<T>(
    val operationName: String,
    val query: String,
    val variables: T
)

data class CategoryGQLQueryVariables(
    val queryInput: CategoryGQLQueryInput
)

data class CategoryGQLQueryInput(
    val categoryId: String,
    val pagination: CategoryGQLQueryInputPagination,
    val showAdultContent: String,
    val filters: List<Any> = emptyList(),
    val sort: String,
)

data class CategoryGQLQueryInputPagination(
    val offset: Long,
    val limit: Int
)
