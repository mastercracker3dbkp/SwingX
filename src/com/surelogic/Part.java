package com.surelogic;

/**
 * An enumeration allowing {@link ThreadSafe} and {@link Immutable} promises to
 * control which portion of a declaration's state they apply to.
 * 
 * @see Immutable
 * @see ThreadSafe
 */
public enum Part {
  /**
   * Indicates that both the instance and static state of the declaration are to
   * be considered.
   */
  InstanceAndStatic,

  /**
   * Indicates that the instance state only of the declaration is to be
   * considered.
   */
  Instance,

  /**
   * Indicates that the static state only of the declaration is to be
   * considered.
   */
  Static
}
