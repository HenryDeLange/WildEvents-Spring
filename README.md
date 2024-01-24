# WildEvents-Spring
The Spring (Java) backend for WildEvents.

## Development

TODO: Add badges

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
PRIVATE_KEY=
# Use value from public_key.pem (only the base64 key, not the header/footer)
PUBLIC_KEY=

#### COSMOS DB ####
AZURE_COSMOS_KEY=
AZURE_COSMOS_ENDPOINT=
AZURE_COSMOS_DATABASE=
```

In development you can also add `server.error.include-stacktrace=always` to the `.env` file to help troubleshoot exceptions.
