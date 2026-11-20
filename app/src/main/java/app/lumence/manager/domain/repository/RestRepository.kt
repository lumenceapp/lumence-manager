package app.lumence.manager.domain.repository

import app.lumence.manager.network.service.RestService

class RestRepository(
    private val service: RestService
) {

    suspend fun getLatestRelease(repo: String) = service.getLatestRelease(repo)

    suspend fun getCommits(repo: String, page: Int = 1) = service.getCommits(repo, page)

}