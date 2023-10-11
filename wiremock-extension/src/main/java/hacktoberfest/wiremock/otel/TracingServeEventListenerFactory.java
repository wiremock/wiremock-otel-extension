package hacktoberfest.wiremock.otel;

import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.extension.ExtensionFactory;
import com.github.tomakehurst.wiremock.extension.WireMockServices;
import com.google.auto.service.AutoService;
import java.util.List;

@AutoService(ExtensionFactory.class)
public class TracingServeEventListenerFactory implements ExtensionFactory {
    @Override
    public List<Extension> create(final WireMockServices services) {
        return List.of(new TracingServeEventListener());
    }
}
