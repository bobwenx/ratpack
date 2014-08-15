/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.http.client.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.http.HttpUrlSpec;
import ratpack.http.MutableHeaders;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.HttpUrlSpecBacking;
import ratpack.util.internal.ByteBufWriteThroughOutputStream;

import java.io.OutputStream;
import java.net.URI;

public class RequestSpecBacking {

  private final MutableHeaders headers;
  private final ByteBufAllocator byteBufAllocator;
  private final HttpUrlSpecBacking httpUrlSpec;

  private ByteBuf bodyByteBuf;

  private String method = "GET";

  public RequestSpecBacking(MutableHeaders headers, ByteBufAllocator byteBufAllocator) {
    this.headers = headers;
    this.byteBufAllocator = byteBufAllocator;
    this.httpUrlSpec = new HttpUrlSpecBacking();
    this.bodyByteBuf = byteBufAllocator.buffer(0, 0);
  }

  public String getMethod() {
    return method;
  }

  @Nullable
  public ByteBuf getBody() {
    return bodyByteBuf;
  }

  public URI getUrl() {
    return httpUrlSpec.getURL();
  }

  public RequestSpec asSpec() {
    return new Spec();
  }

  private class Spec implements RequestSpec {

    private BodyImpl body = new BodyImpl();

    @Override
    public MutableHeaders getHeaders() {
      return headers;
    }

    @Override
    public RequestSpec headers(Action<? super MutableHeaders> action) throws Exception {
      action.execute(getHeaders());
      return this;
    }

    @Override
    public RequestSpec method(String method) {
      RequestSpecBacking.this.method = method.toUpperCase();
      return this;
    }

    @Override
    public HttpUrlSpec getUrl() {
      return RequestSpecBacking.this.httpUrlSpec;
    }

    @Override
    public RequestSpec url(Action<? super HttpUrlSpec> action) throws Exception {
      action.execute(getUrl());
      return this;
    }

    private void setBodyByteBuf(ByteBuf byteBuf) {
      if (bodyByteBuf != null) {
        bodyByteBuf.release();
      }
      bodyByteBuf = byteBuf;
    }


    private class BodyImpl implements Body {
      @Override
      public Body type(String contentType) {
        getHeaders().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
        return this;
      }

      @Override
      public Body stream(Action<? super OutputStream> action) throws Exception {
        ByteBuf byteBuf = byteBufAllocator.buffer();
        try (OutputStream outputStream = new ByteBufWriteThroughOutputStream(byteBuf)) {
          action.execute(outputStream);
        } catch (Throwable t) {
          byteBuf.release();
          throw t;
        }

        setBodyByteBuf(byteBuf);
        return this;
      }

      @Override
      public Body buffer(ByteBuf byteBuf) {
        setBodyByteBuf(byteBuf.retain());
        return this;
      }

      @Override
      public Body bytes(byte[] bytes) {
        setBodyByteBuf(Unpooled.wrappedBuffer(bytes));
        return this;
      }
    }

    @Override
    public Body getBody() {
      return body;
    }

    @Override
    public RequestSpec body(Action<? super Body> action) throws Exception {
      action.execute(getBody());
      return this;
    }
  }
}