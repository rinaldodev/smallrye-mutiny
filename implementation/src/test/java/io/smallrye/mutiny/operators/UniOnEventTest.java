package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;

@SuppressWarnings("ConstantConditions")
public class UniOnEventTest {

    @Test
    public void testActionsOnItem() {
        AtomicInteger Item = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicInteger terminate = new AtomicInteger();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .onItem().invoke(Item::set)
                .onFailure().invoke(failure::set)
                .on().subscribed(subscription::set)
                .on().termination((r, f, c) -> terminate.set(r))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(1);
        assertThat(Item).hasValue(1);
        assertThat(failure.get()).isNull();
        assertThat(subscription.get()).isNotNull();
        assertThat(terminate).hasValue(1);
    }

    @Test
    public void testActionsOnItem2() {
        AtomicInteger Item = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicBoolean terminate = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .onItem().invoke(Item::set)
                .onFailure().invoke(failure::set)
                .on().subscribed(subscription::set)
                .on().termination(() -> terminate.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(1);
        assertThat(Item).hasValue(1);
        assertThat(failure.get()).isNull();
        assertThat(subscription.get()).isNotNull();
        assertThat(terminate).isTrue();
    }

    @Test
    public void testActionsOnFailures() {
        AtomicInteger Item = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Throwable> terminate = new AtomicReference<>();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new IOException("boom"))
                .onItem().invoke(Item::set)
                .onFailure().invoke(failure::set)
                .on().subscribed(subscription::set)
                .on().termination((r, f, c) -> terminate.set(f))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedWithFailure().assertFailure(IOException.class, "boom");
        assertThat(Item).doesNotHaveValue(1);
        assertThat(failure.get()).isInstanceOf(IOException.class);
        assertThat(subscription.get()).isNotNull();
        assertThat(terminate.get()).isInstanceOf(IOException.class);
    }

    @Test
    public void testActionsOnFailures2() {
        AtomicInteger Item = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicBoolean terminate = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new IOException("boom"))
                .onItem().invoke(Item::set)
                .onFailure().invoke(failure::set)
                .on().subscribed(subscription::set)
                .on().termination(() -> terminate.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedWithFailure().assertFailure(IOException.class, "boom");
        assertThat(Item).doesNotHaveValue(1);
        assertThat(failure.get()).isInstanceOf(IOException.class);
        assertThat(subscription.get()).isNotNull();
        assertThat(terminate).isTrue();
    }

    @Test
    public void testWhenOnItemThrowsAnException() {
        AtomicInteger Item = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicInteger ItemFromTerminate = new AtomicInteger();
        AtomicReference<Throwable> failureFromTerminate = new AtomicReference<>();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .onItem().invoke(i -> {
                    throw new IllegalStateException("boom");
                })
                .onFailure().invoke(failure::set)
                .on().subscribed(subscription::set)
                .on().termination((r, f, c) -> {
                    if (r != null) {
                        ItemFromTerminate.set(r);
                    }
                    failureFromTerminate.set(f);
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
        assertThat(Item).doesNotHaveValue(1);
        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
        assertThat(subscription.get()).isNotNull();
        assertThat(ItemFromTerminate).doesNotHaveValue(1);
        assertThat(failureFromTerminate.get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testWhenOnItemThrowsAnException2() {
        AtomicInteger Item = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicBoolean terminated = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .onItem().invoke(i -> {
                    throw new IllegalStateException("boom");
                })
                .onFailure().invoke(failure::set)
                .on().subscribed(subscription::set)
                .on().termination(() -> terminated.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
        assertThat(Item).doesNotHaveValue(1);
        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
        assertThat(subscription.get()).isNotNull();
        assertThat(terminated).isTrue();
    }

    @Test
    public void testWhenOnFailureThrowsAnException() {
        AtomicInteger Item = new AtomicInteger();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicInteger ItemFromTerminate = new AtomicInteger();
        AtomicReference<Throwable> failureFromTerminate = new AtomicReference<>();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new IOException("kaboom"))
                .onItem().invoke(Item::set)
                .onFailure().invoke(e -> {
                    throw new IllegalStateException("boom");
                })
                .on().subscribed(subscription::set)
                .on().termination((r, f, c) -> {
                    if (r != null) {
                        ItemFromTerminate.set(r);
                    }
                    failureFromTerminate.set(f);
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedWithFailure()
                .assertFailure(CompositeException.class, "boom")
                .assertFailure(CompositeException.class, "kaboom");
        assertThat(Item).doesNotHaveValue(1);
        assertThat(subscription.get()).isNotNull();
        assertThat(ItemFromTerminate).doesNotHaveValue(1);
        assertThat(failureFromTerminate.get()).isInstanceOf(CompositeException.class);
    }

    @Test
    public void testWhenOnFailureThrowsAnException2() {
        AtomicInteger Item = new AtomicInteger();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicBoolean terminated = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new IOException("kaboom"))
                .onItem().invoke(Item::set)
                .onFailure().invoke(e -> {
                    throw new IllegalStateException("boom");
                })
                .on().subscribed(subscription::set)
                .on().termination(() -> terminated.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedWithFailure()
                .assertFailure(CompositeException.class, "boom")
                .assertFailure(CompositeException.class, "kaboom");
        assertThat(Item).doesNotHaveValue(1);
        assertThat(subscription.get()).isNotNull();
        assertThat(terminated).isTrue();
    }

    @Test
    public void testWhenOnSubscriptionThrowsAnException() {
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .on().subscribed(s -> {
                    throw new IllegalStateException("boom");
                }).subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailure(IllegalStateException.class, "boom");
    }

    @Test
    public void testOnCancelWithImmediateCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .on().cancellation(() -> called.set(true))
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertNotCompleted();
        assertThat(called).isTrue();
    }

    @Test
    public void testOnTerminationWithCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicBoolean terminated = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .on().termination((r, f, c) -> terminated.set(c))
                .on().cancellation(() -> called.set(true))
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertNotCompleted();
        assertThat(called).isTrue();
        assertThat(terminated).isTrue();
    }

    @Test
    public void testOnTerminationWithCancellation2() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicBoolean terminated = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .on().termination(() -> terminated.set(true))
                .on().cancellation(() -> called.set(true))
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertNotCompleted();
        assertThat(called).isTrue();
        assertThat(terminated).isTrue();
    }

    @Test
    public void testInvokeOnFailureWithPredicate() {

        AtomicBoolean noPredicate = new AtomicBoolean();
        AtomicBoolean exactClassMatch = new AtomicBoolean();
        AtomicBoolean parentClassMatch = new AtomicBoolean();
        AtomicBoolean predicateMatch = new AtomicBoolean();
        AtomicBoolean predicateNoMatch = new AtomicBoolean();
        AtomicBoolean classNoMatch = new AtomicBoolean();

        UniAssertSubscriber<Object> subscriber = Uni.createFrom().failure(new IOException("boom"))
                .onFailure().invoke(t -> noPredicate.set(true))
                .onFailure(IOException.class).invoke(t -> exactClassMatch.set(true))
                .onFailure(Exception.class).invoke(t -> parentClassMatch.set(true))
                .onFailure(IllegalArgumentException.class).invoke(t -> classNoMatch.set(true))
                .onFailure(t -> t.getMessage().equalsIgnoreCase("boom")).invoke(t -> predicateMatch.set(true))
                .onFailure(t -> t.getMessage().equalsIgnoreCase("nope")).invoke(t -> predicateNoMatch.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedWithFailure().assertFailure(IOException.class, "boom");
        assertThat(noPredicate).isTrue();
        assertThat(exactClassMatch).isTrue();
        assertThat(parentClassMatch).isTrue();
        assertThat(predicateMatch).isTrue();
        assertThat(predicateNoMatch).isFalse();
        assertThat(classNoMatch).isFalse();

        AtomicBoolean called = new AtomicBoolean();
        subscriber = Uni.createFrom().failure(new IOException("boom"))
                .onFailure(t -> {
                    throw new NullPointerException();
                }).invoke(t -> called.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompletedWithFailure().assertFailure(CompositeException.class, "boom");
        CompositeException composite = (CompositeException) subscriber.getFailure();
        assertThat(composite.getCauses()).hasSize(2)
                .anySatisfy(t -> assertThat(t).isInstanceOf(NullPointerException.class))
                .anySatisfy(t -> assertThat(t).isInstanceOf(IOException.class));
        assertThat(called).isFalse();
    }

}
