package reactor.quickstart;

import static reactor.Fn.$;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.Fn;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.Registration;
import reactor.fn.Selector;
import reactor.fn.dispatch.RingBufferDispatcher;

/**
 * @author Jon Brisbin
 */
public class WebSocketTradeServerExample {

	public static void main(String[] args) throws Exception {
		final TradeServer server = new TradeServer();

		// Use a Reactor to dispatch events using the high-speed Dispatcher
        RingBufferDispatcher dispatcher = new RingBufferDispatcher();
        final Reactor serverReactor = new Reactor(dispatcher);
        dispatcher.start();


		// Create a single key and Selector for efficiency
		final String tradeExecuteKey = "trade.execute";
		final Selector tradeExecute = $(tradeExecuteKey);

		// For each Trade event, execute that on the server and notify connected clients
		// because each client that connects links to the serverReactor
		serverReactor.on(tradeExecute, new Consumer<Event<Trade>>() {
			@Override
			public void accept(Event<Trade> tradeEvent) {
				server.execute(tradeEvent.getData());

				// Since we're async, for this test, use a latch to tell when we're done
				latch.countDown();
			}
		});

		WebSocketServlet wss = new WebSocketServlet() {
			@Override
			public void configure(WebSocketServletFactory factory) {
				factory.setCreator(new WebSocketCreator() {
					@Override
					public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
						return new WebSocketListener() {
							AtomicLong counter = new AtomicLong();
							Registration<Consumer<Event<Trade>>> registration;

							@Override
							public void onWebSocketBinary(byte[] payload, int offset, int len) {
							}

							@Override
							public void onWebSocketClose(int statusCode, String reason) {
								if(registration != null)
									registration.cancel();
							}

							@Override
							public void onWebSocketConnect(final Session session) {
								LOG.info("Connected a websocket client: {}", session);

								registration = serverReactor.on(tradeExecute, new Consumer<Event<Trade>>() {
									@Override
									public void accept(Event<Trade> tradeEvent) {
										// Send a message every 1000th trade.
										// If not, we completely overwhelm the browser and network.
										if (counter.incrementAndGet() % 1000 == 0) {
											try {
												session.getRemote().sendString(tradeEvent.getData().toString());
											} catch (IOException e) {
												e.printStackTrace();
											}
										}
									}
								});
							}

							@Override
							public void onWebSocketError(Throwable cause) {
								if(registration != null)
									registration.cancel();
							}

							@Override
							public void onWebSocketText(String message) {
							}
						};
					}
				});
			}
		};
		serve(wss);

		LOG.info("Connect websocket clients now (waiting for 20 seconds).");
		LOG.info("Open websockets/src/main/webapp/ws.html in a browser...");
		Thread.sleep(20000);

		// Start a throughput timer
		startTimer();

		// Publish one event per trade
		for (int i = 0; i < totalTrades; i++) {
			// Pull next randomly-generated Trade from server
			Trade t = server.nextTrade();

			// Notify the Reactor the event is ready to be handled
			serverReactor.notify(tradeExecuteKey, Fn.event(t));
		}

		// Stop throughput timer and output metrics
		endTimer();

		server.stop();

	}

	private static void startTimer() {
		LOG.info("Starting throughput test with {} trades...", totalTrades);
		latch = new CountDownLatch(totalTrades);
		startTime = System.currentTimeMillis();
	}

	private static void endTimer() throws InterruptedException {
		latch.await(30, TimeUnit.SECONDS);
		endTime = System.currentTimeMillis();
		elapsed = (endTime - startTime) * 1.0;
		throughput = totalTrades / (elapsed / 1000);

		LOG.info("Executed {} trades/sec in {}ms", (int) throughput, (int) elapsed);
	}

	private static void serve(HttpServlet servlet) throws Exception {
		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(new ServletHolder(servlet), "/");

		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setOutputBufferSize(32 * 1024);
		httpConfig.setRequestHeaderSize(8 * 1024);
		httpConfig.setResponseHeaderSize(8 * 1024);
		httpConfig.setSendDateHeader(true);

		HttpConnectionFactory connFac = new HttpConnectionFactory(httpConfig);

		Server server = new Server(3000);

		ServerConnector connector = new ServerConnector(
				server,
				Executors.newFixedThreadPool(4),
				new ScheduledExecutorScheduler(),
				new MappedByteBufferPool(),
				1,
				4,
				connFac
		);
		connector.setAcceptQueueSize(1000);
		connector.setReuseAddress(true);

		server.setHandler(handler);
		server.start();
	}

	private static final Logger LOG = LoggerFactory.getLogger(WebSocketTradeServerExample.class);
	private static CountDownLatch latch;
	private static int totalTrades = 5000000;
	private static long   startTime;
	private static long   endTime;
	private static double elapsed;
	private static double throughput;

}
