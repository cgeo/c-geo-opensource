package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import android.test.AndroidTestCase;

public class RxUtilsTest extends AndroidTestCase {

    public static void testTakeUntil() {
        final Observable<Integer> observable = Observable.range(1, 10).lift(RxUtils.operatorTakeUntil(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(final Integer value) {
                return value > 6;
            }
        }));
        assertThat(observable.toList().toBlocking().single().toArray()).isEqualTo(new int[]{1, 2, 3, 4, 5, 6, 7});
    }

    public static void testRememberLast() {
        final PublishSubject<String> rawObservable = PublishSubject.create();
        final Observable<String> observable = RxUtils.rememberLast(rawObservable, "initial");

        // Check that the initial value is present, and is kept there
        assertThat(observable.toBlocking().first()).isEqualTo("initial");
        assertThat(observable.toBlocking().first()).isEqualTo("initial");

        // Check that if the observable is not subscribed, changes are not propagated (similar to not keeping the
        // inner subscription active).
        rawObservable.onNext("without subscribers");
        assertThat(observable.toBlocking().first()).isEqualTo("initial");

        // Check that new values are propagated and cached
        final Subscription subscription = observable.subscribe();
        rawObservable.onNext("first");
        assertThat(observable.toBlocking().first()).isEqualTo("first");
        subscription.unsubscribe();
        assertThat(observable.toBlocking().first()).isEqualTo("first");
    }

}
