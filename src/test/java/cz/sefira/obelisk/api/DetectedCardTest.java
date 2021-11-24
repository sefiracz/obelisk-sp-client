package cz.sefira.obelisk.api;

import org.junit.Test;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.DetectedCardTest
 *
 * Created: 04.02.2021
 * Author: hlavnicka
 */


public class DetectedCardTest {

  @Test
  public void test1() throws Exception {

    DetectedCard c1 = new DetectedCard();
    c1.setAtr("ABCD12");
    c1.setTokenLabel(c1.getAtr());
    c1.setTerminalIndex(0);
    c1.setTerminalLabel("Reader 0");

    DetectedCard c2 = new DetectedCard();
    c2.setAtr("ABCD12");
//    c2.setTokenLabel(c2.getAtr());
    c2.setTokenLabel("Token Label");
    c2.setTerminalIndex(0);
    c2.setTerminalLabel("Reader 0");

//    Assert.assertEquals(c1, c2);
  }


}
