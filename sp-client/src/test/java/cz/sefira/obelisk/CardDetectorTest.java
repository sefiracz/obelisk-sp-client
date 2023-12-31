/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package cz.sefira.obelisk;

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.model.EnvironmentInfo;
import cz.sefira.obelisk.view.core.UIDisplay;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ CardDetector.class })
public class CardDetectorTest {

	@Test
	// SCARD_E_NO_SERVICE is sometimes thrown on Mac after waking up from
	// hibernation
	public void testSCardNoServiceIsHandledCorrectly() throws Exception {
		EnvironmentInfo environmentInfo = new EnvironmentInfo();
		UIDisplay display = Mockito.mock(UIDisplay.class);
		InternalAPI api = new InternalAPI(display, null, null, null);
		CardDetector cardDetector = PowerMockito.spy(new CardDetector(api));
		CardTerminals cardTerminals = mock(CardTerminals.class);
		CardException cardException = mock(CardException.class);
		when(cardException.getCause()).thenReturn(new Exception("SCARD_E_NO_SERVICE"));
		when(cardTerminals.list()).thenThrow(cardException);
		Class<CardDetector> cardDetectorClass = CardDetector.class;
		Method establishNewContext = cardDetectorClass.getDeclaredMethod("establishNewContext");
		PowerMockito.doNothing().when(cardDetector, establishNewContext).withNoArguments();
		Whitebox.setInternalState(cardDetector, "cardTerminals", cardTerminals);
		Whitebox.<List<CardTerminal>>invokeMethod(cardDetector, "getCardTerminals");
		PowerMockito.verifyPrivate(cardDetector, times(1)).invoke("closeCardTerminals");
		PowerMockito.verifyPrivate(cardDetector, times(1)).invoke("establishNewContext");
		PowerMockito.verifyPrivate(cardDetector, times(2)).invoke("getCardTerminals");
	}

}
