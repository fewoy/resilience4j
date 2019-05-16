/*
 * Copyright 2019 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ResilienceBaseSubscriber;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Flowable;
import io.reactivex.internal.subscriptions.EmptySubscription;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.time.Duration;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

class FlowableRateLimiter<T> extends Flowable<T> {

    private final RateLimiter rateLimiter;
    private final Publisher<T> upstream;

    FlowableRateLimiter(Publisher<T> upstream, RateLimiter rateLimiter) {
        this.rateLimiter = requireNonNull(rateLimiter);
        this.upstream = Objects.requireNonNull(upstream, "source is null");
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> downstream) {
        if(rateLimiter.acquirePermission(Duration.ZERO)){
            upstream.subscribe(new RateLimiterSubscriber(downstream));
        }else{
            downstream.onSubscribe(EmptySubscription.INSTANCE);
            downstream.onError(new RequestNotPermitted(rateLimiter));
        }
    }

    class RateLimiterSubscriber extends ResilienceBaseSubscriber<T> {

        RateLimiterSubscriber(Subscriber<? super T> downstreamSubscriber) {
            super(downstreamSubscriber);
        }

        @Override
        public void hookOnError(Throwable t) {
            downstreamSubscriber.onError(t);
        }

        @Override
        public void hookOnComplete() {
            downstreamSubscriber.onComplete();
        }

        @Override
        public void hookOnCancel() {
            // Release permission in RateLimiter?
        }

        @Override
        public void hookOnNext(T value) {
            downstreamSubscriber.onNext(value);
        }
    }

}