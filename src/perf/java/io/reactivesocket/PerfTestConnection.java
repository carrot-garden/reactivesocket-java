/**
 * Copyright 2015 Netflix, Inc.
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
package io.reactivesocket;

import static rx.RxReactiveStreams.*;

import org.reactivestreams.Publisher;

import io.reactivesocket.DuplexConnection;
import io.reactivesocket.Frame;
import rx.Observable;
import rx.subjects.PublishSubject;

public class PerfTestConnection implements DuplexConnection {

	public final PublishSubject<Frame> toInput = PublishSubject.create();
	private PublishSubject<Frame> writeSubject = PublishSubject.create();
	public final Observable<Frame> writes = writeSubject;

	@Override
	public Publisher<Void> addOutput(Publisher<Frame> o) {
		return toPublisher(toObservable(o).flatMap(m -> {
			writeSubject.onNext(m);
			return Observable.<Void> empty();
		}));
	}

	@Override
	public Publisher<Frame> getInput() {
		return toPublisher(toInput);
	}

	public void connectToServerConnection(PerfTestConnection serverConnection) {
		writes.subscribe(serverConnection.toInput);
		serverConnection.writes.subscribe(toInput);

	}
}