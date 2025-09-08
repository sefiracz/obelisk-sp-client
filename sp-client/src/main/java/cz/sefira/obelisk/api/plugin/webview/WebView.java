package cz.sefira.obelisk.api.plugin.webview;

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.plugin.InitErrorMessage;
import cz.sefira.obelisk.api.ws.ApacheCookieStore;

import java.util.List;
import java.util.concurrent.FutureTask;

public interface WebView {

  void init(PlatformAPI api) throws Exception;

  FutureTask<List<InitErrorMessage>> futureInit(PlatformAPI api);

  void load(Object sync, PlatformAPI api, ApacheCookieStore cookieStore, String url);

  void dispose();
}
