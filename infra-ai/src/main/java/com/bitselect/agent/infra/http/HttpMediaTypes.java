

package com.bitselect.agent.infra.http;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import okhttp3.MediaType;

/**
 * HTTP 媒体类型常量类
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpMediaTypes {

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
}