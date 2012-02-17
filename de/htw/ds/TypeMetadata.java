package de.htw.ds;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;


/**
 * <p>A type annotated @TypeMetdata contains the minimal meta-data
 * legally required for code, i.e. a copyright statement, a version
 * number and the authors who created the code.</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TypeMetadata(copyright = "2005-2011 Sascha Baumeister, all rights reserved", version = "1.0.1", authors = "Sascha Baumeister")
public @interface TypeMetadata {
	String copyright();
	String version();
	String[] authors();
}