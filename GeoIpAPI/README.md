# GeoIP REST API

A lightweight REST service that downloads GeoIP databases like GeoLite2 or IPLocate
and returns geographic information (country, city, ASN) for a given IP address.


## Quick Start

```shell
docker run -p 8080:8080 ghcr.io/codinux-gmbh/geoip:0.5.0
```

The API will be reachable at `http://localhost:8080/geoip/api/v1/`.


## Running in Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: geoip
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: geoip
  template:
    metadata:
      labels:
        app.kubernetes.io/name: geoip
    spec:
      containers:
        - name: geoip
          image: ghcr.io/codinux-gmbh/geoip:0.5.0
          ports:
            - containerPort: 8080
```

Add the `env:` block shown in later sections to enable optional features. 

For a full Kubernetes example see [docs/Kubernetes/Deployment.yaml](../docs/Kubernetes/Deployment.yaml).


## Persistent Database Storage

The application downloads GeoIP database files to `/var/lib/geoip`.
Mount this directory to avoid re‑downloads at each start-up and to stay within MaxMind’s rate limits.

```shell
docker run \
  --user "$(id -u):$(id -g)" \
  -v $(pwd)/data:/var/lib/geoip \
  -p 8080:8080 \
  ghcr.io/codinux-gmbh/geoip
```

> Mount volume read‑only if you pre‑populate the database yourself.


## GeoIP databases path and download configuration

### GeoLite2

To automatically download GeoLite2 databases [create a MaxMind account and license key](https://support.maxmind.com/hc/en-us/articles/4407111582235-Generate-a-License-Key)
and set environment variables `GEOIP_GEOLITE2_ACCOUNT_ID` and `GEOIP_GEOLITE2_LICENSE_KEY` with it:

```shell
docker run \
  -e GEOIP_GEOLITE2_ACCOUNT_ID=<your AccountID> \
  -e GEOIP_GEOLITE2_LICENSE_KEY=<your License Key> \
  ghcr.io/codinux-gmbh/geoip
```

If you like to turn off downloading GeoLite2 databases and provide them yourself, set `GEOIP_GEOLITE2_DOWNLOAD` to `false`.

If you do not want to use some or all GeoLite2 databases, set the path to it to an empty string with these environment variables:
- ASN: `GEOIP_GEOLITE2_ASN`
- Country: `GEOIP_GEOLITE2_COUNTRY`
- City: `GEOIP_GEOLITE2_CITY`

### IPLocate

IPLocate.io country and ASN database are automatically downloaded per default. (IPLocate does not have a city database.)

If you do not want them to be downloaded, set `GEOIP_IPLOCATE_DOWNLOAD` to `false`.

If you do not want to use some or all IPLocate databases, set the path to it to an empty string with these environment variables:
- ASN: `GEOIP_IPLOCATE_ASN`
- Country: `GEOIP_IPLOCATE_COUNTRY`


## Further configuration

All configs are controlled via environment variables. Below is a compact reference table.

| Variable                   | Default | Description                                                                   |
|----------------------------|---------|-------------------------------------------------------------------------------|
| `GEOIP_SWAGGER_UI_ENABLE`  | `false` | Exposes the OpenAPI UI at `/geoip/swagger‑ui`.                                |
| `GEOIP_ACCESS_LOG_ENABLED` | `false` | Writes calls to API endpoints to console (client IP, path, HTTP status, ...). |
| `GEOIP_LOKI_ENABLE`        | `false` | To send logs to Loki central log storage.                                     |
| `GEOIP_LOKI_BASEURL`       | –       | Base URL of the Loki endpoint (e.g., `http://loki.monitoring:3100`).          |
| `GEOIP_CORS_ENABLED`       | `false` | Enables Cross‑Origin Resource Sharing.                                        |
| `GEOIP_CORS_ORIGINS`       | `*`     | Comma‑separated list of allowed origins for CORS (default `*`).               |


### Enabling Swagger UI

To see all REST endpoints and their responses, enable that Swagger UI gets exposed at `/geoip/swagger-ui`:

```shell
docker run -e GEOIP_SWAGGER_UI_ENABLE=true -p 8080:8080 ghcr.io/codinux-gmbh/geoip
```

```yaml
env:
- name: GEOIP_SWAGGER_UI_ENABLE
  value: "true"
```


### Remote Logging to Loki

Enable pushing logs to Loki by setting `GEOIP_LOKI_ENABLE` to `true` and set Loki's base URL with `GEOIP_LOKI_BASEURL`:

```shell
docker run \
  -e GEOIP_LOKI_ENABLE=true \
  -e GEOIP_LOKI_BASEURL=http://localhost:3100 \
  -p 8080:8080 ghcr.io/codinux-gmbh/geoip
```

```yaml
env:
  - name: GEOIP_LOKI_ENABLE
    value: "true"
  - name: GEOIP_LOKI_BASEURL
    value: "http://loki.monitoring:3100"
```


## API Endpoints (selection)

| Method | Path                                      | Description                                                                                        | Example Response                                                                              |
|--------|-------------------------------------------|----------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| GET    | `/geoip/api/v1/country/{ip}`              | Returns country information for the supplied IPv4/IPv6 address.                                    | `{ "ip":"8.8.8.8", "country":"US", "city":"Mountain View", "asn":15169, "org":"Google LLC" }` |
| GET    | `/geoip/api/v1/country/me`                | Returns country information for caller's IP address. (All endpoints below have a `/me` variant.)   | `{ "ip":"8.8.8.8", "country":"US", "city":"Mountain View", "asn":15169, "org":"Google LLC" }` |
| GET    | `/geoip/api/v1/city/{ip}`                 | Returns city information for the supplied IPv4/IPv6 address if a city GeoIP database is available. | `{ "ip":"8.8.8.8", "country":"US", "city":"Mountain View", "asn":15169, "org":"Google LLC" }` |
| GET    | `/geoip/api/v1/asn/{ip}`                  | Returns autonomous system information for the supplied IPv4/IPv6 address.                          | `{ "ip":"8.8.8.8", "country":"US", "city":"Mountain View", "asn":15169, "org":"Google LLC" }` |
| GET    | `/geoip/api/v1/best/{ip}`                 | Returns best available information of above's category for the supplied IPv4/IPv6 address.         | `{ "ip":"8.8.8.8", "country":"US", "city":"Mountain View", "asn":15169, "org":"Google LLC" }` |
| GET    | `/geoip/api/v1/providers/{provider}/{ip}` | Returns all data of a specific provider for the supplied IPv4/IPv6 address.                        | `{ "ip":"8.8.8.8", "country":"US", "city":"Mountain View", "asn":15169, "org":"Google LLC" }` |
| GET    | `/geoip/api/v1/all/{ip}`                  | Returns all available data of all providers for the supplied IPv4/IPv6 address.                    | `{ "ip":"8.8.8.8", "country":"US", "city":"Mountain View", "asn":15169, "org":"Google LLC" }` |


### Error Codes

- **400 Bad Request** – malformed IP address.
- **404 Not Found** – IP not present in the database (or endpoint does not exist).
- **500 Internal Server Error** – unexpected failure (e.g., DB download error).


## Metrics

**Prometheus metrics** are exposed at `/metrics` (no `/geoip` prefix!). 
Like all other endpoints at port `8080`.

## Health Checks (Kubernetes)

- **Startup**: `/health/startup`. Waits until every database has been attempted to download at least once.
- **Readiness**: `/health/ready`. No implemented logic, always returns 200.
- **Liveness**: `/health/live`. No implemented logic, always returns 200.
