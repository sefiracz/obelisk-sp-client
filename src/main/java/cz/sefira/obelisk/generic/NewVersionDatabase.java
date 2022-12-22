package cz.sefira.obelisk.generic;

/*
 * Copyright 2022 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.generic.NewVersionDatabase
 *
 * Created: 07.11.2022
 * Author: hlavnicka
 */

import cz.sefira.obelisk.DatabaseEventHandler;
import cz.sefira.obelisk.EntityDatabase;
import cz.sefira.obelisk.api.NewVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * New version information
 */
public class NewVersionDatabase implements EntityDatabase {

  private static final Logger log = LoggerFactory.getLogger(SmartcardInfoDatabase.class.getName());

  @XmlElement(name = "newVersion")
  private NewVersion newVersion;

  @XmlTransient
  private DatabaseEventHandler onAddRemoveAction;

  @Override
  public void initialize() {
    log.info("New version information loaded");
  }

  public void setNewVersion(NewVersion newVersion) {
    this.newVersion = newVersion;
    onAddRemove();
  }

  private void onAddRemove() {
    if(onAddRemoveAction != null) {
      onAddRemoveAction.execute(this);
    } else {
      log.warn("No DatabaseEventHandler define, the database cannot be stored");
    }
  }

  public NewVersion getNewVersion() {
    return newVersion;
  }

  @Override
  public void setOnAddRemoveAction(DatabaseEventHandler eventHandler) {
    this.onAddRemoveAction = eventHandler;
  }
}
