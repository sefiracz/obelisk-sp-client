package cz.sefira.obelisk.api;

import cz.sefira.obelisk.api.ws.model.CertificateFilter;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.dss.KeyUsageBit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CertificateFilterHelperTest {

	private static final String NO_NON_REPUDIATION = "I don\'t have non repudiation";
	private static final String NON_REPUDIATION = "I have non repudiation";

	private CertificateFilterHelper helper;
	private List<DSSPrivateKeyEntry> entries;
	private SignatureTokenConnection token;

	@Before
	public void init() {
		helper = new CertificateFilterHelper();
		entries = new ArrayList<>();
		DSSPrivateKeyEntry entry = mock(DSSPrivateKeyEntry.class, Mockito.RETURNS_DEEP_STUBS);
		when(entry.getCertificateToken().checkKeyUsage(KeyUsageBit.nonRepudiation)).thenReturn(false);
		when(entry.getCertificateToken().getDSSIdAsString()).thenReturn(NO_NON_REPUDIATION);
		entries.add(entry);
		token = mock(SignatureTokenConnection.class);
		when(token.getKeys()).thenReturn(entries);
	}

	@Test
	public void testFilterSetOneEntryPassing() {
		CertificateFilter filter = new CertificateFilter();
		filter.setNonRepudiationBit(true);
		DSSPrivateKeyEntry entry = mock(DSSPrivateKeyEntry.class, Mockito.RETURNS_DEEP_STUBS);
		when(entry.getCertificateToken().checkKeyUsage(KeyUsageBit.nonRepudiation)).thenReturn(true);
		when(entry.getCertificateToken().getDSSIdAsString()).thenReturn(NON_REPUDIATION);
		entries.add(entry);
		List<DSSPrivateKeyEntry> filteredEntries = helper.filterKeys(token, filter);
		assertEquals(1, filteredEntries.size());
		assertThat(filteredEntries.get(0).getCertificateToken().getDSSIdAsString(), equalTo(NON_REPUDIATION));
	}

	@Test
	public void testFilterSetNoEntryPassing() {
		CertificateFilter filter = new CertificateFilter();
		filter.setNonRepudiationBit(true);
		DSSPrivateKeyEntry entry = mock(DSSPrivateKeyEntry.class, Mockito.RETURNS_DEEP_STUBS);
		when(entry.getCertificateToken().checkKeyUsage(KeyUsageBit.nonRepudiation)).thenReturn(false);
		when(entry.getCertificateToken().getDSSIdAsString()).thenReturn(NO_NON_REPUDIATION);
		entries.add(entry);
		List<DSSPrivateKeyEntry> filteredEntries = helper.filterKeys(token, filter);
		assertEquals(0, filteredEntries.size());
	}

	@Test
	public void testFilterSetToFalseTwoEntries() {
		CertificateFilter filter = new CertificateFilter();
		filter.setNonRepudiationBit(false);
		DSSPrivateKeyEntry entry = mock(DSSPrivateKeyEntry.class, Mockito.RETURNS_DEEP_STUBS);
		when(entry.getCertificateToken().checkKeyUsage(KeyUsageBit.nonRepudiation)).thenReturn(false);
		when(entry.getCertificateToken().getDSSIdAsString()).thenReturn(NON_REPUDIATION);
		entries.add(entry);
		List<DSSPrivateKeyEntry> filteredEntries = helper.filterKeys(token, filter);
		assertEquals(2, filteredEntries.size());
		assertThat(filteredEntries, equalTo(entries));
	}

}
