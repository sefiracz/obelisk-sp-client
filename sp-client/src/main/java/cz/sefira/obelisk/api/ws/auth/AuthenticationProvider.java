package cz.sefira.obelisk.api.ws.auth;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.auth.AuthenticationProvider
 *
 * Created: 13.03.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.ws.ssl.SSLCommunicationException;

/**
 * Authentication provider interface
 */
public interface AuthenticationProvider {

  /**
   * Returns URI where to redirect HTTP client to use this auth credentials (might be null)
   * @return Endpoint URI where to use given authentication credentials (might be null if not applicable)
   */
  String getRedirectUri();

  /**
   * Returns endpoint authentication credentials
   * @return Authentication credentials
   * @throws AuthenticationProviderException
   * @throws SSLCommunicationException
   */
  String getEndpointAuthentication() throws AuthenticationProviderException, SSLCommunicationException;
}
