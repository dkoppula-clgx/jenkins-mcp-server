# Jenkins CSRF Crumb Issue - Resolution Documentation

**Date:** February 26, 2026  
**Project:** jenkins-mcp-server  
**Issue:** 403 Forbidden - "No valid crumb was included in the request"

---

## Problem Summary

When attempting to trigger Jenkins builds programmatically using Spring Boot's `RestClient`, we consistently received a **403 Forbidden** error with the message:

```json
{
  "servlet": "Stapler",
  "message": "No valid crumb was included in the request",
  "url": "/credit-us/job/pbs/job/build-release/job/build-release/buildWithParameters",
  "status": "403"
}
```

---

## Root Cause Analysis

### Understanding Jenkins CSRF Protection

According to the [official Jenkins documentation](https://www.jenkins.io/doc/book/security/csrf-protection/):

> **Working with Scripted Clients**  
> Requests sent using the POST method are subject to CSRF protection in Jenkins and generally need to provide a crumb. This also applies to scripted clients that authenticate using username and password. **Since the crumb includes the web session ID**, clients need to do the following:
> 1. Send a request to the `/crumbIssuer/api` endpoints, requesting a crumb. **Note the Set-Cookie response header.**
> 2. For all subsequent requests, **provide the crumb and the session cookie** in addition to username and password.

### The Core Issue: Session Cookie Not Preserved

The critical problem in our implementation was that **each HTTP request created a new session**, causing the following sequence:

1. **Request 1 (Get Crumb):**
   - URL: `https://jenkins-cicd.solutions.corelogic.com/credit-us/crumbIssuer/api/json`
   - Jenkins Response: Crumb value + `Set-Cookie: JSESSIONID=node01y0k0v5bkemi2wiyz2haesnia8856.node0`
   - The session cookie was **not stored**

2. **Request 2 (Trigger Build):**
   - URL: `https://jenkins-cicd.solutions.corelogic.com/credit-us/job/pbs/job/build-release/job/build-release/buildWithParameters`
   - Jenkins Response: **New session created**: `Set-Cookie: JSESSIONID=node018jsgt3h4y745ioj47nnv5qfu8858.node0`
   - Result: **403 Forbidden** because the crumb was tied to a different session

### What We Tried (That Didn't Work)

1. **Sending crumb as HTTP header only** - ❌ Failed
2. **Sending crumb in form data only** - ❌ Failed
3. **Sending crumb in both header AND form data** - ❌ Failed
4. **Using multiple RestClient instances** - ❌ Made it worse (different sessions per client)
5. **Hardcoding crumb field name** - ❌ Failed

**None of these worked because the fundamental issue was the missing session cookie.**

---

## The Solution

### Key Insight from Postman

Postman was successfully triggering builds because it **automatically manages cookies** between requests. When we examined the working Postman request, we confirmed:
- Crumb was sent in form data
- Same JSESSIONID cookie was used for both crumb fetch and build trigger

### Implementation

We implemented **Apache HttpClient5 with cookie management** to preserve the session cookie across requests.

#### 1. Added Required Dependencies

**File:** `build.gradle`

```gradle
dependencies {
    // ... existing dependencies ...
    implementation 'org.apache.httpcomponents.client5:httpclient5:5.3.1'
    implementation 'org.apache.httpcomponents.core5:httpcore5:5.2.4'
    implementation 'org.apache.httpcomponents.core5:httpcore5-h2:5.2.4'
}
```

#### 2. Configured Cookie-Aware RestClient

**File:** `RestClientConfig.java`

```java
@Configuration
public class RestClientConfig {

    @Bean
    public CookieStore cookieStore() {
        // Singleton cookie store shared across all requests
        return new BasicCookieStore();
    }

    @Bean
    public HttpClient httpClient(CookieStore cookieStore) {
        HttpClientConnectionManager connectionManager = 
            PoolingHttpClientConnectionManagerBuilder.create().build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultCookieStore(cookieStore)  // Key: Reuse cookies!
                .build();
    }

    @Bean
    public RestClient jenkinsRestClient(HttpClient httpClient) {
        // ... auth setup ...
        
        HttpComponentsClientHttpRequestFactory requestFactory = 
            new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .baseUrl(jenkinsBaseUrl)
                .defaultHeader("Authorization", authHeader)
                .requestFactory(requestFactory)  // Use cookie-aware client
                .build();
    }
}
```

#### 3. Used Single RestClient for Both Requests

**File:** `JenkinsService.java`

```java
public void deployApplication() {
    // Step 1: Get crumb (receives JSESSIONID cookie, auto-stored in CookieStore)
    String crumbUrl = "/credit-us/crumbIssuer/api/json";
    
    JenkinsCrumb crumbResponse = jenkinsRestClient.get()
            .uri(crumbUrl)
            .retrieve()
            .body(JenkinsCrumb.class);

    String crumb = crumbResponse.getCrumb();
    String crumbRequestField = crumbResponse.getCrumbRequestField();

    // Step 2: Trigger build (JSESSIONID cookie auto-included from CookieStore)
    String buildUrl = "/credit-us/job/pbs/job/build-release/job/build-release/buildWithParameters";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("GITHUB_REPO_NAME", "credit_us-pbs-am_input_handler");
    formData.add("BRANCH_NAME", "master");
    formData.add("ARTIFACT_VERSION", "1.0.71");
    formData.add("ENVS_TO_DEPLOY", "dev-usw1-kf");
    formData.add("BUILD", "");
    formData.add("Jenkins-Crumb", crumb);  // Also in form data

    jenkinsRestClient.post()
            .uri(buildUrl)
            .header(crumbRequestField, crumb)  // Crumb in header
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .toBodilessEntity();
}
```

#### 4. Consolidated to Single Base URL

**File:** `application.yml`

```yaml
jenkins:
  base-url: https://jenkins-cicd.solutions.corelogic.com
  username: ${USERNAME}
  password: ${PASSWORD}
```

**Important:** We changed from having separate `pbs-base-url` and `credit-us-base-url` to a single `base-url` to ensure both requests use the **same RestClient instance** with the **same cookie store**.

---

## How the Solution Works

### Request Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. GET /credit-us/crumbIssuer/api/json                     │
│    ↓                                                         │
│    Jenkins returns:                                         │
│    - Crumb: "a9bbdb1d43d1d6bc2cec9067de36441ecc0ca854..." │
│    - Set-Cookie: JSESSIONID=node01y0k0v5...                │
│    ↓                                                         │
│    Apache HttpClient stores cookie in BasicCookieStore     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. POST /credit-us/job/pbs/job/build-release/...           │
│    ↓                                                         │
│    Apache HttpClient automatically includes:                │
│    - Cookie: JSESSIONID=node01y0k0v5... (from CookieStore) │
│    - Jenkins-Crumb header                                   │
│    - Jenkins-Crumb in form data                            │
│    ↓                                                         │
│    Jenkins validates:                                       │
│    ✅ Crumb matches the session → Request accepted!        │
└─────────────────────────────────────────────────────────────┘
```

### Why It Works

1. **`BasicCookieStore`** is a singleton Spring bean that persists for the application lifetime
2. **Apache HttpClient** automatically:
   - Stores cookies from `Set-Cookie` headers
   - Includes stored cookies in subsequent requests to the same domain
3. **Single RestClient** ensures all Jenkins requests share the same cookie store
4. **Jenkins validates** that the crumb's session ID matches the request's JSESSIONID cookie

---

## Key Learnings

### ✅ Critical Success Factors

1. **Session Cookie Persistence:** The crumb is tied to a specific JSESSIONID - you must reuse the same session
2. **Single RestClient Instance:** Using multiple RestClient beans creates separate sessions
3. **Apache HttpClient5:** Spring's default `SimpleClientHttpRequestFactory` doesn't manage cookies
4. **Singleton CookieStore:** Must be a Spring bean to persist across requests

### ❌ Common Mistakes to Avoid

1. **Don't create new RestClient per request** - Each instance gets its own session
2. **Don't use different base URLs** - Can lead to using different RestClient instances
3. **Don't rely on default Spring HTTP client** - It doesn't handle cookies automatically
4. **Don't forget all three HttpClient5 dependencies** - Missing `httpcore5` or `httpcore5-h2` causes ClassNotFoundException

---

## Testing & Validation

### Logs Before Fix (403 Error)

```
Headers: [Authorization:"Basic ...", Content-Type:"application/x-www-form-urlencoded", 
          Jenkins-Crumb:"570c81b59b21ce9b22831ffb172886376f08cafc2378ed82f07808bef1e7b4c4"]
Response: Status code: 403 FORBIDDEN
Set-Cookie: JSESSIONID.87b0fc77=node018jsgt3h4y745ioj47nnv5qfu8858.node0  // Different session!
```

### Logs After Fix (Success)

```
Request successful - Build triggered
Status: 200 OK or 201 Created
```

---

## References

- [Jenkins CSRF Protection Documentation](https://www.jenkins.io/doc/book/security/csrf-protection/)
- [Apache HttpClient5 Documentation](https://hc.apache.org/httpcomponents-client-5.3.x/)
- [Spring RestClient Documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)

---

## Summary

**Problem:** Jenkins CSRF crumb validation failed because each HTTP request created a new session.

**Solution:** Implemented Apache HttpClient5 with `BasicCookieStore` to automatically preserve and reuse the JSESSIONID cookie across requests, ensuring the crumb remains valid.

**Result:** ✅ Successfully triggering Jenkins builds programmatically with proper CSRF protection.

