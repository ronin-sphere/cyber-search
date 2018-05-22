package fund.cyber.api.ethereum.functions

import fund.cyber.cassandra.ethereum.model.CqlEthereumContractMinedBlock
import fund.cyber.cassandra.ethereum.repository.PageableEthereumContractMinedBlockRepository
import fund.cyber.common.toSearchHashFormat
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux

class ContractBlocksByHash(
        private val contractMinedBlockRepository: PageableEthereumContractMinedBlockRepository
) : HandlerFunction<ServerResponse> {


    override fun handle(request: ServerRequest): Mono<ServerResponse> {

        val hash = request.pathVariable("hash")
        val page = request.queryParam("page").orElse("0").toInt()
        val pageSize = request.queryParam("pageSize").orElse("20").toInt()


        var slice = contractMinedBlockRepository
                .findAllByMinerContractHash(hash.toSearchHashFormat(), CassandraPageRequest.first(pageSize))

        for (i in 1..page) {
            if (slice.hasNext()) {
                slice = contractMinedBlockRepository.findAllByMinerContractHash(hash, slice.nextPageable())
            } else return ServerResponse.notFound().build()
        }
        return ServerResponse.ok().body(slice.content.toFlux(), CqlEthereumContractMinedBlock::class.java)
    }
}
