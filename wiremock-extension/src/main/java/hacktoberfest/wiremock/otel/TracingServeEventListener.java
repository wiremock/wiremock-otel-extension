package hacktoberfest.wiremock.otel;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ServeEventListener;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class TracingServeEventListener implements ServeEventListener {
    @Override
    public String getName() {
        return "tracing";
    }

    @Override
    public void beforeMatch(ServeEvent serveEvent, Parameters parameters) {}

    @Override
    public void afterMatch(ServeEvent serveEvent, Parameters parameters) {}

    @Override
    public void beforeResponseSent(ServeEvent serveEvent, Parameters parameters) {}

    @Override
    public void afterComplete(ServeEvent serveEvent, Parameters parameters) {}
}
