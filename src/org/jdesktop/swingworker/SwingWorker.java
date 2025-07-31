package org.jdesktop.swingworker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public abstract class SwingWorker implements Future, Runnable {
   private static final int MAX_WORKER_THREADS = 10;
   private volatile int progress;
   private volatile SwingWorker.StateValue state;
   private final FutureTask future;
   private final SwingPropertyChangeSupport propertyChangeSupport;
   private AccumulativeRunnable doProcess;
   private AccumulativeRunnable doNotifyProgressChange;
   private static final AccumulativeRunnable doSubmit = new SwingWorker.DoSubmitAccumulativeRunnable();
   private static ExecutorService executorService = null;

   public SwingWorker() {
      Callable var1 = new Callable() {
         public Object call() throws Exception {
            SwingWorker.this.setState(SwingWorker.StateValue.STARTED);
            return SwingWorker.this.doInBackground();
         }
      };
      this.future = new FutureTask(var1) {
         protected void done() {
            SwingWorker.this.doneEDT();
            SwingWorker.this.setState(SwingWorker.StateValue.DONE);
         }
      };
      this.state = SwingWorker.StateValue.PENDING;
      this.propertyChangeSupport = new SwingPropertyChangeSupport(this, true);
      this.doProcess = null;
      this.doNotifyProgressChange = null;
   }

   protected abstract Object doInBackground() throws Exception;

   public final void run() {
      this.future.run();
   }

   protected final void publish(Object... var1) {
      synchronized(this) {
         if (this.doProcess == null) {
            this.doProcess = new AccumulativeRunnable() {
               public void run(List var1) {
                  SwingWorker.this.process(var1);
               }

               protected void submit() {
                  SwingWorker.doSubmit.add(this);
               }
            };
         }
      }

      this.doProcess.add(var1);
   }

   protected void process(List var1) {
   }

   protected void done() {
   }

   protected final void setProgress(int var1) {
      if (var1 >= 0 && var1 <= 100) {
         if (this.progress != var1) {
            int var2 = this.progress;
            this.progress = var1;
            if (this.getPropertyChangeSupport().hasListeners("progress")) {
               synchronized(this) {
                  if (this.doNotifyProgressChange == null) {
                     this.doNotifyProgressChange = new AccumulativeRunnable() {
                        public void run(List var1) {
                           SwingWorker.this.firePropertyChange("progress", var1.get(0), var1.get(var1.size() - 1));
                        }

                        protected void submit() {
                           SwingWorker.doSubmit.add(this);
                        }
                     };
                  }
               }

               this.doNotifyProgressChange.add(var2, var1);
            }
         }
      } else {
         throw new IllegalArgumentException("the value should be from 0 to 100");
      }
   }

   public final int getProgress() {
      return this.progress;
   }

   public final void execute() {
      getWorkersExecutorService().execute(this);
   }

   public final boolean cancel(boolean var1) {
      return this.future.cancel(var1);
   }

   public final boolean isCancelled() {
      return this.future.isCancelled();
   }

   public final boolean isDone() {
      return this.future.isDone();
   }

   public final Object get() throws InterruptedException, ExecutionException {
      return this.future.get();
   }

   public final Object get(long var1, TimeUnit var3) throws InterruptedException, ExecutionException, TimeoutException {
      return this.future.get(var1, var3);
   }

   public final void addPropertyChangeListener(PropertyChangeListener var1) {
      this.getPropertyChangeSupport().addPropertyChangeListener(var1);
   }

   public final void removePropertyChangeListener(PropertyChangeListener var1) {
      this.getPropertyChangeSupport().removePropertyChangeListener(var1);
   }

   public final void firePropertyChange(String var1, Object var2, Object var3) {
      this.getPropertyChangeSupport().firePropertyChange(var1, var2, var3);
   }

   public final PropertyChangeSupport getPropertyChangeSupport() {
      return this.propertyChangeSupport;
   }

   public final SwingWorker.StateValue getState() {
      return this.isDone() ? SwingWorker.StateValue.DONE : this.state;
   }

   private void setState(SwingWorker.StateValue var1) {
      SwingWorker.StateValue var2 = this.state;
      this.state = var1;
      this.firePropertyChange("state", var2, var1);
   }

   private void doneEDT() {
      Runnable var1 = new Runnable() {
         public void run() {
            SwingWorker.this.done();
         }
      };
      if (SwingUtilities.isEventDispatchThread()) {
         var1.run();
      } else {
         SwingUtilities.invokeLater(var1);
      }

   }

   private static synchronized ExecutorService getWorkersExecutorService() {
      if (executorService == null) {
         ThreadFactory var0 = new ThreadFactory() {
            final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

            public Thread newThread(Runnable var1) {
               Thread var2 = this.defaultFactory.newThread(var1);
               var2.setName("SwingWorker-" + var2.getName());
               return var2;
            }
         };
         executorService = new ThreadPoolExecutor(0, 10, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue(), var0) {
            private final ReentrantLock pauseLock = new ReentrantLock();
            private final Condition unpaused;
            private boolean isPaused;
            private final ReentrantLock executeLock;

            {
               this.unpaused = this.pauseLock.newCondition();
               this.isPaused = false;
               this.executeLock = new ReentrantLock();
            }

            public void execute(Runnable var1) {
               this.executeLock.lock();

               try {
                  this.pauseLock.lock();

                  try {
                     this.isPaused = true;
                  } finally {
                     this.pauseLock.unlock();
                  }

                  this.setCorePoolSize(10);
                  super.execute(var1);
                  this.setCorePoolSize(0);
                  this.pauseLock.lock();

                  try {
                     this.isPaused = false;
                     this.unpaused.signalAll();
                  } finally {
                     this.pauseLock.unlock();
                  }
               } finally {
                  this.executeLock.unlock();
               }

            }

            protected void afterExecute(Runnable var1, Throwable var2) {
               super.afterExecute(var1, var2);
               this.pauseLock.lock();

               try {
                  while(this.isPaused) {
                     this.unpaused.await();
                  }
               } catch (InterruptedException var7) {
               } finally {
                  this.pauseLock.unlock();
               }

            }
         };
      }

      return executorService;
   }

   private static class DoSubmitAccumulativeRunnable extends AccumulativeRunnable implements ActionListener {
      private static final int DELAY = 33;

      private DoSubmitAccumulativeRunnable() {
      }

      protected void run(List var1) {
         Iterator var2 = var1.iterator();

         while(var2.hasNext()) {
            Runnable var3 = (Runnable)var2.next();
            var3.run();
         }

      }

      protected void submit() {
         Timer var1 = new Timer(33, this);
         var1.setRepeats(false);
         var1.start();
      }

      public void actionPerformed(ActionEvent var1) {
         this.run();
      }

      // $FF: synthetic method
      DoSubmitAccumulativeRunnable(Object var1) {
         this();
      }
   }

   public static enum StateValue {
      PENDING,
      STARTED,
      DONE;
   }
}
