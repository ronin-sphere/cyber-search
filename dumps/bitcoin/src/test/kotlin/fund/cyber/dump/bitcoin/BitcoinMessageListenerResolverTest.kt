package fund.cyber.dump.bitcoin

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import fund.cyber.cassandra.bitcoin.model.CqlBitcoinBlock
import fund.cyber.cassandra.bitcoin.model.CqlBitcoinContractMinedBlock
import fund.cyber.cassandra.bitcoin.model.CqlBitcoinContractTxPreview
import fund.cyber.cassandra.bitcoin.model.CqlBitcoinTx
import fund.cyber.cassandra.bitcoin.repository.*
import fund.cyber.search.model.bitcoin.BitcoinTx
import fund.cyber.search.model.bitcoin.BitcoinTxIn
import fund.cyber.search.model.bitcoin.BitcoinTxOut
import fund.cyber.search.model.bitcoin.SignatureScript
import fund.cyber.search.model.chains.ChainEntityType
import fund.cyber.search.model.chains.ChainFamily
import fund.cyber.search.model.chains.ChainInfo
import fund.cyber.search.model.events.PumpEvent
import fund.cyber.search.model.events.txPumpTopic
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.springframework.util.Assert
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant

class BitcoinMessageListenerResolverTest {

    private val chainInfo = ChainInfo(ChainFamily.BITCOIN)

    @Test
    fun testGetListenerByType() {

        val blockRepository = mock<BitcoinBlockRepository> {
            on { save(any<CqlBitcoinBlock>()) }.thenReturn(Mono.empty<CqlBitcoinBlock>())
            on { delete(any()) }.thenReturn(Mono.empty<Void>())
        }
        val contractMinedBlockRepository = mock<BitcoinContractMinedBlockRepository> {
            on { save(any<CqlBitcoinContractMinedBlock>()) }.thenReturn(Mono.empty<CqlBitcoinContractMinedBlock>())
            on { delete(any()) }.thenReturn(Mono.empty<Void>())
        }

        val txADb = tx("A", "A")
        val txAPool = tx("A", "A").mempoolState()

        val txRepository = mock<BitcoinTxRepository> {
            on { save(any<CqlBitcoinTx>()) }.thenReturn(Mono.empty())
            on { delete(any()) }.thenReturn(Mono.empty())
            on { findById(any<String>()) }.thenReturn(Mono.just(CqlBitcoinTx(txADb)))
        }
        val blockTxRepository = mock<BitcoinBlockTxRepository>()

        val contractTxRepository = mock<BitcoinContractTxRepository> {
            on { saveAll(any<Iterable<CqlBitcoinContractTxPreview>>()) }.thenReturn(Flux.empty())
            on { deleteAll(any<Iterable<CqlBitcoinContractTxPreview>>()) }.thenReturn(Mono.empty())
        }

        val txLatencyMetric = mock<Timer>()

        val bitcoinMessageListenerResolver = BitcoinMessageListenerResolver(
            chainInfo,
            0,
            blockRepository,
            contractMinedBlockRepository,
            txRepository,
            contractTxRepository,
            blockTxRepository,
            SimpleMeterRegistry(),
            txLatencyMetric)

        var chainEntityType = ChainEntityType.BLOCK
        val result = bitcoinMessageListenerResolver.getListenerByType(chainEntityType)
        Assert.notNull(result, "result listener resolver needs to be created")
    }

    fun tx(hash: String, blockHash: String?) = BitcoinTx(
        hash = hash, blockHash = blockHash, blockNumber = 4959189, blockTime = Instant.ofEpochSecond(100000),
        index = 1, firstSeenTime = Instant.ofEpochSecond(100000),
        fee = BigDecimal.ZERO, size = 1, totalOutputsAmount = BigDecimal.ONE, totalInputsAmount = BigDecimal.ONE,
        ins = listOf(BitcoinTxIn(listOf("a"), BigDecimal.ONE, SignatureScript("a", "a"), emptyList(), "a0", 0)),
        outs = listOf(BitcoinTxOut(listOf("b"), BigDecimal.ONE, "a", 0, 1))
    )

    fun record(event: PumpEvent, tx: BitcoinTx) =
        ConsumerRecord<PumpEvent, BitcoinTx>(chainInfo.txPumpTopic, 0, 0, event, tx)
}
