package cz.sefira.obelisk.generic;

import cz.sefira.obelisk.NexuException;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.Feedback;
import cz.sefira.obelisk.api.NexuAPI;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DebugHelperTest {

	private DebugHelper dh;
	private File dummyNexuHome;
	private NexuAPI nexuApi;
	private static final Logger LOGGER = LoggerFactory.getLogger(DebugHelperTest.class);

	@Before
	public void setup() {
		Properties properties = new Properties();
		properties.setProperty("os.version", "27");
		properties.setProperty("os.name", "Fedora Linux");
		properties.setProperty("java.vendor", "Oracle");
		properties.setProperty("os.arch", "x86_64");

		AppConfig appConfig = mock(AppConfig.class);
		dummyNexuHome = Paths.get("tmp", "dummy_nexu_home").toFile();
		assertTrue(dummyNexuHome.mkdirs());
		assertTrue(dummyNexuHome.isDirectory());
		when(appConfig.getNexuHome()).thenReturn(dummyNexuHome);
		dh = spy(new DebugHelper());
		when(dh.getConfig()).thenReturn(appConfig);
		when(dh.getProperties()).thenReturn(properties);
		nexuApi = mock(NexuAPI.class);
		when(nexuApi.getAppConfig()).thenReturn(appConfig);

	}

	@After
	public void cleanup() throws IOException {
		if (dummyNexuHome.exists()) {
			FileUtils.forceDelete(Paths.get("tmp").toFile());
		}
	}

	@Test
	public void testCollectDebugData() throws JAXBException {

		Feedback feedback = dh.collectDebugData(new NexuException("Dummy exception"));
		assertEquals("LINUX", feedback.getInfo().getOs().name());
		assertEquals(NexuException.class, feedback.getException().getClass());
	}

	@Test
	public void testBuildDebugFileWhenLogFound() throws IOException {
		Path nexuLogPath = Paths.get(dummyNexuHome.getAbsolutePath(), "OB-SP-Client.log");
		File nexuLog = nexuLogPath.toFile();
		nexuLog.createNewFile();
		assertTrue(nexuLog.exists());
		String dummyLogContent = "Dummy log content";
		Files.write(nexuLogPath, dummyLogContent.getBytes());
		dh.buildDebugFile();
		Path nexuDebugPath = Paths.get(dummyNexuHome.getAbsolutePath(), dh.getDebugFileName());
		assertTrue(nexuDebugPath.toFile().exists());
	}

	@Test
	public void testBuildDebugFileWhenLogNotFound() throws IOException {
		dh.buildDebugFile();
		Path nexuDebugPath = Paths.get(dummyNexuHome.getAbsolutePath(), dh.getDebugFileName());
		assertTrue(nexuDebugPath.toFile().exists());
	}

	@Test
	public void testAppendFeedbackData() throws JAXBException, IOException {
		Path nexuDebugPath = Paths.get(dummyNexuHome.getAbsolutePath(), dh.getDebugFileName());
		Feedback feedback = new Feedback();
		feedback.setVersion("99");
		FileUtils.write(nexuDebugPath.toFile(),
				String.format("dummy log content %s %s", System.lineSeparator(), System.lineSeparator()), "utf-8");
		File readyFile = dh.appendFeedbackData(nexuDebugPath, feedback);
		File expectedFile = new File(
				this.getClass().getClassLoader().getResource("DebugHelperTest.txt").getPath());
		assertTrue(FileUtils.contentEqualsIgnoreEOL(expectedFile, readyFile, "utf-8"));
	}

	@Test
	public void testProcessError() throws IOException, JAXBException {
		doNothing().when(dh).showDebugFileInExplorer(any(Path.class), any(Feedback.class));
		dh.processError(new NexuException("Dummy Exception"));
		verify(dh, times(1)).collectDebugData(any(Throwable.class));
		verify(dh, times(1)).buildDebugFile();
		verify(dh, times(1)).appendFeedbackData(any(Path.class), any(Feedback.class));
		verify(dh, times(1)).showDebugFileInExplorer(any(Path.class), any(Feedback.class));
	}

}