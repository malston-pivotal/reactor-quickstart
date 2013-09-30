package reactor.quickstart.complex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import reactor.core.Environment;
import reactor.data.spring.config.EnableComposableRepositories;
import reactor.spring.context.config.EnableReactor;

/**
 * @author Jon Brisbin
 */
@Configuration
@EnableReactor
@EnableComposableRepositories(basePackages = {"reactor.quickstart.complex.service"},
                              dispatcher = Environment.RING_BUFFER)
@EnableAutoConfiguration
@ComponentScan
public class ComplexTradeServerExample {
	public static void main(String... args) {
		SpringApplication.run(ComplexTradeServerExample.class, args);
	}
}
