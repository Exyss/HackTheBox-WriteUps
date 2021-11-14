package okhttp3.internal.huc;

import java.io.IOException;
import okhttp3.internal.http.UnrepeatableRequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Pipe;

/* access modifiers changed from: package-private */
public final class StreamedRequestBody extends OutputStreamRequestBody implements UnrepeatableRequestBody {
    private final Pipe pipe;

    StreamedRequestBody(long j) {
        Pipe pipe2 = new Pipe(8192);
        this.pipe = pipe2;
        initOutputStream(Okio.buffer(pipe2.sink()), j);
    }

    @Override // okhttp3.RequestBody
    public void writeTo(BufferedSink bufferedSink) throws IOException {
        Buffer buffer = new Buffer();
        while (this.pipe.source().read(buffer, 8192) != -1) {
            bufferedSink.write(buffer, buffer.size());
        }
    }
}
