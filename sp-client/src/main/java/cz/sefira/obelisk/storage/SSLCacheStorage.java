package cz.sefira.obelisk.storage;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.storage.SSLCacheStorage
 *
 * Created: 27.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.storage.handler.X509CertificateTypeHandler;
import cz.sefira.obelisk.storage.model.CertificateChain;
import one.microstream.persistence.internal.LoggingLegacyTypeMappingResultor;
import one.microstream.persistence.types.PersistenceLegacyTypeMappingResultor;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageFoundation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SSL certificates cache
 */
public class SSLCacheStorage extends AbstractStorage {

  private static final Logger logger = LoggerFactory.getLogger(SSLCacheStorage.class.getName());

  private final Set<CertificateChain> certificateChains = new HashSet<>();

  public SSLCacheStorage(Path store) {
    EmbeddedStorageFoundation<?> foundation = EmbeddedStorage.Foundation(store);
    foundation.getConnectionFoundation().setLegacyTypeMappingResultor(
        LoggingLegacyTypeMappingResultor.New(
            PersistenceLegacyTypeMappingResultor.New()
        )
    );
    foundation.registerTypeHandler(new X509CertificateTypeHandler());
    this.storage = foundation.createEmbeddedStorageManager(certificateChains).start();
    logger.info("Cached certificate chains: "+certificateChains.size());
  }

  public void add(List<X509Certificate> chain) {
    certificateChains.add(new CertificateChain(chain));
    commitChange(certificateChains);
  }

  public Set<CertificateChain> getAll() {
    return certificateChains;
  }

  public void remove(CertificateChain chain) {
    certificateChains.remove(chain);
    commitChange(certificateChains);
  }

}
