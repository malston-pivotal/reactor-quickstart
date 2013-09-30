package reactor.quickstart.complex.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.function.Consumer;
import reactor.function.Function;
import reactor.quickstart.complex.domain.Client;
import reactor.quickstart.complex.repository.ClientComposableRepository;

/**
 * @author Jon Brisbin
 */
@Controller
public class TradeController {

	@Autowired
	private ClientComposableRepository clients;

	@RequestMapping(value = "/{clientId}", method = RequestMethod.POST)
	@ResponseBody
	public DeferredResult<String> trade(@PathVariable Long clientId) {
		final DeferredResult<String> d = new DeferredResult<String>();

		clients.save(clients.findOne(clientId)
		                    .map(new Function<Client, Client>() {
			                    public Client apply(Client cl) {
				                    return cl.setTradeCount(cl.getTradeCount() + 1);
			                    }
		                    }))
		       .map(new Function<Client, String>() {
			       public String apply(Client cl) {
				       return "Hello " + cl.getName() + "! You now have " + cl.getTradeCount() + " trades.";
			       }
		       })
		       .when(Throwable.class, new Consumer<Throwable>() {
			       public void accept(Throwable t) {
				       d.setErrorResult(t);
			       }
		       })
		       .consume(new Consumer<String>() {
			       public void accept(String s) {
				       d.setResult(s);
			       }
		       });

		return d;
	}

}
