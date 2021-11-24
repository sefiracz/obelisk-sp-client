/**
 * © Nowina Solutions, 2015-2016
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class EntityDatabaseLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntityDatabaseLoader.class.getName());

	public static <T extends EntityDatabase> T load(final Class<T> databaseClass, final File f) {
		final T db;
		if (!f.exists()) {
			try {
				db = databaseClass.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new TechnicalException("Cannot create database", e);
			}
		} else {
			try {
				db = load(databaseClass, f.toURI().toURL());
			} catch (MalformedURLException e) {
				throw new TechnicalException("Cannot load database", e);
			}
		}
		db.setOnAddRemoveAction(new DatabaseEventHandler() {
			@Override
			public <P extends EntityDatabase> void execute(P db) {
				saveAs(db, f);
			}
		});
		db.initialize();
		return db;
	}

	@SuppressWarnings("unchecked")
	public static <T extends EntityDatabase> T load(final Class<T> databaseClass, final URL url) {
		try {
			final JAXBContext ctx = createJaxbContext(databaseClass);
			final Unmarshaller u = ctx.createUnmarshaller();
			return (T) u.unmarshal(url);
		} catch (final Exception e) {
			throw new TechnicalException("Cannot load database", e);
		}
	}

	private static <T extends EntityDatabase> JAXBContext createJaxbContext(final Class<T> databaseClass) {
		try {
			return JAXBContext.newInstance(databaseClass);
		} catch (JAXBException e) {
			LOGGER.error("Cannot instanciate JAXBContext", e);
			throw new TechnicalException("Cannot instanciate JAXBContext", e);
		}
	}

	static <T extends EntityDatabase> void saveAs(T db, File file) {
		try {
			final JAXBContext ctx = createJaxbContext(db.getClass());
			final Marshaller m = ctx.createMarshaller();
			try (final FileOutputStream out = new FileOutputStream(file)) {
				m.marshal(db, out);
			}
		} catch (final Exception e) {
			LOGGER.error("Cannot save database", e);
			throw new TechnicalException("Cannot save database", e);
		}
	}
}
