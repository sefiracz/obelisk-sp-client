package cz.sefira.obelisk.util.annotation;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.util.annotation.NotNull
 *
 * Created: 20.03.2023
 * Author: hlavnicka
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation signifying that:
 * <ul>
 *   <li>the given method parameter must not be <code>null</code></li>
 *   <li>method return value is never <code>null</code></li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface NotNull {

}
