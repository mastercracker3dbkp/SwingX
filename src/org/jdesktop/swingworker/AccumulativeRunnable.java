package org.jdesktop.swingworker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;

abstract class AccumulativeRunnable implements Runnable {
   private List arguments = null;

   protected abstract void run(List var1);

   public final void run() {
      this.run(this.flush());
   }

   public final synchronized void add(Object... var1) {
      boolean var2 = true;
      if (this.arguments == null) {
         var2 = false;
         this.arguments = new ArrayList();
      }

      Collections.addAll(this.arguments, var1);
      if (!var2) {
         this.submit();
      }

   }

   protected void submit() {
      SwingUtilities.invokeLater(this);
   }

   private final synchronized List flush() {
      List var1 = this.arguments;
      this.arguments = null;
      return var1;
   }
}
