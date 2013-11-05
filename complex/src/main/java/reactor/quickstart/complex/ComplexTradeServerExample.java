package reactor.quickstart.complex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import reactor.data.spring.config.EnableComposableRepositories;
import reactor.spring.context.config.EnableReactor;

/**
 * @author Jon Brisbin
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableReactor
@EnableComposableRepositories
public class ComplexTradeServerExample {
	public static void main(String... args) {
		SpringApplication.run(ComplexTradeServerExample.class, args);
	}
}
