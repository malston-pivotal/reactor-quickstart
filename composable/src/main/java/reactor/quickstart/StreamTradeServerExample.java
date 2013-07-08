package reactor.quickstart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Deferred;
import reactor.core.Environment;
import reactor.core.Stream;
import reactor.core.Streams;
import reactor.fn.Consumer;
import reactor.fn.Function;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public class StreamTradeServerExample {

	public static void main(String[] args) throws InterruptedException {
		Environment env = new Environment();
		final TradeServer server = new TradeServer();

		// Rather than handling Trades as events, each Trade is accessible via Stream.
		Deferred<Trade, Stream<Trade>> trades = Streams.<Trade>defer()
																	.using(env)
																	.dispatcher(Environment.RING_BUFFER)
																	// We can always set a length to a Stream if we know it (completely optional).
				                          .batch(totalTrades)
																	.get();

		// We compose an action to turn a Trade into an Order by calling server.execute(Trade).
		Stream<Order> orders = trades.compose().map(new Function<Trade, Order>() {
			@Override
			public Order apply(Trade trade) {
				return server.execute(trade);
			}
		});

		// Start a throughput timer.
		startTimer();

		// Publish one event per trade.
		for (int i = 0; i < totalTrades; i++) {
			// Pull next randomly-generated Trade from server into the Composable,
			Trade trade = server.nextTrade();
			// Notify the Composable this Trade is ready to be executed
			trades.accept(trade);
		}

		// Composables can block until all values have passed through them.
		// They know when the end has arrived because we set the length earlier.
		final CountDownLatch latch = new CountDownLatch(1);
		orders.last().consume(new Consumer<Order>() {
			@Override
			public void accept(Order order) {
				LOG.info("Finished processing");
				latch.countDown();
			}
		});
		try{
			latch.await(30,TimeUnit.SECONDS);
		}catch (InterruptedException e){
			LOG.error("Failed timeout",e);
		}

		// Stop throughput timer and output metrics.
		endTimer();

		server.stop();
	}

	private static void startTimer() {
		LOG.info("Starting throughput test with {} trades...", totalTrades);
		startTime = System.currentTimeMillis();
	}

	private static void endTimer() throws InterruptedException {
		endTime = System.currentTimeMillis();
		elapsed = (endTime - startTime) * 1.0;
		throughput = totalTrades / (elapsed / 1000);

		LOG.info("Executed {} trades/sec in {}ms", (int) throughput, (int) elapsed);
	}

	private static final Logger LOG         = LoggerFactory.getLogger(StreamTradeServerExample.class);
	private static       int    totalTrades = 10000000;
	private static long   startTime;
	private static long   endTime;
	private static double elapsed;
	private static double throughput;

}
