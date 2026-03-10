package com.zor.globals.serve;

import com.zor.ZorClass;
import com.zor.ZorInstance;

public record Request(String path, String method) {

  public ZorInstance toZorInstance() {
    var requestInstance = new ZorInstance(new ZorClass("Request"));
    requestInstance.set("path", path);
    requestInstance.set("method", method);
    return requestInstance;
  }
}
