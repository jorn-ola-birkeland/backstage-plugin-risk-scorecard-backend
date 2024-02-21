package no.kvros.ros

import no.kvros.ros.models.ROSWrapperObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ros")
class ROSController(
    private val rosService: ROSService,
) {
    @GetMapping("/{repositoryOwner}/{repositoryName}/all")
    fun fetchAllROSes(
        @RequestHeader("Github-Access-Token") githubAccessToken: String,
        @PathVariable repositoryOwner: String,
        @PathVariable repositoryName: String,
    ): ResponseEntity<List<ROSResultDTO>> {
        val result = rosService.fetchAllROSes(repositoryOwner, repositoryName, githubAccessToken)

        val successResults = result.filter { it.status == ContentStatus.Success }

        return when {
            successResults.isNotEmpty() -> ResponseEntity.ok().body(successResults)
            else -> ResponseEntity.internalServerError().body(result)
        }
    }

    @PostMapping("/{repositoryOwner}/{repositoryName}", produces = ["text/plain"])
    fun createNewROS(
        @RequestHeader("Github-Access-Token") githubAccessToken: String,
        @PathVariable repositoryOwner: String,
        @PathVariable repositoryName: String,
        @RequestBody ros: ROSWrapperObject,
    ): ResponseEntity<ProcessROSResultDTO> {
        val response =
            rosService.createROS(
                owner = repositoryOwner,
                repository = repositoryName,
                accessToken = githubAccessToken,
                content = ros,
            )

        return when (response.status) {
            ProcessingStatus.ROSNotValid,
            ProcessingStatus.EncryptionFailed,
            ProcessingStatus.CouldNotCreateBranch,
            ProcessingStatus.ErrorWhenUpdatingROS,
            ->
                ResponseEntity
                    .internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response)

            ProcessingStatus.CreatedROS,
            ProcessingStatus.UpdatedROS,
            ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response)
        }
    }

    @PutMapping("/{repositoryOwner}/{repositoryName}/{id}", produces = ["text/plain"])
    fun editROS(
        @RequestHeader("Github-Access-Token") githubAccessToken: String,
        @PathVariable repositoryOwner: String,
        @PathVariable id: String,
        @PathVariable repositoryName: String,
        @RequestBody ros: ROSWrapperObject,
    ): ResponseEntity<ProcessROSResultDTO> {
        val editResult =
            rosService.updateROS(
                owner = repositoryOwner,
                repository = repositoryName,
                content = ros,
                rosId = id,
                accessToken = githubAccessToken,
            )

        return when (editResult.status) {
            ProcessingStatus.ROSNotValid,
            ProcessingStatus.EncryptionFailed,
            ProcessingStatus.CouldNotCreateBranch,
            ProcessingStatus.ErrorWhenUpdatingROS,
            ->
                ResponseEntity
                    .internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(editResult)

            ProcessingStatus.CreatedROS,
            ProcessingStatus.UpdatedROS,
            ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(editResult)
        }
    }

    @GetMapping("/{repositoryOwner}/{repositoryName}/sentToPublication")
    fun fetchAllDraftsSentToPublication(
        @RequestHeader("Github-Access-Token") githubAccessToken: String,
        @PathVariable repositoryOwner: String,
        @PathVariable repositoryName: String,
    ): ResponseEntity<ROSIdentifiersResultDTO> {
        val result = rosService.fetchAllROSDraftsSentToPublication(repositoryOwner, repositoryName, githubAccessToken)

        return when (result.status) {
            SimpleStatus.Success -> ResponseEntity.ok().body(result)
            SimpleStatus.Failure -> ResponseEntity.internalServerError().body(result)
        }
    }

    @PostMapping("/{repositoryOwner}/{repositoryName}/publish/{id}", produces = ["application/json"])
    fun sendROSForPublishing(
        @RequestHeader("Github-Access-Token") githubAccessToken: String,
        @PathVariable repositoryOwner: String,
        @PathVariable repositoryName: String,
        @PathVariable id: String,
    ): ResponseEntity<ROSPublishedObjectResultDTO> {
        val result = rosService.publishROS(repositoryOwner, repositoryName, id, githubAccessToken)

        return when (result.status) {
            SimpleStatus.Success -> ResponseEntity.ok().body(result)
            SimpleStatus.Failure -> ResponseEntity.internalServerError().body(result)
        }
    }
}
