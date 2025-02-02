/*-
 * ---license-start
 * eu-digital-green-certificates / dgca-businessrule-service
 * ---
 * Copyright (C) 2022 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package eu.europa.ec.dgc.businessrule.restapi.interceptor;

import eu.europa.ec.dgc.businessrule.service.UserAgentLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class UserAgentLogInterceptor implements HandlerInterceptor {

    private final ObjectProvider<UserAgentLogService> optionalUserAgentLogService;

    /**
     * Interceptor to log UserAgents of completed requests.
     */
    @Override
    public void afterCompletion(
      HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler, Exception ex) {

        String userAgentHeader = request.getHeader(HttpHeaders.USER_AGENT);
        String userAgent = userAgentHeader == null ? "NO USER AGENT" : userAgentHeader;

        optionalUserAgentLogService.ifAvailable(service -> service.registerUserAgent(
            request.getMethod() + " " + request.getRequestURI(), userAgent));
    }
}
