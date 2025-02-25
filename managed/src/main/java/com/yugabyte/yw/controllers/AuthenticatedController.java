// Copyright (c) Yugabyte, Inc.

package com.yugabyte.yw.controllers;

import com.yugabyte.yw.controllers.JWTVerifier.ClientType;
import java.util.Optional;
import java.util.UUID;
import play.mvc.With;

@With(TokenAuthenticator.class)
public abstract class AuthenticatedController extends AbstractPlatformController {
  protected Optional<ClientType> maybeGetJWTClientType() {
    ClientType clientType = (ClientType) ctx().args.get(JWTVerifier.CLIENT_TYPE_CLAIM);
    if (clientType == null) {
      return Optional.empty();
    }
    return Optional.of(clientType);
  }

  protected UUID getJWTClientUuid() {
    return (UUID) ctx().args.get(JWTVerifier.CLIENT_ID_CLAIM);
  }
}
