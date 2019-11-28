package io.smallrye.reactive.operators.multi.multicast;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.infrastructure.Infrastructure;
import io.smallrye.reactive.operators.AbstractMulti;
import io.smallrye.reactive.subscription.Cancellable;

/**
 * A {@link Multi} stays connected to the source as long as there is at least one subscription.
 *
 * @param <T> the type of item
 */
public class MultiReferenceCount<T> extends AbstractMulti<T> implements Multi<T> {

    private final ConnectableMulti<T> upstream;
    private final int numberOfSubscribers;
    private final Duration duration;
    private final ScheduledExecutorService executor;

    private ConnectableMultiConnection connection;

    public MultiReferenceCount(ConnectableMulti<T> upstream) {
        this(upstream, 1, null);
    }

    public MultiReferenceCount(ConnectableMulti<T> upstream, int numberOfSubscribers, Duration duration) {
        this.upstream = upstream;
        this.numberOfSubscribers = numberOfSubscribers;
        this.duration = duration;
        this.executor = Infrastructure.getDefaultWorkerPool();
    }

    @Override
    protected Publisher<T> publisher() {
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        ConnectableMultiConnection conn;
        boolean connect;
        synchronized (this) {
            conn = connection;
            if (conn == null) {
                conn = new ConnectableMultiConnection(this);
                connection = conn;
            }

            conn.cancelTimerIf0();
            connect = conn.shouldConnectAfterIncrement(numberOfSubscribers);
        }

        upstream.subscribe(new MultiReferenceCountSubscriber<>(s, this, conn));

        if (connect) {
            upstream.connect(conn);
        }
    }

    void cancel(ConnectableMultiConnection connection) {
        synchronized (this) {
            if (this.connection == null || this.connection != connection) {
                return;
            }
            long count = connection.decrement();
            if (count != 0L || !connection.isConnected()) {
                return;
            }

            if (duration == null || duration.toMillis() == 0L) {
                timeout(connection);
                return;
            }
        }

        ScheduledFuture<?> future = executor.schedule(connection, duration.toMillis(), TimeUnit.MILLISECONDS);
        connection.setTimer(() -> future.cancel(true));
    }

    void terminated(ConnectableMultiConnection connection) {
        synchronized (this) {
            if (this.connection != null && this.connection == connection) {
                this.connection = null;
                connection.cancel();
            }
            if (connection.decrementAndReached0()) {
                if (upstream instanceof Cancellable) {
                    ((Cancellable) upstream).cancel();
                }
            }
        }
    }

    void timeout(ConnectableMultiConnection connection) {
        synchronized (this) {
            if (connection.getSubscriberCount() == 0 && connection == this.connection) {
                this.connection = null;
                connection.cancel();
                if (upstream instanceof Cancellable) {
                    ((Cancellable) upstream).cancel();
                }
            }
        }
    }

}