package fund.cyber.dump.ethereum

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import fund.cyber.cassandra.ethereum.model.*
import fund.cyber.cassandra.ethereum.repository.*
import fund.cyber.search.model.chains.ChainEntityType
import fund.cyber.search.model.chains.ChainFamily
import fund.cyber.search.model.chains.ChainInfo
import io.micrometer.core.instrument.Timer
import org.junit.jupiter.api.Test
import org.springframework.util.Assert
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class EthereumMessageListenerResolverTest {

    private val chainInfo = ChainInfo(ChainFamily.ETHEREUM)

    private val txLatencyMetric = mock<Timer>()

    @Test
    fun testGetListenerByType() {
        val txRepository = mock<EthereumTxRepository> {
            on { save(any<CqlEthereumTx>()) }.thenReturn(Mono.empty())
            on { delete(any()) }.thenReturn(Mono.empty())
            on { findById(any<String>()) }.thenReturn(Mono.empty())
        }
        val blockTxRepository = mock<EthereumBlockTxRepository> {
            on { save(any<CqlEthereumBlockTxPreview>()) }.thenReturn(Mono.empty())
            on { delete(any()) }.thenReturn(Mono.empty())
        }
        val contractTxRepository = mock<EthereumContractTxRepository> {
            on { saveAll(any<Iterable<CqlEthereumContractTxPreview>>()) }.thenReturn(Flux.empty())
            on { deleteAll(any<Iterable<CqlEthereumContractTxPreview>>()) }.thenReturn(Mono.empty())
        }

        val txDumpProcess = TxDumpProcess(txRepository, blockTxRepository, contractTxRepository, chainInfo, 0, txLatencyMetric)

        val blockRepository = mock<EthereumBlockRepository> {
            on { save(any<CqlEthereumBlock>()) }.thenReturn(Mono.empty<CqlEthereumBlock>())
            on { delete(any()) }.thenReturn(Mono.empty<Void>())
        }

        val contractMinedBlockRepository = mock<EthereumContractMinedBlockRepository> {
            on { save(any<CqlEthereumContractMinedBlock>()) }.thenReturn(Mono.empty<CqlEthereumContractMinedBlock>())
            on { delete(any()) }.thenReturn(Mono.empty<Void>())
        }

        val uncleRepository = mock<EthereumUncleRepository> {
            on { save(any<CqlEthereumUncle>()) }.thenReturn(Mono.empty())
            on { delete(any()) }.thenReturn(Mono.empty())
        }

        val contractUncleRepository = mock<EthereumContractUncleRepository> {
            on { save(any<CqlEthereumContractMinedUncle>()) }.thenReturn(Mono.empty())
            on { delete(any()) }.thenReturn(Mono.empty())
        }

        val ethereumMessageListenerResolver = EthereumMessageListenerResolver(
            chainInfo,
            0,
            blockRepository,
            contractMinedBlockRepository,
            txRepository,
            blockTxRepository,
            contractTxRepository,
            uncleRepository,
            contractUncleRepository,
            txLatencyMetric
        )

        val result = ethereumMessageListenerResolver.getListenerByType(ChainEntityType.BLOCK)
        Assert.notNull(result, "result listener resolver needs to be created")
    }


}
