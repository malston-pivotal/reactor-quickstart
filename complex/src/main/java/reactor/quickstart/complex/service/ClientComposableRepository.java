package reactor.quickstart.complex.service;

import reactor.data.spring.ComposableCrudRepository;
import reactor.quickstart.complex.domain.Client;

/**
 * @author Jon Brisbin
 */
public interface ClientComposableRepository extends ComposableCrudRepository<Client, Long> {
}
