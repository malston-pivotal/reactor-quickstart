package reactor.quickstart.complex.repository;

import reactor.data.core.ComposableCrudRepository;
import reactor.quickstart.complex.domain.Client;

/**
 * @author Jon Brisbin
 */
public interface ClientComposableRepository extends ComposableCrudRepository<Client, Long> {
}
