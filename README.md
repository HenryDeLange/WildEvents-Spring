# WildEvents-Spring
![App Version](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2FHenryDeLange%2FWildEvents-Spring%2Fmain%2Fpom.xml&query=%2F*%5Blocal-name()%3D'project'%5D%2F*%5Blocal-name()%3D'version'%5D&label=version)
![GitHub License](https://img.shields.io/github/license/HenryDeLange/WildEvents-Spring)

The Spring (Java) backend for WildEvents.

## Development

### Build
![Top Language](https://img.shields.io/github/languages/top/HenryDeLange/WildEvents-Spring)
![Maven Build](https://img.shields.io/github/actions/workflow/status/HenryDeLange/WildEvents-Spring/spring-source-build.yml?label=maven%20build)
![Docker Deploy](https://img.shields.io/github/actions/workflow/status/HenryDeLange/WildEvents-Spring/spring-docker-build.yml?label=docker%20deploy)

This project written in `Java 21` using `Spring Boot`, and `Maven` to build.

### Setup

#### Encryption Keys
Use `openssl` to create the necessary keys:
(In Windows you can use WSL to execute the commands.)

```sh
openssl genrsa -out ./keys/private_key.pem 2048
openssl rsa -in ./keys/private_key.pem -outform PEM -pubout -out ./keys/public_key.pem
openssl pkcs8 -topk8 -inform PEM -in ./keys/private_key.pem -outform PEM -nocrypt -out ./keys/private_key_pkcs8.pem
```

#### Environment Variables
Create an `.env` file in the root folder containing the following information (use your own values):

```properties
#### SECURITY ####
JWT_AUDIENCE=development
# Set to the frontend's URL
CORS=*
# Use value from private_key_pkcs8.pem (only the base64 key, not the header/footer)
PRIVATE_KEY=...
# Use value from public_key.pem (only the base64 key, not the header/footer)
PUBLIC_KEY=...

#### COSMOS DB ####
AZURE_COSMOS_KEY=...
AZURE_COSMOS_ENDPOINT=...
AZURE_COSMOS_DATABASE=...
```

In development you can also add `server.error.include-stacktrace=always` to the `.env` file to help troubleshoot exceptions.

#### Properties
See the [application.yml](./src/main/resources/application.yml) file.

These properties can be overwritten using environment variables.
