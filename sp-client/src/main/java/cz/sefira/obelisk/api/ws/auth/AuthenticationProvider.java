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

/**
 * description
 */
public interface AuthenticationProvider {

  String getRedirectUri();

  String getEndpointAuthentication() throws AuthenticationProviderException;
}
