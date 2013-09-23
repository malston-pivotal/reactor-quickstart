package reactor.quickstart.complex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.quickstart.TradeServer;

/**
 * @author Jon Brisbin
 */
@Configuration
public class ServiceConfig {

	@Bean
	public TradeServer tradeServer() {
		return new TradeServer();
	}

}
