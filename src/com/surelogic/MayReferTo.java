/*
 * Copyright (c) 2012 SureLogic, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Constrains the set of types that a type is allowed to reference.
 * 
 * <h3>Semantics:</h3>
 * 
 * Places a structural constraint on the program's implementation. The annotated
 * type is allowed to reference only types within the type set expression.
 * 
 * <h3>Examples:</h3>
 * 
 * Declaring that this type is allowed to reference a set of packages.
 * 
 * <pre>
 * &#064;MayReferTo(&quot;com.surelogic.smallworld.model | org.jdom+ | java.{io, net, util}&quot;)
 * class Example { ... }
 * </pre>
 * 
 * The class {@code Example} is allowed to reference any type in the
 * {@code com.surelogic.smallworld.model} package, any type in the
 * {@code org.jdom} package and its subpackages, and any type any type in the
 * {@code UTIL} typeset, and any type in the {@code java.io}, {@code java.net},
 * and {@code java.util} packages.
 * 
 * Declaring that this type is allowed to reference most, but not all of a
 * package.
 * 
 * <pre>
 * &#064;MayReferTo(&quot;java.util & !(java.util.{Enumeration, Hashtable, Vector}&quot;)
 * class Example2 { ... }
 * </pre>
 * 
 * The class {@code Example2} is allowed to reference all the types in the
 * {@code java.util} package except for the {@code Enumeration},
 * {@code Hashtable}, and {@code Vector} classes.
 * 
 * <h3>Javadoc usage notes:</h3>
 * 
 * This annotation may placed in Javadoc, which can be useful for Java 1.4 code
 * which does not include language support for annotations, via the
 * <code>&#064;annotate</code> tag.
 * 
 * <pre>
 * /**
 *  * @annotate MayReferTo(&quot;java.util&quot;)
 *  &#42;/
 * class Example { ... }
 * </pre>
 * 
 * @see Layer
 * @see TypeSet
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface MayReferTo {
  /**
   * The set of types that may be referred to. This set is declared using a
   * constructive syntax shared with several other annotations. The attribute is
   * restricted to strings that match the following grammar:
   * <p>
   * value = type_set_expr
   * <p>
   * type_set_expr = type_set_disjunct *("<tt>|</tt>" type_set_disjunct) <i>;
   * Set union</i>
   * <p>
   * type_set_disjunct = type_set_conjunct *("<tt>&</tt>" type_set_conjunct)
   * <i>; Set intersection</i>
   * <p>
   * type_set_conjunct = ["<tt>!</tt>"] type_set_leaf <i>; Set complement</i>
   * <p>
   * type_set_leaf = dotted_name <i>; Package name, layer name, type name, or
   * type set name</i> <br>
   * type_set_leaf /= dotted_name "<tt>+</tt>" <i>; Package tree</i> <br>
   * type_set_leaf /= dotted_name "<tt>.</tt>" "<tt>{</tt>" name *("<tt>,</tt>
   * " name) "<tt>}</tt>" <i>; Union of packages/types</i> <br>
   * type_set_leaf /= "<tt>(</tt>" type_set_expr "<tt>)</tt>"
   * <p>
   * The union, intersection, and complement operators, as well as the
   * parentheses have the obvious meanings, and standard precedence order. A
   * package name signifies all the types in that package; a named type
   * indicates a specific type. A named layer stands for all the types in the
   * layer. A named type set stands for the type set specified by the given
   * name, as defined by a {@code @TypeSet} annotation. The package tree suffix
   * "<tt>+</tt>" indicates that all the types in the package and its
   * subpackages are part of the set. The braces "<tt>{</tt>" "<tt>}</tt>" are
   * syntactic sugar used to enumerate a union of packages/types that share the
   * same prefix.
   */
  public String value();
}
