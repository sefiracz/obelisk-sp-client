package lu.nowina.nexu.pkcs11;

import java.io.*;
import java.lang.reflect.Field;
import java.security.AuthProvider;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lu.nowina.nexu.api.DetectedCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.*;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.PasswordInputCallback;
import lu.nowina.nexu.CancelledOperationException;

import sun.security.pkcs11.SunPKCS11;
import sun.security.pkcs11.wrapper.*;

import javax.security.auth.login.LoginException;

import static sun.security.pkcs11.wrapper.PKCS11Constants.CKF_OS_LOCKING_OK;

/**
 * This adapter class allows to manage {@link CancelledOperationException}.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
@Deprecated
public class SunPkcs11SignatureTokenAdapter extends AbstractPkcs11SignatureTokenAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SunPkcs11SignatureTokenAdapter.class.getName());

    private Provider provider;
    private DetectedCard card;

    public SunPkcs11SignatureTokenAdapter(final File pkcs11lib, final PasswordInputCallback callback, final DetectedCard card) {
        super(pkcs11lib.getAbsolutePath(), callback, card.getTerminalIndex());
        this.card = card;
        logger.info("Lib " + pkcs11lib.getAbsolutePath());
    }

    @Override
    public void close() {
        if (this.provider != null) {
            LOG.info("Closing provider "+provider.getName());
            try {
                if (this.provider instanceof AuthProvider) {
                    ((AuthProvider) this.provider).logout();
                }
                if (this.provider instanceof SunPKCS11) {
                    /*
                     * IN CASE WE WANT TO USE MORE THAN ONE TOKEN WITH PKCS#11,
                     * WE NEED TO FINALIZE AND REINITIALIZE THE MODULE EVERY
                     * TIME. THIS REQUIRES A SMALL HACK
                     */

                    CK_C_INITIALIZE_ARGS initArgs = new CK_C_INITIALIZE_ARGS();
                    initArgs.flags = CKF_OS_LOCKING_OK;
                    PKCS11 pkcs11 = PKCS11.getInstance(this.getPkcs11Library(), "C_GetFunctionList", initArgs, true);
                    pkcs11.C_Finalize(PKCS11Constants.NULL_PTR);

                    Field privateStaticField = PKCS11.class.getDeclaredField("moduleMap");
                    privateStaticField.setAccessible(true);
                    ((Map) privateStaticField.get(null)).remove(this.getPkcs11Library());
                }
            } catch (final LoginException e) {
                LOG.error("LoginException on logout of '" + this.provider.getName() + "'", e);
            } catch (IOException | PKCS11Exception | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                LOG.error("Exception finalizing '" + this.provider.getName() + "'", e);
            }
            this.provider.clear();
            try {
                Security.removeProvider(this.provider.getName());
            } catch (final SecurityException e) {
                LOG.error("Unable to remove provider '" + this.provider.getName() + "'", e);
            } finally {
                this.provider = null;
            }
        }
    }

    @Override
    @SuppressWarnings("restriction")
    protected Provider getProvider() {
        if (this.provider == null) {
            /*
             * The smartCardNameIndex int is added at the end of the smartCard name in order to enable the successive
             * loading of multiple pkcs11 libraries
             */
            String aPKCS11LibraryFileName = this.getPkcs11Path();
            aPKCS11LibraryFileName = this.escapePath(aPKCS11LibraryFileName);

            final StringBuilder pkcs11Config = new StringBuilder();
            pkcs11Config.append("name = SmartCard_").append(UUID.randomUUID().toString()).append("\n");
            pkcs11Config.append("library = \"").append(aPKCS11LibraryFileName).append("\"").append("\n");
            pkcs11Config.append("slotListIndex = ").append(this.getSlotListIndex());

            final String configString = pkcs11Config.toString();

            LOG.debug("PKCS11 Config : \n{}", configString);

            try (ByteArrayInputStream confStream = new ByteArrayInputStream(configString.getBytes("ISO-8859-1"))) {
                final sun.security.pkcs11.SunPKCS11 sunPKCS11 = new sun.security.pkcs11.SunPKCS11(confStream);
                // we need to add the provider to be able to sign later
                Security.addProvider(sunPKCS11);
                this.provider = sunPKCS11;
                return this.provider;
            } catch (final Exception e) {
                throw new DSSException("Unable to instantiate PKCS11", e);
            }
        }
        return this.provider;
    }

    protected String escapePath(final String pathToEscape) {
        if (pathToEscape != null) {
            return pathToEscape.replace("\\", "\\\\");
        } else {
            return "";
        }
    }

    protected int getSlotListIndex() {
        return this.card.getTerminalIndex();
    }

    public String getPkcs11Library() {
        return this.getPkcs11Path();
    }

    @Override
    public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
        try {
            return super.getKeys();
        } catch (final Exception e) {
            Throwable t = e;
            while (t != null) {
                if ("CKR_CANCEL".equals(t.getMessage()) || "CKR_FUNCTION_CANCELED".equals(t.getMessage())) {
                    throw new CancelledOperationException(e);
                } else if (t instanceof CancelledOperationException) {
                    throw (CancelledOperationException) t;
                } else if ("PKCS11 not found".equals(t.getMessage())) {
                    throw new PKCS11RuntimeException(e);
                } else if ("Token has been removed".equals(t.getMessage())) {
                    throw new PKCS11RuntimeException(e);
                } else if (t.getMessage() != null && t.getMessage().contains("Reason: null")) {
                    throw new PKCS11RuntimeException(e);
                }
                t = t.getCause();
            }
            // Rethrow exception as is.
            throw e;
        }
    }

    @Override
    public SignatureValue sign(final ToBeSigned toBeSigned, final DigestAlgorithm digestAlgorithm, final MaskGenerationFunction mgf,
            final DSSPrivateKeyEntry keyEntry) throws DSSException {

        try {
            return super.sign(toBeSigned, digestAlgorithm, mgf, keyEntry);
        } catch (final Exception e) {
            Throwable t = e;
            while (t != null) {
                if ("CKR_CANCEL".equals(t.getMessage()) || "CKR_FUNCTION_CANCELED".equals(t.getMessage())) {
                    throw new CancelledOperationException(e);
                } else if (t instanceof CancelledOperationException) {
                    throw (CancelledOperationException) t;
                }
                t = t.getCause();
            }
            // Rethrow exception as is.
            throw e;
        }
    }

}
